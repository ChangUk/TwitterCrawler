package Common;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Utils {
	private String msgLog = new String();
	
	/**
	 * Wait the given period of time
	 * @param milliseconds Waiting time
	 */
	public void sleep(long milliseconds) {
		try {
			Thread.sleep(milliseconds);
		} catch (InterruptedException ie) {
		}
	}
	
	/**
	 * Get current memory usage
	 * @return Memory usage
	 */
	public String getCurMemoryUsage() {
		Runtime runtime = Runtime.getRuntime();
		NumberFormat format = NumberFormat.getInstance();
		return format.format((runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024));
	}
	
	/**
	 * Get program executing time log message.
	 * @param title Additional log message ahead of executing time information
	 * @param time Executing time (milliseconds)
	 * @return Log message of program executing time
	 */
	public String getExecutingTime(String title, long time) {
		return new String("### " + title + ": "
				+ String.format("%02d", time / 3600) + ":"
				+ String.format("%02d", ((time % 3600) / 60)) + ":"
				+ String.format("%02d", time % 60));
	}
	
	/**
	 * Get current system time.
	 * @return Current system time log message according to the given String format.
	 */
	public String getCurrentTime() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Calendar now = Calendar.getInstance();
		return dateFormat.format(now.getTime());
	}
	
	/**
	 * Print a given log message out. You are able to flush the log message to *.log file as well.
	 * @param log log message
	 * @param flush If true, the concatenated log texts are saved into a log file.
	 */
	public void printLog(TwitterNetwork network, String log, boolean flush) {
		System.out.println(log);
		msgLog = msgLog.concat(log + "\r\n");
		
		if (flush == true) {
			PrintWriter writer = null;
			try {
				writer = new PrintWriter(network.getOutputPath() + "crawling_info.log", "utf-8");
				writer.print(msgLog);
				writer.close();
			} catch (UnsupportedEncodingException uee) {
				uee.printStackTrace();
			} catch (FileNotFoundException fnfe) {
				fnfe.printStackTrace();
			} finally {
				msgLog = new String("");
			}
		}
	}
}
