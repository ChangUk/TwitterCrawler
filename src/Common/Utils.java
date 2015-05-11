package Common;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;

import twitter4j.Status;
import TwitterCrawler.TwitterUser;

public class Utils {
	public String printExecutingTime(String title, long time) {
		return new String("### " + title + ": "
				+ String.format("%02d", time / 3600) + ":"
				+ String.format("%02d", ((time % 3600) / 60)) + ":"
				+ String.format("%02d", time % 60));
	}
	
	public void writeFriendsIDs(String outputPath, EgoNetwork network, ExecutorService exeService) {
		if (network == null || network.getNodeMap() == null)
			return;
		
		Thread thread = new Thread() {
			@Override
			public void run() {
				super.run();
				
				for (HashMap.Entry<Long, TwitterUser> entry : network.getNodeMap().entrySet()) {
					writeFriendsIDs(outputPath, entry.getKey(), entry.getValue().friends);
				}
			}
		};
		exeService.submit(thread);
	}
	
	public void writeFriendsIDs(String outputPath, long userID, ArrayList<Long> friendsIDs) {
		if (friendsIDs == null || friendsIDs.isEmpty())
			return;
		
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(outputPath + String.valueOf(userID) + ".friends", "utf-8");
			for (long friendID : friendsIDs) {
				writer.println(friendID);
			}
		} catch (UnsupportedEncodingException uee) {
			uee.printStackTrace();
		} catch (FileNotFoundException fnfe) {
			fnfe.printStackTrace();
		} finally {
			if (writer != null)
				writer.close();
		}
	}
	
	public void writeTweets(String outputPath, long userID, ArrayList<Status> tweets) {
		if (tweets == null || tweets.isEmpty())
			return;
		
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(outputPath + userID + ".tweets", "utf-8");
			for (Status tweet : tweets) {
				writer.println(tweet.getId() + "\t" + tweet.getText());
			}
		} catch (UnsupportedEncodingException uee) {
			uee.printStackTrace();
		} catch (FileNotFoundException fnfe) {
			fnfe.printStackTrace();
		} finally {
			if (writer != null)
				writer.close();
		}
	}
	
	public void writeSharingIDs(String outputPath, long userID, HashMap<Long, Long> sharings) {
		if (sharings == null || sharings.isEmpty())
			return;
		
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(outputPath + userID + ".sharings", "utf-8");
			for (HashMap.Entry<Long, Long> mentionEntry : sharings.entrySet()) {
				writer.println(mentionEntry.getValue() + "\t" + mentionEntry.getKey());
			}
		} catch (UnsupportedEncodingException uee) {
			uee.printStackTrace();
		} catch (FileNotFoundException fnfe) {
			fnfe.printStackTrace();
		} finally {
			if (writer != null)
				writer.close();
		}
	}
	
	public void writeRetweetIDs(String outputPath, long userID, ArrayList<Status> retweets) {
		if (retweets == null || retweets.isEmpty())
			return;
		
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(outputPath + userID + ".retweets", "utf-8");
			for (Status retweet : retweets) {
				writer.println(retweet.getRetweetedStatus().getUser().getId() + "\t"
						+ retweet.getRetweetedStatus().getId());
			}
		} catch (UnsupportedEncodingException uee) {
			uee.printStackTrace();
		} catch (FileNotFoundException fnfe) {
			fnfe.printStackTrace();
		} finally {
			if (writer != null)
				writer.close();
		}
	}
	
	public void writeMentions(String outputPath, long userID, HashMap<Long, Integer> mentions) {
		if (mentions == null || mentions.isEmpty())
			return;
		
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(outputPath + userID + ".mentions", "utf-8");
			for (HashMap.Entry<Long, Integer> mentionEntry : mentions.entrySet()) {
				writer.println(mentionEntry.getKey() + "\t" + mentionEntry.getValue());
			}
		} catch (UnsupportedEncodingException uee) {
			uee.printStackTrace();
		} catch (FileNotFoundException fnfe) {
			fnfe.printStackTrace();
		} finally {
			if (writer != null)
				writer.close();
		}
	}
	
	public void writeFavorites(String outputPath, long userID, ArrayList<Status> favorites) {
		if (favorites == null || favorites.isEmpty())
			return;
		
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(outputPath + userID + ".favorites", "utf-8");
			for (Status favorite : favorites) {
				writer.println(favorite.getUser().getId() + "\t" + favorite.getId() + "\t" + favorite.getText());
			}
		} catch (UnsupportedEncodingException uee) {
			uee.printStackTrace();
		} catch (FileNotFoundException fnfe) {
			fnfe.printStackTrace();
		} finally {
			if (writer != null)
				writer.close();
		}
	}
}
