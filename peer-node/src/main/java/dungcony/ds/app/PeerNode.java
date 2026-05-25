package dungcony.ds.app;

import dungcony.ds.app.PeerNodeDependencies;
import dungcony.ds.app.PeerNodeFactory;
import dungcony.ds.app.PeerNodeRuntime;
import dungcony.ds.dtos.BroadcastResult;
import dungcony.ds.model.Group;
import dungcony.ds.model.Message;
import dungcony.ds.model.PeerInfo;
import dungcony.ds.services.impl.event.SwingEventDispatcher;
import dungcony.ds.services.interfaces.bootstrap.PeerBootstrapGateway;
import dungcony.ds.services.interfaces.chat.ChatService;
import dungcony.ds.services.interfaces.chat.ConversationService;
import dungcony.ds.services.interfaces.event.EventDispatcher;
import dungcony.ds.services.interfaces.group.GroupChatService;
import dungcony.ds.services.interfaces.messaging.InboundMessageService;
import dungcony.ds.services.interfaces.messaging.MessageListener;
import dungcony.ds.services.interfaces.messaging.MessageRetryService;
import dungcony.ds.services.interfaces.messaging.NetworkBroadcastService;
import dungcony.ds.services.interfaces.peer.PeerDirectoryService;
import dungcony.ds.services.interfaces.peer.PeerDiscoverService;
import dungcony.ds.services.interfaces.peer.PeerPresenceService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Facade công khai của peer node, ủy quyền nghiệp vụ cho các service
 */
@Slf4j
@Getter
public class PeerNode {

    public static final int DEFAULT_PORT = 5001;

    private final PeerInfo localPeer;
    private final List<MessageListener> messageListeners = new CopyOnWriteArrayList<>();
    private final List<Runnable> peerChangeListeners = new CopyOnWriteArrayList<>();
    // DIP fix: EventDispatcher được inject thay vì hard-wire SwingEventDispatcher
    private final EventDispatcher eventDispatcher;

    // ── Dịch vụ lõi ─────
    private final PeerDirectoryService peerDirectoryService;

    // ── Dịch vụ nghiệp vụ ────
    private final ChatService chatService;
    private final PeerPresenceService peerPresenceService;
    private final ConversationService conversationService;
    private final NetworkBroadcastService networkBroadcastService;
    private final GroupChatService groupChatService;
    private final PeerDiscoverService peerDiscoverService;
    private final InboundMessageService inboundMessageService;
    private final MessageRetryService messageRetryService;

    private final PeerBootstrapGateway bootstrapGateway;
    private final PeerNodeRuntime runtime;

    /**
     * Khởi tạo facade PeerNode với SwingEventDispatcher mặc định (tương thích ngược).
     * App.java không cần thay đổi.
     */
    public PeerNode(String peerId, String peerName, int port,
                    String bootstrapHost, int bootstrapPort, Path dataDir) {
        this(peerId, peerName, port, bootstrapHost, bootstrapPort, dataDir, new SwingEventDispatcher());
    }

    /**
     * Khởi tạo facade PeerNode với EventDispatcher tùy chỉnh.
     * Dùng khi cần swap UI framework hoặc inject mock trong test.
     */
    public PeerNode(String peerId, String peerName, int port,
                    String bootstrapHost, int bootstrapPort, Path dataDir,
                    EventDispatcher eventDispatcher) {
        this.eventDispatcher = eventDispatcher;
        PeerNodeDependencies dependencies = new PeerNodeFactory().create(
                peerId,
                peerName,
                port,
                bootstrapHost,
                bootstrapPort,
                dataDir,
                this::notifyMessage,
                this::notifyPeersChanged
        );

        this.localPeer = dependencies.localPeer();
        this.peerDirectoryService = dependencies.peerDirectoryService();
        this.chatService = dependencies.chatService();
        this.peerPresenceService = dependencies.peerPresenceService();
        this.conversationService = dependencies.conversationService();
        this.networkBroadcastService = dependencies.networkBroadcastService();
        this.groupChatService = dependencies.groupChatService();
        this.peerDiscoverService = dependencies.peerDiscoverService();
        this.inboundMessageService = dependencies.inboundMessageService();
        this.messageRetryService = dependencies.messageRetryService();
        this.bootstrapGateway = dependencies.bootstrapGateway();
        this.runtime = dependencies.runtime();

        String bootstrapAddress = bootstrapGateway == null
                ? "đã tắt" : "%s:%d".formatted(bootstrapHost, bootstrapPort);
        Path resolvedDataDir = dataDir == null ? Path.of("peer-node", "src", "main", "resources", "data") : dataDir;
        log.info("Đã khởi tạo PeerNode: id={}, tên={}, địaChỉ={}, bootstrap={}, thưMụcDữLiệu={}",
                localPeer.getId(), localPeer.getName(), localPeer.addressKey(),
                bootstrapAddress, resolvedDataDir.toAbsolutePath());
    }

    // Khởi động TCPServer trên daemon thread và vòng sync bootstrap nếu có
    public void start() {
        runtime.start();
    }

    // Gửi LEAVE lên bootstrap rồi dừng TCPServer
    public void stop() {
        runtime.stop();
    }

    // Đăng ký callback để UI được thông báo khi có tin nhắn mới
    public void addMessageListener(MessageListener listener) {
        if (listener != null) {
            messageListeners.add(listener);
        }
    }

    // Đăng ký callback để UI refresh danh sách peer khi trạng thái peer thay đổi
    public void addPeerChangeListener(Runnable listener) {
        if (listener != null) {
            peerChangeListeners.add(listener);
        }
    }

