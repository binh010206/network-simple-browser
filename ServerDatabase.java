import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.OffsetDateTime;
import java.util.Properties;

/** Only the HTTP server accesses PostgreSQL. Credentials stay on the server. */
public final class ServerDatabase {
    private static final Properties config = new Properties();

    public static void initialize() throws Exception {
        Path file = Path.of("database.local.properties");
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                config.load(reader);
            }
        }
        for (String key : new String[]{"url", "user", "password"}) {
            String value = System.getenv("BROWSER_DB_" + key.toUpperCase());
            if (value != null) config.setProperty(key, value);
            if (config.getProperty(key, "").isBlank())
                throw new IllegalStateException("Thiếu cấu hình database.local.properties: " + key);
        }
        Class.forName("org.postgresql.Driver");
        DriverManager.setLoginTimeout(5);
        try (Connection connection = open(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS public.browser_history (
                    id BIGSERIAL PRIMARY KEY,
                    time VARCHAR(50) NOT NULL,
                    method VARCHAR(10) NOT NULL,
                    url TEXT NOT NULL,
                    post_data TEXT,
                    status_code INTEGER NOT NULL CHECK (status_code BETWEEN 100 AND 599),
                    content_length BIGINT NOT NULL CHECK (content_length >= 0),
                    client_ip VARCHAR(45)
                )
                """);
            statement.executeUpdate("ALTER TABLE public.browser_history ADD COLUMN IF NOT EXISTS client_ip VARCHAR(45)");
            System.out.println("[PostgreSQL] Đã kết nối " + connection.getCatalog() + ".public.browser_history");
        }
    }

    private static Connection open() throws SQLException {
        Properties properties = new Properties();
        properties.setProperty("user", config.getProperty("user"));
        properties.setProperty("password", config.getProperty("password"));
        properties.setProperty("connectTimeout", "5");
        properties.setProperty("socketTimeout", "10");
        return DriverManager.getConnection(config.getProperty("url"), properties);
    }

    public static long record(String ip, String method, String url, String data, int status, long length) throws SQLException {
        try (Connection connection = open(); PreparedStatement insert = connection.prepareStatement("""
            INSERT INTO public.browser_history (time, method, url, post_data, status_code, content_length, client_ip)
            VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id
            """)) {
            insert.setString(1, OffsetDateTime.now().toString());
            insert.setString(2, method);
            insert.setString(3, url);
            insert.setString(4, data);
            insert.setInt(5, status);
            insert.setLong(6, length);
            insert.setString(7, ip);
            try (ResultSet rows = insert.executeQuery()) {
                rows.next();
                return rows.getLong(1);
            }
        }
    }
}
