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
	private ExecutorService exeService = null;
	private Queue<TwitterUser> queue;
	
	public Crawler(EgoNetwork network) {
		this.exeService = Executors.newCachedThreadPool();
		this.engine = new Engine(network, exeService);
		this.queue = new LinkedList<TwitterUser>();
		
		TwitterUser egoUser = network.getEgoUser();
		queue.offer(egoUser);
	}
	
	public void run(EgoNetwork network) {
		engine.printLog("TWITTER CRAWLING STARTED: " + network.getEgoUser().id, false);
		long crawling_start = System.currentTimeMillis();
		
		// Get ego-user and find his friends list
		TwitterUser egoUser = network.getEgoUser();
		egoUser.friends = engine.getFriends(egoUser.id);
		network.getNodeMap().put(egoUser.id, egoUser);
		
		// Get 1-level friends of ego-user and their friends list
		for (long friendID : egoUser.friends) {
			TwitterUser friend = new TwitterUser(friendID);
			friend.friends = engine.getFriends(friendID);
			network.getNodeMap().put(friendID, friend);
		}
		
		// Get 2-level friends of ego-user and their friends list
		// This logic is necessary to find unexplored edges among 2-level nodes
		for (long friendID : egoUser.friends) {
			for (long friendOfFriendID : network.getNodeMap().get(friendID).friends) {
				if (network.getNodeMap().containsKey(friendOfFriendID))
					continue;
				TwitterUser friendOfFriend = new TwitterUser(friendOfFriendID);
				friendOfFriend.friends = engine.getFriends(friendOfFriendID);
				network.getNodeMap().put(friendOfFriendID, friendOfFriend);
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