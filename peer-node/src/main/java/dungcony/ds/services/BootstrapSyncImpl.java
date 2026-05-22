package dungcony.ds.services;

import lombok.extern.slf4j.Slf4j;


import dungcony.ds.dtos.JoinResponse;
import dungcony.ds.dtos.OfflineMessage;
import dungcony.ds.enums.MessageType;
import dungcony.ds.interfaces.BootstrapGroupService;
import dungcony.ds.interfaces.BootstrapSyncService;
import dungcony.ds.interfaces.MessageHistoryService;
import dungcony.ds.interfaces.PeerDirectoryService;
import dungcony.ds.model.*;

import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class BootstrapSyncImpl implements BootstrapSyncService {
private static final String GROUP_CHAT_PREFIX = "group:";

    private final BootstrapClient bootstrapClient;
    private final PeerInfo localPeer;
    private final PeerDirectoryService peerDirectoryService;
    private final MessageHistoryService messageHistoryService;
    private final GroupManager groupManager;
    private final BootstrapGroupService bootstrapGroupService;
    private final Runnable peerChangeNotifier;
    private final Consumer<Message> messageNotifier;

    // Khởi tạo service xử lý REGISTER/JOIN/offline/group sync với bootstrap.
    public BootstrapSyncImpl(BootstrapClient bootstrapClient, PeerInfo localPeer,
                             PeerDirectoryService peerDirectoryService,
                             MessageHistoryService messageHistoryService,
                             GroupManager groupManager,
                             BootstrapGroupService bootstrapGroupService,
                             Runnable peerChangeNotifier,
                             Consumer<Message> messageNotifier) {
        this.bootstrapClient = bootstrapClient;
        this.localPeer = localPeer;
        this.peerDirectoryService = peerDirectoryService;
        this.messageHistoryService = messageHistoryService;
        this.groupManager = groupManager;
        this.bootstrapGroupService = bootstrapGroupService;
        this.peerChangeNotifier = peerChangeNotifier;
        this.messageNotifier = messageNotifier;
    }

    // Đăng ký user với bootstrap, join vào mạng, nạp peer/group và offline message.
    @Override
    public void registerAndJoinBootstrap() {
        log.info("Đang đăng ký peer local với bootstrap. peerId={}, tên={}", localPeer.getId(), localPeer.getName());
        boolean registered = bootstrapClient.register(localPeer);
        if (!registered) {
            log.warn("Bootstrap REGISTER thất bại. Peer vẫn chạy ở chế độ TCP trực tiếp.");
            return;
        }

        JoinResponse joinResponse = bootstrapClient.join(localPeer);
        int added = peerDirectoryService.syncOnlinePeers(joinResponse.getOnlinePeers());
        syncGroupsFromBootstrap();
        handleOfflineMessages(joinResponse);
        peerChangeNotifier.run();
        log.info("Đồng bộ bootstrap xong. peerThêm={}, knownPeers={}", added, peerDirectoryService.size());
    }

    // Làm mới danh sách peer online và group từ bootstrap-server.
    @Override
    public void refreshFromBootstrap() {
        JoinResponse joinResponse = bootstrapClient.joinOrNull(localPeer);
        if (joinResponse == null) {
            log.warn("Bỏ qua refresh bootstrap vì tracker không khả dụng. "
                    + "Giữ nguyên trạng thái peer local hiện tại.");
            return;
        }
        int onlineCount = peerDirectoryService.syncOnlinePeers(joinResponse.getOnlinePeers());
        syncGroupsFromBootstrap();
        handleOfflineMessages(joinResponse);
        peerChangeNotifier.run();
        log.info("Refresh bootstrap xong. peerTrựcTuyến={}, knownPeers={}", onlineCount, peerDirectoryService.size());
    }

    // Load group membership từ bootstrap và cache lại vào groups.json của profile hiện tại.
    private void syncGroupsFromBootstrap() {
        List<Group> joinedGroups = bootstrapGroupService.fetchJoinedGroups(peerDirectoryService.list());
        groupManager.replaceAll(joinedGroups);
        log.info("Đồng bộ nhóm bootstrap xong. nhómĐãThamGia={}", joinedGroups.size());
    }

    // Đưa các tin offline bootstrap trả về vào history nếu tìm được peer gửi trong danh sách đã biết.
    private void handleOfflineMessages(JoinResponse joinResponse) {
        for (OfflineMessage offlineMessage : joinResponse.getOfflineMessages()) {
            PeerInfo sender = peerDirectoryService.findKnownPeerById(offlineMessage.senderId());
            boolean isGroupMessage = offlineMessage.groupId() != null && !offlineMessage.groupId().isBlank();
            PeerInfo conversationPeer = isGroupMessage
                    ? groupConversationPeer(offlineMessage.groupId())
                    : sender == null
                      ? new PeerInfo(offlineMessage.senderId(), offlineMessage.senderId(), "", 0, false)
                      : sender;
            String historyKey = isGroupMessage
                    ? GROUP_CHAT_PREFIX + offlineMessage.groupId()
                    : sender == null ? offlineMessage.senderId() : sender.addressKey();
            Message message = Message.restore(
                    offlineMessage.messageId(),
                    isGroupMessage ? MessageType.GROUP_CHAT : MessageType.CHAT,
                    offlineMessage.senderId(),
                    sender == null ? "" : sender.getHost(),
                    sender == null ? 0 : sender.getPort(),
                    localPeer.getId(),
                    localPeer.getHost(),
                    localPeer.getPort(),
                    offlineMessage.groupId(),
                    offlineMessage.content(),
                    offlineMessage.createdAt(),
                    false
            );
            messageHistoryService.addAndSave(historyKey, conversationPeer, message);
            messageNotifier.accept(message);
            log.info("Đã nạp tin offline. messageId={}, senderId={}, historyKey={}", offlineMessage.messageId(), offlineMessage.senderId(), historyKey);
        }
    }

    // Tạo conversation peer đại diện group để message offline group được lưu đúng history.
    private PeerInfo groupConversationPeer(String groupId) {
        Group group = groupManager.getGroup(groupId);
        String name = group == null ? groupId : group.getName();
        return new PeerInfo(groupId, name, GROUP_CHAT_PREFIX + groupId, 0, false);
    }
}
