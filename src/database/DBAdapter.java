package database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import tool.Utils;
import twitter4j.Status;
import twitter4j.User;

public class DBAdapter {
	private static DBAdapter mInstance = null;
	
	public static synchronized DBAdapter getSingleton() {
		return (mInstance != null) ? mInstance : (mInstance = new DBAdapter());
	}
	
	private DBHelper mDBHelper = null;
	
	public DBAdapter() {
		this.mDBHelper = new DBHelper();
	}
	
	public void finalize() {
		mDBHelper.closeDBConnection();
	}
	
	public boolean insertUser(User user) {
		String sql = new String("INSERT OR IGNORE INTO user (id, isProtected, isVerified, lang, followingsCount, followersCount, tweetsCount, latestTweet, date) VALUES ("
				+ user.getId() + ", " + (user.isProtected() ? 1 : 0) + ", " + (user.isVerified() ? 1 : 0) + ", '" + user.getLang() + "', " + user.getFriendsCount() + ", " + user.getFollowersCount() + ", " + user.getStatusesCount() + ", " + (user.getStatus() == null ? -1L : user.getStatus().getId()) + ", " + user.getCreatedAt().getTime() + ")");
		return mDBHelper.execQuery(sql);
	}
	
	public boolean insertUsers(ArrayList<User> users) {
		String sql = new String("INSERT OR IGNORE INTO user (id, isProtected, isVerified, lang, followingsCount, followersCount, tweetsCount, latestTweet, date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
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
		return mDBHelper.batchQueries(sql, values);
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
		return mDBHelper.batchQueries(sql, values);
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
		return mDBHelper.batchQueries(sql, values);
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
		return mDBHelper.batchQueries(sql, values);
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
		return mDBHelper.batchQueries(sql, values);
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
		return mDBHelper.batchQueries(sql, values);
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
		return mDBHelper.batchQueries(sql, values);
	}
	
	public ArrayList<Long> getFriendship(long userID) {
		ArrayList<Long> friendshipList = new ArrayList<Long>();
		String sql = new String("SELECT follow.source FROM follow WHERE follow.target = " + userID
				+ " INTERSECT SELECT follow.target FROM follow WHERE follow.source = " + userID);
		try {
			ResultSet rs = mDBHelper.execSelection(sql);
			while (rs.next())
				friendshipList.add(rs.getLong(0));
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return friendshipList;
	}
}
