import java.util.HashMap;

import TwitterCrawler.Crawler;
import TwitterCrawler.EgoNetwork;

public class TwitterMiner {
	public static void main(String[] arg) {
		HashMap<String, EgoNetwork> networks = new HashMap<String, EgoNetwork>();
//		networks.put("ChangUk",		new EgoNetwork(1188870223));
		networks.put("Jiwon",		new EgoNetwork(78199077));
		
		for (EgoNetwork network : networks.values()) {
			Crawler crawler = new Crawler(network, 2);
			crawler.run();
		}
		
		System.out.println("Finished");
	}
}
