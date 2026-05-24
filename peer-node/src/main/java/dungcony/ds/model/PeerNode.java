package dungcony.ds.model;

import dungcony.ds.network.*;
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
import dungcony.ds.services.impl.messaging.NetworkBroadcastImpl;
import dungcony.ds.services.impl.peer.NetworkAddressImpl;
import dungcony.ds.services.impl.peer.PeerDirectoryImpl;
import dungcony.ds.services.impl.peer.PeerDiscoverImpl;
import dungcony.ds.services.impl.peer.PeerPresenceImpl;
import dungcony.ds.services.impl.profile.ProfileSelectionImpl;
import dungcony.ds.services.interfaces.bootstrap.BootstrapGroupService;
import dungcony.ds.services.interfaces.bootstrap.BootstrapSyncService;
import dungcony.ds.services.interfaces.chat.ChatService;
import dungcony.ds.services.interfaces.chat.ConversationService;
import dungcony.ds.services.interfaces.group.GroupChatService;
import dungcony.ds.services.interfaces.messaging.InboundMessageService;
import dungcony.ds.services.interfaces.messaging.MessageHistoryService;
import dungcony.ds.services.interfaces.messaging.MessageListener;
import dungcony.ds.services.interfaces.messaging.MessageRetryService;
import dungcony.ds.services.interfaces.messaging.NetworkBroadcastService;
import dungcony.ds.services.interfaces.peer.NetworkAddressService;
import dungcony.ds.services.interfaces.peer.PeerDirectoryService;
import dungcony.ds.services.interfaces.peer.PeerDiscoverService;
import dungcony.ds.services.interfaces.peer.PeerPresenceService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Getter
public class PeerNode {

    public static final int DEFAULT_PORT = 5001;
    private static final long BOOTSTRAP_REFRESH_INTERVAL_MS = 5000;

    private final PeerInfo localPeer;
    private final List<MessageListener> messageListeners = new CopyOnWriteArrayList<>();
    private final List<Runnable> peerChangeListeners = new CopyOnWriteArrayList<>();

    // ── Tầng 1: Mạng & peer local
    private final MessageSender messageSender;
    private final TCPServer tcpServer;
    private final BootstrapClient bootstrapClient;

    // ── Tầng 2: Dịch vụ lõi ─────
    private final PeerDirectoryService peerDirectoryService;
    private final MessageHistoryService messageHistoryService;

    // ── Tầng 3: Quản lý nhóm ────
    private final BootstrapGroupService bootstrapGroupService;
    private final GroupManager groupManager;

    // ── Tầng 4: Dịch vụ nghiệp vụ ────
    private final ChatService chatService;
    private final PeerPresenceService peerPresenceService;
    private final ConversationService conversationService;
    private final NetworkBroadcastService networkBroadcastService;
    private final GroupChatService groupChatService;
    private final PeerDiscoverService peerDiscoverService;
    private final InboundMessageService inboundMessageService;
    private final MessageRetryService messageRetryService;

    // ── Tầng 5: Đồng bộ bootstrap (có thể null)
    private final BootstrapSyncService bootstrapSyncService;

    private volatile boolean running;

    public record BroadcastResult(int totalTargets, int delivered, int failed) {
    }

