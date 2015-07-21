package main;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;

import crawler.Crawler;

public class TwitterCrawler {
	public static void main(String[] arg) {
//		// Finding Seed Candidates
//		SeedFinder finder = new SeedFinder();
//		ArrayList<User> seedCandidates = finder.getSeedCandidates();
//		for (User candidate : seedCandidates) {
//			if (Settings.isGoodSeedUser(candidate))
//				System.out.println(candidate.getId());
//		}
		
		// Crawling for each network
		ArrayList<Long> seedList = new ArrayList<Long>();
		seedList.add(1188870223L);		// ChangUk
//		seedList.add(3182892457L);		// JeeIn
//		seedList.add(78199077L);		// Jiwon
		
		for (long seed : seedList) {
			EgoNetwork egoNetwork = new EgoNetwork(seed, 0);
			Crawler crawler = new Crawler();
			crawler.run(egoNetwork);
		}
		
		destroyDrivers();
		
		System.out.println("Finished");
	}
	
	public static void destroyDrivers() {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		Enumeration<Driver> drivers = DriverManager.getDrivers();
		while (drivers.hasMoreElements()) {
			Driver driver = drivers.nextElement();
			if (driver.getClass().getClassLoader() == classLoader) {
				try {
					DriverManager.deregisterDriver(driver);
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
