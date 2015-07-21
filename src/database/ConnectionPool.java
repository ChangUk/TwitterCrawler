package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Queue;

public class ConnectionPool {
	// List of free connections
	private Queue<Connection> freeConnections;
	
	// Number of connections in use.
	private int inUseConnectionCount;
	
	// Connection settings
	private Properties properties;		// Connection properties
	private int maxConn;				// Allow limit-less multiple connections if this value is 0.
	private String URL;					// Path of database
	private String userID;				// Access user ID
	private String password;			// Access user password
	
	public ConnectionPool(String URL) {
		this.freeConnections = new LinkedList<Connection>();
		this.inUseConnectionCount = 0;
		this.properties = null;
		this.maxConn = 0;
		this.URL = URL;
		this.userID = null;
		this.password = null;
	}
	
	public ConnectionPool(String URL, Properties properties) {
		this.freeConnections = new LinkedList<Connection>();
		this.inUseConnectionCount = 0;
		this.properties = properties;
		this.maxConn = 0;
		this.URL = URL;
		this.userID = null;
		this.password = null;
	}
	
	public ConnectionPool(String URL, String userID, String userPwd) {
		this.freeConnections = new LinkedList<Connection>();
		this.inUseConnectionCount = 0;
		this.properties = null;
		this.maxConn = 0;
		this.URL = URL;
		this.userID = userID;
		this.password = userPwd;
	}
	
	/**
	 * Set maximum size of connection pool.
	 * @param num Connection pool size
	 */
	public void setMaxPoolSize(int size) {
		this.maxConn = size;
	}
	
	/**
	 * Get available connection from the pool. 
	 * @return Available connection
	 */
	public synchronized Connection getConnection() {
		Connection conn = null;
		
		// If there is no available connection,
		if (freeConnections.isEmpty()) {
			try {
				// If it is allowed to make more connections,
				if (maxConn == 0 || inUseConnectionCount < maxConn) {
					// Create new connection
					conn = getNewConnection();
					conn.setAutoCommit(false);
				}
			} catch (SQLException e) {
				e.printStackTrace();
				return null;
			}
		} else {
			// If there is available connection, take it.
			conn = freeConnections.poll();
			// If the connection has a problem, get another connection from the pool.
			if (isAvailableConnection(conn) == false)
				conn = getConnection();
		}
		
		// If there is no available connection and all connections are running now, wait until a connection gets to be free.
		if (conn == null) {
			try {
				wait();
			} catch (InterruptedException e) {
			}
			// A connection returns to be free and retry getting connection.
			conn = getConnection();
		} else
			inUseConnectionCount += 1;
		return conn;
	}
	
	/**
	 * Check if the connection is available
	 * @param conn Connection to be tested
	 * @return <code>true</code> if the connection is available, <code>false</code> otherwise.
	 */
	public boolean isAvailableConnection(Connection conn) {
		try {
			if (conn == null || conn.isClosed())
				return false;
		} catch (SQLException e) {
			return false;
		}
		return true;
	}
	
	/**
	 * Create new connection and then get the connection.
	 * If any database does not exist, then it will be created automatically.
	 * @return Newly created connection
	 */
	public Connection getNewConnection() {
		Connection newConnection = null;
		try {
			if (userID == null) {
				if (properties == null)
					newConnection = DriverManager.getConnection(URL);
				else
					newConnection = DriverManager.getConnection(URL, properties);
			} else
				newConnection = DriverManager.getConnection(URL, userID, password);
		} catch (SQLException e) {
			return null;
		}
		return newConnection;
	}
	
	/**
	 * Put the connection at the end of the Vector.
	 * @param conn Connection to be free
	 */
	public synchronized void freeConnection(Connection conn) {
		freeConnections.offer(conn);
		inUseConnectionCount -= 1;
		
		// Notify this event to all threads
		notifyAll();
	}
	
	/**
	 * Close all free connections.
	 * If there is any connection alive, wait until the connection gets to be free.
	 */
	public void closeAll() throws SQLException {
		while (inUseConnectionCount > 0) {
			try {
				// Wait until all connections get to be free
				wait();
			} catch (InterruptedException e) {
			}
		}
		
		while (freeConnections.isEmpty() == false) {
			Connection conn = freeConnections.poll();
			conn.close();
		}
	}
}
