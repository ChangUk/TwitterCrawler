package crawler;

import java.util.ArrayList;

import twitter4j.Status;
import twitter4j.User;

public class SeedFinder {
	private Engine engine;
	
	public SeedFinder() {
		this.engine = Engine.getSingleton();
	}
	
	/**
	 * Get seed candidates from the following site:
	 * <a href="http://selfintro.xguru.net/">http://selfintro.xguru.net/</a>
	 * @return seed-candidate user list
	 */
	public ArrayList<User> getSeedCandidates() {
		ArrayList<Status> searchResult = engine.searchTweets("#self_intro", 5000);
		ArrayList<User> candidates = new ArrayList<User>();
		for (Status tweet : searchResult) {
			if (candidates.contains(tweet.getUser()) == false)
				candidates.add(tweet.getUser());
		}
		return candidates;
	}
}
