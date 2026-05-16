package dungcony.ds.config;

import dungcony.ds.peer.PeerNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Stream;

public class PeerConfig {
    private static final Path DEFAULT_DATA_DIR = Path.of("peer-node", "src", "main", "resources", "data");

    private Path dataRoot;
    private Path dataDir;
    private Path configPath;
    private String peerId;
    private String peerName;
    private int peerPort;
    private String bootstrapHost;
    private int bootstrapPort;

    /**
     * Doc cau hinh peer tu file resources/data/config.properties va tao id neu chua co.
     */
    public static PeerConfig load() {
        return load(DEFAULT_DATA_DIR);
    }

    /**
     * Doc cau hinh peer tu dataDir rieng cua instance hien tai.
     */
    public static PeerConfig load(Path dataDir) {
        Path dataRoot = dataDir == null ? DEFAULT_DATA_DIR : dataDir.normalize();
        Path loadedConfigPath = resolveConfigPath(dataRoot);
        Properties profileProperties = new Properties();
        Properties globalProperties = loadGlobalProperties(dataRoot);
        try {
            Files.createDirectories(dataRoot);
            System.out.println("[INFO] Peer data root ready: " + dataRoot.toAbsolutePath());
        } catch (IOException e) {
            System.out.println("[ERROR] Failed to create peer data root: " + e.getMessage());
        }
        if (loadedConfigPath != null && Files.exists(loadedConfigPath)) {
            try (InputStream inputStream = Files.newInputStream(loadedConfigPath)) {
                profileProperties.load(inputStream);
                System.out.println("[INFO] Loaded peer config from " + loadedConfigPath.toAbsolutePath());
            } catch (IOException e) {
                System.out.println("[WARN] Failed to load peer config. Using defaults. error=" + e.getMessage());
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
        System.out.println("[INFO] Peer data directory selected: " + config.dataDir.toAbsolutePath());
        return config;
    }

    /**
     * Tao profile moi voi peer.id la UUID va folder profile cung ten UUID.
     */
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
        System.out.println("[INFO] Created new peer profile draft. peerId=" + config.peerId
                + ", dataDir=" + config.dataDir.toAbsolutePath());
        return config;
    }

    /**
     * Liet ke cac profile da co trong data root, moi profile la mot folder UUID co config.properties.
     */
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
                System.out.println("[INFO] Found legacy peer config as existing profile. "
                        + "It will be migrated to UUID folder on start.");
                profiles.add(loadFromConfigPath(resolvedDataRoot, legacyConfigPath));
            }
        } catch (IOException e) {
            System.out.println("[WARN] Failed to list peer profiles: " + e.getMessage());
        }
        System.out.println("[INFO] Peer profiles found=" + profiles.size()
                + ", dataRoot=" + resolvedDataRoot.toAbsolutePath());
        return profiles;
    }

    /**
     * Cap nhat ten va port sau khi nguoi dung bam Start o man hinh dang nhap.
     */
    public void updateLogin(String peerId, String peerName, int peerPort) {
        this.peerId = peerId == null || peerId.isBlank() ? this.peerId : peerId.trim();
        this.peerName = peerName == null || peerName.isBlank() ? this.peerName : peerName.trim();
        this.peerPort = peerPort;
        refreshStoragePaths();
    }

    /**
     * Luu cau hinh peer de lan sau app dung lai cung peer.id khi dang nhap.
     */
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
            System.out.println("[INFO] Saved peer config. peerId=" + peerId
                    + ", peerName=" + peerName + ", port=" + peerPort
                    + ", dataDir=" + dataDir.toAbsolutePath());
        } catch (IOException e) {
            System.out.println("[ERROR] Failed to save peer config: " + e.getMessage());
        }
    }

    /**
     * Lay data directory rieng cua instance peer-node hien tai.
     */
    public Path getDataDir() {
        return dataDir;
    }

    /**
     * Lay data root chua cac folder profile peer theo UUID.
     */
    public Path getDataRoot() {
        return dataRoot;
    }

    /**
     * Label ngan gon de hien thi trong dialog chon profile.
     */
    public String getDisplayLabel() {
        return peerName + " | " + peerPort + " | " + peerId;
    }

    /**
     * Lay id on dinh dung lam khoa user_id tren bootstrap-server.
     */
    public String getPeerId() {
        return peerId;
    }

    /**
     * Lay ten hien thi cua peer.
     */
    public String getPeerName() {
        return peerName;
    }

    /**
     * Lay port TCP peer-node se lang nghe.
     */
    public int getPeerPort() {
        return peerPort;
    }

    /**
     * Lay host cua bootstrap-server.
     */
    public String getBootstrapHost() {
        return bootstrapHost;
    }

    /**
     * Lay port cua bootstrap-server.
     */
    public int getBootstrapPort() {
        return bootstrapPort;
    }

    /**
     * Doc string property voi fallback khi value rong.
     */
    private static String readString(Properties properties, String key, String defaultValue) {
        String value = properties.getProperty(key);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    /**
     * Doc int property voi fallback khi value khong hop le.
     */
    private static int readInt(Properties properties, String key, int defaultValue) {
        try {
            return Integer.parseInt(readString(properties, key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            System.out.println("[WARN] Invalid int config key=" + key + ". fallback=" + defaultValue);
            return defaultValue;
        }
    }

    /**
     * Cap nhat dataDir/configPath theo peer.id hien tai de folder duoc dat theo UUID.
     */
    private void refreshStoragePaths() {
        this.dataDir = dataRoot.resolve(safePathSegment(peerId));
        this.configPath = dataDir.resolve("config.properties");
    }

    /**
     * Tim config co san trong data root: uu tien folder UUID con, sau do moi den config legacy.
     */
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
                        System.out.println("[WARN] Multiple peer UUID folders found under data root. "
                                + "Using first folder by name: " + configPaths.get(0).getParent().getFileName());
                    }
                    if (!configPaths.isEmpty()) {
                        return configPaths.get(0);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("[WARN] Failed to scan peer data root: " + e.getMessage());
        }

        Path legacyConfigPath = dataRoot.resolve("config.properties");
        if (isLegacyPeerConfig(legacyConfigPath)) {
            System.out.println("[INFO] Found legacy peer config. It will be saved into UUID folder after login.");
            return legacyConfigPath;
        }
        return null;
    }

    /**
     * Doc mot profile cu the tu file config.properties trong folder UUID.
     */
    private static PeerConfig loadFromConfigPath(Path dataRoot, Path configPath) {
        Properties properties = new Properties();
        Properties globalProperties = loadGlobalProperties(dataRoot);
        try (InputStream inputStream = Files.newInputStream(configPath)) {
            properties.load(inputStream);
        } catch (IOException e) {
            System.out.println("[WARN] Failed to load profile config=" + configPath
                    + ", error=" + e.getMessage());
        }

        PeerConfig config = new PeerConfig();
        config.dataRoot = dataRoot;
        config.peerId = readString(properties, "peer.id", configPath.getParent().getFileName().toString());
        config.peerName = readString(properties, "peer.name", System.getProperty("user.name", "peer"));
        config.peerPort = readInt(properties, "peer.port", PeerNode.DEFAULT_PORT);
        config.bootstrapHost = readString(globalProperties, "bootstrap.host", "localhost");
        config.bootstrapPort = readInt(globalProperties, "bootstrap.port", 9000);
        config.refreshStoragePaths();
        System.out.println("[INFO] Loaded peer profile. " + config.getDisplayLabel()
                + ", dataDir=" + config.dataDir.toAbsolutePath());
        return config;
    }

    /**
     * Chuyen peer.id thanh ten folder an toan tren filesystem.
     */
    private static String safePathSegment(String value) {
        if (value == null || value.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /**
     * Luu bootstrap config chung vao dataRoot/config.properties, khong ghi vao tung profile.
     */
    private void saveGlobalConfig() throws IOException {
        Properties globalProperties = new Properties();
        globalProperties.setProperty("bootstrap.host", bootstrapHost);
        globalProperties.setProperty("bootstrap.port", String.valueOf(bootstrapPort));
        Path globalConfigPath = dataRoot.resolve("config.properties");
        Files.createDirectories(globalConfigPath.getParent());
        try (OutputStream outputStream = Files.newOutputStream(globalConfigPath)) {
            globalProperties.store(outputStream, "Peer-node shared bootstrap config");
        }
        System.out.println("[INFO] Saved shared bootstrap config. path=" + globalConfigPath.toAbsolutePath());
    }

    /**
     * Doc bootstrap config chung tu dataRoot/config.properties.
     */
    private static Properties loadGlobalProperties(Path dataRoot) {
        Properties properties = new Properties();
        Path globalConfigPath = dataRoot.resolve("config.properties");
        if (!Files.exists(globalConfigPath)) {
            return properties;
        }
        try (InputStream inputStream = Files.newInputStream(globalConfigPath)) {
            properties.load(inputStream);
            System.out.println("[INFO] Loaded shared bootstrap config from " + globalConfigPath.toAbsolutePath());
        } catch (IOException e) {
            System.out.println("[WARN] Failed to load shared bootstrap config: " + e.getMessage());
        }
        return properties;
    }

    /**
     * Kiem tra config root cu co chua peer.id hay khong de migrate thanh profile UUID.
     */
    private static boolean isLegacyPeerConfig(Path configPath) {
        if (!Files.exists(configPath)) {
            return false;
        }
        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(configPath)) {
            properties.load(inputStream);
            return properties.getProperty("peer.id") != null;
        } catch (IOException e) {
            System.out.println("[WARN] Failed to inspect legacy peer config: " + e.getMessage());
            return false;
        }
    }
}
