package TwitterCrawler;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import Common.EgoNetwork;
import Common.Utils;

public class Crawler {
	private Engine engine = Engine.getSingleton();
	private EgoNetwork egoNetwork = null;
	
	// Necessary for Breadth-First-Search
	private Queue<Long> queue;
	
	private ExecutorService exeService = null;
	
	public Crawler(EgoNetwork egoNetwork) {
		this.egoNetwork = egoNetwork;
		this.queue = new LinkedList<Long>();
		this.exeService = Executors.newCachedThreadPool();
		queue.offer(egoNetwork.getSeedUser().getID());
	}
	
	public void run() {
		if (egoNetwork.level() < 0) return;
		
		Utils.printLog(egoNetwork, "TWITTER CRAWLING STARTED: " + egoNetwork.getSeedUser().getID() + " - " + Utils.getCurrentTime(), false);
		long crawling_start = System.currentTimeMillis();
		
		// Set visiting limit for exploring with BFS until at the given level
		long[] nNodesToVisit = new long[egoNetwork.level() + 1];
		nNodesToVisit[0] = 1;
		for (int i = 1; i <= egoNetwork.level(); i++)
			nNodesToVisit[i] = 0;
		
		// Current pointer to indicate level
		int curLevel = 0;
		
		// Scan Twitter ego-network at the given level by using Breath First Search (BFS)
		while (queue.isEmpty() == false) {
			// Get a user from queue
			long userID = queue.poll();
			nNodesToVisit[curLevel] -= 1;
			
			// A node
			TwitterUser user;
			
			// Check if the user has ever been crawled
			if (engine.getUserMap().containsKey(userID)) {
				user = engine.getUserMap().get(userID);
			} else {
				// If the user is not obtained by crawling yet, create new user with the given user ID
				user = new TwitterUser(userID);
				
				// Do crawling
				ArrayList<Long> followings = engine.getFollowings(egoNetwork, user, 5000, true);
				user.setFollowingList(followings);
				ArrayList<Long> followers = engine.getFollowers(egoNetwork, user, 5000, true);
				user.setFollowerList(followers);
				ArrayList<Long> friends = engine.getFriendship(egoNetwork, user, true);
				user.setFriendshipList(friends);
				Thread timelineThread = new Thread() {
					@Override
					public void run() {
						super.run();
						engine.getTimeline(egoNetwork, user, true);
						engine.getFavorites(egoNetwork, user, true);
					}
				};
				exeService.submit(timelineThread);
				
				// Register Twitter user
				engine.getUserMap().put(userID, user);
			}
			
			if (egoNetwork.getNodeList().contains(userID) == false) {
				// Add node into network
				egoNetwork.getNodeList().add(userID);
				
				// Increase the number of nodes at the next level
				if (curLevel < egoNetwork.level() && user.isValid()) {
					for (long friendID : user.getFriendshipList()) {
						if (egoNetwork.getNodeList().contains(friendID) || queue.contains(friendID))
							continue;
						queue.offer(friendID);
						nNodesToVisit[curLevel + 1] += 1;
					}
				}
			}
			
			// If all nodes are visited, shift cursor or exit crawling
			if (nNodesToVisit[curLevel] == 0) {
				curLevel += 1;
				if (curLevel > egoNetwork.level())
					break;
			}
		}
		
		// Wait for other friends
		try {
			exeService.shutdown();
			exeService.awaitTermination(600, TimeUnit.SECONDS);
		} catch (InterruptedException ie) {
		}
		
		// Save user map into file
		engine.saveUserMap();
		engine.saveScreenNameMap();
		
		// Garbage collection
		System.gc();
		
		// Print current memory usage
		Utils.printLog(egoNetwork, "### Current memory usage: " + Utils.getCurMemoryUsage() + " MB", false);
		Utils.printLog(egoNetwork, "### Complete: Node(" + egoNetwork.getNodeCount() + "), Edge(" + egoNetwork.getEdgeCount() + ")", false);
		Utils.printLog(egoNetwork, "TWITTER CRAWLING FINISHED: " + egoNetwork.getSeedUser().getID(), false);
		Utils.printLog(egoNetwork, Utils.getExecutingTime("Total executing time", (System.currentTimeMillis() - crawling_start) / 1000L), true);
	}
}