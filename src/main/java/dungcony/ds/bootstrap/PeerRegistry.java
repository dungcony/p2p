package dungcony.ds.bootstrap;

import dungcony.ds.model.PeerInfo;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PeerRegistry {
    private final Map<String, PeerInfo> peers = new ConcurrentHashMap<>();

    /**
     * Đăng ký hoặc cập nhật một peer đang online trong tracker.
     */
    public void join(PeerInfo peerInfo) {
        if (peerInfo != null) {
            peerInfo.setOnline(true);
            peers.put(peerInfo.addressKey(), peerInfo);
            System.out.println("[DEBUG] PeerRegistry join: " + peerInfo.addressKey());
        } else {
            System.out.println("[WARN] PeerRegistry join ignored null peer.");
        }
    }

    /**
     * Xóa peer khỏi registry khi peer rời mạng.
     */
    public void leave(String addressKey) {
        peers.remove(addressKey);
        System.out.println("[DEBUG] PeerRegistry leave: " + addressKey);
    }

    /**
     * Trả về danh sách peer online hiện được tracker biết.
     */
    public Collection<PeerInfo> list() {
        System.out.println("[TRACE] PeerRegistry list size=" + peers.size());
        return Collections.unmodifiableCollection(peers.values());
    }
}
