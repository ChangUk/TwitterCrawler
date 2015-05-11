
import java.util.HashMap;
import Common.EgoNetwork;

import TwitterCrawler.Crawler;

public class TwitterMiner {
	public static void main(String[] arg) {
		HashMap<String, EgoNetwork> networks = new HashMap<String, EgoNetwork>();
		networks.put("ChangUk",		new EgoNetwork(1188870223, 2));
//		networks.put("Jiwon",		new EgoNetwork(78199077, 2));
		
		for (EgoNetwork network : networks.values()) {
			Crawler crawler = new Crawler(network);
			crawler.run();
		}
		
		System.out.println("Finished");
	}
}
