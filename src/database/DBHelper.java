package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import main.Settings;

import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteConfig.JournalMode;

import tool.Utils;
import twitter4j.Status;
import twitter4j.User;

public class DBHelper {
	private static DBHelper mInstance = null;
	
	public static synchronized DBHelper getSingleton() {
		return (mInstance != null) ? mInstance : (mInstance = new DBHelper());
	}
	
	private final String DBNAME = "TwitterData.sqlite";
	private final String DBPATH = "jdbc:sqlite:" + Settings.PATH_DATA + DBNAME;
	private final String DRIVER = "org.sqlite.JDBC";
	
	private Connection conn = null;						// Connection for DB write
	private ConnectionPool connectionPool = null;		// Connection pool for DB read
	
	public DBHelper() {
		try {
			// Register the Driver to the jbdc.driver java property
			Class.forName(DRIVER);
			
			// Configuration
			SQLiteConfig config = new SQLiteConfig();
			config.setJournalMode(JournalMode.WAL);
			config.enforceForeignKeys(true);
			// If database does not exist, then it will be created automatically.
			conn = DriverManager.getConnection(DBPATH, config.toProperties());
			conn.setAutoCommit(false);
			
			// Create connection pool
			SQLiteConfig poolConfig = new SQLiteConfig();
			poolConfig.setReadOnly(true);
			config.enforceForeignKeys(true);
			connectionPool = new ConnectionPool(DBPATH, poolConfig.toProperties());
			connectionPool.setMaxPoolSize(1000);
			
			// Create DB tables
			createDBTables();
		} catch (ClassNotFoundException e) {
		} catch (SQLException e) {
		}
	}
	
