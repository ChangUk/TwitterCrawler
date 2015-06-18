package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import main.Settings;

import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteConfig.Pragma;

public class DBHelper {
	private final String DBNAME = "TwitterData.sqlite";
	private final String DBPATH = "jdbc:sqlite:" + Settings.PATH_DATA + DBNAME;
	private final String DRIVER = "org.sqlite.JDBC";
	
	private Connection conn = null;						// Connection for DB write
	
	public DBHelper() {
		try {
			// Register the Driver to the jbdc.driver java property
			Class.forName(DRIVER);
			
			// Configuration
			SQLiteConfig config = new SQLiteConfig();
			config.setPragma(Pragma.JOURNAL_MODE, "wal");
			
			// If database does not exist, then it will be created automatically.
			conn = DriverManager.getConnection(DBPATH, config.toProperties());
			conn.setAutoCommit(false);
			
			// Create DB tables
			createDBTables();
		} catch (ClassNotFoundException e) {
		} catch (SQLException e) {
		}
	}
	
	/**
	 * Create database if it does not exist.
	 * @throws SQLException
	 */
	public void createDBTables() throws SQLException {
		Statement stmt = conn.createStatement();
		stmt.executeUpdate("CREATE TABLE IF NOT EXISTS user ("
				+ "id INTEGER PRIMARY KEY, isProtected INTEGER, isVerified INTEGER, lang TEXT, followingsCount INTEGER, followersCount INTEGER, tweetsCount INTEGER, latestTweet INTEGER, date INTEGER)");
		stmt.executeUpdate("CREATE TABLE IF NOT EXISTS follow ("
				+ "source INTEGER, target INTEGER, "
				+ "FOREIGN KEY(source) REFERENCES user(id) ON DELETE CASCADE ON UPDATE CASCADE, PRIMARY KEY(source, target))");
		stmt.executeUpdate("CREATE TABLE IF NOT EXISTS tweet ("
				+ "id INTEGER PRIMARY KEY, author INTEGER, text TEXT, date INTEGER)");
		stmt.executeUpdate("CREATE TABLE IF NOT EXISTS retweet ("
				+ "user INTEGER, tweet INTEGER, "
				+ "FOREIGN KEY(user) REFERENCES user(id) ON DELETE CASCADE ON UPDATE CASCADE, PRIMARY KEY(user, tweet))");
		stmt.executeUpdate("CREATE TABLE IF NOT EXISTS share ("
				+ "user INTEGER, tweet INTEGER, "
				+ "FOREIGN KEY(user) REFERENCES user(id) ON DELETE CASCADE ON UPDATE CASCADE, PRIMARY KEY(user, tweet))");
		stmt.executeUpdate("CREATE TABLE IF NOT EXISTS favorite ("
				+ "user INTEGER, tweet INTEGER, FOREIGN KEY(user) REFERENCES user(id) ON DELETE CASCADE ON UPDATE CASCADE, PRIMARY KEY(user, tweet))");
		stmt.executeUpdate("CREATE TABLE IF NOT EXISTS mention ("
				+ "source INTEGER, target INTEGER, date INTEGER, "
				+ "FOREIGN KEY(source) REFERENCES user(id) ON DELETE CASCADE ON UPDATE CASCADE, PRIMARY KEY(source, target, date))");
		conn.commit();
		stmt.close();
	}
	
	public void closeDBConnection() {
		try {
			if (conn != null)
				conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public ResultSet execSelection(String sql) throws SQLException {
		Statement statement = null;
		try {
			statement = conn.createStatement();
			ResultSet result = statement.executeQuery(sql);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			if (statement != null)
				statement.close();
		}
	}
	
	public synchronized boolean execQuery(String sql) {
		try {
			Statement stmt = conn.createStatement();
			stmt.executeUpdate(sql);
			conn.commit();
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public synchronized boolean batchQueries(String sql, ArrayList<String[]> values) {
		try {
			PreparedStatement prep = conn.prepareStatement(sql);		// SQL query to be compiled should involve question('?') marks.
			for (String[] value : values) {
				for (int i = 0; i < value.length; i++)
					prep.setString(i + 1, value[i]);
				prep.addBatch();
			}
			prep.executeBatch();
			conn.commit();
			prep.close();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
