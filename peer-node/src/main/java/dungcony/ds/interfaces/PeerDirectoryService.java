package dungcony.ds.interfaces;

import dungcony.ds.model.Message;
import dungcony.ds.model.PeerInfo;

import java.util.Collection;

public interface PeerDirectoryService {
    /**
     * Them peer vao danh ba theo addressKey.
     */
    void put(PeerInfo peerInfo);

    /**
     * Lay danh sach peer runtime hien tai.
     */
    Collection<PeerInfo> list();

    /**
     * Lay so peer da biet.
     */
    int size();

    /**
     * Them mot peer tu input name va host/port.
     */
    PeerInfo addKnownPeer(String name, String hostAndMaybePort);

    /**
     * Tim peer da biet hoac phan tich dia chi dau vao thanh PeerInfo tam thoi.
     */
    PeerInfo resolvePeer(String hostAndMaybePort);

    /**
     * Chuyen chuoi host hoac host:port thanh PeerInfo voi port mac dinh neu khong nhap port.
     */
    PeerInfo parsePeer(String name, String hostAndMaybePort);

    /**
     * Tim peer runtime theo user_id on dinh do bootstrap cap.
     */
    PeerInfo findKnownPeerById(String peerId);

    /**
     * Tao PeerInfo tu message den va giu lai ten hien thi neu peer da co trong map.
     */
    PeerInfo mergeSenderFromKnownPeers(Message message);

    /**
     * Dong bo danh sach online bootstrap tra ve, danh dau peer vang mat la offline.
     */
    int syncOnlinePeers(Collection<PeerInfo> onlinePeers);

    /**
     * Kiem tra peer co tro ve local peer hay khong.
     */
    boolean isSelfPeer(PeerInfo peerInfo);
}
