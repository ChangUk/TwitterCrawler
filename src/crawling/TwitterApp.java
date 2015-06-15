package crawling;

import java.util.Map;
import java.util.logging.Logger;

import twitter4j.RateLimitStatus;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

public class TwitterApp {
	// The name of this TwitterApp
	public final String name;
	
	// twitter4j instance
	public Twitter twitter = null;
	
	public TwitterApp(String name, String API_KEY, String API_SECRET, String ACCESS_KEY, String ACCESS_SECRET) {
		this.name = name;
		
		ConfigurationBuilder builder = new ConfigurationBuilder();
		builder.setOAuthConsumerKey(API_KEY);
		builder.setOAuthConsumerSecret(API_SECRET);
		builder.setOAuthAccessToken(ACCESS_KEY);
		builder.setOAuthAccessTokenSecret(ACCESS_SECRET);
		this.twitter = new TwitterFactory(builder.build()).getInstance();
		
		/**
		 * Do not use the following RateLimitStatusListener
		 * due to its possible unstable cases when the crawler is starting.
		 * I replace this listener into TwitterException handler for each API call.
		 * @author ChangUk
		 */
//		this.twitter.addRateLimitStatusListener(new RateLimitStatusListener() {
//			@Override
//			public void onRateLimitStatus(RateLimitStatusEvent event) {
//				RateLimitStatus status = event.getRateLimitStatus();
//				AppManager appManager = AppManager.getSingleton();
//				if (status.getRemaining() == 0) {
//					appManager.registerLimitedApp(TwitterApp.this, status.getSecondsUntilReset());
//					printRateLimitStatus();
//				}
//			}
//			
//			@Override
//			public void onRateLimitReached(RateLimitStatusEvent event) {
//				RateLimitStatus status = event.getRateLimitStatus();
//				AppManager appManager = AppManager.getSingleton();
//				appManager.registerLimitedApp(TwitterApp.this, status.getSecondsUntilReset());
//				printRateLimitStatus();
//			}
//		});
	}
	
	/**
	 * Print current limited endpoints list
	 * @param curEndpoint
	 */
	public void printRateLimitStatus(String curEndpoint) {
		Logger logger = Logger.getGlobal();
		AppManager appManager = AppManager.getSingleton();
		logger.info("----------------------- Limited Endpoint Status -----------------------\n"
				+ "- App(" + name + " - '" + curEndpoint + "') reaches rate limited.");
		for (String endpoint : appManager.limitedEndpoints.keySet()) {
			logger.info("- Endpoint('" + endpoint + "'): " + appManager.limitedEndpoints.get(endpoint).size() + " apps.");
		}
		logger.info("-----------------------------------------------------------------------");
	}
	
	/**
	 * Warning: This function prints out weird result of rate limit status.
	 */
	@Deprecated
	public void printRateLimitStatusFromTwitter() {
		Logger logger = Logger.getGlobal();
		try {
			Map<String, RateLimitStatus> rateLimitStatus = twitter.getRateLimitStatus();
			for (String endpoint : rateLimitStatus.keySet()) {
				RateLimitStatus status = rateLimitStatus.get(endpoint);
				logger.info("\tApp" + name + " - '" + endpoint + "' -\t\t" + status.getRemaining() + "/" + status.getLimit()
						+ ": " + status.getSecondsUntilReset());
			}
		} catch (TwitterException te) {
			te.printStackTrace();
		}
	}
}
