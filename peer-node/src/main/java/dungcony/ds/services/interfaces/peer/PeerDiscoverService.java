package dungcony.ds.services.interfaces.peer;

import dungcony.ds.model.Message;
import dungcony.ds.model.PeerInfo;

import java.util.List;

// Đại diện use case hỏi peer đã biết để lấy thêm danh sách peer khi bootstrap không sẵn sàng
public interface PeerDiscoverService {

    // Gửi PEER_LIST_REQUEST tới một peer đã biết và merge kết quả vào danh bạ runtime
    List<PeerInfo> discoverPeersFromKnownPeer(PeerInfo knownPeer);

    // Tạo PEER_LIST_RESPONSE gồm local peer và danh bạ runtime hiện tại
    Message buildPeerListResponse(Message request);

    // Merge PEER_LIST_RESPONSE nhận từ peer khác vào danh bạ runtime
    int onPeerListResponse(Message response);
}
