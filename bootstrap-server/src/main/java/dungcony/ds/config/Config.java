package dungcony.ds.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Properties;

public class Config {
    private final int serverPort;
    private final Path databasePath;

    public Config(int serverPort, Path databasePath) {
        this.serverPort = serverPort;
        this.databasePath = databasePath;
    }

    /**
     * Doc cau hinh bootstrap-server tu resource config.properties.
     */
    public static Config load() {
        Properties properties = new Properties();
        try (InputStream inputStream = Config.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (IOException e) {
            System.out.println("[WARN] Không thể nạp cấu hình bootstrap: " + e.getMessage());
        }

        int port = parseInt(properties.getProperty("server.port"), 9000);
        Path databasePath = Path.of(properties.getProperty(
                "database.path",
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
}
