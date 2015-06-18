package main;

public class EgoNetwork extends TwitterNetwork {
	// Network depth
	private int level = -1;
	
	public EgoNetwork(long egoUserID, int level) {
		super(egoUserID);
		this.level = level;
	}
	
	public int level() {
		return level;
	}
}
