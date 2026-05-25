package dungcony.ds.repositories;

import dungcony.ds.app.PeerNode;
import dungcony.ds.config.PeerProfile;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * Repository chịu trách nhiệm duy nhất: đọc/ghi/liệt kê peer profiles từ
 * filesystem.
 */
@Slf4j
public class PeerProfileRepository {

    private static final Path DEFAULT_DATA_DIR = Path.of("peer-node", "src", "main", "resources", "data");

    // Nạp profile đầu tiên tìm được trong DEFAULT_DATA_DIR (backward-compat)
    public PeerProfile load() {
        return load(DEFAULT_DATA_DIR);
    }

    // Nạp profile đầu tiên tìm được trong dataRoot
    public PeerProfile load(Path dataRoot) {
        Path resolvedRoot = resolve(dataRoot);
        Path configPath = resolveConfigPath(resolvedRoot);
        Properties profileProps = new Properties();
        Properties globalProps = loadGlobalProperties(resolvedRoot);
        ensureDirectory(resolvedRoot);
        if (configPath != null && Files.exists(configPath)) {
            try (InputStream in = Files.newInputStream(configPath)) {
                profileProps.load(in);
                log.info("Đã nạp cấu hình peer từ {}", configPath.toAbsolutePath());
            } catch (IOException e) {
                log.warn("Không thể nạp cấu hình peer. Dùng mặc định. lỗi={}", e.getMessage());
            }
        }
        PeerProfile profile = buildProfile(resolvedRoot, profileProps, globalProps, null);
        log.info("Đã chọn thư mục dữ liệu peer: {}", profile.getDataDir().toAbsolutePath());
        return profile;
    }

    // Tạo profile mới với peer.id là UUID mới và folder profile cùng tên UUID
    public PeerProfile createNew(Path dataRoot) {
        Path resolvedRoot = resolve(dataRoot);
        Properties globalProps = loadGlobalProperties(resolvedRoot);
        String newId = UUID.randomUUID().toString();
        int bootstrapPort = readInt(globalProps, "bootstrap.port", 9000);
        PeerProfile profile = new PeerProfile(
                newId,
                System.getProperty("user.name", "peer"),
                findAvailablePeerPort(resolvedRoot, bootstrapPort),
                readString(globalProps, "bootstrap.host", "localhost"),
                bootstrapPort,
                resolvedRoot);
        log.info("Đã tạo nháp profile peer mới. peerId={}, thưMụcDữLiệu={}", profile.getPeerId(),
                profile.getDataDir().toAbsolutePath());
        return profile;
    }

    // Tạo profile mới từ tên hiển thị; peer.id luôn là UUID nội bộ, không nhập từ
    // người dùng
    public PeerProfile createNewWithName(Path dataRoot, String peerName) {
        Path resolvedRoot = resolve(dataRoot);
        Properties globalProps = loadGlobalProperties(resolvedRoot);
        String newId = UUID.randomUUID().toString();
        int bootstrapPort = readInt(globalProps, "bootstrap.port", 9000);
        PeerProfile profile = new PeerProfile(
                newId,
                peerName == null || peerName.isBlank() ? System.getProperty("user.name", "peer") : peerName.trim(),
                findAvailablePeerPort(resolvedRoot, bootstrapPort),
                readString(globalProps, "bootstrap.host", "localhost"),
                bootstrapPort,
                resolvedRoot);
        log.info("Đã tạo profile mới từ tên hiển thị. {}", profile.getDisplayLabel());
        return profile;
    }

    // Tìm profile theo tên hiển thị để người dùng chỉ cần nhập name khi chạy lại
    public Optional<PeerProfile> findByName(Path dataRoot, String peerName) {
        if (peerName == null || peerName.isBlank()) {
            return Optional.empty();
        }
        String expectedName = peerName.trim();
        List<PeerProfile> matches = listProfiles(dataRoot).stream()
                .filter(profile -> expectedName.equalsIgnoreCase(profile.getPeerName()))
                .toList();
        if (matches.size() > 1) {
            log.warn("Tìm thấy nhiều profile cùng tên '{}'. Dùng profile đầu tiên: {}", expectedName,
                    matches.getFirst().getDisplayLabel());
        }
        return matches.isEmpty() ? Optional.empty() : Optional.of(matches.getFirst());
    }

    // Nạp profile theo peer.id/folder cụ thể, dùng cho --profile=<id>
    public Optional<PeerProfile> loadById(Path dataRoot, String profileId) {
        if (profileId == null || profileId.isBlank()) {
            return Optional.empty();
        }
        Path resolvedRoot = resolve(dataRoot);
        Properties globalProps = loadGlobalProperties(resolvedRoot);
        Path configPath = resolvedRoot.resolve(safePathSegment(profileId)).resolve("config.properties");
        if (!Files.exists(configPath)) {
            log.warn("Không tìm thấy profile runtime. profile={}, path={}", profileId, configPath.toAbsolutePath());
            return Optional.empty();
        }
        return Optional.of(loadFromConfigPath(resolvedRoot, configPath, globalProps));
    }

