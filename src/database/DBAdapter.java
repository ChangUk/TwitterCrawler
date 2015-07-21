package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public abstract class DBAdapter {
	// Connection information
	protected String CONNECTION_URL;					// JDBC URL to connect
	protected String DATABASE_NAME;						// Database name
	protected String USER_ID;							// Access user ID
	protected String USER_PASSWORD;						// Access user password
	
	// Connection variables
	protected Connection conn = null;					// Connection for DB write
	protected ConnectionPool connectionPool = null;		// Connection pool for DB read
	
	// Abstract methods
	public abstract boolean createDatabase();
	public abstract boolean makeConnections();
	public abstract boolean closeConnections();
	
	protected synchronized boolean execQuery(String sql) {
		try {
			Statement stmt = conn.createStatement();
			stmt.executeUpdate(sql);
			conn.commit();
			stmt.close();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	protected synchronized boolean execQuery(ArrayList<String> sqls) {
		try {
			Statement stmt = conn.createStatement();
			for (String sql : sqls)
				stmt.executeUpdate(sql);
			conn.commit();
			stmt.close();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	protected synchronized boolean execBatchQueries(String sql, ArrayList<String[]> values) {
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
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
}
