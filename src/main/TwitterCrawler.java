package main;

import java.util.ArrayList;

import crawler.Crawler;

public class TwitterCrawler {
	public static void main(String[] arg) {
		ArrayList<Long> seedList = new ArrayList<Long>();
		seedList.add(1188870223L);		// ChangUk
		seedList.add(3182892457L);		// JeeIn
//		seedList.add(78199077L);		// Jiwon
		
		for (long seed : seedList) {
			EgoNetwork egoNetwork = new EgoNetwork(seed, 2);
			Crawler crawler = new Crawler();
			crawler.run(egoNetwork);
		}
		
		System.out.println("Finished");
	}
}
