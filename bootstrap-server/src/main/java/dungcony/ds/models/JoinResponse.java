package dungcony.ds.models;

import dungcony.ds.entities.OfflineMessageEntity;

import java.util.Collection;

public class JoinResponse {
    private final Collection<PeerInfo> onlinePeers;
    private final Collection<OfflineMessageEntity> offlineMessages;

    public JoinResponse(Collection<PeerInfo> onlinePeers, Collection<OfflineMessageEntity> offlineMessages) {
        this.onlinePeers = onlinePeers;
        this.offlineMessages = offlineMessages;
    }

    public Collection<PeerInfo> getOnlinePeers() {
        return onlinePeers;
    }

    public Collection<OfflineMessageEntity> getOfflineMessages() {
        return offlineMessages;
    }
}
