package crawler;

import java.util.ArrayList;

import crawler.engine.Engine;
import twitter4j.User;

public class SeedSeeker {
	private Engine engine;
	
	public SeedSeeker() {
		this.engine = Engine.getSingleton();
	}
	
	/**
	 * Get seed users from the followers of CNN.
	 * @return A list of seed users
	 */
	public ArrayList<Long> getSeedUsersFromCNNFollowers() {
		ArrayList<Long> seedList = new ArrayList<Long>();
		User seedUser = engine.getUserByScreenName("CNN");
		ArrayList<Long> followers = engine.getFollowers(seedUser.getId(), 100000);
		ArrayList<User> candidates = engine.lookupUsersByID(followers);
		for (User user : candidates) {
			if (isGoodSeed(user))
				seedList.add(user.getId());
		}
		return seedList;
	}
	
	private boolean isGoodSeed(User user) {
		if (user == null)
			return false;
		if (user.isProtected())
			return false;
		if (user.isVerified())
			return false;
		if (user.getLang().equals("en") == false)
			return false;
		if (user.getFriendsCount() > 500 || user.getFriendsCount() < 50)
			return false;
		if (user.getFollowersCount() > 500 || user.getFollowersCount() < 50)
			return false;
		if (user.getStatusesCount() < 100)
			return false;
		return true;
	}
	
//	/**
//	 * Get seed candidates from the following site:
//	 * <a href="http://selfintro.xguru.net/">http://selfintro.xguru.net/</a>
//	 * @return seed-candidate user list
//	 */
//	public ArrayList<User> getSeedCandidatesFromSelfIntro() {
//		ArrayList<Status> searchResult = engine.searchTweets("#self_intro", 5000);
//		ArrayList<User> candidates = new ArrayList<User>();
//		for (Status tweet : searchResult) {
//			if (candidates.contains(tweet.getUser()) == false)
//				candidates.add(tweet.getUser());
//		}
//		return candidates;
//	}
}
