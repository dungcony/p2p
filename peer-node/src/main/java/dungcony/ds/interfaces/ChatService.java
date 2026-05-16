package dungcony.ds.interfaces;

public interface ChatService {

    // gửi tin nhắn đến peer khác và lưu lại lịch sử
    boolean sendMessage(String content, String hostAndMaybePort);
}
