package agzam4.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

import agzam4.database.SQL.TableColumnInfo;
import agzam4.utils.Log;
import arc.files.Fi;
import arc.func.Cons;
import arc.func.Func;
import arc.struct.Seq;
import arc.util.Nullable;

public class Database implements AutoCloseable {

	private @Nullable Connection connection;
	private Fi path;

	public Database(Fi path) throws ClassNotFoundException, SQLException {
		this.path = path;
		Class.forName("org.sqlite.JDBC");
        String url = "jdbc:sqlite:" + path.absolutePath();
//        try {
        var c = DriverManager.getConnection(url);
        Log.info("Connected to SQL: [blue]@[]", path.name());
        connection = c;
//        } catch (SQLException e) {
//        	Log.err(e);
//        }
	}
	
	@Override
	public void close() throws Exception {
        Log.info("Disconnected from: [blue]@[]", path.name());
		connection.close();		
	}
	
	/**
	 * Creates new table
	 * @param name - name of table
	 */
	public <T> Table<T> createTable(String name, Class<T> type) {
		return new Table<T>(this, name, type);
	}

	/**
	 * Executes the given SQL statement
	 * @param SQL statement
	 */
	public void execute(String sql) {
		try (Statement s = connection.createStatement()) {
			s.execute(sql);
		} catch (SQLException e) {
			Log.err(e);
		}
	}
	
	/**
	 * Executes the SQL statement in this PreparedStatement object, 
	 * which must be an SQL Data Manipulation Language (DML) statement, 
	 * such as INSERT, UPDATE or DELETE; or an SQL statement that returns nothing, 
	 * such as a DDL statement.
	 * 
	 * @param SQL statement
	 * @param args - object to "?" places
	 */
	public void update(String sql, Object... args) {
		try (PreparedStatement s = connection.prepareStatement(sql)) {
			for (int i = 0; i < args.length; i++) {
				s.setObject(i+1, args[i]);
			}
			s.executeUpdate();
		} catch (SQLException e) {
			Log.err(e);
		}		
	}
	
	/**
	 * Executes the several SQL statement, and rollback on error
	 * @param executes
	 * @return true if all success
	 */
	public boolean executeTransaction(String...executes) {
		try (Statement s = connection.createStatement()) {
			connection.setAutoCommit(false);
			Log.info("[SQL] == [Transaction] ==========");
			for (int i = 0; i < executes.length; i++) {
				if(executes[i] == null) {
					Log.info("[SQL] Transaction (SKIPED)");
				}
				Log.info("[SQL] Transaction > @", executes[i]);
				s.execute(executes[i]);
			}
			connection.commit();
			return true;
		} catch (SQLException e) {
			try {
				connection.rollback();
			} catch (SQLException e1) {
				Log.err(e1);
			}
			Log.err(e);
			return false;
		} finally {
			try {
				Log.info("[SQL] == [Transaction] ==========");
				connection.setAutoCommit(true);
			} catch (SQLException e) {
				Log.err(e);
	        	Log.err("SQL:\n@", Arrays.toString(executes));
			}
		}
	}

	/**
	 * Executes the several SQL statement, and rollback on error
	 * @param sql - SQL statement
	 * @param cons - consumer of results
	 * @return
	 */
	public void queryCons(String sql, Cons<ResultSet> cons) {
		try (Statement stmt = connection.createStatement()) {
			ResultSet set = stmt.executeQuery(sql);
			while (set.next()) cons.get(set);
		} catch (SQLException e) {
			Log.err(e);
		}
	}

	public @Nullable void queryCons(String sql, Cons<ResultSet> cons, Object... args) {
		try (PreparedStatement s = connection.prepareStatement(sql)) {
			for (int i = 0; i < args.length; i++) {
				s.setObject(i+1, args[i]);
			}
			cons.get(s.executeQuery());
		} catch (SQLException e) {
			Log.err(e);
		}	
	}

	public @Nullable <T> T query(String sql, Func<ResultSet, T> func) {
		T result = null;
		try (Statement stmt = connection.createStatement()) {
			result = func.get(stmt.executeQuery(sql));
		} catch (SQLException e) {
			Log.err(e);
		}
		return result;
	}

	public @Nullable <T> T query(String sql, Func<ResultSet, T> func, Object... args) {
		try (PreparedStatement s = connection.prepareStatement(sql)) {
			for (int i = 0; i < args.length; i++) {
				s.setObject(i+1, args[i]);
			}
			return func.get(s.executeQuery());
		} catch (SQLException e) {
			Log.err(e);
		}	
		return null;
	}

	/**
	 * Query info about table by name
	 * @param name - name of table
	 * @return map of type by filed name
	 */
	public @Nullable Seq<TableColumnInfo> queryTableInfo(String name) {
		return query("PRAGMA table_info(" + name + ")", result -> {
			try {
				Seq<TableColumnInfo> infos = new Seq<>();
				while (result.next()) infos.add(new TableColumnInfo(result));
				return infos;
			} catch (SQLException e) {
				Log.err(e);
			}
			return null;
		});
	}


	
	
}
