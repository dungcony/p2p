package dungcony.ds.peer;

import dungcony.ds.interfaces.BootstrapGroupService;
import dungcony.ds.interfaces.BootstrapSyncService;
import dungcony.ds.interfaces.ChatService;
import dungcony.ds.interfaces.LanDiscoveryService;
import dungcony.ds.interfaces.MessageListener;
import dungcony.ds.interfaces.MessageHistoryService;
import dungcony.ds.interfaces.NetworkAddressService;
import dungcony.ds.interfaces.PeerDirectoryService;
import dungcony.ds.interfaces.PeerPresenceService;
import dungcony.ds.mapper.Mes;
import dungcony.ds.model.BootstrapClient;
import dungcony.ds.model.Group;
import dungcony.ds.model.Message;
import dungcony.ds.model.PeerInfo;
import dungcony.ds.network.TCPClient;
import dungcony.ds.network.TCPServer;
import dungcony.ds.repositories.LocalGroupRepo;
import dungcony.ds.repositories.LocalMessageRepo;
import dungcony.ds.services.BootstrapGroupImpl;
import dungcony.ds.services.BootstrapSyncImpl;
import dungcony.ds.services.ChatImpl;
import dungcony.ds.services.LanDiscoveryImpl;
import dungcony.ds.services.MessageHistoryImpl;
import dungcony.ds.services.NetworkAddressImpl;
import dungcony.ds.services.PeerDirectoryImpl;
import dungcony.ds.services.PeerPresenceImpl;