    // Kiểm tra peer.id đã tồn tại trong data root chưa để tránh tạo trùng định danh
    // peer
    public boolean existsByPeerId(Path dataRoot, String peerId) {
        if (peerId == null || peerId.isBlank()) {
            return false;
        }
        String expectedPeerId = peerId.trim();
        Path resolvedRoot = resolve(dataRoot);
        Path directConfigPath = resolvedRoot.resolve(safePathSegment(expectedPeerId))
                .resolve("config.properties");
        if (Files.exists(directConfigPath)) {
            return true;
        }
        return listProfiles(resolvedRoot).stream()
                .anyMatch(profile -> expectedPeerId.equalsIgnoreCase(profile.getPeerId()));
    }

    // Liệt kê các profile đã có trong data root; mỗi profile là folder UUID có
    // config.properties
    public List<PeerProfile> listProfiles(Path dataRoot) {
        Path resolvedRoot = resolve(dataRoot);
        List<PeerProfile> profiles = new ArrayList<>();
        Properties globalProps = loadGlobalProperties(resolvedRoot);
        try {
            ensureDirectory(resolvedRoot);
            try (Stream<Path> paths = Files.list(resolvedRoot)) {
                List<Path> configPaths = paths
                        .filter(Files::isDirectory)
                        .map(dir -> dir.resolve("config.properties"))
                        .filter(Files::exists)
                        .sorted(Comparator.comparing(p -> p.getParent().getFileName().toString()))
                        .toList();
                for (Path cp : configPaths) {
                    profiles.add(loadFromConfigPath(resolvedRoot, cp, globalProps));
                }
            }
            // Migrate legacy config.properties tại root
            Path legacyConfigPath = resolvedRoot.resolve("config.properties");
            if (profiles.isEmpty() && isLegacyPeerConfig(legacyConfigPath)) {
                log.info("Tìm thấy cấu hình peer cũ — sẽ migrate vào folder UUID khi lưu lần đầu.");
                profiles.add(loadFromConfigPath(resolvedRoot, legacyConfigPath, globalProps));
            }
        } catch (IOException e) {
            log.warn("Không thể liệt kê profile peer: {}", e.getMessage());
        }
        log.info("Số profile peer tìm thấy={}, dataRoot={}", profiles.size(), resolvedRoot.toAbsolutePath());
        return profiles;
    }

    // Lưu profile vào file config.properties của profile và lưu bootstrap config
    // chung
    public void save(PeerProfile profile) {
        Path configPath = profile.getDataDir().resolve("config.properties");
        Properties profileProps = new Properties();
        profileProps.setProperty("peer.id", profile.getPeerId());
        profileProps.setProperty("peer.name", profile.getPeerName());
        profileProps.setProperty("peer.port", String.valueOf(profile.getPeerPort()));
        try {
            Files.createDirectories(configPath.getParent());
            try (OutputStream out = Files.newOutputStream(configPath)) {
                profileProps.store(out, "Local peer identity profile");
            }
            saveGlobalConfig(profile);
            log.info("Đã lưu cấu hình peer. peerId={}, tênPeer={}, cổng={}, thưMụcDữLiệu={}",
                    profile.getPeerId(), profile.getPeerName(), profile.getPeerPort(),
                    profile.getDataDir().toAbsolutePath());
        } catch (IOException e) {
            log.error("Không thể lưu cấu hình peer: {}", e.getMessage());
        }
    }

    // ── private helpers ──

