package TwitterCrawler;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;

import twitter4j.HttpResponseCode;
import twitter4j.IDs;
import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.URLEntity;
import twitter4j.User;
import twitter4j.UserMentionEntity;
import Common.Settings;
import Common.TwitterNetwork;
import Common.Utils;

public class Engine {
	private static Engine mInstance = null;
	
	public static synchronized Engine getSingleton() {
		return (mInstance != null) ? mInstance : (mInstance = new Engine());
	}
	
	// Twitter application manager
	private AppManager mAppManager = null;
	private TwitterApp app;
	
	// <Crawled user ID - TwitterUser instance> pairs
	private HashMap<Long, TwitterUser> mUserMap;
	
	// <Screen name - Twitter user ID> pairs
	private HashMap<String, Long> mScreenNameMap;		// Includes only valid users
	
	public Engine() {
		this.mAppManager = AppManager.getSingleton();
		this.mUserMap = new HashMap<Long, TwitterUser>();
		this.mScreenNameMap = new HashMap<String, Long>();
		
		loadExistingData();
	}
	
	public HashMap<Long, TwitterUser> getUserMap() {
		return mUserMap;
	}
	
	public HashMap<String, Long> getScreenNameMap() {
		return mScreenNameMap;
	}
	
	private void loadExistingData() {
		// TODO: load existing file into memory
	}
	
	/**
	 * Load following users' IDs into target user's following list.
	 * The following users' IDs are obtained by Twitter REST API.
	 * @param network Target network to be crawled
	 * @param user User that data is loaded into.
	 * @param maxCount Maximum number of record to be obtained.
	 * @param writeFile Set <b>true</b> if you want to save data as a file.
	 */
	public void loadFollowings(TwitterNetwork network, TwitterUser user, int maxCount, boolean writeFile) {
		ArrayList<Long> result = new ArrayList<Long>();
		loadFollowingsIDs(network, user, maxCount, result, -1);
		user.setFollowingList(result);
		
		// Write crawled data into file
		if (writeFile == true) {
			try {
				PrintWriter writer = new PrintWriter(network.getPathFollowingData() + user.getID() + ".following", "utf-8");
				for (long followingID : result) {
					writer.println(followingID);
				}
				writer.close();
			} catch (UnsupportedEncodingException uee) {
				uee.printStackTrace();
			} catch (FileNotFoundException fnfe) {
				fnfe.printStackTrace();
			}
		}
	}
	
	public void loadFollowingsIDs(TwitterNetwork network, TwitterUser user, int maxCount, ArrayList<Long> result, long cursor) {
		String endpoint = "/friends/ids";
		if (user.isValid() == false) return;
		
		try {
			while (cursor != 0) {
				// Read friend_IDs from Twitter API per page
				app = mAppManager.getAvailableApp(endpoint);
				IDs followingsIDs = app.twitter.getFriendsIDs(user.getID(), cursor);
				
				// Set friend list per page
				for (long followingID : followingsIDs.getIDs()) {
					result.add(followingID);
					if (result.size() == maxCount) return;
				}
				
				cursor = followingsIDs.getNextCursor();
			}
		} catch (TwitterException te) {
			if (te.exceededRateLimitation()) {
				// Register current application as limited one
				mAppManager.registerLimitedApp(app, endpoint, te.getRateLimitStatus().getSecondsUntilReset());
				app.printRateLimitStatus(endpoint);
				
				// Keep crawling
				loadFollowingsIDs(network, user, maxCount, result, cursor);
			} else {
				try {
					switch (te.getStatusCode()) {
					case 401:	// Authentication credentials were missing or incorrect.
						user.setInvalid();
						break;
					case 404:	// The URI requested is invalid or the resource requested, such as a user, does not exists.
						System.out.println("Error 404 on getting followings: " + user.getID());
						user.setInvalid();
						break;
					case 503:	// The Twitter servers are up, but overloaded with requests.
					case -1:	// Caused by: java.net.UnknownHostException: api.twitter.com
						te.printStackTrace();
						Thread.sleep(5000);
						
						// Keep crawling
						loadFollowingsIDs(network, user, maxCount, result, cursor);
						break;
					default:
						te.printStackTrace();
						break;
					}
				} catch (InterruptedException ie) {
				}
			}
		}
	}
	
