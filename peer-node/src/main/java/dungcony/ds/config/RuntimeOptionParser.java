package dungcony.ds.config;

import lombok.extern.slf4j.Slf4j;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/**
 * Helper đọc option runtime từ command line
 */
@Slf4j
public final class RuntimeOptionParser {

    private RuntimeOptionParser() {
        // Utility class không cần khởi tạo
    }

    // Đọc tham số runtime: --data-dir và --peer-port
    public static RuntimeOption resolveRuntimeOptions(String[] args) {
        Path dataRoot = Path.of("peer-node", "src", "main", "resources", "data");
        Integer peerPort = null;

        for (int index = 0; args != null && index < args.length; index++) {
            String arg = args[index] == null ? "" : args[index].trim();
            if (arg.isBlank()) {
                continue;
            }
            if (arg.startsWith("--data-dir=")) {
                dataRoot = parseDataRoot(arg.substring("--data-dir=".length()), dataRoot);
                continue;
            }
            if ("--data-dir".equals(arg)) {
                if (index + 1 >= args.length || isOptionName(args[index + 1])) {
                    log.warn("Thiếu giá trị cho --data-dir. Giữ thư mục dữ liệu={}", dataRoot.toAbsolutePath());
                } else {
                    dataRoot = parseDataRoot(args[++index], dataRoot);
                }
                continue;
            }
            if (arg.startsWith("--peer-port=")) {
                peerPort = parsePeerPort(arg.substring("--peer-port=".length()), peerPort);
                continue;
            }
            if (arg.startsWith("--port=")) {
                peerPort = parsePeerPort(arg.substring("--port=".length()), peerPort);
                continue;
            }
            if ("--peer-port".equals(arg) || "--port".equals(arg)) {
                if (index + 1 >= args.length || isOptionName(args[index + 1])) {
                    log.warn("Thiếu giá trị cho {}. Giữ cổng peer runtime={}", arg, peerPort);
                } else {
                    peerPort = parsePeerPort(args[++index], peerPort);
                }
                continue;
            }
            log.warn("Đã bỏ qua tham số runtime không hỗ trợ={}", arg);
        }

        log.info("Thư mục dữ liệu runtime={}", dataRoot.toAbsolutePath());
        if (peerPort != null) {
            log.info("Cổng peer runtime={}", peerPort);
        }
        return new RuntimeOption(dataRoot, peerPort);
    }


    // Parse data root từ CLI, giữ giá trị hiện tại nếu value không hợp lệ
    private static Path parseDataRoot(String value, Path currentDataRoot) {
        if (value == null || value.isBlank()) {
            log.warn("Đã bỏ qua giá trị --data-dir rỗng. Giữ thư mục dữ liệu={}", currentDataRoot.toAbsolutePath());
            return currentDataRoot;
        }
        try {
            return Path.of(value.trim()).normalize();
        } catch (InvalidPathException e) {
            log.warn("Đã bỏ qua giá trị --data-dir không hợp lệ={}. Giữ thư mục dữ liệu={}", value, currentDataRoot.toAbsolutePath());
            return currentDataRoot;
        }
    }

    // Parse peer port từ CLI, giữ giá trị hiện tại nếu value không hợp lệ
    private static Integer parsePeerPort(String value, Integer currentPeerPort) {
        if (value == null || value.isBlank()) {
            log.warn("Đã bỏ qua giá trị --peer-port rỗng. Giữ cổng peer runtime={}", currentPeerPort);
            return currentPeerPort;
        }
        try {
            int port = Integer.parseInt(value.trim());
            if (port < 1 || port > 65535) {
                throw new NumberFormatException("Port out of range");
            }
            return port;
        } catch (NumberFormatException e) {
            log.warn("Đã bỏ qua giá trị --peer-port không hợp lệ={}. Giữ cổng peer runtime={}", value, currentPeerPort);
            return currentPeerPort;
        }
    }

    private static boolean isOptionName(String value) {
        return value != null && value.trim().startsWith("--");
    }

}
