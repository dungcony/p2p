package dungcony.ds.interfaces;

import dungcony.ds.model.PeerNode;

// Đại diện use case gửi một nội dung tới toàn bộ peer online trong mạng
public interface NetworkBroadcastService {

    // Gửi broadcast tới các peer online và trả thống kê giao thành công thất bại
    PeerNode.BroadcastResult broadcastToNetwork(String content);
}