	/**
	 * Load followers' IDs into target user's follower list.
	 * The followers' IDs are obtained by Twitter REST API.
	 * @param network Target network to be crawled
	 * @param user User that data is loaded into.
	 * @param maxCount Maximum number of record to be obtained.
	 * @param writeFile Set <b>true</b> if you want to save data as a file.
	 */
	public void loadFollowers(TwitterNetwork network, TwitterUser user, int maxCount, boolean writeFile) {
		ArrayList<Long> result = new ArrayList<Long>();
		loadFollowersIDs(network, user, maxCount, result, -1);
		user.setFollowerList(result);
		
		// Write crawled data into file
		if (writeFile == true) {
			try {
				PrintWriter writer = new PrintWriter(network.getPathFollowerData() + user.getID() + ".follower", "utf-8");
				for (long followerID : result) {
					writer.println(followerID);
				}
				writer.close();
			} catch (UnsupportedEncodingException uee) {
				uee.printStackTrace();
			} catch (FileNotFoundException fnfe) {
				fnfe.printStackTrace();
			}
		}
	}
	
	public void loadFollowersIDs(TwitterNetwork network, TwitterUser user, int maxCount, ArrayList<Long> result, long cursor) {
		String endpoint = "/followers/ids";
		if (user.isValid() == false) return;
		
		try {
			while (cursor != 0) {
				// Read followers' IDs from Twitter API per page
				app = mAppManager.getAvailableApp(endpoint);
				IDs followersIDs = app.twitter.getFollowersIDs(user.getID(), cursor);
				
				// Set follower list per page
				for (long followerID : followersIDs.getIDs()) {
					result.add(followerID);
					if (result.size() == maxCount) return;
				}
				
				cursor = followersIDs.getNextCursor();
			}
		} catch (TwitterException te) {
			if (te.exceededRateLimitation()) {
				// Register current application as limited one
				mAppManager.registerLimitedApp(app, endpoint, te.getRateLimitStatus().getSecondsUntilReset());
				app.printRateLimitStatus(endpoint);
				
				// Keep crawling
				loadFollowersIDs(network, user, maxCount, result, cursor);
			} else {
				try {
					switch (te.getStatusCode()) {
					case 401:	// Authentication credentials were missing or incorrect.
						user.setInvalid();
						break;
					case 404:	// The URI requested is invalid or the resource requested, such as a user, does not exists.
						System.out.println("Error 404 on getting followers: " + user.getID());
						user.setInvalid();
						break;
					case 503:	// The Twitter servers are up, but overloaded with requests.
					case -1:	// Caused by: java.net.UnknownHostException: api.twitter.com
						te.printStackTrace();
						Thread.sleep(5000);
						
						// Keep crawling
						loadFollowersIDs(network, user, maxCount, result, cursor);
						break;
					default:
						te.printStackTrace();
						break;
					}
				} catch (InterruptedException ie) {
				}
			}
		}
	}
	
	/**
	 * Get friendship between followings ID list and followers ID list,
	 * and then load it into target user's friendship list.
	 * To judge friendship between users, they should mutually follow each other.
	 * The <b>reason why finding friendship should be preceded</b> is to reduce API call count.
	 * @param network Target network to be crawled
	 * @param user User that data is loaded into.
	 * @param lang A filtering option by language
	 * @param writeFile Set <b>true</b> if you want to save data as a file.
	 */
	public void loadFriendship(TwitterNetwork network, TwitterUser user, boolean writeFile) {
		if (user.isValid() == false) return;
		if (user.getFollowerList() == null || user.getFollowingList() == null) return;
		
		// Find intersection between followers and followees
		ArrayList<Long> intersection = new ArrayList<Long>();
		for (long follower : user.getFollowerList()) {
			if (user.getFollowingList().contains(follower))
				intersection.add(follower);
		}
		
		// Filtering with some criteria and then set friendship list of a user
		ArrayList<Long> friends = lookupUsers(network, intersection);
		user.setFriendshipList(friends);
		
		// Write crawled data into file
		if (writeFile == true && friends.isEmpty() == false) {
			try {
				PrintWriter writer = new PrintWriter(network.getPathFriendshipData() + user.getID() + ".friendship", "utf-8");
				for (long friendID : friends)
					writer.println(friendID);
				writer.close();
			} catch (UnsupportedEncodingException uee) {
				uee.printStackTrace();
			} catch (FileNotFoundException fnfe) {
				fnfe.printStackTrace();
			}
		}
	}
	
