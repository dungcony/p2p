package dungcony.ds.services.interfaces.peer;

import dungcony.ds.model.PeerInfo;

// Đại diện contract tìm địa chỉ mạng local và nhận diện self peer
public interface NetworkAddressService {
    // Tìm địa chỉ IPv4 LAN phù hợp nhất của máy hiện tại để peer khác có thể kết nối
    String resolveLocalHost();

    // So sánh PeerInfo với localPeer để chặn self-chat trong mọi luồng logic
    boolean isSelfPeer(PeerInfo localPeer, PeerInfo peerInfo);

    // So sánh host theo literal, localhost loopback và địa chỉ IP resolve được
    boolean isSameHost(String candidateHost, String localHost);
}
