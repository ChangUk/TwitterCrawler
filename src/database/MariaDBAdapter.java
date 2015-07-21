package database;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;

public class MariaDBAdapter extends DBAdapter {
	private static MariaDBAdapter mInstance = null;
	
	public static synchronized MariaDBAdapter getSingleton() {
		return (mInstance != null) ? mInstance : (mInstance = new MariaDBAdapter());
	}
	
	public MariaDBAdapter() {
		// Connection information
		DRIVER_NAME		= "org.mariadb.jdbc.Driver";
		URL				= "jdbc:mariadb://localhost:3306/";
		DATABASE_NAME	= "TwitterData";
		USER_ID			= "root";
		USER_PASSWORD	= "";
		
		try {
			// Register the Driver to the jbdc.driver java property
			driver = (Driver) Class.forName(DRIVER_NAME).newInstance();
			DriverManager.registerDriver(driver);
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		}
		
		// Create database with the name DATABASE_NAME
		createDatabase();
	}
	
	public synchronized boolean createDatabase() {
		// TODO: 
		return true;
	}
	
	@Override
	public synchronized boolean makeConnections() {
		try {
			// If database does not exist, then it will be created automatically
			if (conn == null || conn.isClosed()) {
				conn = DriverManager.getConnection(URL, USER_ID, USER_PASSWORD);
				conn.setAutoCommit(false);
			}
			
			// Create connection pool
			if (connectionPool == null) {
				connectionPool = new ConnectionPool(URL, USER_ID, USER_PASSWORD);
				connectionPool.setMaxPoolSize(1000);
			}
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	@Override
	public synchronized boolean closeConnections() {
		try {
			if (conn != null) {
				conn.close();
				conn = null;
			}
			if (connectionPool != null) {
				connectionPool.closeAll();
				connectionPool = null;
			}
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	@Override
	public synchronized boolean destroy() {
		try {
			// Removes the specified driver from the DriverManager's list of registered drivers
			if (driver != null)
				DriverManager.deregisterDriver(driver);
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean createDBTables() {
		ArrayList<String> sqls = new ArrayList<String>();
		sqls.add("CREATE TABLE IF NOT EXISTS user ("
				+ "id INTEGER PRIMARY KEY, isSeed INTEGER, isComplete INTEGER, isProtected INTEGER, isVerified INTEGER, lang TEXT, followingsCount INTEGER, followersCount INTEGER, tweetsCount INTEGER, favoritesCount INTEGER, date INTEGER)");
		sqls.add("CREATE TABLE IF NOT EXISTS follow ("
				+ "source INTEGER, target INTEGER, PRIMARY KEY(source, target))");
		sqls.add("CREATE TABLE IF NOT EXISTS tweet ("
				+ "id INTEGER PRIMARY KEY, author INTEGER, text TEXT, isMention INTEGER, date INTEGER)");
		sqls.add("CREATE TABLE IF NOT EXISTS retweet ("
				+ "user INTEGER, tweet INTEGER, PRIMARY KEY(user, tweet))");
		sqls.add("CREATE TABLE IF NOT EXISTS share ("
				+ "user INTEGER, tweet INTEGER, PRIMARY KEY(user, tweet))");
		sqls.add("CREATE TABLE IF NOT EXISTS favorite ("
				+ "user INTEGER, tweet INTEGER, PRIMARY KEY(user, tweet))");
		sqls.add("CREATE TABLE IF NOT EXISTS mention ("
				+ "source INTEGER, target INTEGER, date INTEGER, PRIMARY KEY(source, target, date))");
		return execQuery(sqls);
	}
}