    /**
     * Khởi tạo PeerNode theo thứ tự tầng phụ thuộc.
     * Mỗi factory method chỉ sử dụng các field đã được gán ở tầng trước.
     */
    public PeerNode(String peerId, String peerName, int port,
                    String bootstrapHost, int bootstrapPort, Path dataDir) {

        // ── Tầng 1: Mạng & peer local ─
        NetworkAddressService networkAddressService = new NetworkAddressImpl();
        this.localPeer = new PeerInfo(peerId, peerName, networkAddressService.resolveLocalHost(), port);
        this.messageSender = new MessageSender(new TCPClient());
        this.tcpServer = new TCPServer(port, new MessageReceiver(this));
        this.bootstrapClient = buildBootstrapClient(bootstrapHost, bootstrapPort);

        // ── Tầng 2: Dịch vụ lõi ─
        this.peerDirectoryService = new PeerDirectoryImpl(localPeer, networkAddressService);
        this.messageHistoryService = new MessageHistoryImpl(new LocalMessageRepo(dataDir));

        // ── Tầng 3: Quản lý nhóm
        this.bootstrapGroupService = new BootstrapGroupImpl(bootstrapClient, localPeer);
        this.groupManager = new GroupManager(new LocalGroupRepo(dataDir), bootstrapGroupService::publishGroup);

        // ── Tầng 4: Dịch vụ nghiệp vụ
        this.chatService = buildChatService();
        this.peerPresenceService = buildPeerPresenceService();
        this.conversationService = new ConversationImpl(peerDirectoryService, messageHistoryService);
        this.networkBroadcastService = buildNetworkBroadcastService();
        this.groupChatService = buildGroupChatService();
        this.peerDiscoverService = buildPeerDiscoverService();
        this.inboundMessageService = buildInboundMessageService();
        this.messageRetryService = buildMessageRetryService();

        // ── Tầng 5: Đồng bộ bootstrap (tùy chọn) ───
        this.bootstrapSyncService = buildBootstrapSyncService();

        String bootstrapAddress = bootstrapClient == null
                ? "đã tắt" : "%s:%d".formatted(bootstrapHost, bootstrapPort);
        log.info("Đã khởi tạo PeerNode: id={}, tên={}, địaChỉ={}, bootstrap={}, thưMụcDữLiệu={}",
                localPeer.getId(), localPeer.getName(), localPeer.addressKey(),
                bootstrapAddress, dataDir.toAbsolutePath());
    }

    // Khởi động TCPServer trên daemon thread và vòng sync bootstrap nếu có.
    public void start() {
        log.info("Đang khởi động bộ lắng nghe TCP cho peer local {}", localPeer.addressKey());
        running = true;
        Thread serverThread = new Thread(tcpServer::listen, "PeerNode-TCPServer-" + localPeer.getPort());
        serverThread.setDaemon(true);
        serverThread.start();
        if (bootstrapSyncService != null) {
            Thread bootstrapThread = new Thread(this::runBootstrapSyncLoop, "PeerNode-Bootstrap");
            bootstrapThread.setDaemon(true);
            bootstrapThread.start();
        }
    }

    // Gửi LEAVE lên bootstrap rồi dừng TCPServer.
    public void stop() {
        log.info("Đang dừng PeerNode {}", localPeer.addressKey());
        running = false;
        if (bootstrapClient != null) {
            bootstrapClient.leave(localPeer.addressKey());
        }
        tcpServer.stop();
    }

    // Đăng ký callback để UI được thông báo khi có tin nhắn mới.
    public void addMessageListener(MessageListener listener) {
        if (listener != null) {
            messageListeners.add(listener);
        }
    }

    // Đăng ký callback để UI refresh danh sách peer khi trạng thái peer thay đổi.
    public void addPeerChangeListener(Runnable listener) {
        if (listener != null) {
            peerChangeListeners.add(listener);
        }
    }

    // Thêm một peer đã biết vào bộ nhớ runtime.
    public PeerInfo addKnownPeer(String name, String hostAndMaybePort) {
        PeerInfo peerInfo = peerDirectoryService.addKnownPeer(name, hostAndMaybePort);
        notifyPeersChanged();
        return peerInfo;
    }

    // Lấy danh sách peer mà node hiện đang biết để UI hiển thị trong ChatList.
    public Collection<PeerInfo> getKnownPeers() {
        return peerDirectoryService.list();
    }

    // Lấy danh sách chat: peer online từ bootstrap và peer offline đã từng có message.
    public Collection<PeerInfo> getChatListPeers() {
        return conversationService.getChatListPeers();
    }

