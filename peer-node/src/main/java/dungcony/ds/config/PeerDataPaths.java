package dungcony.ds.config;

import java.nio.file.Path;

/**
 * Đường dẫn lưu dữ liệu runtime của peer-node.
 */
public final class PeerDataPaths {
    public static final Path DEFAULT_DATA_ROOT = Path.of("runtime-data", "peer-node");
    public static final Path LEGACY_PROJECT_DATA_ROOT = Path.of("peer-node", "src", "main", "resources", "data");
    public static final Path LEGACY_MODULE_DATA_ROOT = Path.of("src", "main", "resources", "data");

    private PeerDataPaths() {
        // Utility class
    }

    public static Path resolve(Path dataDir) {
        return dataDir == null ? DEFAULT_DATA_ROOT : dataDir.normalize();
    }
}