	public synchronized void closeDBConnections() {
		try {
			if (conn != null)
				conn.close();
			if (connectionPool != null)
				connectionPool.closeAll();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public synchronized boolean execQuery(String sql) {
		try {
			Statement stmt = conn.createStatement();
			stmt.executeUpdate(sql);
			conn.commit();
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public synchronized boolean execQuery(ArrayList<String> sqls) {
		try {
			Statement stmt = conn.createStatement();
			for (String sql : sqls)
				stmt.executeUpdate(sql);
			conn.commit();
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public synchronized boolean batchQueries(String sql, ArrayList<String[]> values) {
		try {
			PreparedStatement prep = conn.prepareStatement(sql);		// SQL query to be compiled should involve question('?') marks.
			for (String[] value : values) {
				for (int i = 0; i < value.length; i++)
					prep.setString(i + 1, value[i]);
				prep.addBatch();
			}
			prep.executeBatch();
			conn.commit();
			prep.close();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public boolean createDBTables() {
		ArrayList<String> sqls = new ArrayList<String>();
		sqls.add("CREATE TABLE IF NOT EXISTS user ("
				+ "id INTEGER PRIMARY KEY, isProtected INTEGER, isVerified INTEGER, lang TEXT, followingsCount INTEGER, followersCount INTEGER, tweetsCount INTEGER, latestTweet INTEGER, date INTEGER)");
		sqls.add("CREATE TABLE IF NOT EXISTS follow ("
				+ "source INTEGER, target INTEGER, "
				+ "FOREIGN KEY(source) REFERENCES user(id) ON DELETE CASCADE ON UPDATE CASCADE, PRIMARY KEY(source, target))");
		sqls.add("CREATE TABLE IF NOT EXISTS tweet ("
				+ "id INTEGER PRIMARY KEY, author INTEGER, text TEXT, date INTEGER)");
		sqls.add("CREATE TABLE IF NOT EXISTS retweet ("
				+ "user INTEGER, tweet INTEGER, "
				+ "FOREIGN KEY(user) REFERENCES user(id) ON DELETE CASCADE ON UPDATE CASCADE, PRIMARY KEY(user, tweet))");
		sqls.add("CREATE TABLE IF NOT EXISTS share ("
				+ "user INTEGER, tweet INTEGER, "
				+ "FOREIGN KEY(user) REFERENCES user(id) ON DELETE CASCADE ON UPDATE CASCADE, PRIMARY KEY(user, tweet))");
		sqls.add("CREATE TABLE IF NOT EXISTS favorite ("
				+ "user INTEGER, tweet INTEGER, "
				+ "FOREIGN KEY(user) REFERENCES user(id) ON DELETE CASCADE ON UPDATE CASCADE, PRIMARY KEY(user, tweet))");
		sqls.add("CREATE TABLE IF NOT EXISTS mention ("
				+ "source INTEGER, target INTEGER, date INTEGER, "
				+ "FOREIGN KEY(source) REFERENCES user(id) ON DELETE CASCADE ON UPDATE CASCADE, PRIMARY KEY(source, target, date))");
		return execQuery(sqls);
	}
	
	public boolean insertUser(User user) {
		if (user == null) return false;
		String sql = new String("INSERT OR IGNORE INTO user ("
				+ "id, isProtected, isVerified, lang, followingsCount, followersCount, tweetsCount, latestTweet, date) VALUES ("
				+ user.getId() + ", " + (user.isProtected() ? 1 : 0) + ", " + (user.isVerified() ? 1 : 0) + ", '"
				+ user.getLang() + "', " + user.getFriendsCount() + ", " + user.getFollowersCount() + ", " + user.getStatusesCount() + ", "
				+ (user.getStatus() == null ? -1L : user.getStatus().getId()) + ", " + user.getCreatedAt().getTime() + ")");
		return execQuery(sql);
	}
	
	public boolean insertUsers(ArrayList<User> users) {
		if (users == null) return false;
		String sql = new String("INSERT OR IGNORE INTO user ("
				+ "id, isProtected, isVerified, lang, followingsCount, followersCount, tweetsCount, latestTweet, date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
		ArrayList<String[]> values = new ArrayList<String[]>();
		for (User user : users) {
			String[] value = new String[9];
			value[0] = String.valueOf(user.getId());
			value[1] = String.valueOf(user.isProtected() ? 1 : 0);
			value[2] = String.valueOf(user.isVerified() ? 1 : 0);
			value[3] = user.getLang();
			value[4] = String.valueOf(user.getFriendsCount());
			value[5] = String.valueOf(user.getFollowersCount());
			value[6] = String.valueOf(user.getStatusesCount());
			value[7] = String.valueOf(user.getStatus() == null ? -1L : user.getStatus().getId());
			value[8] = String.valueOf(user.getCreatedAt().getTime());
			values.add(value);
		}
		return batchQueries(sql, values);
	}
	
	public ArrayList<Long> getFollowingList(long userID) {
		ArrayList<Long> followingUserIDs = new ArrayList<Long>();
		String sql = new String("SELECT target FROM follow WHERE source = " + userID);
		Connection conn = connectionPool.getConnection();
		try {
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next())
				followingUserIDs.add(rs.getLong(1));
			rs.close();
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		connectionPool.freeConnection(conn);
		return followingUserIDs;
	}
	
	public boolean insertFollowingList(long userID, ArrayList<Long> followingList) {
		String sql = new String("INSERT OR IGNORE INTO follow (source, target) VALUES (?, ?)");
		ArrayList<String[]> values = new ArrayList<String[]>();
		for (long followingUserID : followingList) {
			String[] value = new String[2];
			value[0] = String.valueOf(userID);
			value[1] = String.valueOf(followingUserID);
			values.add(value);
		}
		return batchQueries(sql, values);
	}
	
	public boolean insertTweets(ArrayList<Status> tweets) {
		String sql = new String("INSERT OR IGNORE INTO tweet (id, author, text, date) VALUES (?, ?, ?, ?)");
		ArrayList<String[]> values = new ArrayList<String[]>();
		for (Status tweet : tweets) {
			// Exclude mentioning tweets
			if (Utils.containsMention(tweet))
				continue;
			
			String[] value = new String[4];
			Status target = tweet;
			if (tweet.isRetweet())
				target = tweet.getRetweetedStatus();
			value[0] = String.valueOf(target.getId());
			value[1] = String.valueOf(target.getUser().getId());
			value[2] = target.getText();
			value[3] = String.valueOf(target.getCreatedAt().getTime());
			values.add(value);
		}
		return batchQueries(sql, values);
	}
	
	public boolean insertRetweetHistory(long userID, ArrayList<Status> retweets) {
		String sql = new String("INSERT OR IGNORE INTO retweet (user, tweet) VALUES (?, ?)");
		ArrayList<String[]> values = new ArrayList<String[]>();
		for (Status retweet : retweets) {
			String[] value = new String[2];
			value[0] = String.valueOf(userID);
			value[1] = String.valueOf(retweet.getId());
			values.add(value);
		}
		return batchQueries(sql, values);
	}
	
	public boolean insertShareHistory(long userID, ArrayList<Long> sharedTweetIDs) {
		String sql = new String("INSERT OR IGNORE INTO share (user, tweet) VALUES (?, ?)");
		ArrayList<String[]> values = new ArrayList<String[]>();
		for (long sharedTweet : sharedTweetIDs) {
			String[] value = new String[2];
			value[0] = String.valueOf(userID);
			value[1] = String.valueOf(sharedTweet);
			values.add(value);
		}
		return batchQueries(sql, values);
	}
	
	public boolean insertFavoriteHistory(long userID, ArrayList<Status> favorites) {
		insertTweets(favorites);
		
		String sql = new String("INSERT OR IGNORE INTO favorite (user, tweet) VALUES (?, ?)");
		ArrayList<String[]> values = new ArrayList<String[]>();
		for (Status favorite : favorites) {
			String[] value = new String[2];
			value[0] = String.valueOf(userID);
			value[1] = String.valueOf(favorite.getId());
			values.add(value);
		}
		return batchQueries(sql, values);
	}
	
	public boolean insertMentionHistory(long userID, HashMap<Long, ArrayList<Date>> mentionHistory) {
		String sql = new String("INSERT OR IGNORE INTO mention (source, target, date) VALUES (?, ?, ?)");
		ArrayList<String[]> values = new ArrayList<String[]>();
		for (HashMap.Entry<Long, ArrayList<Date>> entry : mentionHistory.entrySet()) {
			long targetUserID = entry.getKey();
			ArrayList<Date> history = entry.getValue();
			for (Date date : history) {
				String[] value = new String[3];
				value[0] = String.valueOf(userID);
				value[1] = String.valueOf(targetUserID);
				value[2] = String.valueOf(date.getTime());
				values.add(value);
			}
		}
		return batchQueries(sql, values);
	}
	
	public ArrayList<Long> getFriendship(long userID) {
		ArrayList<Long> friendshipList = new ArrayList<Long>();
		String sql = new String("SELECT follow.source FROM follow WHERE follow.target = " + userID
				+ " INTERSECT SELECT follow.target FROM follow WHERE follow.source = " + userID);
		
		Connection conn = connectionPool.getConnection();
		try {
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next())
				friendshipList.add(rs.getLong(1));
			rs.close();
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
		connectionPool.freeConnection(conn);
		
		return friendshipList;
	}
	
	public boolean hasRecord(long userID) {
		String sql = new String("SELECT * FROM user WHERE id = " + userID);
		boolean result = false;
		Connection conn = connectionPool.getConnection();
		try {
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			if (rs.next())
				result = true;
			rs.close();
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		connectionPool.freeConnection(conn);
		return result;
	}
}
