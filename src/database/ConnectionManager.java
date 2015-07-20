package database;

public class ConnectionManager {
	public static final String PATH_DATA = "../Data/TwitterData/";
	private static DatabaseInfo mDBInfo = DatabaseInfo.SQLITE;
	
	public static DatabaseInfo getDatabaseType() {
		return mDBInfo;
	}
	
	public static String getDriverName() {
		return mDBInfo.driver;
	}
	
	public static String getDatabaseName() {
		return mDBInfo.name;
	}
	
	public static String getConnectionURL() {
		return mDBInfo.URL + PATH_DATA + mDBInfo.name;
	}
	
	public static String getDatabasePath() {
		return PATH_DATA + mDBInfo.name;
	}
	
	public static String getBackupPath() {
		return PATH_DATA + mDBInfo.name + ".backup";
	}
	
	public static String getUserID() {
		return mDBInfo.uid;
	}
	
	public static String getUserPwd() {
		return mDBInfo.pwd;
	}
	
	public static enum DatabaseInfo {
		SQLITE	("org.sqlite.JDBC",			"jdbc:sqlite:",		"TwitterData.sqlite",	null,	null),
		MARIADB	("org.mariadb.jdbc.Driver",	"jdbc:mariadb://",	"TwitterData.mariadb",	null,	null);
		
		public final String driver;
		public final String URL;
		public final String name;
		public final String uid;
		public final String pwd;
		
		private DatabaseInfo (String driver, String URL, String name, String uid, String pwd) {
			this.driver = driver;
			this.URL = URL;
			this.name = name;
			this.uid = uid;
			this.pwd = pwd;
		}
	}
}
