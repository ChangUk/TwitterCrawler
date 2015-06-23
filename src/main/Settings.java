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
		if (user.isVerified())						// If the user is a verified celebrity, the user is not normal user.
			return false;
		if (user.getLang().equals("ko") == false)
			return false;
		if (user.getFriendsCount() > 5000)
			return false;
		if (user.getFollowersCount() > 5000)
			return false;
		if (user.getStatusesCount() < 1)
			return false;
		return true;
	}
}
