package crawling;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import main.Settings;
import main.TwitterUser;
import tool.Utils;
import twitter4j.HttpResponseCode;
import twitter4j.IDs;
import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.URLEntity;
import twitter4j.User;
import twitter4j.UserMentionEntity;

public class Engine {
	private static Engine mInstance = null;
	
	public static synchronized Engine getSingleton() {
		return (mInstance != null) ? mInstance : (mInstance = new Engine());
	}
	
	// Twitter application manager
	private AppManager mAppManager = null;
	private TwitterApp app;
	
	/**
	 * <Crawled user ID - TwitterUser instance> pairs
	 * This hash map contains users(nodes) which have ever been looked up at least once.
	 */
	private HashMap<Long, TwitterUser> mUserMap = null;
	
	public Engine() {
		this.mAppManager = AppManager.getSingleton();
	}
	
	public HashMap<Long, TwitterUser> getUserMap() {
		return mUserMap;
	}
	
	public void setUserMap(HashMap<Long, TwitterUser> userMap) {
		this.mUserMap = userMap;
	}
	
	/**
	 * The following users' IDs are obtained by Twitter REST API.
	 * @param user User that data is loaded into.
	 * @param maxCount Maximum number of record to be obtained
	 * @return Following users' IDs
	 */
	public ArrayList<Long> getFollowings(TwitterUser user, int maxCount) {
		String endpoint = "/friends/ids";
		ArrayList<Long> followingUsers = new ArrayList<Long>();
		
		long cursor = -1;
		while (cursor != 0) {
			try {
				app = mAppManager.getAvailableApp(endpoint);
				IDs followingsIDs = app.twitter.getFriendsIDs(user.getID(), cursor);
				for (long followingID : followingsIDs.getIDs()) {
					followingUsers.add(followingID);
					if (followingUsers.size() == maxCount)
						break;
				}
				cursor = followingsIDs.getNextCursor();
			} catch (TwitterException te) {
				boolean retry = true;
				if (te.exceededRateLimitation()) {				// 429: Rate limit exceeded
					mAppManager.registerLimitedApp(app, endpoint, te.getRateLimitStatus().getSecondsUntilReset());
				} else {
					switch (te.getStatusCode()) {
					default:									// Unknown exception occurs
						te.printStackTrace();
					case HttpResponseCode.UNAUTHORIZED:			// 401: Authentication credentials were missing or incorrect.
					case HttpResponseCode.NOT_FOUND:			// 404: The URI requested is invalid or the resource requested, such as a user, does not exists.
						user.setProtected();
						retry = false;							// Do not retry anymore
						break;
					case HttpResponseCode.SERVICE_UNAVAILABLE:	// 503: The Twitter servers are up, but overloaded with requests.
					case -1:									// Caused by: java.net.UnknownHostException: api.twitter.com
						Utils.sleep(5000);					// Retry crawling 5 seconds later
						break;
					}
				}
				if (retry == false) break;
			}
		}
		
		return followingUsers;
	}
	
	/**
	 * The followers' IDs are obtained by Twitter REST API.
	 * @param user User that data is loaded into.
	 * @param maxCount Maximum number of record to be obtained
	 * @return Followers' IDs
	 */
	public ArrayList<Long> getFollowers(TwitterUser user, int maxCount) {
		String endpoint = "/followers/ids";
		ArrayList<Long> followers = new ArrayList<Long>();
		
		long cursor = -1;
		while (cursor != 0) {
			try {
				app = mAppManager.getAvailableApp(endpoint);
				IDs followersIDs = app.twitter.getFollowersIDs(user.getID(), cursor);
				for (long followerID : followersIDs.getIDs()) {
					followers.add(followerID);
					if (followers.size() == maxCount)
						break;
				}
				cursor = followersIDs.getNextCursor();
			} catch (TwitterException te) {
				boolean retry = true;
				if (te.exceededRateLimitation()) {				// 429: Rate limit exceeded
					mAppManager.registerLimitedApp(app, endpoint, te.getRateLimitStatus().getSecondsUntilReset());
				} else {
					switch (te.getStatusCode()) {
					default:									// Unknown exception occurs
						te.printStackTrace();
					case HttpResponseCode.UNAUTHORIZED:			// 401: Authentication credentials were missing or incorrect.
					case HttpResponseCode.NOT_FOUND:			// 404: The URI requested is invalid or the resource requested, such as a user, does not exists.
						user.setProtected();
						retry = false;							// Do not retry anymore
						break;
					case HttpResponseCode.SERVICE_UNAVAILABLE:	// 503: The Twitter servers are up, but overloaded with requests.
					case -1:									// Caused by: java.net.UnknownHostException: api.twitter.com
						Utils.sleep(5000);						// Retry crawling 5 seconds later
						break;
					}
				}
				if (retry == false) break;
			}
		}
		
		return followers;
	}
	
