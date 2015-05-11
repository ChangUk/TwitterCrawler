package Common;

import java.util.ArrayList;
import java.util.HashMap;

import TwitterCrawler.TwitterUser;

public class TwitterNetwork {
	private HashMap<Long, TwitterUser> mNodeMap;
	private ArrayList<Long> mAuthInvalidList;
	
	public int nDirectedEdges = 0;
	
	public TwitterNetwork() {
		this.mNodeMap = new HashMap<Long, TwitterUser>();
		this.mAuthInvalidList = new ArrayList<Long>();
	}
	
	public HashMap<Long, TwitterUser> getNodeMap() {
		return mNodeMap;
	}
	
	public ArrayList<Long> getAuthInvalidList() {
		return mAuthInvalidList;
	}
}
