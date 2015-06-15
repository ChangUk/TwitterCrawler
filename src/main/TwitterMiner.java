package main;

import java.util.ArrayList;

import twitter4j.User;
import crawling.Crawler;
import crawling.Engine;

public class TwitterMiner {
	public static void main(String[] arg) {
		Engine engine = Engine.getSingleton();
		
//		DataPreprocessor dp = new DataPreprocessor();
//		String asdf = "정리하자면: 하드커버, 미국에서 총 4판 이하로 출간, 초여명에서 안 냈고, 국내 발간 안 됐고, 초인동맹 TRPG 아니고, 알파벳 순으로 L보다 앞이며, 이능물이라고 할 수도 있고 아닐 수도 있고, 질문은 20개만 받습니다.";
//		System.out.println(asdf);
//		System.out.println(dp.getRefinedTweet(asdf));
		
		ArrayList<User> seedUsers = new ArrayList<User>();
//		seedUsers.add(engine.showUser(78199077L));			// Jiwon
		seedUsers.add(engine.showUser(1188870223L));		// ChangUk
//		seedUsers.add(engine.showUser(3182892457L));		// JeeIn
		
		for (User seedUser : seedUsers) {
			EgoNetwork egoNetwork = new EgoNetwork(seedUser, 2);
			egoNetwork.init();
			Crawler crawler = new Crawler(egoNetwork);
			crawler.run();
		}
		
		System.out.println("Finished");
	}
}
