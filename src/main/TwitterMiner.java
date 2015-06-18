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
		
//		DataPreprocessor dp = new DataPreprocessor();
//		String asdf = "정리하자면: 하드커버, 미국에서 총 4판 이하로 출간, 초여명에서 안 냈고, 국내 발간 안 됐고, 초인동맹 TRPG 아니고, 알파벳 순으로 L보다 앞이며, 이능물이라고 할 수도 있고 아닐 수도 있고, 질문은 20개만 받습니다.";
//		System.out.println(asdf);
//		System.out.println(dp.getRefinedTweet(asdf));
		
//		ArrayList<User> seedUsers = new ArrayList<User>();
//		seedUsers.add(engine.showUser(78199077L));			// Jiwon
//		seedUsers.add(engine.showUser(1188870223L));		// ChangUk
//		seedUsers.add(engine.showUser(3182892457L));		// JeeIn
		
//		for (User seedUser : seedUsers) {
//			EgoNet egoNetwork = new EgoNet(seedUser, 1);
//			egoNetwork.init();
//			Crawler crawler = new Crawler(egoNetwork);
//			crawler.run();
//			
//			DBAdapter dbAdapter = DBAdapter.getSingleton();
//			for (long userID : egoNetwork.getNodeList()) {
//				TwitterUser user = engine.getUserMap().get(userID);
//				if (user.isValid()) {
//					dbAdapter.execQuery("INSERT INTO user (id, isProtected, lang) VALUES ("
//							+ user.getID() + ", "
//							+ (user.isValid() ? 0 : 1) + ", '"
//							+ user.getLang() + "')");
//					
//					String sql = new String("INSERT INTO follow (source, target) VALUES (?, ?)");
//					ArrayList<String[]> values = new ArrayList<String[]>();
//					for (long followingID : user.getFollowingList()) {
//						String[] value = new String[2];
//						value[0] = String.valueOf(user.getID());
//						value[1] = String.valueOf(followingID);
//						values.add(value);
//					}
//					dbAdapter.batchQueries(sql, values);
//				}
//			}
//			dbAdapter.finalize();
//		}
		
		System.out.println("Finished");
	}
}
