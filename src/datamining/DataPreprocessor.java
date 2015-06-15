package datamining;

import java.util.ArrayList;
import java.util.StringTokenizer;

import tool.Utils;

public class DataPreprocessor {
	public DataPreprocessor() {
		
	}
	
	public String getRefinedTweet(String tweet) {
		ArrayList<String> wordList = new ArrayList<String>();
		StringTokenizer st = new StringTokenizer(tweet, " \t\r\n");
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			if (token.startsWith("@") || token.startsWith("http://t.co/") || token.startsWith("https://t.co/"))
				continue;
			if (Utils.isTextWithNumbers(token) || Utils.isTextWithSymbols(token))
				continue;
			String[] words = token.split(Utils.REGEX_SYMBOLS);
			for (String word : words) {
				if (word.equals("RT"))
					continue;
				
				if (Utils.isHangulSyllables(word) || Utils.isHangulWithEnglish(word))
					wordList.add(word.toLowerCase());
			}
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
}
