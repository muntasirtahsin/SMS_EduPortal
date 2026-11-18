package com.eduportal.utils;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * High-performance Database Connection Manager supporting:
 * 1. Cloud Mode: Supabase (PostgreSQL) / MySQL over Internet
 * 2. Local Fallback Mode: SQLite with WAL mode & 8MB memory cache
 */
public class DatabaseConnection {

    private static String dbType = "sqlite";

    private static final String DB_DIR = System.getenv("LOCALAPPDATA") + File.separator + "EduPortal";

    static {
        new File(DB_DIR).mkdirs();
    }

    private static String dbUrl = "jdbc:sqlite:" + DB_DIR + File.separator + "faculty_system.db";

    private static String dbUser = "";
    private static String dbPassword = "";

    private static Connection rawConnection = null;

    static {
        loadConfig();
    }

    private static void loadConfig() {
        File cfg = new File("config.properties");
        String dataDirectory = System.getenv("EDUPORTAL_DATA_DIR");
        if (dataDirectory != null && !dataDirectory.trim().isEmpty()) {
            File persistentConfig = new File(dataDirectory, "config.properties");
            if (persistentConfig.isFile()) {
                cfg = persistentConfig;
            }
        }
        if (cfg.exists()) {
            try (FileInputStream fis = new FileInputStream(cfg)) {
                Properties props = new Properties();
                props.load(fis);
                dbType = props.getProperty("DB_TYPE", "sqlite").trim().toLowerCase();
                String configDbUrl = props.getProperty("DB_URL", "").trim();
                if (!configDbUrl.isEmpty()) {
                    dbUrl = configDbUrl;
                }
                dbUser = props.getProperty("DB_USER", "").trim();
                dbPassword = props.getProperty("DB_PASSWORD", "").trim();
            } catch (Exception e) {
                System.err.println("[DatabaseConnection] Config read error: " + e.getMessage());
            }
        }

        // Register JDBC Drivers
        try {
            if ("postgres".equals(dbType) || "postgresql".equals(dbType)) {
                Class.forName("org.postgresql.Driver");
            } else if ("mysql".equals(dbType)) {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } else {
                Class.forName("org.sqlite.JDBC");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("[DatabaseConnection] JDBC Driver Error: " + e.getMessage());
        }
    }

    private static synchronized Connection getRawConnection() throws SQLException {
        if (rawConnection == null || rawConnection.isClosed()) {
            if ("sqlite".equals(dbType)) {
                rawConnection = DriverManager.getConnection(dbUrl);
                try (Statement stmt = rawConnection.createStatement()) {
                    stmt.execute("PRAGMA journal_mode = WAL;");
                    stmt.execute("PRAGMA synchronous = NORMAL;");
                    stmt.execute("PRAGMA busy_timeout = 5000;");
                    stmt.execute("PRAGMA cache_size = -8000;");
                } catch (Exception ignored) {
                }
            } else {
                if (!dbUser.isEmpty()) {
                    rawConnection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                } else {
                    rawConnection = DriverManager.getConnection(dbUrl);
                }
            }
        }
        return rawConnection;
    }

    /**
     * Returns a high-performance proxy connection that reuses the persistent
     * database
     * connection and safely ignores close() calls so subsequent queries execute in
     * microseconds.
     */
    public static Connection getConnection() throws SQLException {
        final Connection raw = getRawConnection();
        return (Connection) Proxy.newProxyInstance(
                DatabaseConnection.class.getClassLoader(),
                new Class<?>[] { Connection.class },
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if ("close".equals(method.getName())) {
                            return null;
                        }
                        if ("isClosed".equals(method.getName())) {
                            return raw.isClosed();
                        }
                        return method.invoke(raw, args);
                    }
                });
    }
}
