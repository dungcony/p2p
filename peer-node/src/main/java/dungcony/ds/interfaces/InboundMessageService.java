package dungcony.ds.interfaces;

import dungcony.ds.model.Message;

// Đại diện use case xử lý message nhận từ TCP trước khi MessageReceiver trả ACK
public interface InboundMessageService {

    // Lưu message chat hoặc broadcast nhận vào và thông báo UI
    void onInboundMessage(Message message);

    // Cập nhật group local khi nhận snapshot membership từ peer khác
    void onGroupMembersSync(Message message);

    // Đánh dấu peer gửi heartbeat hoặc JOIN là online
    void markPeerOnline(Message message);
}
