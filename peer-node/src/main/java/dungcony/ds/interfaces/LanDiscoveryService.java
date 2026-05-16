package dungcony.ds.interfaces;

import dungcony.ds.model.PeerInfo;

import java.util.List;

public interface LanDiscoveryService {
    /**
     * Quet LAN de tim cac peer dang lang nghe cung port voi peer hien tai.
     */
    List<PeerInfo> discoverPeersOnLocalNetwork();
}
