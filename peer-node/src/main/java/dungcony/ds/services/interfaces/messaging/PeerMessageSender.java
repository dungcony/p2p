package dungcony.ds.services.interfaces.messaging;

import dungcony.ds.enums.MessageType;
import dungcony.ds.model.Message;
import dungcony.ds.model.PeerInfo;

public interface PeerMessageSender {
    boolean send(PeerInfo peerInfo, Message message);

    Message sendForResponse(PeerInfo peerInfo, Message message, MessageType expectedType);
}
