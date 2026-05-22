package dungcony.ds.interfaces;

import dungcony.ds.model.PeerInfo;

public interface NetworkAddressService {
    // Tìm địa chỉ IPv4 LAN phù hợp nhất của máy hiện tại để peer khác có thể kết nối.
    String resolveLocalHost();

    // So sánh PeerInfo với localPeer để chặn self-chat trong mới luồng logic.
    boolean isSelfPeer(PeerInfo localPeer, PeerInfo peerInfo);

    // So sánh host theo literal, localhost/loopback và địa chỉ IP resolve được.
    boolean isSameHost(String candidateHost, String localHost);
}