    private PeerProfile loadFromConfigPath(Path dataRoot, Path configPath, Properties globalProps) {
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(configPath)) {
            props.load(in);
        } catch (IOException e) {
            log.warn("Không thể nạp cấu hình profile={}, lỗi={}", configPath, e.getMessage());
        }
        String defaultId = configPath.getParent().equals(dataRoot)
                ? UUID.randomUUID().toString()
                : configPath.getParent().getFileName().toString();
        PeerProfile profile = buildProfile(dataRoot, props, globalProps, defaultId);
        log.info("Đã nạp profile peer. {}, thưMụcDữLiệu={}", profile.getDisplayLabel(),
                profile.getDataDir().toAbsolutePath());
        return profile;
    }

    private PeerProfile buildProfile(Path dataRoot, Properties profileProps, Properties globalProps, String defaultId) {
        String peerId = readString(profileProps, "peer.id",
                defaultId != null ? defaultId : UUID.randomUUID().toString());
        String peerName = readString(profileProps, "peer.name", System.getProperty("user.name", "peer"));
        int peerPort = readInt(profileProps, "peer.port", PeerNode.DEFAULT_PORT);
        String bootstrapHost = readString(globalProps, "bootstrap.host", "localhost");
        int bootstrapPort = readInt(globalProps, "bootstrap.port", 9000);
        return new PeerProfile(peerId, peerName, peerPort, bootstrapHost, bootstrapPort, dataRoot);
    }

    // Tìm config có sẵn trong data root: ưu tiên folder UUID con, sau đó legacy
    private Path resolveConfigPath(Path dataRoot) {
        try {
            if (Files.exists(dataRoot)) {
                try (Stream<Path> paths = Files.list(dataRoot)) {
                    List<Path> configPaths = paths
                            .filter(Files::isDirectory)
                            .map(dir -> dir.resolve("config.properties"))
                            .filter(Files::exists)
                            .sorted(Comparator.comparing(p -> p.getParent().getFileName().toString()))
                            .toList();
                    if (configPaths.size() > 1) {
                        log.warn("Tìm thấy nhiều folder UUID peer. Dùng folder đầu tiên: {}",
                                configPaths.get(0).getParent().getFileName());
                    }
                    if (!configPaths.isEmpty()) {
                        return configPaths.get(0);
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Không thể quét thư mục dữ liệu peer: {}", e.getMessage());
        }
        Path legacyConfigPath = dataRoot.resolve("config.properties");
        if (isLegacyPeerConfig(legacyConfigPath)) {
            log.info("Tìm thấy cấu hình peer cũ. Sẽ lưu vào folder UUID sau khi đăng nhập.");
            return legacyConfigPath;
        }
        return null;
    }

    // Lưu bootstrap config chung vào dataRoot/config.properties, không ghi vào từng
    // profile
    private void saveGlobalConfig(PeerProfile profile) throws IOException {
        Properties globalProps = new Properties();
        globalProps.setProperty("bootstrap.host", profile.getBootstrapHost());
        globalProps.setProperty("bootstrap.port", String.valueOf(profile.getBootstrapPort()));
        Path globalConfigPath = profile.getDataRoot().resolve("config.properties");
        Files.createDirectories(globalConfigPath.getParent());
        try (OutputStream out = Files.newOutputStream(globalConfigPath)) {
            globalProps.store(out, "Peer-node shared bootstrap config");
        }
        log.info("Đã lưu cấu hình bootstrap dùng chung. path={}", globalConfigPath.toAbsolutePath());
    }

    // Đọc bootstrap config chung từ dataRoot/config.properties
    private Properties loadGlobalProperties(Path dataRoot) {
        Properties properties = new Properties();
        Path globalConfigPath = dataRoot.resolve("config.properties");
        if (!Files.exists(globalConfigPath)) {
            return properties;
        }
        try (InputStream in = Files.newInputStream(globalConfigPath)) {
            properties.load(in);
            log.info("Đã nạp cấu hình bootstrap dùng chung từ {}", globalConfigPath.toAbsolutePath());
        } catch (IOException e) {
            log.warn("Không thể nạp cấu hình bootstrap dùng chung: {}", e.getMessage());
        }
        return properties;
    }

    // Kiểm tra config root cũ có chứa peer.id không để migrate thành profile UUID
    private boolean isLegacyPeerConfig(Path configPath) {
        if (!Files.exists(configPath)) {
            return false;
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(configPath)) {
            props.load(in);
            return props.getProperty("peer.id") != null;
        } catch (IOException e) {
            log.warn("Không thể kiểm tra cấu hình peer cũ: {}", e.getMessage());
            return false;
        }
    }

    private void ensureDirectory(Path dir) {
        try {
            Files.createDirectories(dir);
            log.info("Thư mục dữ liệu peer đã sẵn sàng: {}", dir.toAbsolutePath());
        } catch (IOException e) {
            log.error("Không thể tạo thư mục dữ liệu peer: {}", e.getMessage());
        }
    }

    private int findAvailablePeerPort(Path dataRoot, int bootstrapPort) {
        Set<Integer> usedPorts = new HashSet<>();
        usedPorts.add(bootstrapPort);
        for (PeerProfile profile : listProfiles(dataRoot)) {
            usedPorts.add(profile.getPeerPort());
        }
        for (int port = PeerNode.DEFAULT_PORT; port <= 65535; port++) {
            if (!usedPorts.contains(port) && isPortAvailable(port)) {
                return port;
            }
        }
        log.warn("Không tìm được cổng trống. Fallback về cổng mặc định={}", PeerNode.DEFAULT_PORT);
        return PeerNode.DEFAULT_PORT;
    }

    private boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            socket.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static String safePathSegment(String value) {
        if (value == null || value.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static String readString(Properties properties, String key, String defaultValue) {
        String value = properties.getProperty(key);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private static int readInt(Properties properties, String key, int defaultValue) {
        try {
            return Integer.parseInt(readString(properties, key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            log.warn("Config số nguyên không hợp lệ. key={}. fallback={}", key, defaultValue);
            return defaultValue;
        }
    }

    private static Path resolve(Path dataRoot) {
        return dataRoot == null ? DEFAULT_DATA_DIR : dataRoot.normalize();
    }
}
