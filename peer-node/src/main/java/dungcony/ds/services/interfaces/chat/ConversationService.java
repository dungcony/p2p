package dungcony.ds.services.interfaces.chat;

import dungcony.ds.model.Message;
import dungcony.ds.model.PeerInfo;

import java.util.Collection;
import java.util.List;

// Đại diện use case đọc danh sách hội thoại và lịch sử chat cho UI
public interface ConversationService {

    // Lấy danh sách peer cần hiển thị trong chat list từ peer online và lịch sử local
    Collection<PeerInfo> getChatListPeers();

    // Lấy lịch sử tin nhắn với một peer theo địa chỉ hoặc khóa fallback
    List<Message> getMessagesWithPeer(String hostAndMaybePort);

    // Lấy tin nhắn cuối cùng với một peer để UI hiển thị preview
    Message getLastMessage(String hostAndMaybePort);

    // Lấy lịch sử broadcast toàn mạng
    List<Message> getBroadcastMessages();

    // Lấy broadcast cuối cùng để UI hiển thị preview
    Message getLastBroadcastMessage();
}