import javax.swing.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class PeerNode {
    public static final int DEFAULT_PORT = 5001;
    private static final long BOOTSTRAP_REFRESH_INTERVAL_MS = 5000;
    private static final String GROUP_CHAT_PREFIX = "group:";

    private final PeerInfo localPeer;
    private final List<MessageListener> messageListeners = new CopyOnWriteArrayList<>();
    private final List<Runnable> peerChangeListeners = new CopyOnWriteArrayList<>();
    private final PeerDirectoryService peerDirectoryService;
    private final MessageHistoryService messageHistoryService;
    private final ChatService chatService;
    private final PeerPresenceService peerPresenceService;
    private final LanDiscoveryService lanDiscoveryService;
    private final BootstrapSyncService bootstrapSyncService;
    private final GroupManager groupManager;
    private final MessageSender messageSender;
    private final TCPServer tcpServer;
    private final BootstrapClient bootstrapClient;
    private volatile boolean running;

    /**
     * Khởi tạo một peer cục bộ với tên định danh và port lắng nghe do người dùng nhập.
     */
    public PeerNode(String peerId, int port) {
        this(peerId, peerId, port, null, 0, Path.of("peer-node", "src", "main", "resources", "data"));
    }

    /**
     * Khởi tạo peer với id ổn định, tên hiển thị, port lắng nghe và bootstrap-server.
     */
    public PeerNode(String peerId, String peerName, int port, String bootstrapHost, int bootstrapPort) {
        this(peerId, peerName, port, bootstrapHost, bootstrapPort,
                Path.of("peer-node", "src", "main", "resources", "data"));
    }

    /**
     * Khởi tạo peer với dataDir riêng để test nhiều instance trên cùng một máy.
     */
    public PeerNode(String peerId, String peerName, int port, String bootstrapHost, int bootstrapPort, Path dataDir) {
        NetworkAddressService networkAddressService = new NetworkAddressImpl();
        this.localPeer = new PeerInfo(peerId, peerName, networkAddressService.resolveLocalHost(), port);
        TCPClient tcpClient = new TCPClient();
        this.messageSender = new MessageSender(tcpClient);
        this.tcpServer = new TCPServer(port, new MessageReceiver(this));
        this.bootstrapClient = bootstrapHost == null || bootstrapHost.isBlank()
                ? null
                : new BootstrapClient(bootstrapHost, bootstrapPort);
        this.peerDirectoryService = new PeerDirectoryImpl(localPeer, networkAddressService);
        this.messageHistoryService = new MessageHistoryImpl(new LocalMessageRepo(dataDir));
        this.chatService = new ChatImpl(
                localPeer,
                messageSender,
                bootstrapClient,
                peerDirectoryService,
                messageHistoryService,
                this::notifyMessage,
                this::notifyPeersChanged
        );
        this.peerPresenceService = new PeerPresenceImpl(
                localPeer,
                messageSender,
                bootstrapClient,
                peerDirectoryService,
                this::notifyPeersChanged
        );
        BootstrapGroupService bootstrapGroupService = new BootstrapGroupImpl(bootstrapClient, localPeer);
        this.groupManager = new GroupManager(new LocalGroupRepo(dataDir), bootstrapGroupService::publishGroup);
        this.lanDiscoveryService = new LanDiscoveryImpl(localPeer, messageSender, peerDirectoryService);
        this.bootstrapSyncService = bootstrapClient == null ? null : new BootstrapSyncImpl(
                bootstrapClient,
                localPeer,
                peerDirectoryService,
                messageHistoryService,
                groupManager,
                bootstrapGroupService,
                this::notifyPeersChanged,
                this::notifyMessage
        );
        System.out.println("[INFO] PeerNode initialized: id=" + localPeer.getId()
                + ", name=" + localPeer.getName()
                + ", address=" + localPeer.addressKey()
                + ", bootstrap=" + (bootstrapClient == null ? "disabled" : bootstrapHost + ":" + bootstrapPort)
                + ", dataDir=" + dataDir.toAbsolutePath());
    }

    /**
     * Khởi động vai trò nhận tin của peer bằng TCPServer trên một thread riêng.
     */
    public void start() {
        System.out.println("[INFO] Starting TCP listener for local peer " + localPeer.addressKey());
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

    /**
     * Dừng TCPServer để peer ngừng nhận kết nối mới.
     */
    public void stop() {
        System.out.println("[INFO] Stopping PeerNode " + localPeer.addressKey());
        running = false;
        if (bootstrapClient != null) {
            bootstrapClient.leave(localPeer.addressKey());
        }
        tcpServer.stop();
    }

    public PeerInfo getLocalPeer() {
        return localPeer;
    }

    /**
     * Đăng ký callback để UI được thông báo khi có tin nhắn mới.
     */
    public void addMessageListener(MessageListener listener) {
        if (listener != null) {
            messageListeners.add(listener);
        }
    }

    /**
     * Đăng ký callback để UI refresh danh sách peer khi trạng thái peer thay đổi.
     */
    public void addPeerChangeListener(Runnable listener) {
        if (listener != null) {
            peerChangeListeners.add(listener);
        }
    }

    /**
     * Thêm một peer đã biết vào bộ nhớ runtime, thường được gọi từ màn Add Friend hoặc scanner.
     */
    public PeerInfo addKnownPeer(String name, String hostAndMaybePort) {
        PeerInfo peerInfo = peerDirectoryService.addKnownPeer(name, hostAndMaybePort);
        notifyPeersChanged();
        return peerInfo;
    }

    /**
     * Lấy danh sách peer mà node hiện đang biết để UI hiển thị trong ChatList.
     */
    public Collection<PeerInfo> getKnownPeers() {
        return peerDirectoryService.list();
    }

    /**
     * Lay danh sach chat: peer online tu bootstrap va peer offline da tung co message.
     */
    public Collection<PeerInfo> getChatListPeers() {
        Map<String, PeerInfo> conversations = new LinkedHashMap<>();
        for (PeerInfo peerInfo : peerDirectoryService.list()) {
            if (peerInfo.isOnline()) {
                conversations.put(peerInfo.getId(), peerInfo);
            }
        }
        for (PeerInfo historyPeer : messageHistoryService.getDirectConversationPeers()) {
            PeerInfo runtimePeer = peerDirectoryService.findKnownPeerById(historyPeer.getId());
            if (runtimePeer == null) {
                peerDirectoryService.put(historyPeer);
                conversations.put(historyPeer.getId(), historyPeer);
            } else {
                conversations.put(historyPeer.getId(), runtimePeer);
            }
        }
        System.out.println("[DEBUG] Chat list peers resolved. count=" + conversations.size());
        return conversations.values();
    }

    /**
     * Kiem tra peer co online khong, uu tien trang thai tu bootstrap-server.
     */
    public boolean checkUserIsOnline(String hostAndMaybePort) {
        return peerPresenceService.checkUserIsOnline(hostAndMaybePort);
    }

    /**
     * Kiem tra bootstrap-server co dang reachable khong de quyet dinh luong tao group.
     */
    public boolean isBootstrapAvailable() {
        if (bootstrapClient == null) {
            return false;
        }
        boolean available = bootstrapClient.listOrNull() != null;
        System.out.println("[INFO] Bootstrap availability checked. available=" + available);
        return available;
    }

    /**
     * Gửi tin nhắn 1-1 trực tiếp tới peer đích, lưu lịch sử nếu nhận được ACK.
     */
    public boolean sendMessage(String content, String hostAndMaybePort) {
        return chatService.sendMessage(content, hostAndMaybePort);
    }

    /**
     * Gửi một tin nhắn tới tất cả thành viên của nhóm đã tạo.
     */
    public void sendGroupMessage(String groupId, String content) {
        Group group = groupManager.getGroup(groupId);
        if (group == null) {
            System.out.println("[WARN] Cannot send group message. Group not found: " + groupId);
            return;
        }
        Message message = Message.groupChat(localPeer, groupId, group.getName(), content);
        System.out.println("[INFO] Broadcasting GROUP_CHAT message id=" + message.getId()
                + " to group=" + groupId + ", members=" + group.getMembers().size());
        for (PeerInfo member : group.getMembers()) {
            PeerInfo target = peerDirectoryService.findKnownPeerById(member.getId());
            if (target == null) {
                target = member;
            }
            if (peerDirectoryService.isSelfPeer(target)) {
                continue;
            }
            Message memberMessage = Message.groupChat(localPeer, target, groupId, group.getName(), content);
            boolean sent = target.isOnline() && messageSender.send(target, memberMessage);
            target.setOnline(sent);
            member.setOnline(sent);
            if (!sent) {
                storeGroupOfflineIfPossible(memberMessage, target);
            }
        }
        messageHistoryService.addAndSave(groupHistoryKey(groupId), groupConversationPeer(group), message);
        notifyMessage(message);
        notifyPeersChanged();
    }

    /**
     * Cung cấp GroupManager để tầng UI hoặc service khác quản lý nhóm chat.
     */
    public GroupManager getGroupManager() {
        return groupManager;
    }

    /**
     * Tao group chat moi tu danh sach peer duoc chon trong UI.
     */
    public Group createGroup(String name, Collection<PeerInfo> members) {
        Group group = groupManager.createGroup(name, members);
        notifyPeersChanged();
        return group;
    }

    /**
     * Lay cac group hien tai cua peer de UI hien thi.
     */
    public Collection<Group> getGroups() {
        return groupManager.getAllGroups();
    }

    /**
     * Lấy lịch sử tin nhắn với một peer cụ thể theo địa chỉ host hoặc host:port.
     */
    public List<Message> getMessagesWithPeer(String hostAndMaybePort) {
        PeerInfo peerInfo = peerDirectoryService.resolvePeer(hostAndMaybePort);
        return messageHistoryService.getMessages(peerInfo, hostAndMaybePort);
    }

    /**
     * Lấy tin nhắn cuối cùng với một peer để hiển thị preview trong danh sách chat.
     */
    public Message getLastMessage(String hostAndMaybePort) {
        PeerInfo peerInfo = peerDirectoryService.resolvePeer(hostAndMaybePort);
        return messageHistoryService.getLastMessage(peerInfo, hostAndMaybePort);
    }

    /**
     * Lay lich su message cua group.
     */
    public List<Message> getMessagesWithGroup(String groupId) {
        Group group = groupManager.getGroup(groupId);
        PeerInfo groupPeer = group == null
                ? new PeerInfo(groupId, groupId, GROUP_CHAT_PREFIX + groupId, 0, false)
                : groupConversationPeer(group);
        return messageHistoryService.getMessages(groupPeer, groupHistoryKey(groupId));
    }

    /**
     * Lay message cuoi cung cua group de hien preview.
     */
    public Message getLastGroupMessage(String groupId) {
        Group group = groupManager.getGroup(groupId);
        PeerInfo groupPeer = group == null
                ? new PeerInfo(groupId, groupId, GROUP_CHAT_PREFIX + groupId, 0, false)
                : groupConversationPeer(group);
        return messageHistoryService.getLastMessage(groupPeer, groupHistoryKey(groupId));
    }

    /**
     * Quét subnet LAN hiện tại bằng heartbeat để tìm các peer đang chạy cùng port.
     */
    public List<PeerInfo> discoverPeersOnLocalNetwork() {
        List<PeerInfo> discovered = lanDiscoveryService.discoverPeersOnLocalNetwork();
        notifyPeersChanged();
        return discovered;
    }

    /**
     * Xử lý tin nhắn đến từ network: cập nhật peer, lưu lịch sử và notify UI.
     */
    public void onInboundMessage(Message message) {
        PeerInfo sender = peerDirectoryService.mergeSenderFromKnownPeers(message);
        peerDirectoryService.put(sender);
        if (message.getGroupId() != null && !message.getGroupId().isBlank()) {
            Group group = groupManager.getGroup(message.getGroupId());
            if (group == null) {
                group = groupManager.ensureLocalGroup(
                        message.getGroupId(),
                        message.getGroupName(),
                        List.of(localPeer, sender)
                );
            } else {
                groupManager.ensureLocalGroup(message.getGroupId(), group.getName(), List.of(localPeer, sender));
            }
            PeerInfo groupPeer = groupConversationPeer(group);
            messageHistoryService.addAndSave(groupHistoryKey(message.getGroupId()), groupPeer, message);
        } else {
            messageHistoryService.addAndSave(sender, message);
        }
        System.out.println("[INFO] Inbound " + message.getType() + " message stored. id=" + message.getId()
                + ", from=" + sender.addressKey());
        notifyPeersChanged();
        notifyMessage(message);
    }

    /**
     * Đánh dấu peer gửi heartbeat/JOIN là online trong danh sách peer đã biết.
     */
    public void markPeerOnline(Message message) {
        PeerInfo sender = peerDirectoryService.mergeSenderFromKnownPeers(message);
        peerDirectoryService.put(sender);
        System.out.println("[DEBUG] Marked peer online from " + message.getType()
                + ": " + sender.addressKey());
        notifyPeersChanged();
    }

    /**
     * Kiểm tra địa chỉ người dùng nhập có trỏ về chính peer hiện tại hay không.
     */
    public boolean isSelfAddress(String hostAndMaybePort) {
        PeerInfo peerInfo = peerDirectoryService.parsePeer(hostAndMaybePort, hostAndMaybePort);
        return peerDirectoryService.isSelfPeer(peerInfo);
    }

    /**
     * Notify các MessageListener, bảo đảm callback chạy trên Swing EDT khi cần cập nhật UI.
     */
    private void notifyMessage(Message message) {
        Runnable notifier = () -> messageListeners.forEach(listener -> listener.onMessageReceived(message));
        if (SwingUtilities.isEventDispatchThread()) {
            notifier.run();
        } else {
            SwingUtilities.invokeLater(notifier);
        }
    }

    /**
     * Notify các listener đang quan sát thay đổi danh sách/trạng thái peer.
     */
    private void notifyPeersChanged() {
        Runnable notifier = () -> peerChangeListeners.forEach(Runnable::run);
        if (SwingUtilities.isEventDispatchThread()) {
            notifier.run();
        } else {
            SwingUtilities.invokeLater(notifier);
        }
    }

    /**
     * Chay REGISTER/JOIN mot lan, sau do dinh ky refresh LIST/GROUP de thay peer moi join.
     */
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
                System.out.println("[ERROR] Bootstrap refresh loop failed: " + e.getMessage());
            }
        }
    }

    /**
     * Tao key rieng cho history cua group.
     */
    private String groupHistoryKey(String groupId) {
        return GROUP_CHAT_PREFIX + groupId;
    }

    /**
     * Tao PeerInfo dai dien group de LocalMessageRepo luu conversation theo groupId.
     */
    private PeerInfo groupConversationPeer(Group group) {
        return new PeerInfo(group.getGroupId(), group.getName(), GROUP_CHAT_PREFIX + group.getGroupId(), 0, true);
    }

    /**
     * Luu group message offline len bootstrap theo receiverId cua tung member.
     */
    private void storeGroupOfflineIfPossible(Message message, PeerInfo member) {
        if (bootstrapClient == null) {
            System.out.println("[WARN] Cannot store offline group message because bootstrap is disabled. member="
                    + member.getId());
            return;
        }
        boolean stored = bootstrapClient.storeOffline(Mes.fromMessage(message));
        System.out.println("[INFO] Offline group fallback stored=" + stored
                + ", messageId=" + message.getId()
                + ", groupId=" + message.getGroupId()
                + ", receiverId=" + member.getId());
    }

}
