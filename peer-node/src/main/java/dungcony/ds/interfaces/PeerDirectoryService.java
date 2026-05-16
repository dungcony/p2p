package dungcony.ds.interfaces;

import dungcony.ds.model.Message;
import dungcony.ds.model.PeerInfo;

import java.util.Collection;

public interface PeerDirectoryService {
    // thêm peer vào danh bạ theo address key
    void put(PeerInfo peerInfo);

    // lấy danh sasch peer runtime hiện tại
    Collection<PeerInfo> list();

    // lấy số peer đã biết
    int size();

    // thêm 1 peer đã biết
    PeerInfo addKnownPeer(String name, String hostAndMaybePort);

    // tìm peer đã biết
    PeerInfo resolvePeer(String hostAndMaybePort);

    // chuyển chuỗi host thành peerInfo
    PeerInfo parsePeer(String name, String hostAndMaybePort);

    // tìm peer theo id
    PeerInfo findKnownPeerById(String peerId);

    // tạo peer info và lưu lại
    PeerInfo mergeSenderFromKnownPeers(Message message);

    /**
     * Kiem tra peer co tro ve local peer hay khong.
     */
    boolean isSelfPeer(PeerInfo peerInfo);
}
