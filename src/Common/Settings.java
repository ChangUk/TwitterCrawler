package Common;

import twitter4j.User;

public class Settings {
	public static final String PATH_APPS = "../TwitterApp.dat";
	public static final String PATH_DATA = "../Data/TwitterData/";
	
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
