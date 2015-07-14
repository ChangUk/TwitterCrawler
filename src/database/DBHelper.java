package database;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.Driver;
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
	private final String BACKUP = Settings.PATH_DATA + DBNAME + ".backup";
	private final String DRIVER_NAME = "org.sqlite.JDBC";
	
	private Driver driver = null;						// Database driver
	private Connection conn = null;						// Connection for DB write
	private ConnectionPool connectionPool = null;		// Connection pool for DB read
	
	public DBHelper() {
		// If the location for storing database does not exist, generate the path
		File dataPath = new File(Settings.PATH_DATA);
		if (dataPath.exists() == false)
			dataPath.mkdirs();
		
		// Backup existing database file
		makeBackupFile();
		
		try {
			// Register the Driver to the jbdc.driver java property
			driver = (Driver) Class.forName(DRIVER_NAME).newInstance();
			DriverManager.registerDriver(driver);
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		}
		
		// Create DB connection and DB tables
		makeConnections();
		createDBTables();
	}
	
	private void makeBackupFile() {
		File existingDatabase = new File(Settings.PATH_DATA + DBNAME);
		if (existingDatabase.exists()) {
			deleteBackupFile();
			
			File backup = new File(BACKUP);
			try {
				Files.copy(existingDatabase.toPath(), backup.toPath());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void deleteBackupFile() {
		try {
			File backup = new File(BACKUP);
			if (backup.exists())
				Files.delete(backup.toPath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public synchronized boolean makeConnections() {
		try {
			// If database does not exist, then it will be created automatically
			if (conn == null || conn.isClosed()) {
				SQLiteConfig config = new SQLiteConfig();
				config.setJournalMode(JournalMode.WAL);
				config.enforceForeignKeys(true);
				conn = DriverManager.getConnection(DBPATH, config.toProperties());
				conn.setAutoCommit(false);
			}
			
			// Create connection pool
			if (connectionPool == null) {
				SQLiteConfig poolConfig = new SQLiteConfig();
				poolConfig.setReadOnly(true);
				poolConfig.enforceForeignKeys(true);
				connectionPool = new ConnectionPool(DBPATH, poolConfig.toProperties());
				connectionPool.setMaxPoolSize(1000);
			}
			
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public synchronized void closeConnections() {
		try {
			if (conn != null) {
				conn.close();
				conn = null;
			}
			if (connectionPool != null) {
				connectionPool.closeAll();
				connectionPool = null;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public synchronized void destroy() {
		try {
			// Removes the specified driver from the DriverManager's list of registered drivers
			if (driver != null)
				DriverManager.deregisterDriver(driver);
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
				+ "id INTEGER PRIMARY KEY, isSeed INTEGER, isComplete INTEGER, isProtected INTEGER, isVerified INTEGER, lang TEXT, followingsCount INTEGER, followersCount INTEGER, tweetsCount INTEGER, favoritesCount INTEGER, date INTEGER)");
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
				+ "FOREIGN KEY(user) REFERENCES user(id) ON DELETE CASCADE ON UPDATE CASCADE,	PRIMARY KEY(user, tweet))");
		sqls.add("CREATE TABLE IF NOT EXISTS favorite ("
				+ "user INTEGER, tweet INTEGER, "
				+ "FOREIGN KEY(user) REFERENCES user(id) ON DELETE CASCADE ON UPDATE CASCADE, PRIMARY KEY(user, tweet))");
		sqls.add("CREATE TABLE IF NOT EXISTS mention ("
				+ "source INTEGER, target INTEGER, date INTEGER, "
				+ "FOREIGN KEY(source) REFERENCES user(id) ON DELETE CASCADE ON UPDATE CASCADE, PRIMARY KEY(source, target, date))");
		return execQuery(sqls);
	}
	
	public boolean setSeed(long seedUserID) {
		if (seedUserID < 0) return false;
		String sql = new String("UPDATE user SET isSeed = 1 WHERE id = " + seedUserID);
		return execQuery(sql);
	}
	
	public boolean insertUser(User user) {
		if (user == null) return false;
		String sql = new String("INSERT OR REPLACE INTO user ("
				+ "id, isSeed, isComplete, isProtected, isVerified, lang, followingsCount, followersCount, tweetsCount, favoritesCount, date) VALUES ("
				+ user.getId() + ", 0, 0, " + (user.isProtected() ? 1 : 0) + ", " + (user.isVerified() ? 1 : 0) + ", '"
				+ user.getLang() + "', " + user.getFriendsCount() + ", " + user.getFollowersCount() + ", " + user.getStatusesCount() + ", " + user.getFavouritesCount() + ", " + user.getCreatedAt().getTime() + ")");
		return execQuery(sql);
	}
	
	public boolean insertUsers(ArrayList<User> users) {
		if (users == null) return false;
		String sql = new String("INSERT OR REPLACE INTO user ("
				+ "id, isSeed, isComplete, isProtected, isVerified, lang, followingsCount, followersCount, tweetsCount, favoritesCount, date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
		ArrayList<String[]> values = new ArrayList<String[]>();
		for (User user : users) {
			String[] value = new String[11];
			value[0] = String.valueOf(user.getId());
			value[1] = new String("0");
			value[2] = new String("0");
			value[3] = String.valueOf(user.isProtected() ? 1 : 0);
			value[4] = String.valueOf(user.isVerified() ? 1 : 0);
			value[5] = user.getLang();
			value[6] = String.valueOf(user.getFriendsCount());
			value[7] = String.valueOf(user.getFollowersCount());
			value[8] = String.valueOf(user.getStatusesCount());
			value[9] = String.valueOf(user.getFavouritesCount());
			value[10] = String.valueOf(user.getCreatedAt().getTime());
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
	
	public boolean deleteFollowingList(long userID) {
		String sql = new String("DELETE FROM follow WHERE source = " + userID);
		return execQuery(sql);
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
	
	public boolean isComplete(long userID) {
		String sql = new String("SELECT isComplete FROM user WHERE id = " + userID);
		boolean result = false;
		Connection conn = connectionPool.getConnection();
		try {
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			if (rs.next())
				result = (rs.getInt(1) == 1 ? true : false);
			rs.close();
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		connectionPool.freeConnection(conn);
		return result;
	}
	
	public boolean setUserComplete(long seedUserID) {
		if (seedUserID < 0) return false;
		String sql = new String("UPDATE user SET isComplete = 1 WHERE id = " + seedUserID);
		return execQuery(sql);
	}
	
	public long getLatestTweetID(long userID) {
		String sql = new String("SELECT MAX(id) FROM tweet WHERE author = " + userID);
		long latestTweetID = -1;
		Connection conn = connectionPool.getConnection();
		try {
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			if (rs.next())
				latestTweetID = rs.getLong(1);
			rs.close();
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		connectionPool.freeConnection(conn);
		return latestTweetID;
	}
}
