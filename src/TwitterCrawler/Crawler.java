package TwitterCrawler;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import Util.Utils;

public class Crawler {
	private Engine engine = null;
	private EgoNetwork network = null;
	private final int level;
	private ExecutorService exeService = null;
	private Queue<TwitterUser> queue;
	
	public Crawler(EgoNetwork network, int level) {
		this.exeService = Executors.newCachedThreadPool();
		this.engine = new Engine(network, level, exeService);
		this.network = network;
		this.level = level;
		this.queue = new LinkedList<TwitterUser>();
		
		TwitterUser egoUser = network.getEgoUser();
		network.getNodeMap().put(egoUser.id, egoUser);
		queue.offer(egoUser);
	}
	
	public void run() {
		if (level < 0) return;
		
		engine.printLog("TWITTER CRAWLING STARTED: " + network.getEgoUser().id, false);
		long crawling_start = System.currentTimeMillis();
		
		// Set visiting limit for exploring with BFS until at the given level
		int[] visitingLimit = new int[level + 1];
		visitingLimit[0] = 1;
		for (int i = 1; i <= level; i++)
			visitingLimit[i] = 0;
		int cursor = 0;
		
		// Scan Twitter ego-network at the given level by using Breath First Search (BFS)
		while (queue.isEmpty() == false) {
			TwitterUser user = queue.poll();
			visitingLimit[cursor] -= 1;
			user.friends = engine.getFriends(user.id);
			
			if (cursor < level) {
				int newNodeCount = 0;
				for (long friendID : user.friends) {
					if (network.getNodeMap().containsKey(friendID))
						continue;
					TwitterUser friend = new TwitterUser(friendID);
					network.getNodeMap().put(friend.id, friend);
					queue.offer(friend);
					newNodeCount += 1;
				}
				visitingLimit[cursor + 1] += newNodeCount;
			}
			
			if (visitingLimit[cursor] == 0) {
				cursor += 1;
				if (cursor > level)
					break;
			}
		}
		
		// Filter invalid user IDs
		for (long invalidID : network.getAuthInvalidList())
			network.getNodeMap().remove(invalidID);
		for (TwitterUser user : network.getNodeMap().values()) {
			ArrayList<Long> filteredList = new ArrayList<Long>();
			for (long friendsID : user.friends) {
				if (network.getNodeMap().containsKey(friendsID)) {
					filteredList.add(friendsID);
					network.nDirectedEdges += 1;
				}
			}
			user.friends = filteredList;
		}
		
		// Write friendship information into files
		engine.writeFriendsList();
		
		engine.printLog("### Complete: construct network(" + network.getEgoUser().id + ")"
				+ " - Node(" + network.getNodeMap().size() + "), Edge(" + network.nDirectedEdges / 2
				+ "), Excluded Invalid Node(" + network.getAuthInvalidList().size() + ")", false);
		engine.printLog(new Utils().printExecutingTime(
				"Network constructing time", (System.currentTimeMillis() - crawling_start) / 1000L), false);
		long crawling_start2 = System.currentTimeMillis();
		
		// Get timelines of users of a given network
		for (long userID : network.getNodeMap().keySet()) {
			engine.loadTimeline(userID);
			engine.loadFavorites(userID);
		}
		
		engine.printLog("### Complete: load timelines from Twitter server. "
				+ "- Excluded Invalid Node(" + network.getAuthInvalidList().size() + ")", false);
		engine.printLog(new Utils().printExecutingTime(
				"Timeline crawling time", (System.currentTimeMillis() - crawling_start2) / 1000L), false);
		
		engine.printLog("TWITTER CRAWLING FINISHED: " + network.getEgoUser().id, false);
		engine.printLog(new Utils().printExecutingTime(
				"Total executing time", (System.currentTimeMillis() - crawling_start) / 1000L), true);
		
		try {
			exeService.shutdown();
			exeService.awaitTermination(600, TimeUnit.SECONDS);
		} catch (InterruptedException ie) {
		}
	}
}