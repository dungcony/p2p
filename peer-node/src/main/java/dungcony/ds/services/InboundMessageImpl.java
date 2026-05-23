package dungcony.ds.services;

import dungcony.ds.enums.MessageStatus;
import dungcony.ds.interfaces.InboundMessageService;
import dungcony.ds.interfaces.MessageHistoryService;
import dungcony.ds.interfaces.PeerDirectoryService;
import dungcony.ds.model.Group;
import dungcony.ds.model.GroupManager;
import dungcony.ds.model.Message;
import dungcony.ds.model.PeerInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
// Service xử lý message đi vào từ TCP receiver và cập nhật danh bạ, group, history
public class InboundMessageImpl implements InboundMessageService {
    private static final String GROUP_CHAT_PREFIX = "group:";

    private final PeerInfo localPeer;
    private final PeerDirectoryService peerDirectoryService;
    private final MessageHistoryService messageHistoryService;
    private final GroupManager groupManager;
    private final Consumer<Message> messageNotifier;
    private final Runnable peerChangeNotifier;

    // Khởi tạo service inbound với peer local, danh bạ, history và group manager
    public InboundMessageImpl(PeerInfo localPeer,
                              PeerDirectoryService peerDirectoryService,
                              MessageHistoryService messageHistoryService,
                              GroupManager groupManager,
                              Consumer<Message> messageNotifier,
                              Runnable peerChangeNotifier) {
        this.localPeer = localPeer;
        this.peerDirectoryService = peerDirectoryService;
        this.messageHistoryService = messageHistoryService;
        this.groupManager = groupManager;
        this.messageNotifier = messageNotifier;
        this.peerChangeNotifier = peerChangeNotifier;
    }

    // Lưu message chat, group chat hoặc broadcast nhận vào rồi notify UI
    @Override
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
        peerChangeNotifier.run();
        messageNotifier.accept(message);
    }

    // Đồng bộ group local bằng snapshot membership do peer khác gửi tới
    @Override
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
        peerChangeNotifier.run();
    }

    // Đánh dấu sender của HEARTBEAT hoặc JOIN là online trong danh bạ runtime
    @Override
    public void markPeerOnline(Message message) {
        PeerInfo sender = peerDirectoryService.mergeSenderFromKnownPeers(message);
        peerDirectoryService.put(sender);
        log.debug("Đã đánh dấu peer trực tuyến từ {}: {}", message.getType(), sender.addressKey());
        peerChangeNotifier.run();
    }

    // Tạo key history riêng cho group để không trùng conversation 1-1
    private String groupHistoryKey(String groupId) {
        return GROUP_CHAT_PREFIX + groupId;
    }

    // Tạo peer ảo đại diện group khi lưu tin group vào history
    private PeerInfo groupConversationPeer(Group group) {
        return new PeerInfo(group.getGroupId(), group.getName(), GROUP_CHAT_PREFIX + group.getGroupId(), 0, true);
    }
}
