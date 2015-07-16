package crawler;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import tool.Utils;
import twitter4j.HttpResponseCode;
import twitter4j.IDs;
import twitter4j.Paging;
import twitter4j.Query;
import twitter4j.QueryResult;
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
	
	public Engine() {
		this.mAppManager = AppManager.getSingleton();
	}
	
	/**
	 * The following users' IDs are obtained by Twitter REST API.
	 * @param userID User that data is loaded into.
	 * @param maxCount Maximum number of record to be obtained
	 * @return Following users' IDs
	 */
	public ArrayList<Long> getFollowings(long userID, int maxCount) {
		String endpoint = "/friends/ids";
		ArrayList<Long> followingUsers = new ArrayList<Long>();
		
		long cursor = -1;
		while (cursor != 0) {
			TwitterApp app = mAppManager.getAvailableApp(endpoint);
			try {
				IDs followingsIDs = app.twitter.getFriendsIDs(userID, cursor);
				for (long followingID : followingsIDs.getIDs()) {
					followingUsers.add(followingID);
					if (followingUsers.size() == maxCount)
						break;
				}
				cursor = followingsIDs.getNextCursor();
			} catch (TwitterException te) {
				boolean retry = true;
				if (te.exceededRateLimitation()) {					// 429: Rate limit exceeded
					mAppManager.registerLimitedApp(app, endpoint, te.getRateLimitStatus().getSecondsUntilReset());
				} else {
					switch (te.getStatusCode()) {
					default:										// Unknown exception occurs
						te.printStackTrace();
					case HttpResponseCode.UNAUTHORIZED:				// 401: Authentication credentials were missing or incorrect.
					case HttpResponseCode.NOT_FOUND:				// 404: The URI requested is invalid or the resource requested, such as a user, does not exists.
						retry = false;								// Do not retry anymore
						break;
					case HttpResponseCode.INTERNAL_SERVER_ERROR:	// 500: Something is broken. Please post to the group so the Twitter team can investigate.
					case HttpResponseCode.SERVICE_UNAVAILABLE:		// 503: The Twitter servers are up, but overloaded with requests.
					case -1:										// Caused by: java.net.UnknownHostException: api.twitter.com
						Utils.sleep(5000);							// Retry crawling 5 seconds later
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
	 * @param userID User that data is loaded into.
	 * @param maxCount Maximum number of record to be obtained
	 * @return Followers' IDs
	 */
	public ArrayList<Long> getFollowers(long userID, int maxCount) {
		String endpoint = "/followers/ids";
		ArrayList<Long> followers = new ArrayList<Long>();
		
		long cursor = -1;
		while (cursor != 0) {
			TwitterApp app = mAppManager.getAvailableApp(endpoint);
			try {
				IDs followersIDs = app.twitter.getFollowersIDs(userID, cursor);
				for (long followerID : followersIDs.getIDs()) {
					followers.add(followerID);
					if (followers.size() == maxCount)
						break;
				}
				cursor = followersIDs.getNextCursor();
			} catch (TwitterException te) {
				boolean retry = true;
				if (te.exceededRateLimitation()) {					// 429: Rate limit exceeded
					mAppManager.registerLimitedApp(app, endpoint, te.getRateLimitStatus().getSecondsUntilReset());
				} else {
					switch (te.getStatusCode()) {
					default:										// Unknown exception occurs
						te.printStackTrace();
					case HttpResponseCode.UNAUTHORIZED:				// 401: Authentication credentials were missing or incorrect.
					case HttpResponseCode.NOT_FOUND:				// 404: The URI requested is invalid or the resource requested, such as a user, does not exists.
						retry = false;								// Do not retry anymore
						break;
					case HttpResponseCode.INTERNAL_SERVER_ERROR:	// 500: Something is broken. Please post to the group so the Twitter team can investigate.
					case HttpResponseCode.SERVICE_UNAVAILABLE:		// 503: The Twitter servers are up, but overloaded with requests.
					case -1:										// Caused by: java.net.UnknownHostException: api.twitter.com
						Utils.sleep(5000);							// Retry crawling 5 seconds later
						break;
					}
				}
				if (retry == false) break;
			}
		}
		
		return followers;
	}
	
	/**
	 * Look up user list.
	 * If a requested user is unknown, suspended, or deleted, then that user will not be returned in the results list.
	 * If none of your lookup criteria can be satisfied by returning a user object, a HTTP 404 will be thrown.
	 * @param userList Users to be looked up
	 * @return User instances
	 */
	public ArrayList<User> lookupUsersByID(ArrayList<Long> userList) {
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
			long[] buffer = Utils.getLongArray(bufferList);
			
			// Lookup buffer
			while (true) {
				TwitterApp app = mAppManager.getAvailableApp(endpoint);
				try {
					ResponseList<User> testset = app.twitter.lookupUsers(buffer);
					for (User user : testset)
						users.add(user);
					break;
				} catch (TwitterException te) {
					boolean retry = true;
					if (te.exceededRateLimitation()) {					// 429: Rate limit exceeded
						mAppManager.registerLimitedApp(app, endpoint, te.getRateLimitStatus().getSecondsUntilReset());
					} else {
						switch (te.getStatusCode()) {
						default:										// Unknown exception occurs
							te.printStackTrace();
						case HttpResponseCode.UNAUTHORIZED:				// 401: Authentication credentials were missing or incorrect.
						case HttpResponseCode.NOT_FOUND:				// 404: The URI requested is invalid or the resource requested, such as a user, does not exists.
							System.out.println("Lookup error: " + te.getStatusCode() + ", buffer: " + buffer);
							for (long id : buffer)
								users.add(showUserByID(id));
							retry = false;								// Do not retry anymore
							break;
						case HttpResponseCode.INTERNAL_SERVER_ERROR:	// 500: Something is broken. Please post to the group so the Twitter team can investigate.
						case HttpResponseCode.SERVICE_UNAVAILABLE:		// 503: The Twitter servers are up, but overloaded with requests.
						case -1:										// Caused by: java.net.UnknownHostException: api.twitter.com
							Utils.sleep(5000);							// Retry crawling 5 seconds later
							break;
						}
					}
					if (retry == false) break;
				}
			}
		}
		
		return users;
	}
	
	public ArrayList<User> lookupUsersByScreenName(ArrayList<String> userList) {
		String endpoint = "/users/lookup";
		ArrayList<User> users = new ArrayList<User>();
		
		int cursor = 0;
		while (cursor < userList.size()) {
			// Make buffer space and fill it with IDs
			ArrayList<String> bufferList = new ArrayList<String>();
			for (int i = cursor; i < userList.size() && bufferList.size() < 100; i++) {
				String screenName = userList.get(i);
				bufferList.add(screenName);
				cursor++;
			}
			if (bufferList.isEmpty()) break;
			String[] buffer = Utils.getStringArray(bufferList);
			
			// Lookup buffer
			while (true) {
				TwitterApp app = mAppManager.getAvailableApp(endpoint);
				try {
					ResponseList<User> testset = app.twitter.lookupUsers(buffer);
					for (User user : testset)
						users.add(user);
					break;
				} catch (TwitterException te) {
					boolean retry = true;
					if (te.exceededRateLimitation()) {					// 429: Rate limit exceeded
						mAppManager.registerLimitedApp(app, endpoint, te.getRateLimitStatus().getSecondsUntilReset());
					} else {
						switch (te.getStatusCode()) {
						default:										// Unknown exception occurs
							te.printStackTrace();
						case HttpResponseCode.UNAUTHORIZED:				// 401: Authentication credentials were missing or incorrect.
						case HttpResponseCode.NOT_FOUND:				// 404: The URI requested is invalid or the resource requested, such as a user, does not exists.
							System.out.println("Lookup error: " + te.getStatusCode() + ", buffer: " + buffer);
							for (String id : buffer)
								users.add(showUserByScreenName(id));
							retry = false;								// Do not retry anymore
							break;
						case HttpResponseCode.INTERNAL_SERVER_ERROR:	// 500: Something is broken. Please post to the group so the Twitter team can investigate.
						case HttpResponseCode.SERVICE_UNAVAILABLE:		// 503: The Twitter servers are up, but overloaded with requests.
						case -1:										// Caused by: java.net.UnknownHostException: api.twitter.com
							Utils.sleep(5000);							// Retry crawling 5 seconds later
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
	 * @param userID
	 * @return User instance
	 */
	public User showUserByID(long userID) {
		String endpoint = "/users/show";
		
		while (true) {
			TwitterApp app = mAppManager.getAvailableApp(endpoint);
			try {
				User user = app.twitter.showUser(userID);
				return user;
			} catch (TwitterException te) {
				if (te.exceededRateLimitation()) {					// 429: Rate limit exceeded
					mAppManager.registerLimitedApp(app, endpoint, te.getRateLimitStatus().getSecondsUntilReset());
				} else {
					switch (te.getStatusCode()) {
					default:										// Unknown exception occurs
						te.printStackTrace();
					case HttpResponseCode.UNAUTHORIZED:				// 401: Authentication credentials were missing or incorrect.
					case HttpResponseCode.NOT_FOUND:				// 404: The URI requested is invalid or the resource requested, such as a user, does not exists.
						return null;
					case HttpResponseCode.INTERNAL_SERVER_ERROR:	// 500: Something is broken. Please post to the group so the Twitter team can investigate.
					case HttpResponseCode.SERVICE_UNAVAILABLE:		// 503: The Twitter servers are up, but overloaded with requests.
					case -1:										// Caused by: java.net.UnknownHostException: api.twitter.com
						Utils.sleep(5000);							// Retry crawling 5 seconds later
						break;
					}
				}
			}
		}
	}
	
	public User showUserByScreenName(String username) {
		String endpoint = "/users/show";
		
		while (true) {
			TwitterApp app = mAppManager.getAvailableApp(endpoint);
			try {
				User user = app.twitter.showUser(username);
				return user;
			} catch (TwitterException te) {
				if (te.exceededRateLimitation()) {					// 429: Rate limit exceeded
					mAppManager.registerLimitedApp(app, endpoint, te.getRateLimitStatus().getSecondsUntilReset());
				} else {
					switch (te.getStatusCode()) {
					default:										// Unknown exception occurs
						te.printStackTrace();
					case HttpResponseCode.UNAUTHORIZED:				// 401: Authentication credentials were missing or incorrect.
					case HttpResponseCode.NOT_FOUND:				// 404: The URI requested is invalid or the resource requested, such as a user, does not exists.
						return null;
					case HttpResponseCode.INTERNAL_SERVER_ERROR:	// 500: Something is broken. Please post to the group so the Twitter team can investigate.
					case HttpResponseCode.SERVICE_UNAVAILABLE:		// 503: The Twitter servers are up, but overloaded with requests.
					case -1:										// Caused by: java.net.UnknownHostException: api.twitter.com
						Utils.sleep(5000);							// Retry crawling 5 seconds later
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
	 * @param userID Target user's ID
	 * @return Timeline of the given user
	 */
	public ArrayList<Status> getTimeline(long userID) {
		String endpoint = "/statuses/user_timeline";
		ArrayList<Status> timeline = new ArrayList<Status>();
		
		int page = 1;
		while (true) {
			TwitterApp app = mAppManager.getAvailableApp(endpoint);
			try {
				ResponseList<Status> onePage = app.twitter.getUserTimeline(userID, new Paging(page, 200));
				if (onePage.isEmpty()) break;
				timeline.addAll(onePage);
				page++;
			} catch (TwitterException te) {
				boolean retry = true;
				if (te.exceededRateLimitation()) {					// 429: Rate limit exceeded
					mAppManager.registerLimitedApp(app, endpoint, te.getRateLimitStatus().getSecondsUntilReset());
				} else {
					switch (te.getStatusCode()) {
					default:										// Unknown exception occurs
						te.printStackTrace();
					case HttpResponseCode.UNAUTHORIZED:				// 401: Authentication credentials were missing or incorrect.
					case HttpResponseCode.NOT_FOUND:				// 404: The URI requested is invalid or the resource requested, such as a user, does not exists.
						retry = false;								// Do not retry anymore
						break;
					case HttpResponseCode.INTERNAL_SERVER_ERROR:	// 500: Something is broken. Please post to the group so the Twitter team can investigate.
					case HttpResponseCode.SERVICE_UNAVAILABLE:		// 503: The Twitter servers are up, but overloaded with requests.
					case -1:										// Caused by: java.net.UnknownHostException: api.twitter.com
						Utils.sleep(5000);							// Retry crawling 5 seconds later
						break;
					}
				}
				if (retry == false) break;
			}
		}
		
		return timeline;
	}
	
	/**
	 * Get shared status IDs from the given timeline
	 * @param timeline
	 * @return Shared status IDs
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
	
	/**
	 * Get retweet IDs from the given timeline.
	 * @param timeline
	 * @return Retweet IDs
	 */
	public ArrayList<Status> getRetweets(ArrayList<Status> timeline) {
		ArrayList<Status> retweetList = new ArrayList<Status>();
		for (Status tweet : timeline) {
			if (tweet.isRetweet())
				retweetList.add(tweet.getRetweetedStatus());
		}
		return retweetList;
	}
	
	/**
	 * Get mention history from the given userID.
	 * This task returns {mentioned user's ID - its date} pairs
	 * @param userID
	 * @param timeline
	 * @return Mentioned user's ID and its date pairs
	 */
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
	 * @param userID Target user's ID
	 * @return Favorite tweet list of target user
	 */
	public ArrayList<Status> getFavorites(long userID) {
		String endpoint = "/favorites/list";
		ArrayList<Status> favorites = new ArrayList<Status>();
		
		int page = 1;
		while (true) {
			TwitterApp app = mAppManager.getAvailableApp(endpoint);
			try {
				ResponseList<Status> onePage = app.twitter.getFavorites(userID, new Paging(page, 200));
				if (onePage.isEmpty()) break;
				favorites.addAll(onePage);
				page++;
			} catch (TwitterException te) {
				boolean retry = true;
				if (te.exceededRateLimitation()) {					// 429: Rate limit exceeded
					mAppManager.registerLimitedApp(app, endpoint, te.getRateLimitStatus().getSecondsUntilReset());
				} else {
					switch (te.getStatusCode()) {
					default:										// Unknown exception occurs
						te.printStackTrace();
					case HttpResponseCode.UNAUTHORIZED:				// 401: Authentication credentials were missing or incorrect.
					case HttpResponseCode.NOT_FOUND:				// 404: The URI requested is invalid or the resource requested, such as a user, does not exists.
						retry = false;								// Do not retry anymore
						break;
					case HttpResponseCode.INTERNAL_SERVER_ERROR:	// 500: Something is broken. Please post to the group so the Twitter team can investigate.
					case HttpResponseCode.SERVICE_UNAVAILABLE:		// 503: The Twitter servers are up, but overloaded with requests.
					case -1:										// Caused by: java.net.UnknownHostException: api.twitter.com
						Utils.sleep(5000);							// Retry crawling 5 seconds later
						break;
					}
				}
				if (retry == false) break;
			}
		}
		
		return favorites;
	}
	
	public ArrayList<Status> searchTweets(String queryString, int num) {
		String endpoint = "/search/tweets";
		ArrayList<Status> tweets = new ArrayList<Status>();
		
		Query query = new Query(queryString);
		query.setCount(100);
		QueryResult result = null;
		
		while (true) {
			TwitterApp app = mAppManager.getAvailableApp(endpoint);
			try {
				result = app.twitter.search(query);
				tweets.addAll(result.getTweets());
				
				if (tweets.size() > num)
					break;
				
				if (result.hasNext())
					query = result.nextQuery();
				else
					break;
			} catch (TwitterException te) {
				boolean retry = true;
				if (te.exceededRateLimitation()) {					// 429: Rate limit exceeded
					mAppManager.registerLimitedApp(app, endpoint, te.getRateLimitStatus().getSecondsUntilReset());
				} else {
					switch (te.getStatusCode()) {
					default:										// Unknown exception occurs
						te.printStackTrace();
					case HttpResponseCode.UNAUTHORIZED:				// 401: Authentication credentials were missing or incorrect.
					case HttpResponseCode.NOT_FOUND:				// 404: The URI requested is invalid or the resource requested, such as a user, does not exists.
						retry = false;								// Do not retry anymore
						break;
					case HttpResponseCode.INTERNAL_SERVER_ERROR:	// 500: Something is broken. Please post to the group so the Twitter team can investigate.
					case HttpResponseCode.SERVICE_UNAVAILABLE:		// 503: The Twitter servers are up, but overloaded with requests.
					case -1:										// Caused by: java.net.UnknownHostException: api.twitter.com
						Utils.sleep(5000);							// Retry crawling 5 seconds later
						break;
					}
				}
				if (retry == false) break;
			}
		}
		
		return tweets;
	}
}
