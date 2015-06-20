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
import main.TwitterUser;
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
		
		if (engine.getUserMap() == null) {
			HashMap<Long, TwitterUser> userMap = mDBHelper.loadUserMap();
			engine.setUserMap(userMap);
		}
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
			
			// Get TwitterUser instance with the given userID
			final TwitterUser user;
			if (engine.getUserMap().containsKey(userID)) {
				user = engine.getUserMap().get(userID);
			} else {
				user = new TwitterUser(engine.showUser(userID));
				engine.getUserMap().put(userID, user);
			}
			
			// If the user is not obtained by crawling yet, do crawling over it!
			if (user.isVisited() == false && Settings.isValidUser(user)) {
				// Get following user list
				ArrayList<Long> followings = engine.getFollowings(user, 5000);
				user.setFollowingList(followings);
				
//				// Look up and register following users
//				ArrayList<Long> unregisteredUsers = new ArrayList<Long>();
//				for (long followingUserID : followings) {
//					if (engine.getUserMap().containsKey(followingUserID) == false)
//						unregisteredUsers.add(followingUserID);
//				}
//				ArrayList<User> unregisteredFollowingUsers = engine.lookupUsers(unregisteredUsers);
//				for (User unregisteredUser : unregisteredFollowingUsers) {
//					engine.getUserMap().put(unregisteredUser.getId(), new TwitterUser(unregisteredUser));
//				}
				
				// Get timeline and save it into local database
				Thread thread = new Thread() {
					@Override
					public void run() {
						super.run();
						ArrayList<Status> timeline = engine.getTimeline(user);
						mDBHelper.insertTweets(timeline);
						
						ArrayList<Status> retweets = engine.getRetweets(timeline);
						mDBHelper.insertRetweetHistory(userID, retweets);
						
						ArrayList<Long> shareList = engine.getSharedTweets(timeline);
						mDBHelper.insertShareHistory(userID, shareList);
						
						HashMap<Long, ArrayList<Date>> mentions = engine.getMentionHistory(userID, timeline);
						mDBHelper.insertMentionHistory(userID, mentions);
						
						ArrayList<Status> favorites = engine.getFavorites(user);
						mDBHelper.insertFavoriteHistory(userID, favorites);
					}
				};
				exeService.execute(thread);
				
				// Set visited
				user.setVisited();
			}
			
			if (egoNetwork.getVisitedNodeList().contains(userID) == false) {
				// Add node into network
				egoNetwork.getVisitedNodeList().add(userID);
				
				if (curLevel < egoNetwork.level()) {
					// If the user is valid user, increase the number of nodes to visit at the next level
					if (Settings.isValidUser(user)) {
						for (long friendID : user.getFollowingList()) {
							if (egoNetwork.getVisitedNodeList().contains(friendID) || queue.contains(friendID))
								continue;
							queue.offer(friendID);
							nNodesToVisit[curLevel + 1] += 1;
						}
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
		
		exeService.shutdown();
		while (exeService.isTerminated() == false) {
			// Wait for other running threads
		}
		
		// Save user map into file
		mDBHelper.updateUserMap();
		
		// Close database connection
		mDBHelper.closeDBConnections();
		
		// Garbage collection
		System.gc();
		
		// Print crawling result
		Utils.printLog("### Current memory usage: " + Utils.getCurMemoryUsage() + " MB", false);
		Utils.printLog("TWITTER CRAWLING FINISHED", false);
		Utils.printLog(Utils.getExecutingTime("Total executing time", (System.currentTimeMillis() - crawling_start) / 1000L), true);
	}
}