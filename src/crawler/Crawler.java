package crawler;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import database.SQLiteAdapter;
import main.EgoNetwork;
import main.Settings;
import tool.Utils;
import twitter4j.Status;
import twitter4j.User;

public class Crawler {
	private Engine engine;
	
	// Necessary for Breadth-First-Search
	private Queue<Long> queue;
	
	// Need to wait for existing threads
	private ExecutorService exeService;
	
	// Database transaction manager
	private SQLiteAdapter mDBAdapter;
	
	public Crawler() {
		this.engine = Engine.getSingleton();
		this.queue = new LinkedList<Long>();
		this.exeService = Executors.newFixedThreadPool(1000);
		this.mDBAdapter = SQLiteAdapter.getSingleton();
	}
	
	public void run(EgoNetwork egoNetwork) {
		if (egoNetwork.level() < 0) return;
		
		// Make DB connection
		mDBAdapter.makeConnections();
		
		// Seed check
		if (mDBAdapter.isSeed(egoNetwork.getSeedUserID()))
			return;
		
		Utils.printLog("TWITTER CRAWLING STARTED: " + egoNetwork.getSeedUserID() + " - " + Utils.getCurrentTime(), false);
		long crawling_start = System.currentTimeMillis();
		
		// Lookup seed user and put it into BFS queue
		User seedUser = engine.showUserByID(egoNetwork.getSeedUserID());
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
			
			if (mDBAdapter.isComplete(userID) == false) {
				User user = engine.showUserByID(userID);
				
				// Register valid user to database
				mDBAdapter.insertUser(user);
				
				if (Settings.isNormalUser(user)) {
					// Get following user list
					ArrayList<Long> followings = engine.getFollowings(userID, 5000);
					mDBAdapter.insertFollowingList(userID, followings);
					
					// Get timeline and save into stand-alone database
					exeService.execute(new Runnable() {
						@Override
						public void run() {
							ArrayList<Status> timeline = engine.getTimeline(userID);
							if (timeline.size() > 0) {
								mDBAdapter.insertTweets(timeline);

								ArrayList<Status> retweets = engine.getRetweets(timeline);
								mDBAdapter.insertRetweetHistory(userID, retweets);
								
								ArrayList<Long> shareList = engine.getSharedTweets(timeline);
								mDBAdapter.insertShareHistory(userID, shareList);
								
								HashMap<Long, ArrayList<Date>> mentions = engine.getMentionHistory(userID, timeline);
								mDBAdapter.insertMentionHistory(userID, mentions);
							}
							
							ArrayList<Status> favorites = engine.getFavorites(userID);
							mDBAdapter.insertFavoriteHistory(userID, favorites);
							
							// Mark this user completed
							mDBAdapter.setUserComplete(userID);
						}
					});
				}
			}
			
			/*
			 * Register the user as a visited node.
			 * Once the user is registered into visited user list, the user does not become a candidate of crawler.
			 * However, in another network, the crawler will regard this user as an unvisited node and try to get his data.
			 */
			egoNetwork.getVisitedNodeList().add(userID);
			
			/*
			 * Set the number of nodes to visit at the next level.
			 * This task is a preparation for crawling at the next level.
			 * If the user does not satisfy requirements given by you, getFollowingList() returns empty array list.
			 */
			if (curLevel < egoNetwork.level()) {
				ArrayList<Long> followingList = mDBAdapter.getFollowingList(userID);
				for (long followingUserID : followingList) {
					if (egoNetwork.getVisitedNodeList().contains(followingUserID) || queue.contains(followingUserID))
						continue;
					queue.offer(followingUserID);
					nNodesToVisit[curLevel + 1] += 1;
				}
			}
			
			// If all nodes are visited, shift cursor or exit crawling
			if (nNodesToVisit[curLevel] == 0) {
				curLevel += 1;
				if (curLevel > egoNetwork.level())
					break;
			}
		}
		
		// Graph searching is finished
		Utils.printLog(Utils.getExecutingTime("Graph searching time", crawling_start), false);
		
		exeService.shutdown();
		while (exeService.isTerminated() == false) {
			// Do nothing and wait for other running threads
		}
		
		// Mark this task as complete and then close database connections
		mDBAdapter.setSeed(egoNetwork.getSeedUserID());
		mDBAdapter.closeConnections();
		
		// Print crawling result
		Utils.printLog("### Current memory usage: " + Utils.getCurMemoryUsage() + " MB", false);
		
		// Garbage collection
		System.gc();
		
		Utils.printLog("TWITTER CRAWLING FINISHED", false);
		Utils.printLog(Utils.getExecutingTime("Total executing time", crawling_start), true);
	}
}
