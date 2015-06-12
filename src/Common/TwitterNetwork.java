package Common;

import java.io.File;
import java.util.ArrayList;

import twitter4j.User;
import TwitterCrawler.Engine;
import TwitterCrawler.TwitterUser;

public class TwitterNetwork {
	// Seed user
	protected final TwitterUser mSeedUser;
	
	// User ID list of network, which includes private nodes(users)
	protected ArrayList<Long> mNodeList;
	
	// # of directed edges
	protected int nDirectedEdges;
	
	// Language
	protected final String lang;
	
	// Path for output file
	protected String outputPath;
	
	public TwitterNetwork(User seedUser) {
		this.mSeedUser = new TwitterUser(seedUser.getId());
		this.mNodeList = new ArrayList<Long>();
		this.nDirectedEdges = 0;
		this.lang = seedUser.getLang();
	}
	
	public void init() {
		this.outputPath = Settings.PATH_SAVE + "global/";
		makeDirectories();
	}
	
	public TwitterUser getSeedUser() {
		return mSeedUser;
	}
	
	public ArrayList<Long> getNodeList() {
		return mNodeList;
	}
	
	public int getNodeCount() {
		return getNodeCount(true);
	}
	
	public int getInvalidNodeCount() {
		return getNodeCount(false);
	}
	
	public int getNodeCount(boolean isValid) {
		Engine engine = Engine.getSingleton();
		int cnt = 0;
		for (long userID : mNodeList) {
			TwitterUser user = engine.getUserMap().get(userID);
			if (user.isValid() == isValid)
				cnt += 1;
		}
		return cnt;
	}
	
	public int getEdgeCount() {
		return (nDirectedEdges / 2);
	}
	
	public int getDirectedEdgesCount() {
		return nDirectedEdges;
	}
	
	public void increaseDirectedEdgeCount(int num) {
		nDirectedEdges += num;
	}
	
	public void increaseDirectedEdgeCount() {
		increaseDirectedEdgeCount(1);
	}
	
	public String getLang() {
		return lang;
	}
	
	public String getOutputPath() {
		return outputPath;
	}
	
	public String getPathFollowingData() {
		return this.outputPath + "following/";
	}
	
	public String getPathFollowerData() {
		return this.outputPath + "follower/";
	}
	
	public String getPathFriendshipData() {
		return this.outputPath + "friendship/";
	}
	
	public String getPathTweetData() {
		return this.outputPath + "timeline/";
	}
	
	public String getPathShareData() {
		return this.outputPath + "share/";
	}
	
	public String getPathRetweetData() {
		return this.outputPath + "retweet/";
	}
	
	public String getPathMentionData() {
		return this.outputPath + "mention/";
	}
	
	public String getPathFavoriteData() {
		return this.outputPath + "favorite/";
	}
	
	public void makeDirectories() {
		new File(getPathFollowingData()).mkdirs();
		new File(getPathFollowerData()).mkdirs();
		new File(getPathFriendshipData()).mkdirs();
		new File(getPathTweetData()).mkdirs();
		new File(getPathShareData()).mkdirs();
		new File(getPathRetweetData()).mkdirs();
		new File(getPathMentionData()).mkdirs();
		new File(getPathFavoriteData()).mkdirs();
	}
}
