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
	private Engine engine = null;
	private EgoNetwork egoNetwork = null;
	
	// Necessary for multi-threads tasking
	private ExecutorService exeService = null;
	
	// Necessary for Breadth-First-Search
	private Queue<TwitterUser> queue;
	
	public Crawler(EgoNetwork network) {
		this.exeService = Executors.newCachedThreadPool();
		this.engine = new Engine(network, exeService);
		this.egoNetwork = network;
		this.queue = new LinkedList<TwitterUser>();
		
		TwitterUser egoUser = network.getEgoUser();
		network.getNodeMap().put(egoUser.id, egoUser);
		queue.offer(egoUser);
	}
	
	public void run() {
		if (egoNetwork.level() < 0) return;
		
		engine.printLog("TWITTER CRAWLING STARTED: " + egoNetwork.getEgoUser().id, false);
		long crawling_start = System.currentTimeMillis();
		
		// Set visiting limit for exploring with BFS until at the given level
		int[] visitingLimit = new int[egoNetwork.level() + 1];
		visitingLimit[0] = 1;
		for (int i = 1; i <= egoNetwork.level(); i++)
			visitingLimit[i] = 0;
		int cursor = 0;
		int cnt = 0;
		
		// Scan Twitter ego-network at the given level by using Breath First Search (BFS)
		while (queue.isEmpty() == false) {
			TwitterUser user = queue.poll();
			visitingLimit[cursor] -= 1;
			user.friends = engine.getFriends(user.id);
			
			cnt += 1;
			if (cnt % 500 == 0)
				System.out.println("Crawling process: " + cnt / 500);
			
			if (cursor < egoNetwork.level()) {
				int newNodeCount = 0;
				for (long friendID : user.friends) {
					if (egoNetwork.getNodeMap().containsKey(friendID))
						continue;
					TwitterUser friend = new TwitterUser(friendID);
					egoNetwork.getNodeMap().put(friend.id, friend);
					queue.offer(friend);
					newNodeCount += 1;
				}
				visitingLimit[cursor + 1] += newNodeCount;
			}
			
			if (visitingLimit[cursor] == 0) {
				cursor += 1;
				if (cursor > egoNetwork.level())
					break;
			}
		}
		
		// Filter invalid user IDs
		for (long invalidID : egoNetwork.getAuthInvalidList())
			egoNetwork.getNodeMap().remove(invalidID);
		for (TwitterUser user : egoNetwork.getNodeMap().values()) {
			ArrayList<Long> filteredList = new ArrayList<Long>();
			for (long friendsID : user.friends) {
				if (egoNetwork.getNodeMap().containsKey(friendsID)) {
					filteredList.add(friendsID);
					egoNetwork.nDirectedEdges += 1;
				}
			}
			user.friends = filteredList;
		}
		
		// Print current memory usage
		engine.printLog(engine.getMemoryUsage(), false);
		
		// Write friendship information into files
		engine.writeFriendsList();
		
		engine.printLog("### Complete: construct network(" + egoNetwork.getEgoUser().id + ")"
				+ " - Node(" + egoNetwork.getNodeMap().size() + "), Edge(" + egoNetwork.nDirectedEdges / 2
				+ "), Excluded Invalid Node(" + egoNetwork.getAuthInvalidList().size() + ")", false);
		engine.printLog(new Utils().getExecutingTime(
				"Network construction time", (System.currentTimeMillis() - crawling_start) / 1000L), false);
		long crawling_start2 = System.currentTimeMillis();
		
		// Get timelines of users of a given network
		cnt = 0;
		for (long userID : egoNetwork.getNodeMap().keySet()) {
			engine.loadTimeline(userID);
			engine.loadFavorites(userID);
			
			cnt += 1;
			if (cnt % 500 == 0)
				System.out.println("Timeline process: " + cnt / 500);
		}
		
		engine.printLog("### Complete: load timelines from Twitter server. "
				+ "- Excluded Invalid Node(" + egoNetwork.getAuthInvalidList().size() + ")", false);
		engine.printLog(new Utils().getExecutingTime(
				"Timeline crawling time", (System.currentTimeMillis() - crawling_start2) / 1000L), false);
		
		engine.printLog("TWITTER CRAWLING FINISHED: " + egoNetwork.getEgoUser().id, false);
		engine.printLog(new Utils().getExecutingTime(
				"Total executing time", (System.currentTimeMillis() - crawling_start) / 1000L), true);
		
		// Wait for other friends
		try {
			exeService.shutdown();
			exeService.awaitTermination(600, TimeUnit.SECONDS);
		} catch (InterruptedException ie) {
		}
	}
}