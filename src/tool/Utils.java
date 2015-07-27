package tool;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.StringTokenizer;

import twitter4j.Status;

public class Utils {
	/**
	 * Convert ArrayList type into array[] type.
	 * @param list ArrayList type data
	 * @return Array type of data
	 */
	public static long[] getLongArray(ArrayList<Long> list) {
		long[] array = new long[list.size()];
		for (int i = 0; i < array.length; i++)
			array[i] = list.get(i);
		return array;
	}
	
	public static String[] getStringArray(ArrayList<String> list) {
		String[] array = new String[list.size()];
		for (int i = 0; i < array.length; i++)
			array[i] = list.get(i);
		return array;
	}
	
	/**
	 * Wait the given period of time
	 * @param milliseconds Waiting time
	 */
	public static void sleep(long milliseconds) {
		try {
			Thread.sleep(milliseconds);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Check if the tweet contains mentioning.
	 * @param tweet Target tweet to be tested
	 * @return True if the tweet contains mentioning in its text, false otherwise.
	 */
	public static boolean containsMention(Status tweet) {
		if (tweet.getMediaEntities().length > 0)
			return true;
		
		StringTokenizer st = new StringTokenizer(tweet.getText(), " \t\r\n");
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			if (token.startsWith("@"))
				return true;
		}
		return false;
	}
	
	/**
	 * Get current memory usage
	 * @return Memory usage
	 */
	public static String getCurMemoryUsage() {
		Runtime runtime = Runtime.getRuntime();
		NumberFormat format = NumberFormat.getInstance();
		return format.format((runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024));
	}
	
	/**
	 * Get program executing time log message.
	 * @param title Additional log message ahead of executing time information
	 * @param startingTime Starting time
	 * @return Log message of program executing time
	 */
	public static String getExecutingTime(String title, long startingTime) {
		long time = (System.currentTimeMillis() - startingTime) / 1000L;	// Milliseconds
		return new String("### " + title + ": "
				+ String.format("%02d", time / 3600) + ":"
				+ String.format("%02d", ((time % 3600) / 60)) + ":"
				+ String.format("%02d", time % 60));
	}
	
	/**
	 * Get current system time.
	 * @return Current system time log message according to the given String format.
	 */
	public static String getCurrentTime() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Calendar now = Calendar.getInstance();
		return dateFormat.format(now.getTime());
	}
}