    // Kiểm tra peer có online không, ưu tiên trạng thái từ bootstrap-server.
    public boolean checkUserIsOnline(String hostAndMaybePort) {
        return peerPresenceService.checkUserIsOnline(hostAndMaybePort);
    }

    // Kiểm tra bootstrap-server có đang reachable không.
    public boolean isBootstrapAvailable() {
        if (bootstrapClient == null) {
            return false;
        }
        boolean available = bootstrapClient.listOrNull() != null;
        log.info("Đã kiểm tra bootstrap. khảDụng={}", available);
        return available;
    }

    // Gửi tin nhắn 1-1 trực tiếp tới peer đích.
    public boolean sendMessage(String content, String hostAndMaybePort) {
        return chatService.sendMessage(content, hostAndMaybePort);
    }

    // Gửi một message đến toàn bộ peer online mà node biết.
    public BroadcastResult broadcastToNetwork(String content) {
        return networkBroadcastService.broadcastToNetwork(content);
    }

    // Gửi một tin nhắn tới tất cả thành viên của nhóm đã tạo.
    public void sendGroupMessage(String groupId, String content) {
        groupChatService.sendGroupMessage(groupId, content);
    }

    // Tạo group chat mới từ danh sách peer được chọn trong UI.
    public Group createGroup(String name, Collection<PeerInfo> members) {
        return groupChatService.createGroup(name, members);
    }

    // Thêm peer vào group hiện có.
    public Group addMembersToGroup(String groupId, Collection<PeerInfo> members) {
        return groupChatService.addMembersToGroup(groupId, members);
    }

    // Lấy các group hiện tại của peer để UI hiển thị.
    public Collection<Group> getGroups() {
        return groupChatService.getGroups();
    }

    // Lấy lịch sử tin nhắn với một peer cụ thể.
    public List<Message> getMessagesWithPeer(String hostAndMaybePort) {
        return conversationService.getMessagesWithPeer(hostAndMaybePort);
    }

    // Lấy tin nhắn cuối cùng với một peer để hiển thị preview.
    public Message getLastMessage(String hostAndMaybePort) {
        return conversationService.getLastMessage(hostAndMaybePort);
    }

    // Lấy lịch sử message của group.
    public List<Message> getMessagesWithGroup(String groupId) {
        return groupChatService.getMessagesWithGroup(groupId);
    }

    // Lấy message cuối cùng của group để hiển thị preview.
    public Message getLastGroupMessage(String groupId) {
        return groupChatService.getLastGroupMessage(groupId);
    }

    // Hỏi một peer đã biết danh sách peer mà nó đang biết (fallback khi không có bootstrap).
    public List<PeerInfo> discoverPeersFromKnownPeer(PeerInfo knownPeer) {
        return peerDiscoverService.discoverPeersFromKnownPeer(knownPeer);
    }

    // Tạo PEER_LIST_RESPONSE gồm local peer và danh bạ runtime hiện tại.
    public Message buildPeerListResponse(Message request) {
        return peerDiscoverService.buildPeerListResponse(request);
    }

    // Merge danh sách peer nhận từ PEER_LIST_RESPONSE vào danh bạ local.
    public int onPeerListResponse(Message response) {
        return peerDiscoverService.onPeerListResponse(response);
    }

    // Xử lý tin nhắn đến từ network.
    public void onInboundMessage(Message message) {
        inboundMessageService.onInboundMessage(message);
    }

    // Cập nhật group local khi nhận snapshot membership từ peer khác.
    public void onGroupMembersSync(Message message) {
        inboundMessageService.onGroupMembersSync(message);
    }

    // Đánh dấu peer gửi heartbeat/JOIN là online.
    public void markPeerOnline(Message message) {
        inboundMessageService.markPeerOnline(message);
    }

    // Retry thủ công một tin nhắn 1-1 FAILED/PENDING.
    public boolean retryMessage(Message message) {
        return messageRetryService.retryMessage(message);
    }

