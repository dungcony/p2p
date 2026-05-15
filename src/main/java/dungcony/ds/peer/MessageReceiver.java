package dungcony.ds.peer;

import dungcony.ds.model.Message;
import dungcony.ds.model.MessageType;

public class MessageReceiver {
    private final PeerNode peerNode;

    public MessageReceiver(PeerNode peerNode) {
        this.peerNode = peerNode;
    }

    public Message receive(Message message) {
        if (message == null) {
            return null;
        }

        if (message.getType() == MessageType.HEARTBEAT) {
            peerNode.markPeerOnline(message);
            return Message.ack(message, peerNode.getLocalPeer());
        }

        if (message.getType() == MessageType.CHAT || message.getType() == MessageType.GROUP_CHAT) {
            message.setFromCurrentUser(false);
            peerNode.onInboundMessage(message);
            return Message.ack(message, peerNode.getLocalPeer());
        }

        if (message.getType() == MessageType.JOIN) {
            peerNode.markPeerOnline(message);
            return Message.ack(message, peerNode.getLocalPeer());
        }

        return Message.ack(message, peerNode.getLocalPeer());
    }
}
