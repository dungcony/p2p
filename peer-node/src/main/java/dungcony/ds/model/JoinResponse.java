package dungcony.ds.model;

import java.util.Collection;
import java.util.Collections;

public class JoinResponse {
    private Collection<PeerInfo> onlinePeers;
    private Collection<OfflineMessage> offlineMessages;

    /**
     * Lay danh sach peer online bootstrap tra ve sau khi peer JOIN vao mang.
     */
    public Collection<PeerInfo> getOnlinePeers() {
        return onlinePeers == null ? Collections.emptyList() : onlinePeers;
    }

    /**
     * Lay cac tin offline server dang giu cho peer nay.
     */
    public Collection<OfflineMessage> getOfflineMessages() {
        return offlineMessages == null ? Collections.emptyList() : offlineMessages;
    }
}
