package TwitterCrawler;

import java.io.Serializable;
import java.util.ArrayList;

public class TwitterUser implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private long id			= -1L;
	private boolean isValid	= true;
	private String lang		= null;
	
	private ArrayList<Long> mFollowingList	= null;
	private ArrayList<Long> mFollowerList	= null;
	private ArrayList<Long> mFriendshipList	= null;
	
	public TwitterUser(long userID) {
		this.id = userID;
	}
	
	public long getID() {
		return id;
	}
	
	public boolean isValid() {
		return isValid;
	}
	
	public void setInvalid() {
		this.isValid = false;
	}
	
	public String getLang() {
		return lang;
	}
	
	public void setLang(String lang) {
		this.lang = lang;
	}
	
	public ArrayList<Long> getFollowingList() {
		return mFollowingList;
	}
	
	public void setFollowingList(ArrayList<Long> followingsIDs) {
		this.mFollowingList = followingsIDs;
	}
	
	public ArrayList<Long> getFollowerList() {
		return mFollowerList;
	}
	
	public void setFollowerList(ArrayList<Long> followersIDs) {
		this.mFollowerList = followersIDs;
	}
	
	public ArrayList<Long> getFriendshipList() {
		return mFriendshipList;
	}
	
	public void setFriendshipList(ArrayList<Long> friendsIDs) {
		this.mFriendshipList = friendsIDs;
	}
	
	public void nullifyLists() {
		this.mFollowingList = null;
		this.mFollowerList = null;
		this.mFriendshipList = null;
	}
	
//	/**
//	 * Check if this user is normal and heavy user or not.
//	 * @param lang Criterion #1: language matching (If null, this criterion is ignored)
//	 * @param minTweets Criterion #2: minimum number of tweets for filtering light user out (If 0, this criterion is ignored)
//	 * @param maxFollowings Criterion #3: number of following uesrs for detecting normal user (If 0, this criterion is ignored)
//	 * @param maxFollowers Criterion #4: number of followers for detecting normal user (If 0, this criterion is ignored)
//	 * @return 1 if the requirements are satisfied, 0 otherwise.
//	 * (If the user's information is not enough, negative integer(-1) will be returned.)
//	 */
//	public int checkRequirements(String lang, int minTweets, int maxFollowings, int maxFollowers) {
//		if (this.lang == null || this.nTweets < 0 || this.nFollowings < 0 || this.nFollowers < 0)
//			return -1;
//		
//		if (this.isValid == true)
//			return 0;
//		if (lang != null && this.lang.equals(lang) == false)		// Criterion #1
//			return 0;
//		if (minTweets > 0 && this.nTweets < minTweets)				// Criterion #2
//			return 0;
//		if (maxFollowings > 0 && this.nFollowings > maxFollowings)	// Criterion #3
//			return 0;
//		if (maxFollowers > 0 && this.nFollowers > maxFollowers)		// Criterion #4
//			return 0;
//		
//		return 1;		// Return 1 if all criteria is satisfied
//	}
}
