package Common;
import TwitterCrawler.TwitterUser;

public class EgoNetwork extends TwitterNetwork {
	private final TwitterUser egoUser;
	private final int level;
	
	/**
	 * To get user's Twitter ID, visit here: <a href="http://tweeterid.com/">http://tweeterid.com/</a>.
	 */
	public EgoNetwork(long egoUserID, int level) {
		this.egoUser = new TwitterUser(egoUserID);
		this.level = level;
	}
	
	public TwitterUser getEgoUser() {
		return egoUser;
	}
	
	public int level() {
		return level;
	}
}
