package TwitterCrawler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

import Common.CircularQueue;
import Common.Settings;

public class AppManager {
	private static AppManager mInstance = null;
	
	private CircularQueue<TwitterApp> mAppQueue = null;
	public HashMap<String, ArrayList<TwitterApp>> limitedEndpoints;
	
	public static synchronized AppManager getSingleton() {
		return (mInstance != null) ? (mInstance) : (mInstance = new AppManager());
	}
	
	public AppManager() {
		mAppQueue = new CircularQueue<TwitterApp>();
		limitedEndpoints = new HashMap<String, ArrayList<TwitterApp>>();
		
		if (loadTwitterApps() == false) {
			System.out.println("### Applications loading failed!");
			System.exit(0);
		}
	}
	
	public boolean loadTwitterApps() {
		try {
			File file = new File(Settings.PATH_APPS_INFO);
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			
			String oneLine;
			int idx = 1;
			while ((oneLine = br.readLine()) != null) {
				String token[] = oneLine.split("\t");
				if (token.length != 4)
					continue;
				mAppQueue.enqueue(new TwitterApp(String.valueOf(idx), token[0], token[1], token[2], token[3]), false);
				idx++;
			}
			
			br.close();
			fr.close();
		} catch (FileNotFoundException fnfe) {
			fnfe.printStackTrace();
			return false;
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return false;
		}
		
		System.out.println("### " + mAppQueue.size() + " applications are loaded!");
		return true;
	}
	
	/**
	 * Register TwitterApp which reaches rate limit
	 * @param app TwitterApp
	 * @param endpoint endpoint
	 * @param secondsUntilReset time(second) until reset
	 */
	public void registerLimitedApp(TwitterApp app, String endpoint, int secondsUntilReset) {
		ArrayList<TwitterApp> limitedAppList = limitedEndpoints.get(endpoint);
		if (limitedAppList == null)
			limitedAppList = new ArrayList<TwitterApp>();
		if (limitedAppList.contains(app) == false)
			limitedAppList.add(app);
		limitedEndpoints.put(endpoint, limitedAppList);
		
		new Thread() {
			@Override
			public void run() {
				super.run();
				try {
					Thread.sleep((secondsUntilReset + 1) * 1000);		// 1s slack
					limitedEndpoints.get(endpoint).remove(app);
				} catch (InterruptedException ie) {
				}
			}
		}.start();
	}
	
	/**
	 * Get available TwitterApp which is not limited yet.
	 * @see <a href="http://twitter4j.org/en/api-support.html#Friends%20&%20Followers%20Resources">
	 * http://twitter4j.org/en/api-support.html#Friends%20&%20Followers%20Resources</a>
	 * @param endpoint
	 * @return available TwitterApp
	 */
	public TwitterApp getAvailableApp(String endpoint) {
		Logger logger = Logger.getGlobal();
		
		int cursor = mAppQueue.getCursor();
		while (true) {
			TwitterApp app = mAppQueue.getCurrentItem();
			mAppQueue.next();
			
			if (limitedEndpoints.containsKey(endpoint) == false || limitedEndpoints.get(endpoint).contains(app) == false) {
//				logger.info("\tApp(" + app.name + " - '" + endpoint + "')");
				return app;
			}
			
			if (mAppQueue.peekNextCursor() == cursor) {
				try {
					logger.info("All applications are sleeping. ('" + endpoint + "')");
					Thread.sleep(10000);
				} catch (InterruptedException ie) {
				}
			}
		}
	}
}
