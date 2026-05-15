package dungcony.ds.bootstrap;

import dungcony.ds.model.PeerInfo;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PeerRegistry {
    private final Map<String, PeerInfo> peers = new ConcurrentHashMap<>();

    public void join(PeerInfo peerInfo) {
        if (peerInfo != null) {
            peerInfo.setOnline(true);
            peers.put(peerInfo.addressKey(), peerInfo);
        }
    }

    public void leave(String addressKey) {
        peers.remove(addressKey);
    }

    public Collection<PeerInfo> list() {
        return Collections.unmodifiableCollection(peers.values());
    }
}
