package tool;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.StringTokenizer;

import twitter4j.Status;
import main.Settings;

public class Utils {
	/**
	 * Convert ArrayList type into array[] type.
	 * @param list ArrayList type data
	 * @return Array type of data
	 */
	public static long[] getArray(ArrayList<Long> list) {
		long[] array = new long[list.size()];
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
		} catch (InterruptedException ie) {
		}
	}
	
	/**
	 * Get refined tweet message after simple text cleaning.
	 * This task involves removing URLs and mentions marks.
	 * @param tweet Tweet status
	 * @return Refined tweet texts
	 */
	public static String simpleTweetCleaning(Status tweet) {
		StringTokenizer st = new StringTokenizer(tweet.getText(), " \t\r\n");
		if (tweet.isRetweet())
			st.nextToken();
		ArrayList<String> wordList = new ArrayList<String>();
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			if (token.startsWith("@") || token.startsWith("http://") || token.startsWith("https://"))
				continue;
			wordList.add(token.toLowerCase());
		}
		
		if (wordList.isEmpty()) {
			return new String("");
		} else {
			String result = new String(wordList.get(0));
			wordList.remove(0);
			for (String word : wordList)
				result = result.concat(" " + word);
			return result;
		}
	}
	
	/**
	 * Check if the tweet contains mentioning.
	 * @param tweet Target tweet to be tested
	 * @return True if the tweet contains mentioning in its text, false otherwise.
	 */
	public static boolean containsMention(Status tweet) {
		StringTokenizer st = new StringTokenizer(tweet.getText(), " \t\r\n");
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			if (token.startsWith("@"))
				return true;
		}
		return false;
	}
	
	// Regular expressions (Latin)
	public static final String REGEX_LATIN_BASIC = "\\p{InBasic_Latin}";				//	"A-Za-z";
	public static final String REGEX_LATIN_SUPPLEMENT = "\\p{InLatin-1_Supplement}";	//	"À-ÖÙ-öù-ÿ";
	public static final String REGEX_LATIN_EXTENDED_A = "\\p{InLatin_Extended-A}";		//	"Ā-ſ";
	public static final String REGEX_LATIN_EXTENDED_B = "\\p{InLatin_Extended-B}";		//	"ƀ-ɏ";
	public static final String REGEX_LATIN = "\\p{Latin}";								//	REGEX_LATIN_BASIC + REGEX_LATIN_SUPPLEMENT + REGEX_LATIN_EXTENDED_A + REGEX_LATIN_EXTENDED_B;
	
	// Regular expressions (Korean)
	public static final String REGEX_HANGUL_JAMO = "\\p{InHangul_Jamo}";				//	"ㄱ-ㅎㅏ-ㅣ";
	public static final String REGEX_HANGUL_SYLLABLES = "\\p{InHangul_Syllables}";		//	"가-힣";
	public static final String REGEX_HANGUL = "\\p{Hangul}";							//	REGEX_HANGUL_JAMO + REGEX_HANGUL_SYLLABLES;
	
	// Regular expressions (Text)
	public static final String REGEX_SCRIPTS = REGEX_LATIN + REGEX_HANGUL;
	
	// Regular expressions (Symbol)
	public static final String REGEX_SYMBOLS = "\\P{L}";								//	"!-/,:-?";
	
	// Regular expressions (Number)
	public static final String REGEX_NUMBERS = "\\p{N}";								// "0-9";
	
	/**
	 * Check if the given word is Hangul word or not.
	 * @param word
	 * @return True if the given word is Hangul.
	 */
	public static boolean isHangul(String word) {
		if (word.length() == 0)
			return false;
		return word.matches("^[" + REGEX_HANGUL + "]+$");
	}
	
	/**
	 * Check if the given word is Hangul syllables or not.
	 * @param word
	 * @return True if the given word is Hangul.
	 */
	public static boolean isHangulSyllables(String word) {
		if (word.length() == 0)
			return false;
		return word.matches("^[" + REGEX_HANGUL_SYLLABLES + "]+$");
	}
	
	/**
	 * Check if the given word contains both Hangul and English.
	 * @param word
	 * @return True if the given word contains English.
	 */
	public static boolean isHangulWithEnglish(String word) {
		if (word.length() == 0)
			return false;
		return word.matches("^[" + REGEX_HANGUL_SYLLABLES + "]*[" + REGEX_LATIN_BASIC + "]+[" + REGEX_HANGUL_SYLLABLES + "]*$");
	}
	
	public static boolean isTextWithNumbers(String word) {
		if (word.length() == 0)
			return false;
		return word.matches("^[" + REGEX_HANGUL_SYLLABLES + REGEX_LATIN_BASIC + "]+[" + REGEX_NUMBERS + "]+[" + REGEX_HANGUL_SYLLABLES + REGEX_LATIN_BASIC + "]+$");
	}
	
	public static boolean isTextWithSymbols(String word) {
		if (word.length() == 0)
			return false;
		return word.matches("^[" + REGEX_HANGUL_SYLLABLES + REGEX_LATIN_BASIC + "]+[" + REGEX_SYMBOLS + "]+[" + REGEX_HANGUL_SYLLABLES + REGEX_LATIN_BASIC + "]+$");
	}
	
	/**
	 * Check if the given word is English word or not.
	 * @param word
	 * @return True if the given word is English.
	 */
	public static boolean isEnglish(String word) {
		if (word.length() == 0)
			return false;
		return word.matches("^[" + REGEX_LATIN_BASIC + "]+$");
	}
	
	/**
	 * Check if the given word is Latin word or not.
	 * @param word
	 * @return True if the given word is Latin.
	 */
	public static boolean isLatin(String word) {
		if (word.length() == 0)
			return false;
		return word.matches("['" + REGEX_LATIN + "]+");
	}
	
	/**
	 * Check if the given word has no symbol or any other marks(i.e., question mark).
	 * @param word
	 * @return True if the given word has no symbol.
	 */
	public static boolean isWord(String word) {
		if (word.length() == 0)
			return false;
		return word.matches("['" + REGEX_SCRIPTS + "]+");
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
	
	/**
	 * Print a given log message out. You are able to flush the log message to *.log file as well.
	 * @param log log message
	 * @param flush If true, the concatenated log texts are saved into a log file.
	 */
	private static String msgLog = new String();
	public static void printLog(String log, boolean flush) {
		System.out.println(log);
		msgLog = msgLog.concat(log + "\r\n");
		
		if (flush == true) {
			PrintWriter writer = null;
			try {
				writer = new PrintWriter(Settings.PATH_DATA + "crawling_info.log", "utf-8");
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
