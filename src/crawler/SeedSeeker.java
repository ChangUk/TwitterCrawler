package crawler;

import java.util.ArrayList;

import crawler.engine.Engine;
import database.SQLiteAdapter;
import twitter4j.Status;
import twitter4j.User;

public class SeedSeeker {
	private Engine engine;
	private SQLiteAdapter mDBAdapter;
	
	public SeedSeeker() {
		this.engine = Engine.getSingleton();
		this.mDBAdapter = SQLiteAdapter.getSingleton();
	}
	
	public void findGoodSeeds() {
		// Make DB connections
		mDBAdapter.makeConnections();
		
		User seedUser = engine.showUserByScreenName("CNN");
		ArrayList<Long> followers = engine.getFollowers(seedUser.getId(), 1000);
		ArrayList<User> candidates = engine.lookupUsersByID(followers);
		for (User user : candidates) {
			if (isGoodSeed(user))
				mDBAdapter.insertUser(user);
		}
		
		// Close DB connections
		mDBAdapter.closeConnections();
	}
	
	/**
	 * Get seed candidates from the following site:
	 * <a href="http://selfintro.xguru.net/">http://selfintro.xguru.net/</a>
	 * @return seed-candidate user list
	 */
	public ArrayList<User> getSeedCandidatesFromSelfIntro() {
		ArrayList<Status> searchResult = engine.searchTweets("#self_intro", 5000);
		ArrayList<User> candidates = new ArrayList<User>();
		for (Status tweet : searchResult) {
			if (candidates.contains(tweet.getUser()) == false)
				candidates.add(tweet.getUser());
		}
		return candidates;
	}
	
	public ArrayList<User> getCNNFollowers() {
		ArrayList<User> candidates = new ArrayList<User>();
		return candidates;
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
}
