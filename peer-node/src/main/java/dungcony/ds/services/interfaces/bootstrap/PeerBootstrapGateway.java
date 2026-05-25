package dungcony.ds.services.interfaces.bootstrap;

import dungcony.ds.dtos.JoinResponse;
import dungcony.ds.model.PeerInfo;

import java.util.Collection;

/**
 * Giao tiếp với bootstrap-server cho peer lifecycle và peer directory.
 * Tách ra từ BootstrapGateway theo ISP — chỉ dùng bởi các service
 * cần register/join/leave/list peer.
 */
public interface PeerBootstrapGateway {

    boolean register(PeerInfo peerInfo);

    JoinResponse join(PeerInfo peerInfo);

    JoinResponse joinOrNull(PeerInfo peerInfo);

    void leave(String peerKey);

    Collection<PeerInfo> list();

    Collection<PeerInfo> listOrNull();
}
