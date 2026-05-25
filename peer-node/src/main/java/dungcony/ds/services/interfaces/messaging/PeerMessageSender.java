package dungcony.ds.services.interfaces.messaging;

import dungcony.ds.enums.MessageType;
import dungcony.ds.model.Message;
import dungcony.ds.model.PeerInfo;

/**
 * Abstraction gửi message tới một peer qua network transport
 */
public interface PeerMessageSender {
    boolean send(PeerInfo peerInfo, Message message);

    Message sendForResponse(PeerInfo peerInfo, Message message, MessageType expectedType);
}
