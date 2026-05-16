package dungcony.ds.interfaces;

import dungcony.ds.model.PeerInfo;

public interface NetworkAddressService {
    /**
     * Tim dia chi IPv4 LAN phu hop nhat cua may hien tai de peer khac co the ket noi.
     */
    String resolveLocalHost();

    /**
     * So sanh PeerInfo voi localPeer de chan self-chat trong moi luong logic.
     */
    boolean isSelfPeer(PeerInfo localPeer, PeerInfo peerInfo);

    /**
     * So sanh host theo literal, localhost/loopback va dia chi IP resolve duoc.
     */
    boolean isSameHost(String candidateHost, String localHost);
}