	/**
	 * Filter invalid users out by checking them with some criteria.
	 * @param network Target network to be crawled
	 * @param candidatesIDs Test set
	 * @return Valid Users' IDs
	 */
	public ArrayList<Long> lookupUsers(TwitterNetwork network, ArrayList<Long> candidatesIDs) {
		String endpoint = "/users/lookup";
		ArrayList<Long> validUsersIDs = new ArrayList<Long>();
		
		int cursor = 0;
		while (cursor < candidatesIDs.size()) {
			// Make buffer space and fill it with IDs
			int remaining = candidatesIDs.size() - cursor;
			long[] buffer = new long[(remaining >= 100) ? 100 : remaining];
			for (int i = 0; i < buffer.length; i++) {
				buffer[i] = candidatesIDs.get(cursor);
				cursor++;
			}
			
			// Lookup buffer
			while (true) {
				try {
					app = mAppManager.getAvailableApp(endpoint);
					ResponseList<User> testset = app.twitter.lookupUsers(buffer);
					for (User user : testset) {
						if (handleUserValidity(network, user) != -1L)
							validUsersIDs.add(user.getId());
					}
					break;
				} catch (TwitterException te) {
					boolean retry = true;
					if (te.exceededRateLimitation()) {				// 429: Rate limit exceeded
						mAppManager.registerLimitedApp(app, endpoint, te.getRateLimitStatus().getSecondsUntilReset());
						app.printRateLimitStatus(endpoint);
					} else {
						switch (te.getStatusCode()) {
						case HttpResponseCode.SERVICE_UNAVAILABLE:	// 503: The Twitter servers are up, but overloaded with requests.
						case -1:									// Caused by: java.net.UnknownHostException: api.twitter.com
							new Utils().sleep(5000);				// Retry crawling 5 seconds later
							break;
						case HttpResponseCode.UNAUTHORIZED:			// 401: Authentication credentials were missing or incorrect.
						case HttpResponseCode.NOT_FOUND:			// 404: The URI requested is invalid or the resource requested, such as a user, does not exists.
						default:									// Unknown exception occurs
							te.printStackTrace();
							for (long id : buffer) {
								User user = getUser(id);
								if (handleUserValidity(network, user) != -1L)
									validUsersIDs.add(id);
							}
							retry = false;							// Do not retry anymore
							break;
						}
					}
					if (retry == false) break;
				}
			}
		}
		
		return validUsersIDs;
	}
	
	/**
	 * Check if the user is valid and then register his validity as a global information.
	 * @param network Target network to be crawled
	 * @param user Test user
	 * @return User ID if the user is valid, -1L otherwise.
	 */
	public long handleUserValidity(TwitterNetwork network, User user) {
		if (user == null) return -1L;
		
		if (Settings.isRequirementSatisfied(network, user, true)) {
			mScreenNameMap.put(user.getScreenName(), user.getId());
			return user.getId();
		} else {
			if (mUserMap.containsKey(user.getId())) {
				mUserMap.get(user.getId()).setInvalid();
			} else {
				TwitterUser invalidUser = new TwitterUser(user.getId());
				invalidUser.setInvalid();
				mUserMap.put(user.getId(), invalidUser);
			}
			return -1L;
		}
	}
	
	/**
	 * Get 'User' instance for the given user.
	 * @param screenName
	 * @return User instance
	 */
	public User getUser(long userID) {
		String endpoint = "/users/show/:id";
		
		while (true) {
			try {
				app = mAppManager.getAvailableApp(endpoint);
				User user = app.twitter.showUser(userID);
				return user;
			} catch (TwitterException te) {
				if (te.exceededRateLimitation()) {				// 429: Rate limit exceeded
					mAppManager.registerLimitedApp(app, endpoint, te.getRateLimitStatus().getSecondsUntilReset());
					app.printRateLimitStatus(endpoint);
				} else {
					switch (te.getStatusCode()) {
					default:									// Unknown exception occurs
						te.printStackTrace();
					case HttpResponseCode.UNAUTHORIZED:			// 401: Authentication credentials were missing or incorrect.
					case HttpResponseCode.NOT_FOUND:			// 404: The URI requested is invalid or the resource requested, such as a user, does not exists.
						return null;
					case HttpResponseCode.SERVICE_UNAVAILABLE:	// 503: The Twitter servers are up, but overloaded with requests.
					case -1:									// Caused by: java.net.UnknownHostException: api.twitter.com
						new Utils().sleep(5000);				// Retry crawling 5 seconds later
						break;
					}
				}
			}
		}
	}
	
	/**
	 * Get timeline data of a given user. Note that there are pagination limits Rest API Limit.
	 * Clients may access a theoretical <b>maximum of 3,200 statuses</b> via the page
	 * and count parameters for the user_timeline REST API methods.
	 * Requests for more than the limit will result in a reply with a status code of 200
	 * and an empty result in the format requested.
	 * To ensure performance of the site, this artificial limit is temporarily in place.
	 * @param network Target network to be crawled
	 * @param user Target user
	 * @param writeFile
	 * @return Timeline of the given user
	 */
	public ArrayList<Status> getTimeline(TwitterNetwork network, TwitterUser user, boolean writeFile) {
		return getTimeline(network, user, true, writeFile);
	}
	
