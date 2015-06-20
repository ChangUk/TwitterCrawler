package main;

import twitter4j.User;

public class Settings {
	public static final String PATH_APPS = "../TwitterApp.dat";
	public static final String PATH_DATA = "../Data/TwitterData/";
	
	public static boolean isValidUser(User user) {
		if (user == null)
			return false;
		if (user.isProtected())
			return false;
		if (user.isVerified())							// If the user is a verified celebrity, the user is not normal user.
			return false;
		if (user.getFriendsCount() > 5000)				// Requirement #1: maximum number of following users
			return false;
		if (user.getFollowersCount() > 5000)			// Requirement #2: maximum number of followers
			return false;
		if (user.getStatusesCount() < 1)				// Requirement #3: minimum number of tweets
			return false;
		return true;
	}
}
