package dungcony.ds.network;

import lombok.extern.slf4j.Slf4j;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dungcony.ds.model.Message;

@Slf4j
public class MessageProtocol {
private final Gson gson = new GsonBuilder().create();

    // Chuyển Message thành chuỗi JSON để gửi qua TCP socket.
    public String serialize(Message message) {
        String payload = gson.toJson(message);
        log.trace("Đã serialize message id="
                + (message == null ? "null" : message.getId())
                + ", bytes=" + payload.length());
        return payload;
    }

    // Chuyển chuỗi JSON nhận qua TCP socket thành đối tượng Message.
    public Message deserialize(String payload) {
        Message message = gson.fromJson(payload, Message.class);
        log.trace("Đã deserialize message id="
                + (message == null ? "null" : message.getId())
                + ", type=" + (message == null ? "null" : message.getType()));
        return message;
    }
}