	public ArrayList<Status> getTimeline(TwitterNetwork network, TwitterUser user, boolean getSubInfo, boolean writeFile) {
		if (user.isValid() == false) return null;
		
		String endpoint = "/statuses/user_timeline";
		ArrayList<Status> timeline = new ArrayList<Status>();
		
		int page = 1;
		while (true) {
			try {
				app = mAppManager.getAvailableApp(endpoint);
				ResponseList<Status> onePage = app.twitter.getUserTimeline(user.getID(), new Paging(page, 200));
				if (onePage.size() == 0)
					break;
				timeline.addAll(onePage);
				page++;
			} catch (TwitterException te) {
				boolean retry = true;
				if (te.exceededRateLimitation()) {				// 429: Rate limit exceeded
					mAppManager.registerLimitedApp(app, endpoint, te.getRateLimitStatus().getSecondsUntilReset());
					app.printRateLimitStatus(endpoint);
				} else {
					switch (te.getStatusCode()) {
					default:									// Unknown exception occurs
						te.printStackTrace();
					case HttpResponseCode.UNAUTHORIZED:			// 401: Authentication credentials were missing or incorrect.
					case HttpResponseCode.NOT_FOUND:			// 404: The URI requested is invalid or the resource requested, such as a user, does not exists.
						user.setInvalid();
						retry = false;							// Do not retry anymore
						break;
					case HttpResponseCode.SERVICE_UNAVAILABLE:	// 503: The Twitter servers are up, but overloaded with requests.
					case -1:									// Caused by: java.net.UnknownHostException: api.twitter.com
						new Utils().sleep(5000);				// Retry crawling 5 seconds later
						break;
					}
				}
				if (retry == false) break;
			}
		}
		
		if (writeFile == true) {
			try {
				// Write tweet data
				PrintWriter writer = new PrintWriter(network.getPathTweetData() + user.getID() + ".timeline", "utf-8");
				for (Status tweet : timeline)
					writer.println(tweet.getId() + "\t" + tweet.getText());
				writer.close();
			} catch (UnsupportedEncodingException uee) {
				uee.printStackTrace();
			} catch (FileNotFoundException fnfe) {
				fnfe.printStackTrace();
			}
		}
		
		/**
		 * A set of tweet messages of timeline involve user's shared tweets, mention, and retweets.
		 * Extract shared tweets, retweet, and mention data from the obtained timeline in the above.
		 */
		if (getSubInfo == true) {
			HashMap<Long, String> shareList = getSharedTweets(user.getID(), timeline);
			ArrayList<Status> retweetList = getRetweets(user.getID(), timeline);
			HashMap<Long, Integer> mentionList = getMentionCount(user.getID(), timeline);
			
			if (writeFile == true) {
				try {
					PrintWriter writer = new PrintWriter(network.getPathShareData() + user.getID() + ".share", "utf-8");
					for (HashMap.Entry<Long, String> entry : shareList.entrySet())
						writer.println(entry.getValue() + "\t" + entry.getKey());
					writer.close();
					
					writer = new PrintWriter(network.getPathRetweetData() + user.getID() + ".retweet", "utf-8");
					for (Status retweet : retweetList)
						writer.println(retweet.getRetweetedStatus().getUser().getId() + "\t" + retweet.getRetweetedStatus().getId() + "\t" + retweet.getText());
					writer.close();
					
					writer = new PrintWriter(network.getPathMentionData() + user.getID() + ".mention", "utf-8");
					for (HashMap.Entry<Long, Integer> entry : mentionList.entrySet())
						writer.println(entry.getKey() + "\t" + entry.getValue());
					writer.close();
				} catch (UnsupportedEncodingException uee) {
					uee.printStackTrace();
				} catch (FileNotFoundException fnfe) {
					fnfe.printStackTrace();
				}
			}
		}
		
		return timeline;
	}
	
