package TwitterCrawler;
import java.util.ArrayList;
import java.util.logging.Logger;

import twitter4j.TwitterException;
import twitter4j.User;

public class TwitterUser {
	public long id;
	public ArrayList<Long> friends = null;
	
	public TwitterUser(long id) {
		this.id = id;
		this.friends = new ArrayList<Long>();
	}
	
	public User getUserInfo() {
		String endpoint = "/followers/ids";
		TwitterApp app = null;
		try {
			app = AppManager.getSingleton().getAvailableApp(endpoint);
			return app.twitter.showUser(id);
		} catch (TwitterException te) {
			if (te.exceededRateLimitation()) {
				// Register current application as limited one
				AppManager.getSingleton().registerLimitedApp(app, endpoint, te.getRateLimitStatus().getSecondsUntilReset());
				app.printRateLimitStatus(endpoint);
				
				// Retry
				return getUserInfo();
			} else {
				te.printStackTrace();
				return null;
			}
		}
	}
	
	// Check if the given user is a normal user.
	public boolean isNormalUser() {
		User info = getUserInfo();
		if (info.getFriendsCount() > 5000 || info.getFollowersCount() > 5000)
			return false;
		return true;
	}
	
	// Verify user information.
	public void verifyUserInfo() {
		Logger logger = Logger.getGlobal();
		try {
			TwitterApp app = AppManager.getSingleton().getAvailableApp("/users/show/:id");
			User user = app.twitter.showUser(id);
			logger.info("--------------------------------------------------------------\n"
					+ "ID(" + id + ") - Name: " + user.getName() + "(" + user.getScreenName() + ")\n"
					+ "- Followings(" + user.getFriendsCount() + "), Followers(" + user.getFollowersCount() + "), "
					+ "Favorites(" + user.getFavouritesCount() + ")\n"
					+ "--------------------------------------------------------------");
		} catch (TwitterException te) {
			te.printStackTrace();
		}
	}
}
