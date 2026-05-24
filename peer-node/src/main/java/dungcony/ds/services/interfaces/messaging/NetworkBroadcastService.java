package dungcony.ds.services.interfaces.messaging;

import dungcony.ds.dtos.BroadcastResult;

// Đại diện use case gửi một nội dung tới toàn bộ peer online trong mạng
public interface NetworkBroadcastService {

    // Gửi broadcast tới các peer online và trả thống kê giao thành công thất bại
    BroadcastResult broadcastToNetwork(String content);
}
