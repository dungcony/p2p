package dungcony.ds.config;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dungcony.ds.model.PeerNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class PeerConfig {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PeerConfig.class);
private static final Path DEFAULT_DATA_DIR = Path.of("peer-node", "src", "main", "resources", "data");

    private Path dataRoot;
    private Path dataDir;
    private Path configPath;
    private String peerId;
    private String peerName;
    private int peerPort;
    private String bootstrapHost;
    private int bootstrapPort;

    // Đọc cấu hình peer từ file resources/data/config.properties và tạo id nếu chưa có.
    public static PeerConfig load() {
        return load(DEFAULT_DATA_DIR);
    }

    // Đọc cấu hình peer từ dataDir riêng của instance hiện tại.
    public static PeerConfig load(Path dataDir) {
        Path dataRoot = dataDir == null ? DEFAULT_DATA_DIR : dataDir.normalize();
        Path loadedConfigPath = resolveConfigPath(dataRoot);
        Properties profileProperties = new Properties();
        Properties globalProperties = loadGlobalProperties(dataRoot);
        try {
            Files.createDirectories(dataRoot);
            LOGGER.info("Thư mục dữ liệu peer đã sẵn sàng: " + dataRoot.toAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("Không thể tạo thư mục dữ liệu peer: " + e.getMessage());
        }
        if (loadedConfigPath != null && Files.exists(loadedConfigPath)) {
            try (InputStream inputStream = Files.newInputStream(loadedConfigPath)) {
                profileProperties.load(inputStream);
                LOGGER.info("Đã nạp cấu hình peer từ " + loadedConfigPath.toAbsolutePath());
            } catch (IOException e) {
                LOGGER.warn("Không thể nạp cấu hình peer. Dùng mặc định. lỗi=" + e.getMessage());
            }
        }

        PeerConfig config = new PeerConfig();
        config.dataRoot = dataRoot;
        config.peerId = readString(profileProperties, "peer.id", UUID.randomUUID().toString());
        config.peerName = readString(profileProperties, "peer.name", System.getProperty("user.name", "peer"));
        config.peerPort = readInt(profileProperties, "peer.port", PeerNode.DEFAULT_PORT);
        config.bootstrapHost = readString(globalProperties, "bootstrap.host", "localhost");
        config.bootstrapPort = readInt(globalProperties, "bootstrap.port", 9000);
        config.refreshStoragePaths();
        LOGGER.info("Đã chọn thư mục dữ liệu peer: " + config.dataDir.toAbsolutePath());
        return config;
    }

    // Tạo profile mới với peer.id la UUID và folder profile cùng tên UUID.
    public static PeerConfig createNew(Path dataRoot) {
        Path resolvedDataRoot = dataRoot == null ? DEFAULT_DATA_DIR : dataRoot.normalize();
        PeerConfig config = new PeerConfig();
        config.dataRoot = resolvedDataRoot;
        config.peerId = UUID.randomUUID().toString();
        config.peerName = System.getProperty("user.name", "peer");
        config.peerPort = PeerNode.DEFAULT_PORT;
        Properties globalProperties = loadGlobalProperties(resolvedDataRoot);
        config.bootstrapHost = readString(globalProperties, "bootstrap.host", "localhost");
        config.bootstrapPort = readInt(globalProperties, "bootstrap.port", 9000);
        config.refreshStoragePaths();
        LOGGER.info("Đã tạo nháp profile peer mới. peerId=" + config.peerId
                + ", thưMụcDữLiệu=" + config.dataDir.toAbsolutePath());
        return config;
    }

    // Liet ke các profile đã có trong data root, mới profile la một folder UUID có config.properties.
    public static List<PeerConfig> listProfiles(Path dataRoot) {
        Path resolvedDataRoot = dataRoot == null ? DEFAULT_DATA_DIR : dataRoot.normalize();
        List<PeerConfig> profiles = new ArrayList<>();
        try {
            Files.createDirectories(resolvedDataRoot);
            try (Stream<Path> paths = Files.list(resolvedDataRoot)) {
                List<Path> configPaths = paths
                        .filter(Files::isDirectory)
                        .map(path -> path.resolve("config.properties"))
                        .filter(Files::exists)
                        .sorted(Comparator.comparing(path -> path.getParent().getFileName().toString()))
                        .toList();
                for (Path configPath : configPaths) {
                    profiles.add(loadFromConfigPath(resolvedDataRoot, configPath));
                }
            }
            Path legacyConfigPath = resolvedDataRoot.resolve("config.properties");
            if (profiles.isEmpty() && isLegacyPeerConfig(legacyConfigPath)) {
                LOGGER.info("Tìm thấy cấu hình peer cũ dưới dạng profile có sẵn. "
                        + "It will be migrated to UUID folder on start.");
                profiles.add(loadFromConfigPath(resolvedDataRoot, legacyConfigPath));
            }
        } catch (IOException e) {
            LOGGER.warn("Không thể liệt kê profile peer: " + e.getMessage());
        }
        LOGGER.info("Số profile peer tìm thấy=" + profiles.size()
                + ", dataRoot=" + resolvedDataRoot.toAbsolutePath());
        return profiles;
    }

    // Cập nhật tên và port sau khi người dùng bấm Start ở màn hình đăng nhập.
    public void updateLogin(String peerId, String peerName, int peerPort) {
        this.peerId = peerId == null || peerId.isBlank() ? this.peerId : peerId.trim();
        this.peerName = peerName == null || peerName.isBlank() ? this.peerName : peerName.trim();
        this.peerPort = peerPort;
        refreshStoragePaths();
    }

    // Cập nhật định danh hiển thị; port lắng nghe không đổi trong dialog UI.
    public void updateIdentity(String peerId, String peerName) {
        this.peerId = peerId == null || peerId.isBlank() ? this.peerId : peerId.trim();
        this.peerName = peerName == null || peerName.isBlank() ? this.peerName : peerName.trim();
        refreshStoragePaths();
    }

    // Cập nhật port lắng nghe của peer khi tạo profile mới.
    public boolean updatePeerPort(int peerPort) {
        if (peerPort < 1 || peerPort > 65535) {
            LOGGER.warn("Đã bỏ qua cổng peer không hợp lệ=" + peerPort);
            return false;
        }
        if (peerPort == bootstrapPort) {
            LOGGER.warn("Từ chối cổng peer vì trùng với cổng bootstrap=" + bootstrapPort);
            return false;
        }
        this.peerPort = peerPort;
        return true;
    }

    // Áp dụng peer port được truyền lúc chạy app qua CLI.
    public void applyRuntimePeerPort(Integer runtimePeerPort) {
        if (runtimePeerPort == null) {
            protectBootstrapPort();
            return;
        }
        if (runtimePeerPort < 1 || runtimePeerPort > 65535) {
            LOGGER.warn("Đã bỏ qua cổng peer runtime không hợp lệ=" + runtimePeerPort
                    + ". Giữ cổng=" + peerPort);
            protectBootstrapPort();
            return;
        }
        if (!updatePeerPort(runtimePeerPort)) {
            protectBootstrapPort();
        }
    }

    // Lưu cấu hình peer để lần sau app dùng lại cùng peer.id khi đăng nhập.
    public void save() {
        Properties profileProperties = new Properties();
        profileProperties.setProperty("peer.id", peerId);
        profileProperties.setProperty("peer.name", peerName);
        profileProperties.setProperty("peer.port", String.valueOf(peerPort));
        try {
            Files.createDirectories(configPath.getParent());
            try (OutputStream outputStream = Files.newOutputStream(configPath)) {
                profileProperties.store(outputStream, "Local peer identity profile");
            }
            saveGlobalConfig();
            LOGGER.info("Đã lưu cấu hình peer. peerId=" + peerId
                    + ", tênPeer=" + peerName + ", cổng=" + peerPort
                    + ", thưMụcDữLiệu=" + dataDir.toAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("Không thể lưu cấu hình peer: " + e.getMessage());
        }
    }

    // Lấy data directory riêng của instance peer-node hiện tại.
    public Path getDataDir() {
        return dataDir;
    }

    // Lấy data root chua các folder profile peer theo UUID.
    public Path getDataRoot() {
        return dataRoot;
    }

    // Label ngắn gọn để hiển thị trong dialog chọn profile.
    public String getDisplayLabel() {
        return peerName + " | " + peerPort + " | " + peerId;
    }

    // Lấy id ổn định dùng làm khóa user_id trên bootstrap-server.
    public String getPeerId() {
        return peerId;
    }

    // Lấy tên hiển thị của peer.
    public String getPeerName() {
        return peerName;
    }

    // Lấy port TCP peer-node se lắng nghe.
    public int getPeerPort() {
        return peerPort;
    }

    // Lấy host của bootstrap-server.
    public String getBootstrapHost() {
        return bootstrapHost;
    }

    // Lấy port của bootstrap-server.
    public int getBootstrapPort() {
        return bootstrapPort;
    }

    // Nếu peer.port trùng bootstrap.port thì không cho PeerNode chiếm cổng tracker.
    private void protectBootstrapPort() {
        if (peerPort == bootstrapPort) {
            LOGGER.warn("peer.port trùng với cổng bootstrap=" + bootstrapPort
                    + ". Chuyển về cổng mặc định của peer=" + PeerNode.DEFAULT_PORT);
            peerPort = PeerNode.DEFAULT_PORT;
        }
    }

    // Đọc string property với fallback khi value rỗng.
    private static String readString(Properties properties, String key, String defaultValue) {
        String value = properties.getProperty(key);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    // Đọc int property với fallback khi value không hợp lệ.
    private static int readInt(Properties properties, String key, int defaultValue) {
        try {
            return Integer.parseInt(readString(properties, key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            LOGGER.warn("Config số nguyên không hợp lệ. key=" + key + ". fallback=" + defaultValue);
            return defaultValue;
        }
    }

    // Cập nhật dataDir/configPath theo peer.id hiện tại để folder được dat theo UUID.
    private void refreshStoragePaths() {
        this.dataDir = dataRoot.resolve(safePathSegment(peerId));
        this.configPath = dataDir.resolve("config.properties");
    }

    // Tìm config có sẵn trong data root: ưu tiên folder UUID con, sau đó mới đến config legacy.
    private static Path resolveConfigPath(Path dataRoot) {
        try {
            if (Files.exists(dataRoot)) {
                try (Stream<Path> paths = Files.list(dataRoot)) {
                    List<Path> configPaths = paths
                            .filter(Files::isDirectory)
                            .map(path -> path.resolve("config.properties"))
                            .filter(Files::exists)
                            .sorted(Comparator.comparing(path -> path.getParent().getFileName().toString()))
                            .toList();
                    if (configPaths.size() > 1) {
                        LOGGER.warn("Tìm thấy nhiều thư mục UUID peer trong data root. "
                                + "Using first folder by name: " + configPaths.get(0).getParent().getFileName());
                    }
                    if (!configPaths.isEmpty()) {
                        return configPaths.get(0);
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Không thể quét thư mục dữ liệu peer: " + e.getMessage());
        }

        Path legacyConfigPath = dataRoot.resolve("config.properties");
        if (isLegacyPeerConfig(legacyConfigPath)) {
            LOGGER.info("Tìm thấy cấu hình peer cũ. Cấu hình này sẽ được lưu vào thư mục UUID sau khi đăng nhập.");
            return legacyConfigPath;
        }
        return null;
    }

    // Đọc một profile cũ từ file config.properties trong folder UUID.
    private static PeerConfig loadFromConfigPath(Path dataRoot, Path configPath) {
        Properties properties = new Properties();
        Properties globalProperties = loadGlobalProperties(dataRoot);
        try (InputStream inputStream = Files.newInputStream(configPath)) {
            properties.load(inputStream);
        } catch (IOException e) {
            LOGGER.warn("Không thể nạp cấu hình profile=" + configPath
                    + ", lỗi=" + e.getMessage());
        }

        PeerConfig config = new PeerConfig();
        config.dataRoot = dataRoot;
        config.peerId = readString(properties, "peer.id", configPath.getParent().getFileName().toString());
        config.peerName = readString(properties, "peer.name", System.getProperty("user.name", "peer"));
        config.peerPort = readInt(properties, "peer.port", PeerNode.DEFAULT_PORT);
        config.bootstrapHost = readString(globalProperties, "bootstrap.host", "localhost");
        config.bootstrapPort = readInt(globalProperties, "bootstrap.port", 9000);
        config.refreshStoragePaths();
        LOGGER.info("Đã nạp profile peer. " + config.getDisplayLabel()
                + ", thưMụcDữLiệu=" + config.dataDir.toAbsolutePath());
        return config;
    }

    // Chuyển peer.id thành tên folder an toàn trên filesystem.
    private static String safePathSegment(String value) {
        if (value == null || value.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    // Lưu bootstrap config chung vào dataRoot/config.properties, không ghi vào tung profile.
    private void saveGlobalConfig() throws IOException {
        Properties globalProperties = new Properties();
        globalProperties.setProperty("bootstrap.host", bootstrapHost);
        globalProperties.setProperty("bootstrap.port", String.valueOf(bootstrapPort));
        Path globalConfigPath = dataRoot.resolve("config.properties");
        Files.createDirectories(globalConfigPath.getParent());
        try (OutputStream outputStream = Files.newOutputStream(globalConfigPath)) {
            globalProperties.store(outputStream, "Peer-node shared bootstrap config");
        }
        LOGGER.info("Đã lưu cấu hình bootstrap dùng chung. path=" + globalConfigPath.toAbsolutePath());
    }

    // Đọc bootstrap config chung từ dataRoot/config.properties.
    private static Properties loadGlobalProperties(Path dataRoot) {
        Properties properties = new Properties();
        Path globalConfigPath = dataRoot.resolve("config.properties");
        if (!Files.exists(globalConfigPath)) {
            return properties;
        }
        try (InputStream inputStream = Files.newInputStream(globalConfigPath)) {
            properties.load(inputStream);
            LOGGER.info("Đã nạp cấu hình bootstrap dùng chung từ " + globalConfigPath.toAbsolutePath());
        } catch (IOException e) {
            LOGGER.warn("Không thể nạp cấu hình bootstrap dùng chung: " + e.getMessage());
        }
        return properties;
    }

    // Kiểm tra config root cũ có chứa peer.id hay không để migrate thành profile UUID.
    private static boolean isLegacyPeerConfig(Path configPath) {
        if (!Files.exists(configPath)) {
            return false;
        }
        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(configPath)) {
            properties.load(inputStream);
            return properties.getProperty("peer.id") != null;
        } catch (IOException e) {
            LOGGER.warn("Không thể kiểm tra cấu hình peer cũ: " + e.getMessage());
            return false;
        }
    }
}