    // Thêm một peer đã biết vào bộ nhớ runtime
    public PeerInfo addKnownPeer(String name, String hostAndMaybePort) {
        PeerInfo peerInfo = peerDirectoryService.addKnownPeer(name, hostAndMaybePort);
        notifyPeersChanged();
        return peerInfo;
    }

    // Lấy danh sách peer mà node hiện đang biết để UI hiển thị trong ChatList
    public Collection<PeerInfo> getKnownPeers() {
        return peerDirectoryService.list();
    }

    // Lấy danh sách chat: peer online từ bootstrap và peer offline đã từng có message
    public Collection<PeerInfo> getChatListPeers() {
        return conversationService.getChatListPeers();
    }

    // Kiểm tra peer có online không, ưu tiên trạng thái từ bootstrap-server
    public boolean checkUserIsOnline(String hostAndMaybePort) {
        return peerPresenceService.checkUserIsOnline(hostAndMaybePort);
    }

    // Kiểm tra bootstrap-server có đang reachable không
    public boolean isBootstrapAvailable() {
        if (bootstrapGateway == null) {
            return false;
        }
        boolean available = bootstrapGateway.listOrNull() != null;
        log.info("Đã kiểm tra bootstrap. khảDụng={}", available);
        return available;
    }

    // Gửi tin nhắn 1-1 trực tiếp tới peer đích
    public boolean sendMessage(String content, String hostAndMaybePort) {
        return chatService.sendMessage(content, hostAndMaybePort);
    }

    // Gửi một message đến toàn bộ peer online mà node biết
    public BroadcastResult broadcastToNetwork(String content) {
        return networkBroadcastService.broadcastToNetwork(content);
    }

    // Gửi một tin nhắn tới tất cả thành viên của nhóm đã tạo
    public void sendGroupMessage(String groupId, String content) {
        groupChatService.sendGroupMessage(groupId, content);
    }

    // Tạo group chat mới từ danh sách peer được chọn trong UI
    public Group createGroup(String name, Collection<PeerInfo> members) {
        return groupChatService.createGroup(name, members);
    }

    // Thêm peer vào group hiện có
    public Group addMembersToGroup(String groupId, Collection<PeerInfo> members) {
        return groupChatService.addMembersToGroup(groupId, members);
    }

    // Đổi tên group hiện có
    public Group renameGroup(String groupId, String name) {
        return groupChatService.renameGroup(groupId, name);
    }

    // Lấy các group hiện tại của peer để UI hiển thị
    public Collection<Group> getGroups() {
        return groupChatService.getGroups();
    }

    // Lấy lịch sử tin nhắn với một peer cụ thể
    public List<Message> getMessagesWithPeer(String hostAndMaybePort) {
        return conversationService.getMessagesWithPeer(hostAndMaybePort);
    }

    // Lấy tin nhắn cuối cùng với một peer để hiển thị preview
    public Message getLastMessage(String hostAndMaybePort) {
        return conversationService.getLastMessage(hostAndMaybePort);
    }

    // Lấy lịch sử broadcast toàn mạng
    public List<Message> getBroadcastMessages() {
        return conversationService.getBroadcastMessages();
    }

    // Lấy broadcast cuối cùng để hiển thị preview
    public Message getLastBroadcastMessage() {
        return conversationService.getLastBroadcastMessage();
    }

    // Lấy lịch sử message của group
    public List<Message> getMessagesWithGroup(String groupId) {
        return groupChatService.getMessagesWithGroup(groupId);
    }

    // Lấy message cuối cùng của group để hiển thị preview
    public Message getLastGroupMessage(String groupId) {
        return groupChatService.getLastGroupMessage(groupId);
    }

    // Hỏi một peer đã biết danh sách peer mà nó đang biết (fallback khi không có bootstrap)
    public List<PeerInfo> discoverPeersFromKnownPeer(PeerInfo knownPeer) {
        return peerDiscoverService.discoverPeersFromKnownPeer(knownPeer);
    }

    // Tạo PEER_LIST_RESPONSE gồm local peer và danh bạ runtime hiện tại
    public Message buildPeerListResponse(Message request) {
        return peerDiscoverService.buildPeerListResponse(request);
    }

    // Merge danh sách peer nhận từ PEER_LIST_RESPONSE vào danh bạ local
    public int onPeerListResponse(Message response) {
        return peerDiscoverService.onPeerListResponse(response);
    }

    // Xử lý tin nhắn đến từ network
    public void onInboundMessage(Message message) {
        inboundMessageService.onInboundMessage(message);
    }

    // Cập nhật group local khi nhận snapshot membership từ peer khác
    public void onGroupMembersSync(Message message) {
        inboundMessageService.onGroupMembersSync(message);
    }

    // Đánh dấu peer gửi heartbeat/JOIN là online
    public void markPeerOnline(Message message) {
        inboundMessageService.markPeerOnline(message);
    }

    // Retry thủ công một tin nhắn 1-1 FAILED/PENDING
    public boolean retryMessage(Message message) {
        return messageRetryService.retryMessage(message);
    }

    // Kiểm tra địa chỉ người dùng nhập có trỏ về chính peer hiện tại hay không
    public boolean isSelfAddress(String hostAndMaybePort) {
        PeerInfo peerInfo = peerDirectoryService.parsePeer(hostAndMaybePort, hostAndMaybePort);
        return peerDirectoryService.isSelfPeer(peerInfo);
    }

    // ── Notify helpers ──

    // Notify các MessageListener, bảo đảm callback chạy trên Swing EDT
    private void notifyMessage(Message message) {
        eventDispatcher.dispatch(() -> messageListeners.forEach(l -> l.onMessageReceived(message)));
    }

    // Notify các listener đang quan sát thay đổi danh sách/trạng thái peer
    private void notifyPeersChanged() {
        eventDispatcher.dispatch(() -> peerChangeListeners.forEach(Runnable::run));
    }

}
