package dungcony.ds.config;

import java.nio.file.Path;

/**
 * Cấu hình runtime lấy từ command line
 */
public record RuntimeOption(Path dataRoot, Integer peerPort) {
}
