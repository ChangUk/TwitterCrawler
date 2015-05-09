package TwitterCrawler;
import java.util.ArrayList;
import java.util.HashMap;

public class EgoNetwork {
	private final TwitterUser egoUser;
	private final int level;
	
	private HashMap<Long, TwitterUser> mNodeMap;
	private ArrayList<Long> mAuthInvalidList;
	
	public int nDirectedEdges = 0;
	
	/**
	 * To get user's Twitter ID, visit here: <a href="http://tweeterid.com/">http://tweeterid.com/</a>.
	 */
	public EgoNetwork(long egoUserID, int level) {
		this.egoUser = new TwitterUser(egoUserID);
		this.level = level;
		
		this.mNodeMap = new HashMap<Long, TwitterUser>();
		this.mAuthInvalidList = new ArrayList<Long>();
	}
	
	public TwitterUser getEgoUser() {
		return egoUser;
	}
	
	public int level() {
		return level;
	}
	
	public HashMap<Long, TwitterUser> getNodeMap() {
		return mNodeMap;
	}
	
	public ArrayList<Long> getAuthInvalidList() {
		return mAuthInvalidList;
	}
}
