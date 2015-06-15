package Common;

import java.io.File;

import twitter4j.User;

public class Settings {
	public static final String PATH_APPS = "../TwitterApp.dat";
	public static final String PATH_DATA = "../Data/TwitterData/";
	
	public static final String PATH_DATA_FOLLOWING	= "../Data/TwitterData/following/";
	public static final String PATH_DATA_FOLLOWER	= "../Data/TwitterData/follower/";
	public static final String PATH_DATA_FRIENDSHIP	= "../Data/TwitterData/friendship/";
	public static final String PATH_DATA_TIMELINE	= "../Data/TwitterData/timeline/";
	public static final String PATH_DATA_SHARE		= "../Data/TwitterData/share/";
	public static final String PATH_DATA_RETWEET	= "../Data/TwitterData/retweet/";
	public static final String PATH_DATA_MENTION	= "../Data/TwitterData/mention/";
	public static final String PATH_DATA_FAVORITE	= "../Data/TwitterData/favorite/";
	
	public static void makeDirectories() {
		File pathFollowingData = new File(PATH_DATA_FOLLOWING);
		if (pathFollowingData.exists() == false)
			pathFollowingData.mkdirs();
		File pathFollowerData = new File(PATH_DATA_FOLLOWER);
		if (pathFollowerData.exists() == false)
			pathFollowerData.mkdirs();
		File pathFriendshipData = new File(PATH_DATA_FRIENDSHIP);
		if (pathFriendshipData.exists() == false)
			pathFriendshipData.mkdirs();
		File pathTweetData = new File(PATH_DATA_TIMELINE);
		if (pathTweetData.exists() == false)
			pathTweetData.mkdirs();
		File pathShareData = new File(PATH_DATA_SHARE);
		if (pathShareData.exists() == false)
			pathShareData.mkdirs();
		File pathRetweetData = new File(PATH_DATA_RETWEET);
		if (pathRetweetData.exists() == false)
			pathRetweetData.mkdirs();
		File pathMentionData = new File(PATH_DATA_MENTION);
		if (pathMentionData.exists() == false)
			pathMentionData.mkdirs();
		File pathFavoriteData = new File(PATH_DATA_FAVORITE);
		if (pathFavoriteData.exists() == false)
			pathFavoriteData.mkdirs();
	}
	
	/**
	 * Check if this user is normal and heavy user or not.
	 * @param network Target network
	 * @param user User
	 * @param checkLanguage if true, test process involves language checking.
	 * @return true if the user is valid user, false otherwise.
	 */
	public static boolean isRequirementSatisfied(TwitterNetwork network, User user, boolean checkLanguage) {
		if (user.isProtected())
			return false;
		if (checkLanguage == true && user.getLang().equals(network.getLang()) == false)	// Requirement #1: language matching
			return false;
		if (user.getStatusesCount() < 1)		// Requirement #2: minimum number of tweets
			return false;
		if (user.getFriendsCount() > 5000)		// Requirement #3: maximum number of following users
			return false;
		if (user.getFollowersCount() > 5000)	// Requirement #4: maximum number of followers
			return false;
		return true;
	}
}
