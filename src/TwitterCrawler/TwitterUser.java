package TwitterCrawler;
import java.util.ArrayList;

public class TwitterUser {
	public long id;
	public ArrayList<Long> friends = null;
	
	public TwitterUser(long id) {
		this.id = id;
		this.friends = new ArrayList<Long>();
	}
}
