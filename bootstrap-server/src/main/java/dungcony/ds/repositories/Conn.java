package dungcony.ds.repositories;

import lombok.extern.slf4j.Slf4j;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


@Slf4j
public class Conn {
    private final String jdbcUrl;

    public Conn(Path databasePath) {
        ensureParentDirectory(databasePath);
        this.jdbcUrl = "jdbc:sqlite:" + databasePath.toAbsolutePath();
        log.info("Đã cấu hình kết nối database bootstrap. url={}", jdbcUrl);
    }

    public Connection getConnection() throws SQLException {
        log.debug("Đang mở kết nối SQLite. url={}", jdbcUrl);
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
            log.debug("Đã đảm bảo thư mục database tồn tại: {}", parent);
        } catch (IOException e) {
            throw new IllegalStateException("Không thể tạo thư mục database: " + parent, e);
        }
    }


}
