package dungcony.ds.config;

import lombok.extern.slf4j.Slf4j;


import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Properties;

@Slf4j
public class Config {
private final int serverPort;
    private final Path databasePath;

    public Config(int serverPort, Path databasePath) {
        this.serverPort = serverPort;
        this.databasePath = databasePath;
    }

    // Đọc cấu hình bootstrap-server từ resource config.properties.
    public static Config load() {
        Properties properties = new Properties();
        try (InputStream inputStream = Config.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (IOException e) {
            log.warn("Không thể nạp cấu hình bootstrap: " + e.getMessage());
        }

        String configuredPort = firstNonBlank(
                System.getProperty("server.port"),
                System.getenv("BOOTSTRAP_SERVER_PORT"),
                System.getenv("PORT"),
                properties.getProperty("server.port")
        );
        int port = parseInt(configuredPort, 9000);
        Path databasePath = Path.of(firstNonBlank(
                System.getProperty("database.path"),
                System.getenv("BOOTSTRAP_DATABASE_PATH"),
                properties.getProperty("database.path"),
                "bootstrap-server/src/main/resources/database/bootstrap-server.db"
        ));
        return new Config(port, databasePath);
    }

    public int getServerPort() {
        return serverPort;
    }

    public Path getDatabasePath() {
        return databasePath;
    }

    private static int parseInt(String value, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
