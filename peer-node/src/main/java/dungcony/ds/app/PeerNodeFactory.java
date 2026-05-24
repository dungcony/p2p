package dungcony.ds.app;

import dungcony.ds.model.Message;
import dungcony.ds.model.PeerInfo;
import dungcony.ds.network.BootstrapClient;
import dungcony.ds.network.MessageReceiver;
import dungcony.ds.network.MessageSender;
import dungcony.ds.network.TCPClient;
import dungcony.ds.network.TCPServer;
import dungcony.ds.repositories.LocalGroupRepo;
import dungcony.ds.repositories.LocalMessageRepo;
import dungcony.ds.services.impl.bootstrap.BootstrapGroupImpl;
import dungcony.ds.services.impl.bootstrap.BootstrapSyncImpl;
import dungcony.ds.services.impl.chat.ChatImpl;
import dungcony.ds.services.impl.chat.ConversationImpl;
import dungcony.ds.services.impl.group.GroupChatImpl;
import dungcony.ds.services.impl.group.GroupManager;
import dungcony.ds.services.impl.messaging.InboundMessageImpl;
import dungcony.ds.services.impl.messaging.MessageHistoryImpl;
import dungcony.ds.services.impl.messaging.MessageRetryImpl;
import dungcony.ds.services.impl.messaging.MessageRouterImpl;
import dungcony.ds.services.impl.messaging.NetworkBroadcastImpl;
import dungcony.ds.services.impl.peer.NetworkAddressImpl;
import dungcony.ds.services.impl.peer.PeerDirectoryImpl;
import dungcony.ds.services.impl.peer.PeerDiscoverImpl;
import dungcony.ds.services.impl.peer.PeerPresenceImpl;
import dungcony.ds.services.interfaces.bootstrap.BootstrapGateway;
import dungcony.ds.services.interfaces.bootstrap.BootstrapGroupService;
import dungcony.ds.services.interfaces.bootstrap.BootstrapSyncService;
import dungcony.ds.services.interfaces.chat.ChatService;
import dungcony.ds.services.interfaces.chat.ConversationService;
import dungcony.ds.services.interfaces.group.GroupChatService;
import dungcony.ds.services.interfaces.messaging.InboundMessageService;
import dungcony.ds.services.interfaces.messaging.MessageHistoryService;
import dungcony.ds.services.interfaces.messaging.MessageRetryService;
import dungcony.ds.services.interfaces.messaging.MessageRouterService;
import dungcony.ds.services.interfaces.messaging.NetworkBroadcastService;
import dungcony.ds.services.interfaces.messaging.PeerMessageSender;
import dungcony.ds.services.interfaces.peer.NetworkAddressService;
import dungcony.ds.services.interfaces.peer.PeerDirectoryService;
import dungcony.ds.services.interfaces.peer.PeerDiscoverService;
import dungcony.ds.services.interfaces.peer.PeerPresenceService;

import java.nio.file.Path;
import java.util.function.Consumer;

public class PeerNodeFactory {
    public PeerNodeDependencies create(String peerId,
                                       String peerName,
                                       int port,
                                       String bootstrapHost,
                                       int bootstrapPort,
                                       Path dataDir,
                                       Consumer<Message> messageNotifier,
                                       Runnable peerChangeNotifier) {
        NetworkAddressService networkAddressService = new NetworkAddressImpl();
        PeerInfo localPeer = new PeerInfo(peerId, peerName, networkAddressService.resolveLocalHost(), port);
        PeerMessageSender messageSender = new MessageSender(new TCPClient());
        BootstrapGateway bootstrapGateway = buildBootstrapGateway(bootstrapHost, bootstrapPort);

        PeerDirectoryService peerDirectoryService = new PeerDirectoryImpl(localPeer, networkAddressService);
        MessageHistoryService messageHistoryService = new MessageHistoryImpl(new LocalMessageRepo(dataDir));

        BootstrapGroupService bootstrapGroupService = new BootstrapGroupImpl(bootstrapGateway, localPeer);
        GroupManager groupManager = new GroupManager(new LocalGroupRepo(dataDir), bootstrapGroupService::publishGroup);

        ChatService chatService = new ChatImpl(localPeer, messageSender, bootstrapGateway,
                peerDirectoryService, messageHistoryService,
                messageNotifier, peerChangeNotifier);
        PeerPresenceService peerPresenceService = new PeerPresenceImpl(localPeer, messageSender, bootstrapGateway,
                peerDirectoryService, peerChangeNotifier);
        ConversationService conversationService = new ConversationImpl(peerDirectoryService, messageHistoryService);
        NetworkBroadcastService networkBroadcastService = new NetworkBroadcastImpl(localPeer, messageSender, bootstrapGateway,
                peerDirectoryService, peerChangeNotifier);
        GroupChatService groupChatService = new GroupChatImpl(localPeer, messageSender, bootstrapGateway,
                peerDirectoryService, messageHistoryService,
                bootstrapGroupService, groupManager,
                messageNotifier, peerChangeNotifier);
        PeerDiscoverService peerDiscoverService = new PeerDiscoverImpl(localPeer, messageSender,
                peerDirectoryService, peerChangeNotifier);
        InboundMessageService inboundMessageService = new InboundMessageImpl(localPeer, peerDirectoryService,
                messageHistoryService, groupManager,
                messageNotifier, peerChangeNotifier);
        MessageRetryService messageRetryService = new MessageRetryImpl(messageSender, bootstrapGateway,
                peerDirectoryService, messageHistoryService,
                messageNotifier, peerChangeNotifier);
        BootstrapSyncService bootstrapSyncService = buildBootstrapSyncService(bootstrapGateway, localPeer,
                peerDirectoryService, messageHistoryService,
                groupManager, bootstrapGroupService,
                peerChangeNotifier, messageNotifier);

        MessageRouterService messageRouterService = new MessageRouterImpl(localPeer, inboundMessageService, peerDiscoverService);
        TCPServer tcpServer = new TCPServer(port, new MessageReceiver(messageRouterService));
        PeerNodeRuntime runtime = new PeerNodeRuntime(localPeer, tcpServer, bootstrapGateway, bootstrapSyncService);

        return new PeerNodeDependencies(
                localPeer,
                peerDirectoryService,
                messageHistoryService,
                chatService,
                peerPresenceService,
                conversationService,
                networkBroadcastService,
                groupChatService,
                peerDiscoverService,
                inboundMessageService,
                messageRetryService,
                bootstrapGateway,
                runtime
        );
    }

    private BootstrapGateway buildBootstrapGateway(String host, int port) {
        return (host == null || host.isBlank()) ? null : new BootstrapClient(host, port);
    }

    private BootstrapSyncService buildBootstrapSyncService(BootstrapGateway bootstrapGateway,
                                                          PeerInfo localPeer,
                                                          PeerDirectoryService peerDirectoryService,
                                                          MessageHistoryService messageHistoryService,
                                                          GroupManager groupManager,
                                                          BootstrapGroupService bootstrapGroupService,
                                                          Runnable peerChangeNotifier,
                                                          Consumer<Message> messageNotifier) {
        if (bootstrapGateway == null) {
            return null;
        }
        return new BootstrapSyncImpl(bootstrapGateway, localPeer,
                peerDirectoryService, messageHistoryService,
                groupManager, bootstrapGroupService,
                peerChangeNotifier, messageNotifier);
    }
}
