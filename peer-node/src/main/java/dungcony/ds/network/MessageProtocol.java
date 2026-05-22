package dungcony.ds.network;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dungcony.ds.model.Message;

public class MessageProtocol {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageProtocol.class);
private final Gson gson = new GsonBuilder().create();

    // Chuyển Message thành chuỗi JSON để gửi qua TCP socket.
    public String serialize(Message message) {
        String payload = gson.toJson(message);
        LOGGER.trace("Đã serialize message id="
                + (message == null ? "null" : message.getId())
                + ", bytes=" + payload.length());
        return payload;
    }

    // Chuyển chuỗi JSON nhận qua TCP socket thành đối tượng Message.
    public Message deserialize(String payload) {
        Message message = gson.fromJson(payload, Message.class);
        LOGGER.trace("Đã deserialize message id="
                + (message == null ? "null" : message.getId())
                + ", type=" + (message == null ? "null" : message.getType()));
        return message;
    }
}
