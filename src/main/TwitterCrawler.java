package main;

import java.util.ArrayList;

import crawler.EgoNetworkCrawler;
import crawler.SeedSeeker;
import tool.SimpleLogger;

public class TwitterCrawler {
	public static void main(String[] arg) {
		// Seed list
		ArrayList<Long> seedList;
		
		// Finding good seed users
		SeedSeeker seedSeeker = new SeedSeeker();
		seedList = seedSeeker.getSeedUsersFromCNNFollowers();
		
		for (long seed : seedList) {
			EgoNetwork egoNetwork = new EgoNetwork(seed, 1);
			EgoNetworkCrawler crawler = new EgoNetworkCrawler(egoNetwork);
			crawler.run();
		}
		
		SimpleLogger.flush();
		System.out.println("Finished");
	}
}
