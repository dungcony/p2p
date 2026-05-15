package dungcony.ds.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dungcony.ds.model.Message;

public class MessageProtocol {
    private final Gson gson = new GsonBuilder().create();

    public String serialize(Message message) {
        return gson.toJson(message);
    }

    public Message deserialize(String payload) {
        return gson.fromJson(payload, Message.class);
    }
}
