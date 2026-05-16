package dungcony.ds.repositories;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public class Conn {
    private static final Logger LOGGER = LoggerFactory.getLogger(Conn.class);
    private final String jdbcUrl;

    public Conn(Path databasePath) {
        ensureParentDirectory(databasePath);
        this.jdbcUrl = "jdbc:sqlite:" + databasePath.toAbsolutePath();
        LOGGER.info("Đã cấu hình kết nối database bootstrap. url={}", jdbcUrl);
    }

    public Connection getConnection() throws SQLException {
        LOGGER.debug("Đang mở kết nối SQLite. url={}", jdbcUrl);
        return DriverManager.getConnection(jdbcUrl);
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    private void ensureParentDirectory(Path databasePath) {
        Path parent = databasePath.toAbsolutePath().getParent();
        if (parent == null) {
            return;
        }
        try {
            Files.createDirectories(parent);
            LOGGER.debug("Đã đảm bảo thư mục database tồn tại: {}", parent);
        } catch (IOException e) {
            throw new IllegalStateException("Không thể tạo thư mục database: " + parent, e);
        }
    }


}
