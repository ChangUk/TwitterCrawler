package main;

import java.util.ArrayList;

import crawling.Engine;
import twitter4j.User;

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
	public String PATH_DATA;
	public String PATH_DATA_FOLLOWING;
	public String PATH_DATA_FOLLOWER;
	public String PATH_DATA_FRIENDSHIP;
	public String PATH_DATA_TIMELINE;
	public String PATH_DATA_SHARE;
	public String PATH_DATA_RETWEET;
	public String PATH_DATA_MENTION;
	public String PATH_DATA_FAVORITE;
	
	public TwitterNetwork(User seedUser) {
		this.mSeedUser = new TwitterUser(seedUser.getId());
		this.mNodeList = new ArrayList<Long>();
		this.nDirectedEdges = 0;
		this.lang = seedUser.getLang();
	}
	
	public void init() {
		this.PATH_DATA = Settings.PATH_DATA + "network/" + + mSeedUser.getID() + "/";
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
		return PATH_DATA;
	}
}
