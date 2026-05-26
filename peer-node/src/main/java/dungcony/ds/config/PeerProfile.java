package dungcony.ds.config;

import dungcony.ds.app.PeerNode;
import dungcony.ds.repositories.PeerProfileRepository;
import dungcony.ds.security.PeerKeyPair;
import dungcony.ds.security.RsaKeyPairUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;

/**
 * Value object chứa định danh và cấu hình của một peer profile.
 *
 * <p>
 * Chỉ chịu trách nhiệm duy nhất: giữ và cho phép cập nhật thông tin
 * profile peer. Không biết gì về file I/O hay filesystem.
 * </p>
 *
 * <p>
 * Để load/save/list profiles, dùng {@link PeerProfileRepository}.
 * </p>
 */
@Getter
@Slf4j
public class PeerProfile {

    private String peerId;
    private String peerName;
    private int peerPort;
    private final String publicKey;
    private final String privateKey;
    private final String bootstrapHost;
    private final int bootstrapPort;
    private final Path dataRoot;
    private Path dataDir;

    // Chỉ PeerProfileRepository nên tạo instance này.
    public PeerProfile(String peerId, String peerName, int peerPort,
                       String bootstrapHost, int bootstrapPort, Path dataRoot) {
        this(peerId, peerName, peerPort, bootstrapHost, bootstrapPort, dataRoot, null, null);
    }

    public PeerProfile(String peerId, String peerName, int peerPort,
                       String bootstrapHost, int bootstrapPort, Path dataRoot,
                       String publicKey, String privateKey) {
        PeerKeyPair keyPair = RsaKeyPairUtil.resolveOrGenerate(publicKey, privateKey);
        this.peerId = peerId;
        this.peerName = peerName;
        this.peerPort = peerPort;
        this.publicKey = keyPair.publicKey();
        this.privateKey = keyPair.privateKey();
        this.bootstrapHost = bootstrapHost;
        this.bootstrapPort = bootstrapPort;
        this.dataRoot = dataRoot;
        refreshDataDir();
    }

    // Cập nhật định danh hiển thị; port lắng nghe không đổi trong dialog UI
    public void updateIdentity(String peerId, String peerName) {
        this.peerId = peerId == null || peerId.isBlank() ? this.peerId : peerId.trim();
        this.peerName = peerName == null || peerName.isBlank() ? this.peerName : peerName.trim();
        refreshDataDir();
    }

    // Cập nhật port lắng nghe của peer khi tạo profile mới
    public boolean updatePeerPort(int peerPort) {
        if (peerPort < 1 || peerPort > 65535) {
            log.warn("Đã bỏ qua cổng peer không hợp lệ={}", peerPort);
            return false;
        }
        if (peerPort == bootstrapPort) {
            log.warn("Từ chối cổng peer vì trùng với cổng bootstrap={}", bootstrapPort);
            return false;
        }
        this.peerPort = peerPort;
        return true;
    }

    // Áp dụng peer port được truyền lúc chạy app qua CLI
    public void applyRuntimePeerPort(Integer runtimePeerPort) {
        if (runtimePeerPort == null) {
            protectBootstrapPort();
            return;
        }
        if (runtimePeerPort < 1 || runtimePeerPort > 65535) {
            log.warn("Đã bỏ qua cổng peer runtime không hợp lệ={}. Giữ cổng={}", runtimePeerPort, peerPort);
            protectBootstrapPort();
            return;
        }
        if (!updatePeerPort(runtimePeerPort)) {
            protectBootstrapPort();
        }
    }

    // Label ngắn gọn để hiển thị trong dialog chọn profile (JList renderer)
    public String getDisplayLabel() {
        return peerName + " | " + peerPort + " | " + peerId;
    }

    @Override
    public String toString() {
        return getDisplayLabel();
    }

    // ── private helpers ──

    // Nếu peer.port trùng bootstrap.port thì không cho PeerNode chiếm cổng tracker
    private void protectBootstrapPort() {
        if (peerPort == bootstrapPort) {
            log.warn("peer.port trùng với cổng bootstrap={}. Chuyển về cổng mặc định của peer={}", bootstrapPort,
                    PeerNode.DEFAULT_PORT);
            peerPort = PeerNode.DEFAULT_PORT;
        }
    }

    // Cập nhật dataDir khi peer.id thay đổi
    private void refreshDataDir() {
        this.dataDir = dataRoot.resolve(safePathSegment(peerId));
    }

    // Chuyển peer.id thành tên folder an toàn trên filesystem
    static String safePathSegment(String value) {
        if (value == null || value.isBlank()) {
            return java.util.UUID.randomUUID().toString();
        }
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