	/**
	 * Get friendship from intersection between followings ID list and followers ID list.
	 * This task involves looking up process.
	 * @param followings Following user list
	 * @param followers Follower list
	 * @param useFiltering If <code>true</code>, we can get filtered friendship list by setting criteria
	 * @return Friendship ID list
	 */
	public ArrayList<Long> getFriendship(ArrayList<Long> followings, ArrayList<Long> followers, boolean useFiltering) {
		if (followings == null || followers == null) return null;
		
		// Find intersection between followers and followees
		ArrayList<Long> intersection = new ArrayList<Long>();
		for (long follower : followers) {
			if (followings.contains(follower))
				intersection.add(follower);
		}
		
		if (useFiltering) {
			// Filtering with some criteria and then set friendship list of a user
			ArrayList<User> users = lookupUsers(intersection);
			
			ArrayList<Long> validUsersIDs = new ArrayList<Long>();
			for (User user : users) {
				// Register user into user map
				if (mUserMap.containsKey(user.getId()) == false)
					mUserMap.put(user.getId(), new TwitterUser(user));
				
				// Test if the user is valid.
				if (Settings.isValidUser(user))
					validUsersIDs.add(user.getId());
			}
			return validUsersIDs;
		} else
			return intersection;
	}
	
	/**
	 * Look up user list.
	 * If a requested user is unknown, suspended, or deleted, then that user will not be returned in the results list.
	 * If none of your lookup criteria can be satisfied by returning a user object, a HTTP 404 will be thrown.
	 * @param userList Users to be looked up
	 * @return User instances
	 */
	public ArrayList<User> lookupUsers(ArrayList<Long> userList) {
		String endpoint = "/users/lookup";
		ArrayList<User> users = new ArrayList<User>();
		
		int cursor = 0;
		while (cursor < userList.size()) {
			// Make buffer space and fill it with IDs
			ArrayList<Long> bufferList = new ArrayList<Long>();
			for (int i = cursor; i < userList.size() && bufferList.size() < 100; i++) {
				long userID = userList.get(i);
				bufferList.add(userID);
				cursor++;
			}
			if (bufferList.isEmpty()) break;
			long[] buffer = Utils.getArray(bufferList);
			
			// Lookup buffer
			while (true) {
				try {
					app = mAppManager.getAvailableApp(endpoint);
					ResponseList<User> testset = app.twitter.lookupUsers(buffer);
					for (User user : testset)
						users.add(user);
					break;
				} catch (TwitterException te) {
					boolean retry = true;
					if (te.exceededRateLimitation()) {				// 429: Rate limit exceeded
						mAppManager.registerLimitedApp(app, endpoint, te.getRateLimitStatus().getSecondsUntilReset());
					} else {
						switch (te.getStatusCode()) {
						default:									// Unknown exception occurs
							te.printStackTrace();
						case HttpResponseCode.UNAUTHORIZED:			// 401: Authentication credentials were missing or incorrect.
						case HttpResponseCode.NOT_FOUND:			// 404: The URI requested is invalid or the resource requested, such as a user, does not exists.
							System.out.println("Lookup error: " + te.getStatusCode() + ", buffer: " + buffer);
							for (long id : buffer)
								users.add(showUser(id));
							retry = false;							// Do not retry anymore
							break;
						case HttpResponseCode.SERVICE_UNAVAILABLE:	// 503: The Twitter servers are up, but overloaded with requests.
						case -1:									// Caused by: java.net.UnknownHostException: api.twitter.com
							Utils.sleep(5000);						// Retry crawling 5 seconds later
							break;
						}
					}
					if (retry == false) break;
				}
			}
		}
		
		return users;
	}
	
