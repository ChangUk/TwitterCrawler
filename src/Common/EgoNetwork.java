package Common;

import twitter4j.User;

public class EgoNetwork extends TwitterNetwork {
	private final int level;
	
	/**
	 * To get user's Twitter ID, visit here: <a href="http://tweeterid.com/">http://tweeterid.com/</a>.
	 */
	public EgoNetwork(User egoUser, int level) {
		super(egoUser);
		this.level = level;
	}
	
	public void init() {
		this.PATH_DATA = Settings.PATH_DATA + "__ego_network/" + mSeedUser.getID() + "_" + level + "/";
	}
	
	public int level() {
		return level;
	}
}
