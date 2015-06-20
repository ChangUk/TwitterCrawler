package main;

import java.io.Serializable;
import java.util.ArrayList;

import twitter4j.User;

public class TwitterUser implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private final long id;
	private boolean isVisited;				// Indicates if the crawler had visited this user
	private boolean isProtected;
	private boolean isVerifiedCelebrity;
	private String lang;
	private int followingsCount;
	private int followersCount;
	private int tweetsCount;
	private long latestTweetID;
	private long date;
	private ArrayList<Long> mFollowingUserList	= null;
	
	public TwitterUser(long id, boolean isVisited, boolean isProtected, boolean isVerifiedCelebrity, String lang,
			int followingsCount, int followersCount, int tweetsCount, long latestTweetID, long date) {
		this.id = id;
		this.isVisited = isVisited;
		this.isProtected = isProtected;
		this.isVerifiedCelebrity = isVerifiedCelebrity;
		this.lang = lang;
		this.followingsCount = followingsCount;
		this.followersCount = followersCount;
		this.tweetsCount = tweetsCount;
		this.latestTweetID = latestTweetID;
		this.date = date;
	}
	
	public TwitterUser(User user) {
		this.id = user.getId();
		this.isVisited = false;
		this.isProtected = user.isProtected();
		this.isVerifiedCelebrity = user.isVerified();
		this.lang = user.getLang();
		this.followingsCount = user.getFriendsCount();
		this.followersCount = user.getFollowersCount();
		this.tweetsCount = user.getStatusesCount();
		this.latestTweetID = (isProtected || user.getStatus() == null ? -1L : user.getStatus().getId());
		this.date = user.getCreatedAt().getTime();
	}
	
	public long getID() {
		return id;
	}
	
	public boolean isVisited() {
		return isVisited;
	}
	
	public void setVisited() {
		this.isVisited = true;
	}
	
	public boolean isProtected() {
		return isProtected;
	}
	
	public void setProtected() {
		this.isProtected = true;
	}
	
	public boolean isVerifiedCelebrity() {
		return isVerifiedCelebrity;
	}
	
	public String getLang() {
		return lang;
	}
	
	public int getFollowingsCount() {
		return followingsCount;
	}
	
	public int getFollowersCount() {
		return followersCount;
	}
	
	public int getTweetsCount() {
		return tweetsCount;
	}
	
	public long getLatestTweetID() {
		return latestTweetID;
	}
	
	public long getDate() {
		return date;
	}
	
	public ArrayList<Long> getFollowingList() {
		return mFollowingUserList;
	}
	
	public void setFollowingList(ArrayList<Long> followingUserIDs) {
		this.mFollowingUserList = followingUserIDs;
	}
}