    // Kiểm tra địa chỉ người dùng nhập có trỏ về chính peer hiện tại hay không.
    public boolean isSelfAddress(String hostAndMaybePort) {
        PeerInfo peerInfo = peerDirectoryService.parsePeer(hostAndMaybePort, hostAndMaybePort);
        return peerDirectoryService.isSelfPeer(peerInfo);
    }

    // ── Factory methods (dùng khi khởi tạo) ─────

    private static BootstrapClient buildBootstrapClient(String host, int port) {
        return (host == null || host.isBlank()) ? null : new BootstrapClient(host, port);
    }

    private ChatService buildChatService() {
        return new ChatImpl(localPeer, messageSender, bootstrapClient,
                peerDirectoryService, messageHistoryService,
                this::notifyMessage, this::notifyPeersChanged);
    }

    private PeerPresenceService buildPeerPresenceService() {
        return new PeerPresenceImpl(localPeer, messageSender, bootstrapClient,
                peerDirectoryService, this::notifyPeersChanged);
    }

    private NetworkBroadcastService buildNetworkBroadcastService() {
        return new NetworkBroadcastImpl(localPeer, messageSender, bootstrapClient,
                peerDirectoryService, this::notifyPeersChanged);
    }

    private GroupChatService buildGroupChatService() {
        return new GroupChatImpl(localPeer, messageSender, bootstrapClient,
                peerDirectoryService, messageHistoryService,
                bootstrapGroupService, groupManager,
                this::notifyMessage, this::notifyPeersChanged);
    }

    private PeerDiscoverService buildPeerDiscoverService() {
        return new PeerDiscoverImpl(localPeer, messageSender,
                peerDirectoryService, this::notifyPeersChanged);
    }

    private InboundMessageService buildInboundMessageService() {
        return new InboundMessageImpl(localPeer, peerDirectoryService,
                messageHistoryService, groupManager,
                this::notifyMessage, this::notifyPeersChanged);
    }

    private MessageRetryService buildMessageRetryService() {
        return new MessageRetryImpl(messageSender, bootstrapClient,
                peerDirectoryService, messageHistoryService,
                this::notifyMessage, this::notifyPeersChanged);
    }

    private BootstrapSyncService buildBootstrapSyncService() {
        if (bootstrapClient == null) return null;
        return new BootstrapSyncImpl(bootstrapClient, localPeer,
                peerDirectoryService, messageHistoryService,
                groupManager, bootstrapGroupService,
                this::notifyPeersChanged, this::notifyMessage);
    }

    // ── Notify helpers ──

    // Notify các MessageListener, bảo đảm callback chạy trên Swing EDT.
    private void notifyMessage(Message message) {
        Runnable notifier = () -> messageListeners.forEach(l -> l.onMessageReceived(message));
        if (SwingUtilities.isEventDispatchThread()) {
            notifier.run();
        } else {
            SwingUtilities.invokeLater(notifier);
        }
    }

    // Notify các listener đang quan sát thay đổi danh sách/trạng thái peer.
    private void notifyPeersChanged() {
        Runnable notifier = () -> peerChangeListeners.forEach(Runnable::run);
        if (SwingUtilities.isEventDispatchThread()) {
            notifier.run();
        } else {
            SwingUtilities.invokeLater(notifier);
        }
    }

    // Chạy REGISTER/JOIN một lần, sau đó định kỳ JOIN lại như heartbeat.
    private void runBootstrapSyncLoop() {
        bootstrapSyncService.registerAndJoinBootstrap();
        while (running) {
            try {
                Thread.sleep(BOOTSTRAP_REFRESH_INTERVAL_MS);
                bootstrapSyncService.refreshFromBootstrap();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (RuntimeException e) {
                log.error("Vòng refresh bootstrap lỗi: {}", e.getMessage());
            }
        }
    }
}