	/**
	 * Get shared tweet ID and its author's name pairs from the given timeline.
	 * @param userID
	 * @param timeline
	 * @return <Shared tweet ID - author name> pairs
	 */
	public HashMap<Long, String> getSharedTweets(long userID, ArrayList<Status> timeline) {
		HashMap<Long, String> shareList = new HashMap<Long, String>();
		for (Status tweet : timeline) {
			// Extract user's share history
			URLEntity[] urlEntities = tweet.getURLEntities();
			for (int i = 0; i < urlEntities.length; i++) {
				String expandedURL = urlEntities[i].getExpandedURL();
				
				if (expandedURL.startsWith("https://twitter.com/")) {
					String tokens[] = expandedURL.split("/");
					
					// Check if the status is shared from another Twitter user's timeline
					for (int j = 0; j < tokens.length - 1; j++) {
						try {
							if (tokens[j].equals("status") == true) {
								String targetUser = tokens[j - 1];
								String targetTweet = tokens[j + 1];
								Long targetTweetID = null;
								for (int c = 1; c <= targetTweet.length(); c++) {
									try {
										targetTweetID = Long.parseLong(targetTweet.substring(0, c));
									} catch (Exception e) {
										if (c == 1)
											targetTweetID = null;
										else
											targetTweetID = Long.parseLong(targetTweet.substring(0, c - 1));
										break;
									}
								}
								
								if (targetTweetID != null)
									shareList.put(targetTweetID, targetUser);
								break;
							}
						} catch (Exception e) {
							System.out.println(expandedURL);
						}
					}
				}
			}
		}
		return shareList;
	}
	
	public ArrayList<Status> getRetweets(long userID, ArrayList<Status> timeline) {
		ArrayList<Status> retweetList = new ArrayList<Status>();
		for (Status tweet : timeline) {
			if (tweet.isRetweet() == true && userID != tweet.getId())
				retweetList.add(tweet);
		}
		return retweetList;
	}
	
	public HashMap<Long, Integer> getMentionCount(long userID, ArrayList<Status> timeline) {
		HashMap<Long, Integer> mentionList = new HashMap<Long, Integer>();
		for (Status tweet : timeline) {
			UserMentionEntity[] mentionEntities = tweet.getUserMentionEntities();
			for (int i = 0; i < mentionEntities.length; i++) {
				Long targetUserID = mentionEntities[i].getId();
				if (userID != targetUserID) {
					if (mentionList.containsKey(targetUserID)) {
						int cnt = mentionList.get(targetUserID);
						mentionList.put(targetUserID, cnt + 1);
					} else {
						mentionList.put(targetUserID, 1);
					}
				}
			}
		}
		return mentionList;
	}
	
	/**
	 * Get favorites data of a given user using Twitter API.
	 * @param network Target network to be crawled
	 * @param user Target user
	 * @param writeFile
	 * @return Favorite tweet list of target user
	 */
	public ArrayList<Status> getFavorites(TwitterNetwork network, TwitterUser user, boolean writeFile) {
		if (user.isValid() == false) return null;
		
		String endpoint = "/favorites/list";
		ArrayList<Status> favorites = new ArrayList<Status>();
		
		int page = 1;
		while (true) {
			try {
				app = mAppManager.getAvailableApp(endpoint);
				ResponseList<Status> onePage = app.twitter.getFavorites(user.getID(), new Paging(page, 200));
				if (onePage.size() == 0)
					break;
				favorites.addAll(onePage);
				page++;
			} catch (TwitterException te) {
				boolean retry = true;
				if (te.exceededRateLimitation()) {				// 429: Rate limit exceeded
					mAppManager.registerLimitedApp(app, endpoint, te.getRateLimitStatus().getSecondsUntilReset());
					app.printRateLimitStatus(endpoint);
				} else {
					switch (te.getStatusCode()) {
					default:									// Unknown exception occurs
						te.printStackTrace();
					case HttpResponseCode.UNAUTHORIZED:			// 401: Authentication credentials were missing or incorrect.
					case HttpResponseCode.NOT_FOUND:			// 404: The URI requested is invalid or the resource requested, such as a user, does not exists.
						user.setInvalid();
						retry = false;							// Do not retry anymore
						break;
					case HttpResponseCode.SERVICE_UNAVAILABLE:	// 503: The Twitter servers are up, but overloaded with requests.
					case -1:									// Caused by: java.net.UnknownHostException: api.twitter.com
						new Utils().sleep(5000);				// Retry crawling 5 seconds later
						break;
					}
				}
				if (retry == false) break;
			}
		}
		
		if (writeFile == true) {
			try {
				PrintWriter writer = new PrintWriter(network.getPathFavoriteData() + user.getID() + ".favorite", "utf-8");
				for (Status favorite : favorites)
					writer.println(favorite.getUser().getId() + "\t" + favorite.getId() + "\t" + favorite.getText());
				writer.close();
			} catch (UnsupportedEncodingException uee) {
				uee.printStackTrace();
			} catch (FileNotFoundException fnfe) {
				fnfe.printStackTrace();
			}
		}
		
		return favorites;
	}
}
