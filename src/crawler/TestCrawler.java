package crawler;

import crawler.engine.Engine;
import twitter4j.User;

public class TestCrawler {
	private Engine engine;
	
	public TestCrawler() {
		this.engine = Engine.getSingleton();
	}
	
	public void run() {
		User user = engine.showUserByID(1188870223L);
		String timezone = user.getTimeZone();
		if (timezone != null)
			System.out.println(timezone);
	}
}
