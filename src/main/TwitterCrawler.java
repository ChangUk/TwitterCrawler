package main;

import java.util.ArrayList;

import crawler.EgoNetCrawler;
import crawler.SeedSeeker;
import tool.SimpleLogger;

public class TwitterCrawler {
	public static void main(String[] arg) {
		/**
		 * Finding good seed users
		 */
//		SeedSeeker seedSeeker = new SeedSeeker();
//		seedSeeker.findGoodSeeds();
		
		/**
		 * Crawling an ego network
		 */
		ArrayList<Long> seedList = new ArrayList<Long>();
		seedList.add(1188870223L);		// ChangUk
//		seedList.add(3182892457L);		// JeeIn
		
		for (long seed : seedList) {
			EgoNetwork egoNetwork = new EgoNetwork(seed, 1);
			EgoNetCrawler crawler = new EgoNetCrawler(egoNetwork);
			crawler.run();
		}
		
		SimpleLogger.flush();
		System.out.println("Finished");
	}
}
