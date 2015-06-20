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
import main.TwitterUser;

import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteConfig.Pragma;

import crawling.Engine;
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
			config.setPragma(Pragma.JOURNAL_MODE, "wal");
			
			// If database does not exist, then it will be created automatically.
			conn = DriverManager.getConnection(DBPATH, config.toProperties());
			conn.setAutoCommit(false);
			
			// Create DB tables
			createDBTables();
			
			// Create connection pool
			SQLiteConfig poolConfig = new SQLiteConfig();
			poolConfig.setReadOnly(true);
			connectionPool = new ConnectionPool(DBPATH, poolConfig.toProperties());
			connectionPool.setMaxPoolSize(1000);
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
				+ "id INTEGER PRIMARY KEY, isVisited INTEGER, isProtected INTEGER, isVerified INTEGER, lang TEXT, followingsCount INTEGER, followersCount INTEGER, tweetsCount INTEGER, latestTweet INTEGER, date INTEGER)");
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
				+ "user INTEGER, tweet INTEGER, FOREIGN KEY(user) REFERENCES user(id) ON DELETE CASCADE ON UPDATE CASCADE, PRIMARY KEY(user, tweet))");
		sqls.add("CREATE TABLE IF NOT EXISTS mention ("
				+ "source INTEGER, target INTEGER, date INTEGER, "
				+ "FOREIGN KEY(source) REFERENCES user(id) ON DELETE CASCADE ON UPDATE CASCADE, PRIMARY KEY(source, target, date))");
		return execQuery(sqls);
	}
	
	public HashMap<Long, TwitterUser> loadUserMap() {
		HashMap<Long, TwitterUser> userMap = new HashMap<Long, TwitterUser>();
		String sqlSelectUsers = new String("SELECT * FROM user");
		String sqlSelectFollowings = new String("SELECT * FROM follow");
		
		Connection conn = connectionPool.getConnection();
		try {
			Statement stmt = conn.createStatement();
			
			// User information
			ResultSet rs = stmt.executeQuery(sqlSelectUsers);
			while (rs.next()) {
				TwitterUser user = new TwitterUser(rs.getLong("id"), (rs.getInt("isVisited") > 0 ? true : false), (rs.getInt("isProtected") > 0 ? true : false), (rs.getInt("isVerified") > 0 ? true : false), rs.getString("lang"), rs.getInt("followingsCount"), rs.getInt("followersCount"), rs.getInt("tweetsCount"), rs.getLong("latestTweet"), rs.getLong("date"));
				userMap.put(rs.getLong("id"), user);
			}
			rs.close();
			
			// Following relationship
			rs = stmt.executeQuery(sqlSelectFollowings);
			while (rs.next()) {
				long userID = rs.getLong("source");
				if (userMap.get(userID).getFollowingList() == null)
					userMap.get(userID).setFollowingList(new ArrayList<Long>());
				userMap.get(userID).getFollowingList().add(rs.getLong("target"));
			}
			rs.close();
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(0);
		}
		connectionPool.freeConnection(conn);
		return userMap;
	}
	
	public boolean updateUserMap() {
		Engine engine = Engine.getSingleton();
		HashMap<Long, TwitterUser> userMap = engine.getUserMap();
		
		String sqlUser = new String("INSERT OR REPLACE INTO user (id, isVisited, isProtected, isVerified, lang, followingsCount, followersCount, tweetsCount, latestTweet, date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
		try {
			PreparedStatement prep = conn.prepareStatement(sqlUser);		// SQL query to be compiled should involve question('?') marks.
			for (TwitterUser user : userMap.values()) {
				prep.setLong(1, user.getID());
				prep.setInt(2, user.isVisited() ? 1 : 0);
				prep.setInt(3, user.isProtected() ? 1 : 0);
				prep.setInt(4, user.isVerifiedCelebrity() ? 1 : 0);
				prep.setString(5, user.getLang());
				prep.setInt(6, user.getFollowingsCount());
				prep.setInt(7, user.getFollowersCount());
				prep.setInt(8, user.getTweetsCount());
				prep.setLong(9, user.getLatestTweetID());
				prep.setLong(10, user.getDate());
				prep.addBatch();
			}
			prep.executeBatch();
			
			conn.commit();
			prep.close();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		
		String sqlFollow = new String("INSERT OR IGNORE INTO follow (source, target) VALUES (?, ?)");
		try {
			PreparedStatement prep = conn.prepareStatement(sqlFollow);		// SQL query to be compiled should involve question('?') marks.
			for (TwitterUser user : userMap.values()) {
				if (user.getFollowingList() == null) 
					continue;
				for (long target : user.getFollowingList()) {
					prep.setLong(1, user.getID());
					prep.setLong(2, target);
					prep.addBatch();
				}
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
	
	public boolean insertUser(User user) {
		String sql = new String("INSERT OR IGNORE INTO user (id, isVisited, isProtected, isVerified, lang, followingsCount, followersCount, tweetsCount, latestTweet, date) VALUES ("
				+ user.getId() + ", 0, " + (user.isProtected() ? 1 : 0) + ", " + (user.isVerified() ? 1 : 0) + ", '" + user.getLang() + "', " + user.getFriendsCount() + ", " + user.getFollowersCount() + ", " + user.getStatusesCount() + ", " + (user.getStatus() == null ? -1L : user.getStatus().getId()) + ", " + user.getCreatedAt().getTime() + ")");
		return execQuery(sql);
	}
	
	public boolean insertUsers(ArrayList<User> users) {
		String sql = new String("INSERT OR IGNORE INTO user (id, isVisited, isProtected, isVerified, lang, followingsCount, followersCount, tweetsCount, latestTweet, date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
		ArrayList<String[]> values = new ArrayList<String[]>();
		for (User user : users) {
			String[] value = new String[10];
			value[0] = String.valueOf(user.getId());
			value[1] = String.valueOf(0);
			value[2] = String.valueOf(user.isProtected() ? 1 : 0);
			value[3] = String.valueOf(user.isVerified() ? 1 : 0);
			value[4] = user.getLang();
			value[5] = String.valueOf(user.getFriendsCount());
			value[6] = String.valueOf(user.getFollowersCount());
			value[7] = String.valueOf(user.getStatusesCount());
			value[8] = String.valueOf(user.getStatus() == null ? -1L : user.getStatus().getId());
			value[9] = String.valueOf(user.getCreatedAt().getTime());
			values.add(value);
		}
		return batchQueries(sql, values);
	}
	
	public boolean insertFollowing(long userID, ArrayList<Long> followings) {
		String sql = new String("INSERT OR IGNORE INTO follow (source, target) VALUES (?, ?)");
		ArrayList<String[]> values = new ArrayList<String[]>();
		for (long followingUserID : followings) {
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
}
