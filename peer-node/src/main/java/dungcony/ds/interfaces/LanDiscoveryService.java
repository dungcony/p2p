package dungcony.ds.interfaces;

import dungcony.ds.model.PeerInfo;

import java.util.List;

public interface LanDiscoveryService {
    // Quét LAN để tim các peer đang lắng nghe cùng port với peer hiện tại.
    List<PeerInfo> discoverPeersOnLocalNetwork();
}
