package dungcony.ds.interfaces;

// Đại diện contract gửi tin nhắn trực tiếp 1-1
public interface ChatService {

    // Gửi tin nhắn đến peer khác và lưu lại lịch sử
    boolean sendMessage(String content, String hostAndMaybePort);
}
