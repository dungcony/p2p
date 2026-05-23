package dungcony.ds.interfaces;

import dungcony.ds.model.Message;
import dungcony.ds.model.PeerInfo;

import java.util.Collection;

// Đại diện contract quản lý danh bạ peer runtime của PeerNode
public interface PeerDirectoryService {
    // Thêm peer vào danh bạ theo addressKey
    void put(PeerInfo peerInfo);

    // Lấy danh sách peer runtime hiện tại
    Collection<PeerInfo> list();

    // Merge danh sách peer nhận từ bootstrap hoặc peer khác vào danh bạ runtime
    int mergeKnownPeers(Collection<PeerInfo> peers);

    // Lấy số peer đã biết
    int size();

    // Thêm một peer từ input name và host port
    PeerInfo addKnownPeer(String name, String hostAndMaybePort);

    // Tìm peer đã biết hoặc phân tích địa chỉ đầu vào thành PeerInfo tạm thời
    PeerInfo resolvePeer(String hostAndMaybePort);

    // Chuyển chuỗi host hoặc host:port thành PeerInfo với port mặc định nếu không nhập port
    PeerInfo parsePeer(String name, String hostAndMaybePort);

    // Tìm peer runtime theo user_id ổn định do bootstrap cấp
    PeerInfo findKnownPeerById(String peerId);

    // Tạo PeerInfo từ message đến và giữ lại tên hiển thị nếu peer đã có trong map
    PeerInfo mergeSenderFromKnownPeers(Message message);

    // Đồng bộ danh sách online bootstrap trả về và đánh dấu peer vắng mặt là offline
    int syncOnlinePeers(Collection<PeerInfo> onlinePeers);

    // Kiểm tra peer có trỏ về local peer hay không
    boolean isSelfPeer(PeerInfo peerInfo);
}
