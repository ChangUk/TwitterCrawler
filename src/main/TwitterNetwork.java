package main;

import java.util.ArrayList;

public class TwitterNetwork {
	// Seed user ID
	protected final long mSeedUser;

	// Node list that cralwer has ever visited
	protected ArrayList<Long> mVisitedNodeList;
	
	public TwitterNetwork(long seedUserID) {
		this.mSeedUser = seedUserID;
		this.mVisitedNodeList = new ArrayList<Long>();
	}
	
	public long getSeedUserID() {
		return mSeedUser;
	}
	
	public ArrayList<Long> getVisitedNodeList() {
		return mVisitedNodeList;
	}
}
