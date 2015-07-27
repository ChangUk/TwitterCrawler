package tool;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class SimpleLogger {
	private static SimpleLogger mInstance = null;
	
	public static synchronized SimpleLogger getSingleton() {
		return (mInstance != null) ? mInstance : (mInstance = new SimpleLogger());
	}
	
	// Save path of log file
	private static final String PATH_LOGFILE = new String("crawling_info.log");
	
	// This LogPrinter class depends on java.util.logging.Logger class.
	private Logger logger;
	private SimpleFormatter formmater;
	private ConsoleHandler handler;
	
	public SimpleLogger() {
		this.logger = Logger.getLogger(SimpleLogger.class.getName());
		logger.setUseParentHandlers(false);
		this.formmater = new SimpleFormatter();
		this.handler = new ConsoleHandler();
		handler.setFormatter(formmater);
		logger.addHandler(handler);
	}
	
	private static StringBuilder loggingHistory = new StringBuilder();
	private final String LINE_SEPARATOR = System.getProperty("line.separator");
	
	public void print(String msg) {
		System.out.println(msg);
		loggingHistory.append(msg + LINE_SEPARATOR);
	}
	
	/**
	 * All log messages printed by the function <code>print()</code> are stacked into <code>loggingHistory</code>.
	 * The <code>flush()</code> flushes log history (in memory) into a file and then makes the history empty.
	 */
	public static void flush() {
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(PATH_LOGFILE, "utf-8");
			writer.print(loggingHistory.toString());
			writer.close();
		} catch (UnsupportedEncodingException uee) {
			uee.printStackTrace();
		} catch (FileNotFoundException fnfe) {
			fnfe.printStackTrace();
		} finally {
			loggingHistory.setLength(0);
		}
	}
	
	public void info(String msg) {
		logger.info(msg);
	}
	
	public void severe(String msg) {
		logger.severe(msg);
	}
	
	public void fine(String msg) {
		logger.fine(msg);
	}
	
	public void warning(String msg) {
		logger.warning(msg);
	}
	
	private class SimpleFormatter extends Formatter {
		@Override
		public String format(LogRecord logRecord) {
			StringBuilder sb = new StringBuilder();
			sb.append(formatMessage(logRecord)).append(LINE_SEPARATOR);
			return sb.toString();
		}
	}
}
