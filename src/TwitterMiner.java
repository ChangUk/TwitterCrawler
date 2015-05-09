import java.util.HashMap;

import TwitterCrawler.Crawler;
import TwitterCrawler.EgoNetwork;

public class TwitterMiner {
	public static void main(String[] arg) {
		HashMap<String, EgoNetwork> networks = new HashMap<String, EgoNetwork>();
//		networks.put("ChangUk",		new EgoNetwork(1188870223, 1));
		networks.put("Jiwon",		new EgoNetwork(78199077, 2));
		
		for (EgoNetwork network : networks.values()) {
			Crawler crawler = new Crawler(network);
			crawler.run();
		}
		
		System.out.println("Finished");
	}
}
