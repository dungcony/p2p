package dungcony.ds.config;

import dungcony.ds.peer.PeerNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;

public class PeerConfig {
    private static final Path CONFIG_PATH = Path.of("peer-node", "src", "main", "resources", "data", "config.properties");

    private String peerId;
    private String peerName;
    private int peerPort;
    private String bootstrapHost;
    private int bootstrapPort;

    /**
     * Doc cau hinh peer tu file resources/data/config.properties va tao id neu chua co.
     */
    public static PeerConfig load() {
        Properties properties = new Properties();
        if (Files.exists(CONFIG_PATH)) {
            try (InputStream inputStream = Files.newInputStream(CONFIG_PATH)) {
                properties.load(inputStream);
                System.out.println("[INFO] Loaded peer config from " + CONFIG_PATH.toAbsolutePath());
            } catch (IOException e) {
                System.out.println("[WARN] Failed to load peer config. Using defaults. error=" + e.getMessage());
            }
        }

        PeerConfig config = new PeerConfig();
        config.peerId = readString(properties, "peer.id", UUID.randomUUID().toString());
        config.peerName = readString(properties, "peer.name", System.getProperty("user.name", "peer"));
        config.peerPort = readInt(properties, "peer.port", PeerNode.DEFAULT_PORT);
        config.bootstrapHost = readString(properties, "bootstrap.host", "localhost");
        config.bootstrapPort = readInt(properties, "bootstrap.port", 9000);
        return config;
    }

    /**
     * Cap nhat ten va port sau khi nguoi dung bam Start o man hinh dang nhap.
     */
    public void updateLogin(String peerId, String peerName, int peerPort) {
        this.peerId = peerId == null || peerId.isBlank() ? this.peerId : peerId.trim();
        this.peerName = peerName == null || peerName.isBlank() ? this.peerName : peerName.trim();
        this.peerPort = peerPort;
    }

    /**
     * Luu cau hinh peer de lan sau app dung lai cung peer.id khi dang nhap.
     */
    public void save() {
        Properties properties = new Properties();
        properties.setProperty("peer.id", peerId);
        properties.setProperty("peer.name", peerName);
        properties.setProperty("peer.port", String.valueOf(peerPort));
        properties.setProperty("bootstrap.host", bootstrapHost);
        properties.setProperty("bootstrap.port", String.valueOf(bootstrapPort));
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (OutputStream outputStream = Files.newOutputStream(CONFIG_PATH)) {
                properties.store(outputStream, "Local peer identity and bootstrap tracker config");
            }
            System.out.println("[INFO] Saved peer config. peerId=" + peerId
                    + ", peerName=" + peerName + ", port=" + peerPort);
        } catch (IOException e) {
            System.out.println("[ERROR] Failed to save peer config: " + e.getMessage());
        }
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
}
