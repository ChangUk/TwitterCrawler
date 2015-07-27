package tool;

public class TextChecker {
	// REGULAR EXPRESSIONS (LATIN)
	public static final String REGEX_LATIN_BASIC = "\\p{InBasic_Latin}";				//	"A-Za-z";
	public static final String REGEX_LATIN_SUPPLEMENT = "\\p{InLatin-1_Supplement}";	//	"À-ÖÙ-öù-ÿ";
	public static final String REGEX_LATIN_EXTENDED_A = "\\p{InLatin_Extended-A}";		//	"Ā-ſ";
	public static final String REGEX_LATIN_EXTENDED_B = "\\p{InLatin_Extended-B}";		//	"ƀ-ɏ";
	public static final String REGEX_LATIN = "\\p{Latin}";								//	REGEX_LATIN_BASIC + REGEX_LATIN_SUPPLEMENT + REGEX_LATIN_EXTENDED_A + REGEX_LATIN_EXTENDED_B;
	
	// REGULAR EXPRESSIONS (KOREAN)
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
}
