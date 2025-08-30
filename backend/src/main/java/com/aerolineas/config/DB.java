package com.aerolineas.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DB {
    private static final HikariDataSource ds;

    static {
        Dotenv env = Dotenv.configure().ignoreIfMissing().load();

        String host = env.get("ORACLE_HOST", "localhost");
        String port = env.get("ORACLE_PORT", "1521");
        String service = env.get("ORACLE_SERVICE", "XEPDB1");
        String user = env.get("ORACLE_USER", "system");
        String pass = env.get("ORACLE_PASSWORD", "oracle");

        String jdbcUrl = String.format("jdbc:oracle:thin:@//%s:%s/%s", host, port, service);

        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(jdbcUrl);
        cfg.setUsername(user);
        cfg.setPassword(pass);
        cfg.setDriverClassName("oracle.jdbc.OracleDriver");
        cfg.setMaximumPoolSize(Integer.parseInt(env.get("ORACLE_POOL_SIZE", "10")));
        cfg.setConnectionTestQuery("SELECT 1 FROM dual");
        cfg.setPoolName("AEROLINEAS-HIKARI");

        ds = new HikariDataSource(cfg);
    }

    public static Connection getConnection() throws SQLException {
        return ds.getConnection();
    }

    public static boolean ping() {
        try (Connection c = getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT 1 FROM dual")) {
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
