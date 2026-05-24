package dungcony.ds.app;

import dungcony.ds.model.PeerInfo;
import dungcony.ds.services.interfaces.bootstrap.BootstrapGateway;
import dungcony.ds.services.interfaces.chat.ChatService;
import dungcony.ds.services.interfaces.chat.ConversationService;
import dungcony.ds.services.interfaces.group.GroupChatService;
import dungcony.ds.services.interfaces.messaging.InboundMessageService;
import dungcony.ds.services.interfaces.messaging.MessageHistoryService;
import dungcony.ds.services.interfaces.messaging.MessageRetryService;
import dungcony.ds.services.interfaces.messaging.NetworkBroadcastService;
import dungcony.ds.services.interfaces.peer.PeerDirectoryService;
import dungcony.ds.services.interfaces.peer.PeerDiscoverService;
import dungcony.ds.services.interfaces.peer.PeerPresenceService;

public record PeerNodeDependencies(
        PeerInfo localPeer,
        PeerDirectoryService peerDirectoryService,
        MessageHistoryService messageHistoryService,
        ChatService chatService,
        PeerPresenceService peerPresenceService,
        ConversationService conversationService,
        NetworkBroadcastService networkBroadcastService,
        GroupChatService groupChatService,
        PeerDiscoverService peerDiscoverService,
        InboundMessageService inboundMessageService,
        MessageRetryService messageRetryService,
        BootstrapGateway bootstrapGateway,
        PeerNodeRuntime runtime
) {
}