	/**
	 * Get 'User' instance for the given user.
	 * @param screenName
	 * @return User instance
	 */
	public User showUser(long userID) {
		String endpoint = "/users/show/:id";
		
		while (true) {
			try {
				app = mAppManager.getAvailableApp(endpoint);
				User user = app.twitter.showUser(userID);
				return user;
			} catch (TwitterException te) {
				if (te.exceededRateLimitation()) {				// 429: Rate limit exceeded
					mAppManager.registerLimitedApp(app, endpoint, te.getRateLimitStatus().getSecondsUntilReset());
				} else {
					switch (te.getStatusCode()) {
					default:									// Unknown exception occurs
						te.printStackTrace();
					case HttpResponseCode.UNAUTHORIZED:			// 401: Authentication credentials were missing or incorrect.
					case HttpResponseCode.NOT_FOUND:			// 404: The URI requested is invalid or the resource requested, such as a user, does not exists.
						return null;
					case HttpResponseCode.SERVICE_UNAVAILABLE:	// 503: The Twitter servers are up, but overloaded with requests.
					case -1:									// Caused by: java.net.UnknownHostException: api.twitter.com
						Utils.sleep(5000);						// Retry crawling 5 seconds later
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
	 * @param user Target user
	 * @return Timeline of the given user
	 */
	public ArrayList<Status> getTimeline(TwitterUser user) {
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
				} else {
					switch (te.getStatusCode()) {
					default:									// Unknown exception occurs
						te.printStackTrace();
					case HttpResponseCode.UNAUTHORIZED:			// 401: Authentication credentials were missing or incorrect.
					case HttpResponseCode.NOT_FOUND:			// 404: The URI requested is invalid or the resource requested, such as a user, does not exists.
						user.setProtected();
						retry = false;							// Do not retry anymore
						break;
					case HttpResponseCode.SERVICE_UNAVAILABLE:	// 503: The Twitter servers are up, but overloaded with requests.
					case -1:									// Caused by: java.net.UnknownHostException: api.twitter.com
						Utils.sleep(5000);						// Retry crawling 5 seconds later
						break;
					}
				}
				if (retry == false) break;
			}
		}
		
		return timeline;
	}
	
	/**
	 * Get shared tweet ID and its author's name pairs from the given timeline.
	 * @param timeline
	 * @return <Shared tweet ID - author name> pairs
	 */
	public ArrayList<Long> getSharedTweets(ArrayList<Status> timeline) {
		ArrayList<Long> shareList = new ArrayList<Long>();
		for (Status tweet : timeline) {
			if (tweet.isRetweet()) continue;
			
			// Extract user's share history
			URLEntity[] urlEntities = tweet.getURLEntities();
			for (int i = 0; i < urlEntities.length; i++) {
				String expandedURL = urlEntities[i].getExpandedURL();
				if (expandedURL.startsWith("https://twitter.com/") || expandedURL.startsWith("http://twitter.com/")) {
					String tokens[] = expandedURL.split("/");
					
					// Check if the status is shared from another Twitter user's timeline
					for (int j = 0; j < tokens.length - 1; j++) {
						try {
							if (tokens[j].equals("status") == true) {
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
									shareList.add(targetTweetID);
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
	
	public ArrayList<Status> getRetweets(ArrayList<Status> timeline) {
		ArrayList<Status> retweetList = new ArrayList<Status>();
		for (Status tweet : timeline) {
			if (tweet.isRetweet())
				retweetList.add(tweet.getRetweetedStatus());
		}
		return retweetList;
	}
	
	public HashMap<Long, ArrayList<Date>> getMentionHistory(long userID, ArrayList<Status> timeline) {
		HashMap<Long, ArrayList<Date>> mentionHistory = new HashMap<Long, ArrayList<Date>>();
		for (Status tweet : timeline) {
			UserMentionEntity[] mentionEntities = tweet.getUserMentionEntities();
			for (int i = 0; i < mentionEntities.length; i++) {
				Long targetUserID = mentionEntities[i].getId();
				if (userID != targetUserID) {
					ArrayList<Date> mentionedDates = mentionHistory.get(targetUserID);
					if (mentionedDates == null)
						mentionedDates = new ArrayList<Date>();
					mentionedDates.add(tweet.getCreatedAt());
					mentionHistory.put(targetUserID, mentionedDates);
				}
			}
		}
		return mentionHistory;
	}
	
	/**
	 * Get favorites data of a given user using Twitter API.
	 * @param user Target user
	 * @return Favorite tweet list of target user
	 */
	public ArrayList<Status> getFavorites(TwitterUser user) {
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
				} else {
					switch (te.getStatusCode()) {
					default:									// Unknown exception occurs
						te.printStackTrace();
					case HttpResponseCode.UNAUTHORIZED:			// 401: Authentication credentials were missing or incorrect.
					case HttpResponseCode.NOT_FOUND:			// 404: The URI requested is invalid or the resource requested, such as a user, does not exists.
						user.setProtected();
						retry = false;							// Do not retry anymore
						break;
					case HttpResponseCode.SERVICE_UNAVAILABLE:	// 503: The Twitter servers are up, but overloaded with requests.
					case -1:									// Caused by: java.net.UnknownHostException: api.twitter.com
						Utils.sleep(5000);						// Retry crawling 5 seconds later
						break;
					}
				}
				if (retry == false) break;
			}
		}
		
		return favorites;
	}
}
