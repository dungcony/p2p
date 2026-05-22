package dungcony.ds.dtos;

import dungcony.ds.model.PeerInfo;

import java.util.Collection;
import java.util.Collections;

public record JoinResponse(
        Collection<PeerInfo> onlinePeers,
        Collection<OfflineMessage> offlineMessages
) {
    public static JoinResponse empty() {
        return new JoinResponse(Collections.emptyList(), Collections.emptyList());
    }

    // Lấy danh sách peer online bootstrap trả về sau khi JOIN.
    public Collection<PeerInfo> getOnlinePeers() {
        return onlinePeers == null ? Collections.emptyList() : onlinePeers;
    }

    // Lấy các offline message bootstrap đang giu cho peer.
    public Collection<OfflineMessage> getOfflineMessages() {
        return offlineMessages == null ? Collections.emptyList() : offlineMessages;
    }
}
