package dungcony.ds.model;

import dungcony.ds.enums.MessageStatus;
import dungcony.ds.enums.MessageType;
import dungcony.ds.interfaces.*;
import dungcony.ds.network.TCPClient;
import dungcony.ds.network.TCPServer;
import dungcony.ds.repositories.LocalGroupRepo;
import dungcony.ds.repositories.LocalMessageRepo;
import dungcony.ds.services.*;
import dungcony.ds.utils.Mes;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Getter
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
    private final BootstrapSyncService bootstrapSyncService;
    private final BootstrapGroupService bootstrapGroupService;
    private final GroupManager groupManager;
    private final MessageSender messageSender;
    private final TCPServer tcpServer;
    private final BootstrapClient bootstrapClient;
    private volatile boolean running;

    public record BroadcastResult(int totalTargets, int delivered, int failed) {
    }

//    // Khởi tạo một peer cục bộ với tên định danh và port lắng nghe do người dùng nhập
//    public PeerNode(String peerId, int port) {
//        this(peerId, peerId, port, null, 0, Path.of("peer-node", "src", "main", "resources", "data"));
//    }
//
//    // Khởi tạo peer với id ổn định, tên hiển thị, port lắng nghe và bootstrap-server
//    public PeerNode(String peerId, String peerName, int port, String bootstrapHost, int bootstrapPort) {
//        this(peerId, peerName, port, bootstrapHost, bootstrapPort,
//                Path.of("peer-node", "src", "main", "resources", "data"));
//    }

    // Khởi tạo peer với dataDir riêng để test nhiều instance trên cùng một máy
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
                this::notifyPeersChanged);
        this.peerPresenceService = new PeerPresenceImpl(
                localPeer,
                messageSender,
                bootstrapClient,
                peerDirectoryService,
                this::notifyPeersChanged);
        BootstrapGroupService bootstrapGroupService = new BootstrapGroupImpl(bootstrapClient, localPeer);
        this.bootstrapGroupService = bootstrapGroupService;
        this.groupManager = new GroupManager(new LocalGroupRepo(dataDir), bootstrapGroupService::publishGroup);
        this.bootstrapSyncService = bootstrapClient == null ? null
                : new BootstrapSyncImpl(
                bootstrapClient,
                localPeer,
                peerDirectoryService,
                messageHistoryService,
                groupManager,
                bootstrapGroupService,
                this::notifyPeersChanged,
                this::notifyMessage);
        String bootstrapAddress = bootstrapClient == null ? "đã tắt" : "%s:%d".formatted(bootstrapHost, bootstrapPort);
        log.info("Đã khởi tạo PeerNode: id={}, tên={}, địaChỉ={}, bootstrap={}, thưMụcDữLiệu={}", localPeer.getId(), localPeer.getName(), localPeer.addressKey(), bootstrapAddress, dataDir.toAbsolutePath());
    }

    // Khởi động vai trò nhận tin của peer bằng TCPServer trên một thread riêng.
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

    // Dừng TCPServer để peer ngừng nhận kết nối mới.
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

    // Thêm một peer đã biết vào bộ nhớ runtime, thường được gọi từ màn Add Friend
    // hoặc scanner.
    public PeerInfo addKnownPeer(String name, String hostAndMaybePort) {
        PeerInfo peerInfo = peerDirectoryService.addKnownPeer(name, hostAndMaybePort);
        notifyPeersChanged();
        return peerInfo;
    }

    // Lấy danh sách peer mà node hiện đang biết để UI hiển thị trong ChatList.
    public Collection<PeerInfo> getKnownPeers() {
        return peerDirectoryService.list();
    }

    // Lấy danh sách chat: peer online từ bootstrap và peer offline đã từng có
    // message.
    public Collection<PeerInfo> getChatListPeers() {
        Map<String, PeerInfo> conversations = new LinkedHashMap<>();
        for (PeerInfo peerInfo : peerDirectoryService.list()) {
            if (peerDirectoryService.isSelfPeer(peerInfo)) {
                continue;
            }
            if (peerInfo.isOnline()) {
                conversations.put(peerInfo.getId(), peerInfo);
            }
        }
        for (PeerInfo historyPeer : messageHistoryService.getDirectConversationPeers()) {
            if (peerDirectoryService.isSelfPeer(historyPeer)) {
                log.debug("Bỏ qua conversation trỏ về peer local. peerId={}", historyPeer.getId());
                continue;
            }
            PeerInfo runtimePeer = peerDirectoryService.findKnownPeerById(historyPeer.getId());
            if (runtimePeer == null) {
                peerDirectoryService.put(historyPeer);
                conversations.put(historyPeer.getId(), historyPeer);
            } else if (!peerDirectoryService.isSelfPeer(runtimePeer)) {
                conversations.put(historyPeer.getId(), runtimePeer);
            } else {
                log.debug("Bỏ qua runtime peer local trong danh sách chat. peerId={}", runtimePeer.getId());
            }
        }
        log.debug("Đã tạo danh sách chat. sốLượng={}", conversations.size());
        return conversations.values();
    }

    // Kiểm tra peer có online không, ưu tiên trạng thái từ bootstrap-server.
    public boolean checkUserIsOnline(String hostAndMaybePort) {
        return peerPresenceService.checkUserIsOnline(hostAndMaybePort);
    }

    // Kiểm tra bootstrap-server có đang reachable không để quyết định luồng tạo
    // group.
    public boolean isBootstrapAvailable() {
        if (bootstrapClient == null) {
            return false;
        }
        boolean available = bootstrapClient.listOrNull() != null;
        log.info("Đã kiểm tra bootstrap. khảDụng={}", available);
        return available;
    }

    // Gửi tin nhắn 1-1 trực tiếp tới peer đích, lưu lịch sử nếu nhận được ACK.
    public boolean sendMessage(String content, String hostAndMaybePort) {
        return chatService.sendMessage(content, hostAndMaybePort);
    }

    // Gửi một message đến toàn bộ peer online mà node biết, ưu tiên danh sách từ
    // bootstrap.
    public BroadcastResult broadcastToNetwork(String content) {
        if (content == null || content.isBlank()) {
            log.warn("Từ chối broadcast tin nhắn rỗng.");
            return new BroadcastResult(0, 0, 0);
        }

        List<PeerInfo> targets = collectOnlineBroadcastTargets();
        int delivered = 0;
        int failed = 0;
        log.info("Đang broadcast toàn mạng. sốPeerĐích={}", targets.size());
        for (PeerInfo target : targets) {
            Message message = Message.broadcast(localPeer, target, content);
            boolean sent = messageSender.send(target, message);
            target.setOnline(sent);
            if (sent) {
                delivered++;
            } else {
                failed++;
            }
            log.info("Kết quả broadcast toàn mạng. receiver={}, sent={}", target.addressKey(), sent);
        }
        notifyPeersChanged();
        BroadcastResult result = new BroadcastResult(targets.size(), delivered, failed);
        log.info("Broadcast toàn mạng hoàn tất. total={}, delivered={}, failed={}", result.totalTargets(), result.delivered(), result.failed());
        return result;
    }

    // Gửi một tin nhắn tới tất cả thành viên của nhóm đã tạo.
    public void sendGroupMessage(String groupId, String content) {
        Group group = groupManager.getGroup(groupId);
        if (group == null) {
            log.warn("Không thể gửi tin nhắn nhóm. Không tìm thấy nhóm: {}", groupId);
            return;
        }
        Message message = Mes.groupChat(localPeer, groupId, group.getName(), content);
        boolean anyDelivered = false;
        boolean anyPending = false;
        log.info("Đang broadcast tin nhắn nhóm. messageId={} tới nhóm={}, sốThànhViên={}", message.getId(), groupId, group.getMembers().size());
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
                anyPending = storeGroupOfflineIfPossible(memberMessage, target) || anyPending;
            } else {
                anyDelivered = true;
            }
        }
        if (anyDelivered) {
            message.setStatus(MessageStatus.SENT);
        } else if (anyPending) {
            message.setStatus(MessageStatus.PENDING);
        } else {
            message.setStatus(MessageStatus.FAILED);
        }
        messageHistoryService.addAndSave(groupHistoryKey(groupId), groupConversationPeer(group), message);
        notifyMessage(message);
        notifyPeersChanged();
    }

    // Tạo group chat mới từ danh sách peer được chọn trong UI.
    public Group createGroup(String name, Collection<PeerInfo> members) {
        List<PeerInfo> initialMembers = new ArrayList<>();
        initialMembers.add(localPeer);
        if (members != null) {
            initialMembers.addAll(members);
        }
        Group group = groupManager.createGroup(name, initialMembers);
        broadcastGroupMembersSync(group);
        notifyPeersChanged();
        return group;
    }

    // Thêm peer vào group hiện có, lưu local và đồng bộ lên bootstrap nếu có.
    public Group addMembersToGroup(String groupId, Collection<PeerInfo> members) {
        Group group = groupManager.addMembers(groupId, members);
        if (group != null) {
            bootstrapGroupService.addMembersToGroup(groupId, members);
            broadcastGroupMembersSync(group);
            notifyPeersChanged();
        }
        return group;
    }

    // Lấy các group hiện tại của peer để UI hiển thị.
    public Collection<Group> getGroups() {
        return groupManager.getAllGroups();
    }

    // Lấy lịch sử tin nhắn với một peer cụ thể theo địa chỉ host hoặc host:port.
    public List<Message> getMessagesWithPeer(String hostAndMaybePort) {
        PeerInfo peerInfo = peerDirectoryService.resolvePeer(hostAndMaybePort);
        return messageHistoryService.getMessages(peerInfo, hostAndMaybePort);
    }

    // Lấy tin nhắn cuối cùng với một peer để hiển thị preview trong danh sách chat.
    public Message getLastMessage(String hostAndMaybePort) {
        PeerInfo peerInfo = peerDirectoryService.resolvePeer(hostAndMaybePort);
        return messageHistoryService.getLastMessage(peerInfo, hostAndMaybePort);
    }

    // Lấy lịch sử message của group.
    public List<Message> getMessagesWithGroup(String groupId) {
        Group group = groupManager.getGroup(groupId);
        PeerInfo groupPeer = group == null
                ? new PeerInfo(groupId, groupId, GROUP_CHAT_PREFIX + groupId, 0, false)
                : groupConversationPeer(group);
        return messageHistoryService.getMessages(groupPeer, groupHistoryKey(groupId));
    }

    // Lấy message cuối cùng của group để hiển thị preview.
    public Message getLastGroupMessage(String groupId) {
        Group group = groupManager.getGroup(groupId);
        PeerInfo groupPeer = group == null
                ? new PeerInfo(groupId, groupId, GROUP_CHAT_PREFIX + groupId, 0, false)
                : groupConversationPeer(group);
        return messageHistoryService.getLastMessage(groupPeer, groupHistoryKey(groupId));
    }

    // Hỏi một peer đã biết danh sách peer mà nó đang biết để fallback khi bootstrap không sẵn sàng
    public List<PeerInfo> discoverPeersFromKnownPeer(PeerInfo knownPeer) {
        if (knownPeer == null || peerDirectoryService.isSelfPeer(knownPeer)) {
            return List.of();
        }
        log.info("Đang hỏi peer đã biết danh sách peer khác. peer={}", knownPeer.addressKey());
        Message request = Message.peerListRequest(localPeer, knownPeer);
        Message response = messageSender.sendForResponse(knownPeer, request, MessageType.PEER_LIST_RESPONSE);
        if (response == null) {
            knownPeer.setOnline(false);
            log.warn("Không nhận được PEER_LIST_RESPONSE từ peer={}", knownPeer.addressKey());
            notifyPeersChanged();
            return List.of();
        }
        knownPeer.setOnline(true);
        int merged = onPeerListResponse(response);
        List<PeerInfo> discoveredPeers = extractDiscoverablePeers(response);
        log.info("Đã nhận danh sách peer từ peer={}. sốPeer={}, sốPeerMerge={}",
                knownPeer.addressKey(), discoveredPeers.size(), merged);
        notifyPeersChanged();
        return discoveredPeers;
    }

    // Tạo PEER_LIST_RESPONSE gồm local peer và danh bạ runtime hiện tại.
    public Message buildPeerListResponse(Message request) {
        List<PeerInfo> knownPeers = new ArrayList<>();
        knownPeers.add(localPeer);
        knownPeers.addAll(peerDirectoryService.list());
        log.info("Trả danh sách peer cho request id={}, sốPeer={}", request.getId(), knownPeers.size());
        return Message.peerListResponse(localPeer, request, knownPeers);
    }

    // Merge danh sách peer nhận từ PEER_LIST_RESPONSE vào danh bạ local.
    public int onPeerListResponse(Message response) {
        if (response == null || response.getType() != MessageType.PEER_LIST_RESPONSE) {
            return 0;
        }
        PeerInfo sender = peerDirectoryService.mergeSenderFromKnownPeers(response);
        sender.setOnline(true);
        peerDirectoryService.put(sender);
        int merged = peerDirectoryService.mergeKnownPeers(response.getPeers());
        log.info("Đã xử lý PEER_LIST_RESPONSE. sender={}, sốPeerMerge={}", sender.addressKey(), merged);
        notifyPeersChanged();
        return merged;
    }

    private List<PeerInfo> extractDiscoverablePeers(Message response) {
        if (response == null || response.getPeers() == null) {
            return List.of();
        }
        List<PeerInfo> discoveredPeers = new ArrayList<>();
        for (PeerInfo peerInfo : response.getPeers()) {
            if (peerInfo == null || peerDirectoryService.isSelfPeer(peerInfo) || isGroupPseudoPeer(peerInfo)) {
                continue;
            }
            discoveredPeers.add(peerInfo);
        }
        return discoveredPeers;
    }

    private boolean isGroupPseudoPeer(PeerInfo peerInfo) {
        return peerInfo.getHost() != null && peerInfo.getHost().startsWith(GROUP_CHAT_PREFIX);
    }

    // Xử lý tin nhắn đến từ network: cập nhật peer, lưu lịch sử và notify UI.
    public void onInboundMessage(Message message) {
        message.setStatus(MessageStatus.SENT);
        PeerInfo sender = peerDirectoryService.mergeSenderFromKnownPeers(message);
        peerDirectoryService.put(sender);
        if (message.getGroupId() != null && !message.getGroupId().isBlank()) {
            Group group = groupManager.getGroup(message.getGroupId());
            if (group == null) {
                group = groupManager.ensureLocalGroup(
                        message.getGroupId(),
                        message.getGroupName(),
                        List.of(localPeer, sender));
            } else {
                groupManager.ensureLocalGroup(message.getGroupId(), group.getName(), List.of(localPeer, sender));
            }
            PeerInfo groupPeer = groupConversationPeer(group);
            messageHistoryService.addAndSave(groupHistoryKey(message.getGroupId()), groupPeer, message);
        } else {
            messageHistoryService.addAndSave(sender, message);
        }
        log.info("Đã nhận {} và lưu message. id={}, từ={}", message.getType(), message.getId(), sender.addressKey());
        notifyPeersChanged();
        notifyMessage(message);
    }

    // Cập nhật group local khi nhận snapshot membership từ peer khác.
    public void onGroupMembersSync(Message message) {
        if (message == null || message.getGroupId() == null || message.getGroupId().isBlank()) {
            log.warn("Bỏ qua GROUP_MEMBERS_SYNC vì thiếu groupId.");
            return;
        }
        PeerInfo sender = peerDirectoryService.mergeSenderFromKnownPeers(message);
        sender.setOnline(true);
        peerDirectoryService.put(sender);
        List<PeerInfo> members = new ArrayList<>(message.getGroupMembers());
        boolean hasSender = members.stream().anyMatch(member -> sender.getId().equals(member.getId()));
        if (!hasSender) {
            members.add(sender);
        }
        boolean hasLocal = members.stream().anyMatch(peerDirectoryService::isSelfPeer);
        if (!hasLocal) {
            members.add(localPeer);
        }
        peerDirectoryService.mergeKnownPeers(members);
        Group group = groupManager.syncMembers(message.getGroupId(), message.getGroupName(), members);
        log.info("Đã đồng bộ group membership từ peer. groupId={}, sốThànhViên={}", group.getGroupId(), group.getMembers().size());
        notifyPeersChanged();
    }

    // Đánh dấu peer gửi heartbeat/JOIN là online trong danh sách peer đã biết.
    public void markPeerOnline(Message message) {
        PeerInfo sender = peerDirectoryService.mergeSenderFromKnownPeers(message);
        peerDirectoryService.put(sender);
        log.debug("Đã đánh dấu peer trực tuyến từ {}: {}", message.getType(), sender.addressKey());
        notifyPeersChanged();
    }

    // Retry thủ công một tin nhắn 1-1 FAILED/PENDING, cập nhật lại status trong SON local.
    public boolean retryMessage(Message message) {
        if (message == null) {
            return false;
        }
        if (message.getGroupId() != null && !message.getGroupId().isBlank()) {
            log.warn("Chưa hỗ trợ retry thủ công cho tin nhắn nhóm. messageId={}", message.getId());
            return false;
        }
        if (message.getStatus() != MessageStatus.FAILED && message.getStatus() != MessageStatus.PENDING) {
            log.warn("Bỏ qua retry vì trạng thái hiện tại không cần retry. messageId={}, status={}", message.getId(), message.getStatus());
            return false;
        }
        PeerInfo receiver = resolveMessageReceiver(message);
        if (receiver == null || peerDirectoryService.isSelfPeer(receiver)) {
            log.warn("Không thể retry vì không xác định được receiver. messageId={}", message.getId());
            return false;
        }
        message.setStatus(MessageStatus.SENDING);
        messageHistoryService.updateAndSave(receiver, message);
        notifyMessage(message);

        log.info("Đang retry tin nhắn. messageId={}, receiver={}", message.getId(), receiver.addressKey());
        boolean sent = messageSender.send(receiver, message);
        receiver.setOnline(sent);
        if (sent) {
            message.setStatus(MessageStatus.SENT);
        } else if (storeDirectOfflineIfPossible(message)) {
            message.setStatus(MessageStatus.PENDING);
        } else {
            message.setStatus(MessageStatus.FAILED);
        }
        messageHistoryService.updateAndSave(receiver, message);
        notifyMessage(message);
        notifyPeersChanged();
        log.info("Retry tin nhắn kết thúc. messageId={}, status={}", message.getId(), message.getStatus());
        return sent;
    }

    // Kiểm tra địa chỉ người dùng nhập có trỏ về chính peer hiện tại hay không.
    public boolean isSelfAddress(String hostAndMaybePort) {
        PeerInfo peerInfo = peerDirectoryService.parsePeer(hostAndMaybePort, hostAndMaybePort);
        return peerDirectoryService.isSelfPeer(peerInfo);
    }

    // Notify các MessageListener, bảo đảm callback chạy trên Swing EDT khi cần cập
    // nhật UI.
    private void notifyMessage(Message message) {
        Runnable notifier = () -> messageListeners.forEach(listener -> listener.onMessageReceived(message));
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

    // Chạy REGISTER/JOIN một lần, sau đó định kỳ JOIN lại như heartbeat và đồng bộ
    // peer/group.
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

    // Lấy danh sách peer online để broadcast, ưu tiên tracker nhưng vẫn hợp nhất
    // peer đã discover trực tiếp.
    private List<PeerInfo> collectOnlineBroadcastTargets() {
        Map<String, PeerInfo> targets = new LinkedHashMap<>();
        Collection<PeerInfo> bootstrapPeers = bootstrapClient == null ? null : bootstrapClient.listOrNull();
        if (bootstrapPeers != null) {
            peerDirectoryService.mergeKnownPeers(bootstrapPeers);
            appendBroadcastTargets(targets, bootstrapPeers);
        }
        appendBroadcastTargets(targets, peerDirectoryService.list());
        return new ArrayList<>(targets.values());
    }

    private void appendBroadcastTargets(Map<String, PeerInfo> targets, Collection<PeerInfo> candidates) {
        if (candidates == null) {
            return;
        }
        for (PeerInfo candidate : candidates) {
            if (candidate == null || peerDirectoryService.isSelfPeer(candidate)
                    || !candidate.isOnline()
                    || candidate.getHost() == null || candidate.getHost().isBlank()
                    || candidate.getPort() <= 0) {
                continue;
            }
            String key = candidate.getId() == null || candidate.getId().isBlank()
                    ? candidate.addressKey()
                    : candidate.getId();
            targets.put(key, candidate);
        }
    }

    // Tạo key riêng cho history của group.
    private String groupHistoryKey(String groupId) {
        return GROUP_CHAT_PREFIX + groupId;
    }

    // Tạo PeerInfo đại diện group để LocalMessageRepo lưu conversation theo
    // groupId.
    private PeerInfo groupConversationPeer(Group group) {
        return new PeerInfo(group.getGroupId(), group.getName(), GROUP_CHAT_PREFIX + group.getGroupId(), 0, true);
    }

    // Lưu group message offline lên bootstrap theo receiverId của từng member.
    private boolean storeGroupOfflineIfPossible(Message message, PeerInfo member) {
        if (bootstrapClient == null) {
            log.warn("Không thể lưu tin nhắn nhóm offline vì bootstrap đang tắt. member={}", member.getId());
            return false;
        }
        boolean stored = bootstrapClient.storeOffline(Mes.fromMessage(message));
        log.info("Đã lưu fallback tin nhóm offline={}, messageId={}, groupId={}, receiverId={}", stored, message.getId(), message.getGroupId(), member.getId());
        return stored;
    }

    // Gửi snapshot thành viên group trực tiếp tới các member reachable.
    private void broadcastGroupMembersSync(Group group) {
        if (group == null) {
            return;
        }
        log.info("Đang sync membership nhóm trực tiếp. groupId={}, sốThànhViên={}", group.getGroupId(), group.getMembers().size());
        for (PeerInfo member : group.getMembers()) {
            PeerInfo target = peerDirectoryService.findKnownPeerById(member.getId());
            if (target == null) {
                target = member;
            }
            if (peerDirectoryService.isSelfPeer(target) || target.getHost() == null
                    || target.getHost().isBlank() || target.getPort() <= 0) {
                continue;
            }
            Message syncMessage = Message.groupMembersSync(localPeer, target, group);
            boolean sent = messageSender.send(target, syncMessage);
            target.setOnline(sent);
            member.setOnline(sent);
            log.info("Kết quả sync membership trực tiếp. groupId={}, member={}, sent={}", group.getGroupId(), member.getId(), sent);
        }
    }

    // Lưu fallback offline cho retry tin 1-1 nếu bootstrap đang sẵn sàng.
    private boolean storeDirectOfflineIfPossible(Message message) {
        if (bootstrapClient == null) {
            log.warn("Không thể lưu fallback retry vì bootstrap đang tắt. messageId={}", message.getId());
            return false;
        }
        boolean stored = bootstrapClient.storeOffline(Mes.fromMessage(message));
        log.info("Đã lưu fallback retry offline={}, messageId={}", stored, message.getId());
        return stored;
    }

    private PeerInfo resolveMessageReceiver(Message message) {
        PeerInfo receiver = peerDirectoryService.findKnownPeerById(message.getReceiverId());
        if (receiver != null) {
            return receiver;
        }
        if (message.getReceiverHost() == null || message.getReceiverHost().isBlank()
                || message.getReceiverPort() <= 0) {
            return null;
        }
        return new PeerInfo(
                message.getReceiverId(),
                message.getReceiverId(),
                message.getReceiverHost(),
                message.getReceiverPort(),
                false);
    }

}
