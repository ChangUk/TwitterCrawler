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
	private long date;
	private int followingsCount;
	private int followersCount;
	private int tweetsCount;
	private ArrayList<Long> mFollowingUserList	= null;
	
	public TwitterUser(User user) {
		this.id = user.getId();
		this.isVisited = false;
		this.isProtected = user.isProtected();
		this.isVerifiedCelebrity = user.isVerified();
		this.lang = user.getLang();
		this.date = user.getCreatedAt().getTime();
		this.followingsCount = user.getFriendsCount();
		this.followersCount = user.getFollowersCount();
		this.tweetsCount = user.getStatusesCount();
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
	
	public long getDate() {
		return date;
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
	
	public ArrayList<Long> getFollowingList() {
		return mFollowingUserList;
	}
	
	public void setFollowingList(ArrayList<Long> followingUserIDs) {
		this.mFollowingUserList = followingUserIDs;
	}
}
