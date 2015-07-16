package main;

import java.util.ArrayList;

import crawler.Crawler;
import crawler.SeedFinder;
import database.DBHelper;
import twitter4j.User;

public class TwitterCrawler {
	public static void main(String[] arg) {
		// Finding Seed Candidates
		SeedFinder finder = new SeedFinder();
		ArrayList<User> seedCandidates = finder.getSeedCandidates();
		for (User candidate : seedCandidates) {
			if (Settings.isGoodSeedUser(candidate))
				System.out.println(candidate.getId());
		}
		
		// Crawling for each network
		ArrayList<Long> seedList = new ArrayList<Long>();
		seedList.add(1188870223L);		// ChangUk
//		seedList.add(3182892457L);		// JeeIn
//		seedList.add(78199077L);		// Jiwon
		
		for (long seed : seedList) {
			EgoNetwork egoNetwork = new EgoNetwork(seed, 1);
			Crawler crawler = new Crawler();
			crawler.run(egoNetwork);
		}
		
		DBHelper.getSingleton().destroy();
		
		System.out.println("Finished");
	}
}
