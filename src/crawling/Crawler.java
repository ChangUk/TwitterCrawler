package crawling;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import main.EgoNetwork;
import main.Settings;
import tool.Utils;
import twitter4j.Status;
import twitter4j.User;
import database.DBHelper;

public class Crawler {
	private Engine engine;
	
	// Necessary for Breadth-First-Search
	private Queue<Long> queue;
	
	// Need to wait for existing threads
	private ExecutorService exeService;
	
	// Database transaction manager
	private DBHelper mDBHelper;
	
	public Crawler() {
		this.engine = Engine.getSingleton();
		this.queue = new LinkedList<Long>();
		this.exeService = Executors.newCachedThreadPool();
		this.mDBHelper = DBHelper.getSingleton();
	}
	
	public void run(EgoNetwork egoNetwork) {
		if (egoNetwork.level() < 0) return;
		
		Utils.printLog("TWITTER CRAWLING STARTED: " + egoNetwork.getSeedUserID() + " - " + Utils.getCurrentTime(), false);
		long crawling_start = System.currentTimeMillis();
		
		// Lookup seed user and put it into BFS queue
		User seedUser = engine.showUser(egoNetwork.getSeedUserID());
		if (seedUser == null) return;
		queue.offer(seedUser.getId());
		
		// Set visiting limit for exploring with BFS until at the given level
		long[] nNodesToVisit = new long[egoNetwork.level() + 1];
		nNodesToVisit[0] = 1;
		for (int i = 1; i <= egoNetwork.level(); i++)
			nNodesToVisit[i] = 0;
		
		// Scan Twitter ego-network at the given level by using Breath First Search (BFS)
		int curLevel = 0;
		while (queue.isEmpty() == false) {
			// Get a user from queue
			long userID = queue.poll();
			nNodesToVisit[curLevel] -= 1;
			
			// Check if it is necessary to get timeline for the given user.
			if (mDBHelper.hasRecord(userID) == false) {
				// Get TwitterUser instance with the given userID
				User user = engine.showUser(userID);
				mDBHelper.insertUser(user);
				
				if (Settings.isValidUser(user)) {
					// Get following user list
					ArrayList<Long> followings = engine.getFollowings(userID, 5000);
					mDBHelper.insertFollowingList(userID, followings);
					
					// Get timeline and save into stand-alone database
					Thread timelineThread = new Thread() {
						@Override
						public void run() {
							super.run();
							ArrayList<Status> timeline = engine.getTimeline(userID);
							mDBHelper.insertTweets(timeline);
							
							ArrayList<Status> retweets = engine.getRetweets(timeline);
							mDBHelper.insertRetweetHistory(userID, retweets);
							
							ArrayList<Long> shareList = engine.getSharedTweets(timeline);
							mDBHelper.insertShareHistory(userID, shareList);
							
							HashMap<Long, ArrayList<Date>> mentions = engine.getMentionHistory(userID, timeline);
							mDBHelper.insertMentionHistory(userID, mentions);
						}
					};
					exeService.execute(timelineThread);
					
					// Get favorites and save into stand-alone database
					Thread favoritesThread = new Thread() {
						@Override
						public void run() {
							super.run();
							ArrayList<Status> favorites = engine.getFavorites(userID);
							mDBHelper.insertFavoriteHistory(userID, favorites);
						}
					};
					exeService.execute(favoritesThread);
				}
			}
			
			if (egoNetwork.getVisitedNodeList().contains(userID) == false) {
				egoNetwork.getVisitedNodeList().add(userID);
				if (curLevel < egoNetwork.level()) {
					// Set the number of nodes to visit at the next level
					ArrayList<Long> followingUsers = mDBHelper.getFollowingList(userID);
					for (long friendID : followingUsers) {
						if (egoNetwork.getVisitedNodeList().contains(friendID) || queue.contains(friendID))
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
		Utils.printLog(Utils.getExecutingTime("Graph search time", crawling_start), false);
		
		exeService.shutdown();
		while (exeService.isTerminated() == false) {
			// Wait for other running threads
		}
		
		// Close database connection
		mDBHelper.closeDBConnections();
		
		// Garbage collection
		System.gc();
		
		// Print crawling result
		Utils.printLog("### Current memory usage: " + Utils.getCurMemoryUsage() + " MB", false);
		Utils.printLog("TWITTER CRAWLING FINISHED", false);
		Utils.printLog(Utils.getExecutingTime("Total executing time", crawling_start), true);
	}
}
