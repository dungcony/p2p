package dungcony.ds.security;

import dungcony.ds.config.PeerDataPaths;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

@Slf4j
public class PeerKeyStore {
    public static final String PUBLIC_KEY_PROPERTY = "peer.publicKey";
    public static final String PRIVATE_KEY_PROPERTY = "peer.privateKey";

    public PeerKeyPair loadOrCreate(Path dataDir, String peerId, String peerName, int peerPort) {
        Path resolvedDir = PeerDataPaths.resolve(dataDir);
        Path configPath = resolvedDir.resolve("config.properties");
        Properties properties = loadProperties(configPath);
        PeerKeyPair keys = resolveKeys(properties, resolvedDir, peerId);
        properties.setProperty(PUBLIC_KEY_PROPERTY, keys.publicKey());
        properties.setProperty(PRIVATE_KEY_PROPERTY, keys.privateKey());
        putIfPresent(properties, "peer.id", peerId);
        putIfPresent(properties, "peer.name", peerName);
        if (peerPort > 0) {
            properties.setProperty("peer.port", String.valueOf(peerPort));
        }
        saveProperties(configPath, properties);
        return keys;
    }

    private PeerKeyPair resolveKeys(Properties properties, Path dataDir, String peerId) {
        String publicKey = properties.getProperty(PUBLIC_KEY_PROPERTY);
        String privateKey = properties.getProperty(PRIVATE_KEY_PROPERTY);
        if (RsaKeyPairUtil.isUsableKeyPair(publicKey, privateKey)) {
            return new PeerKeyPair(publicKey.trim(), privateKey.trim());
        }
        Optional<PeerKeyPair> reusableKeys = findReusableKeyPair(dataDir, peerId);
        return reusableKeys.orElseGet(RsaKeyPairUtil::generateKeyPair);
    }

    private Optional<PeerKeyPair> findReusableKeyPair(Path dataDir, String peerId) {
        if (peerId == null || peerId.isBlank() || dataDir == null || dataDir.getParent() == null) {
            return Optional.empty();
        }
        Path dataRoot = dataDir.getParent();
        try (Stream<Path> paths = Files.list(dataRoot)) {
            return paths
                    .filter(Files::isDirectory)
                    .filter(path -> !path.equals(dataDir))
                    .map(path -> path.resolve("config.properties"))
                    .filter(Files::exists)
                    .map(this::loadProperties)
                    .filter(properties -> peerId.equals(properties.getProperty("peer.id")))
                    .map(properties -> new PeerKeyPair(
                            properties.getProperty(PUBLIC_KEY_PROPERTY),
                            properties.getProperty(PRIVATE_KEY_PROPERTY)))
                    .filter(keys -> RsaKeyPairUtil.isUsableKeyPair(keys.publicKey(), keys.privateKey()))
                    .findFirst();
        } catch (IOException e) {
            log.warn("Cannot scan sibling peer profiles for reusable key pair. dataRoot={}, cause={}",
                    dataRoot.toAbsolutePath(), e.getMessage());
            return Optional.empty();
        }
    }

    private Properties loadProperties(Path configPath) {
        Properties properties = new Properties();
        if (!Files.exists(configPath)) {
            return properties;
        }
        try (InputStream in = Files.newInputStream(configPath)) {
            properties.load(in);
        } catch (IOException e) {
            log.warn("Cannot read peer key config. path={}, cause={}", configPath.toAbsolutePath(), e.getMessage());
        }
        return properties;
    }

    private void saveProperties(Path configPath, Properties properties) {
        try {
            Files.createDirectories(configPath.getParent());
            try (OutputStream out = Files.newOutputStream(configPath)) {
                properties.store(out, "Local peer identity profile");
            }
        } catch (IOException e) {
            log.error("Cannot save peer key config. path={}, cause={}", configPath.toAbsolutePath(), e.getMessage());
        }
    }

    private void putIfPresent(Properties properties, String key, String value) {
        if (value != null && !value.isBlank()) {
            properties.setProperty(key, value.trim());
        }
    }
}
