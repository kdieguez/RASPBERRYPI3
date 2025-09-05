package com.aerolineas.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.cdimascio.dotenv.Dotenv;

import javax.sql.DataSource;
import java.net.URI;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;

public class DB {
  private static HikariDataSource ds;

  public static void init() {
    try {
      if (ds != null && !ds.isClosed()) return;

      Path cwd = Paths.get("").toAbsolutePath();
      Path jarDir;
      try {
        URI codeSrc = DB.class.getProtectionDomain().getCodeSource().getLocation().toURI();
        jarDir = Paths.get(codeSrc).getParent(); 
      } catch (Exception e) {
        jarDir = null;
      }
      Path projectDirFromJar = (jarDir != null) ? jarDir.getParent() : null; 

      Path[] candidates = new Path[] {
          cwd,                                   
          projectDirFromJar,                     
          cwd.getParent()                        
      };

      Dotenv env = null;
      Path usedDir = null;
      for (Path p : candidates) {
        if (p == null) continue;
        if (Files.exists(p.resolve(".env"))) {
          env = Dotenv.configure()
              .filename(".env")
              .directory(p.toString())
              .ignoreIfMalformed()
              .load();
          usedDir = p;
          break;
        }
      }
      System.out.println("[DB] cwd=" + cwd);
      System.out.println("[DB] .env found at=" + (usedDir == null ? "NOT FOUND" : usedDir));

      if (env == null) {
        env = Dotenv.configure().ignoreIfMissing().ignoreIfMalformed().load();
      }

      final Dotenv finalEnv = env;
      Function<String,String> get = k -> {
        String v = finalEnv.get(k);
        if (v == null || v.isBlank()) v = System.getenv(k);
        return v;
      };

      String user = get.apply("ORACLE_USER");
      String pass = firstNonBlank(get.apply("ORACLE_PASSWORD"), get.apply("ORACLE_PASS"));
      if (isBlank(user)) throw new RuntimeException("Falta ORACLE_USER (en .env o variables)");
      if (isBlank(pass)) throw new RuntimeException("Falta ORACLE_PASSWORD u ORACLE_PASS (en .env o variables)");
      user = user.trim();
      pass = pass.trim();

      String url = get.apply("ORACLE_JDBC_URL");
      if (isBlank(url)) {
        String cs = get.apply("ORACLE_CONNECT_STRING"); // host[:port]/service
        if (isBlank(cs)) {
          String host = nvl(get.apply("ORACLE_HOST"), "localhost");
          String port = nvl(get.apply("ORACLE_PORT"), "1521");
          String svc  = nvl(firstNonBlank(get.apply("ORACLE_SERVICE"), get.apply("ORACLE_SERVICE_NAME"), get.apply("ORACLE_PDB")), "XEPDB1");
          cs = host + ":" + port + "/" + svc;
        } else if (!cs.contains(":")) {
          cs = cs.replace("/", ":1521/"); // agrega puerto 1521 si faltaba
        }
        url = "jdbc:oracle:thin:@//" + cs;
      }

      int pool = Integer.parseInt(nvl(get.apply("DB_POOL"), "10"));

      HikariConfig cfg = new HikariConfig();
      cfg.setJdbcUrl(url);
      cfg.setUsername(user);
      cfg.setPassword(pass);
      cfg.setMaximumPoolSize(pool);
      cfg.setDriverClassName("oracle.jdbc.OracleDriver");
      cfg.addDataSourceProperty("oracle.jdbc.fanEnabled", "false");

      System.out.println("[DB] using url=" + url + " user=" + user + " pass.len=" + pass.length());

      ds = new HikariDataSource(cfg);
      System.out.println("[DB] pool ready");
    } catch (Exception e) {
      throw new RuntimeException("No se pudo inicializar el pool de conexiones", e);
    }
  }

  public static DataSource dataSource(){ return ds; }

  public static Connection getConnection() throws SQLException {
    try {
      if (ds == null) init();
      return ds.getConnection();
    } catch (SQLException e) {
      throw e;
    } catch (RuntimeException re) {
      SQLException ex = new SQLException("No se pudo inicializar la conexión", re);
      ex.addSuppressed(re);
      throw ex;
    }
  }

  public static boolean ping() {
    try (Connection c = getConnection()) { return c.isValid(2); }
    catch (Exception e) { return false; }
  }

  private static boolean isBlank(String s){ return s==null || s.isBlank(); }
  private static String nvl(String v, String def){ return isBlank(v) ? def : v; }
  private static String firstNonBlank(String... vs){
    if (vs == null) return null;
    for (String v : vs) if (!isBlank(v)) return v;
    return null;
  }
}
