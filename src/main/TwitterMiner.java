package main;

import java.util.ArrayList;

import crawling.Crawler;

public class TwitterMiner {
	public static void main(String[] arg) {
		ArrayList<Long> seedList = new ArrayList<Long>();
		seedList.add(78199077L);		// Jiwon
//		seedList.add(1188870223L);		// ChangUk
//		seedList.add(3182892457L);		// JeeIn
		
		for (long seed : seedList) {
			EgoNetwork egoNetwork = new EgoNetwork(seed, 1);
			Crawler crawler = new Crawler();
			crawler.run(egoNetwork);
		}
		
		System.out.println("Finished");
	}
}
