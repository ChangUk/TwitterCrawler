package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import tool.Utils;
import twitter4j.Status;
import twitter4j.User;

public class MariaDBAdapter extends DBAdapter {
	private static MariaDBAdapter mInstance = null;
	
	public static synchronized MariaDBAdapter getSingleton() {
		return (mInstance != null) ? mInstance : (mInstance = new MariaDBAdapter());
	}
	
	public MariaDBAdapter() {
		// Connection information
		CONNECTION_URL	= "jdbc:mariadb://localhost:3306/";
		DATABASE_NAME	= "TwitterData";
		USER_ID			= "root";
		USER_PASSWORD	= "";
		
		// Create database with the name DATABASE_NAME
		createDatabase();
		
		// Create database tables
		makeConnections();
		createDBTables();
	}
	
	@Override
	public synchronized boolean createDatabase() {
		makeConnections();
		ArrayList<String> sqls = new ArrayList<String>();
		sqls.add("CREATE DATABASE IF NOT EXISTS TwitterData CHARACTER SET utf8 COLLATE utf8_general_ci");
		sqls.add("USE TwitterData");
		CONNECTION_URL += "TwitterData";
		boolean result = execQuery(sqls);
		closeConnections();
		return result;
	}
	
	@Override
	public synchronized boolean makeConnections() {
		try {
			// If database does not exist, then it will be created automatically
			if (conn == null || conn.isClosed()) {
				conn = DriverManager.getConnection(CONNECTION_URL, USER_ID, USER_PASSWORD);
				conn.setAutoCommit(false);
			}
			
			// Create connection pool
			if (connectionPool == null) {
				connectionPool = new ConnectionPool(CONNECTION_URL, USER_ID, USER_PASSWORD);
				connectionPool.setMaxPoolSize(1000);
			}
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	@Override
	public synchronized boolean closeConnections() {
		try {
			if (conn != null) {
				conn.close();
				conn = null;
			}
			if (connectionPool != null) {
				connectionPool.closeAll();
				connectionPool = null;
			}
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean createDBTables() {
		ArrayList<String> sqls = new ArrayList<String>();
		
		// Create tables
		sqls.add("CREATE TABLE IF NOT EXISTS user ("
				+ "id BIGINT PRIMARY KEY, screenName VARCHAR(15), description VARCHAR(160), location VARCHAR(30), timezone VARCHAR(30), lang VARCHAR(20), isSeed BOOLEAN, isComplete BOOLEAN, isProtected BOOLEAN, isVerified BOOLEAN, followingsCount INT, followersCount INT, tweetsCount INT, favoritesCount INT, date BIGINT)");
		sqls.add("CREATE TABLE IF NOT EXISTS follow ("
				+ "source BIGINT, target BIGINT, PRIMARY KEY(source, target))");
		sqls.add("CREATE TABLE IF NOT EXISTS tweet ("
				+ "id BIGINT PRIMARY KEY, author BIGINT, text VARCHAR(140), isMention BOOLEAN, retweetCount INT, favoriteCount INT, date BIGINT)");
		sqls.add("CREATE TABLE IF NOT EXISTS retweet ("
				+ "user BIGINT, tweet BIGINT, date BIGINT, PRIMARY KEY(user, tweet))");
		sqls.add("CREATE TABLE IF NOT EXISTS quote ("
				+ "user BIGINT, tweet BIGINT, date BIGINT, PRIMARY KEY(user, tweet))");
		sqls.add("CREATE TABLE IF NOT EXISTS favorite ("
				+ "user BIGINT, tweet BIGINT, PRIMARY KEY(user, tweet))");
		sqls.add("CREATE TABLE IF NOT EXISTS mention ("
				+ "source BIGINT, target BIGINT, date BIGINT, PRIMARY KEY(source, target, date))");
		
		// Create index on a column of table
		sqls.add("CREATE INDEX IF NOT EXISTS index_user_isSeed ON user(isSeed)");
		sqls.add("CREATE INDEX IF NOT EXISTS index_user_isComplete ON user(isComplete)");
		sqls.add("CREATE INDEX IF NOT EXISTS index_tweet_author ON tweet(author)");
		sqls.add("CREATE INDEX IF NOT EXISTS index_tweet_author_and_isMention ON tweet(author, isMention)");
		sqls.add("CREATE INDEX IF NOT EXISTS index_follow_source ON follow(source)");
		sqls.add("CREATE INDEX IF NOT EXISTS index_follow_target ON follow(target)");
		sqls.add("CREATE INDEX IF NOT EXISTS index_retweet_user ON retweet(user)");
		sqls.add("CREATE INDEX IF NOT EXISTS index_quote_user ON quote(user)");
		sqls.add("CREATE INDEX IF NOT EXISTS index_favorite_user ON favorite(user)");
		sqls.add("CREATE INDEX IF NOT EXISTS index_mention_source ON mention(source)");
		
		return execQuery(sqls);
	}
	
	public boolean insertUser(User user) {
		ArrayList<User> userList = new ArrayList<User>();
		userList.add(user);
		return insertUsers(userList);
	}
	
	public boolean insertUsers(ArrayList<User> users) {
		if (users == null || users.size() == 0)
			return false;
		String sql = new String("INSERT INTO user ("
				+ "id, screenName, description, location, timezone, lang, isSeed, isComplete, isProtected, isVerified, followingsCount, followersCount, tweetsCount, favoritesCount, date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
		ArrayList<String[]> values = new ArrayList<String[]>();
		for (User user : users) {
			if (user == null)
				continue;
			String[] value = new String[15];
			value[0] = String.valueOf(user.getId());
			value[1] = user.getScreenName();
			value[2] = user.getDescription();
			value[3] = user.getLocation();
			value[4] = user.getTimeZone();
			value[5] = user.getLang();
			value[6] = new String("0");
			value[7] = new String("0");
			value[8] = String.valueOf(user.isProtected() ? 1 : 0);
			value[9] = String.valueOf(user.isVerified() ? 1 : 0);
			value[10] = String.valueOf(user.getFriendsCount());
			value[11] = String.valueOf(user.getFollowersCount());
			value[12] = String.valueOf(user.getStatusesCount());
			value[13] = String.valueOf(user.getFavouritesCount());
			value[14] = String.valueOf(user.getCreatedAt().getTime());
			values.add(value);
		}
		return execBatchQueries(sql, values);
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
		String sql = new String("INSERT IGNORE INTO follow (source, target) VALUES (?, ?)");
		ArrayList<String[]> values = new ArrayList<String[]>();
		for (long followingUserID : followingList) {
			String[] value = new String[2];
			value[0] = String.valueOf(userID);
			value[1] = String.valueOf(followingUserID);
			values.add(value);
		}
		return execBatchQueries(sql, values);
	}
	
	public boolean insertFollowerList(long userID, ArrayList<Long> followerList) {
		String sql = new String("INSERT IGNORE INTO follow (source, target) VALUES (?, ?)");
		ArrayList<String[]> values = new ArrayList<String[]>();
		for (long followerID : followerList) {
			String[] value = new String[2];
			value[0] = String.valueOf(followerID);
			value[1] = String.valueOf(userID);
			values.add(value);
		}
		return execBatchQueries(sql, values);
	}
	
	public boolean deleteFollowingList(long userID) {
		String sql = new String("DELETE FROM follow WHERE source = " + userID);
		return execQuery(sql);
	}
	
	public boolean deleteFollowerList(long userID) {
		String sql = new String("DELETE FROM follow WHERE target = " + userID);
		return execQuery(sql);
	}

	public boolean insertTweets(ArrayList<Status> tweets) {
		return insertTweets(tweets, true);
	}
	
	public boolean insertTweets(ArrayList<Status> tweets, boolean inclText) {
		String sql = new String("INSERT IGNORE INTO tweet (id, author, text, isMention, retweetCount, favoriteCount, date) VALUES (?, ?, ?, ?, ?, ?, ?)");
		ArrayList<String[]> values = new ArrayList<String[]>();
		for (Status tweet : tweets) {
			String[] value = new String[7];
			Status target = tweet;
			if (tweet.isRetweet())
				target = tweet.getRetweetedStatus();
			value[0] = String.valueOf(target.getId());
			value[1] = String.valueOf(target.getUser().getId());
			if (inclText == false)
				value[2] = new String("");
			else
				value[2] = target.getText();
			value[3] = String.valueOf(Utils.containsMention(target) == true ? 1 : 0);
			value[4] = String.valueOf(target.getRetweetCount());
			value[5] = String.valueOf(target.getFavoriteCount());
			value[6] = String.valueOf(target.getCreatedAt().getTime());
			values.add(value);
		}
		return execBatchQueries(sql, values);
	}
	
	public boolean insertRetweetHistory(long userID, HashMap<Status, Date> retweets) {
		String sql = new String("INSERT IGNORE INTO retweet (user, tweet, date) VALUES (?, ?, ?)");
		ArrayList<String[]> values = new ArrayList<String[]>();
		for (HashMap.Entry<Status, Date> entry : retweets.entrySet()) {
			String[] value = new String[3];
			value[0] = String.valueOf(userID);
			value[1] = String.valueOf(entry.getKey().getId());
			value[2] = String.valueOf(entry.getValue().getTime());
			values.add(value);
		}
		return execBatchQueries(sql, values);
	}
	
	public boolean insertQuoteHistory(long userID, HashMap<Status, Date> quotes) {
		String sql = new String("INSERT IGNORE INTO quote (user, tweet, date) VALUES (?, ?, ?)");
		ArrayList<String[]> values = new ArrayList<String[]>();
		for (HashMap.Entry<Status, Date> entry : quotes.entrySet()) {
			String[] value = new String[3];
			value[0] = String.valueOf(userID);
			value[1] = String.valueOf(entry.getKey().getId());
			value[2] = String.valueOf(entry.getValue().getTime());
			values.add(value);
		}
		return execBatchQueries(sql, values);
	}

	public boolean insertFavoriteHistory(long userID, ArrayList<Status> favorites) {
		return insertFavoriteHistory(userID, favorites, true);
	}
	
	public boolean insertFavoriteHistory(long userID, ArrayList<Status> favorites, boolean inclText) {
		insertTweets(favorites, inclText);
		
		String sql = new String("INSERT IGNORE INTO favorite (user, tweet) VALUES (?, ?)");
		ArrayList<String[]> values = new ArrayList<String[]>();
		for (Status favorite : favorites) {
			String[] value = new String[2];
			value[0] = String.valueOf(userID);
			value[1] = String.valueOf(favorite.getId());
			values.add(value);
		}
		return execBatchQueries(sql, values);
	}
	
	public boolean insertMentionHistory(long userID, HashMap<Long, ArrayList<Date>> mentionHistory) {
		String sql = new String("INSERT IGNORE INTO mention (source, target, date) VALUES (?, ?, ?)");
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
		return execBatchQueries(sql, values);
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
		if (userID < 0) return false;
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
	
	public boolean setUserComplete(long userID) {
		if (userID < 0) return false;
		String sql = new String("UPDATE user SET isComplete = 1 WHERE id = " + userID);
		return execQuery(sql);
	}
	
	public boolean isSeed(long userID) {
		if (userID < 0) return false;
		String sql = new String("SELECT isSeed FROM user WHERE id = " + userID);
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
	
	public boolean setSeed(long userID) {
		if (userID < 0) return false;
		String sql = new String("UPDATE user SET isSeed = 1 WHERE id = " + userID);
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
