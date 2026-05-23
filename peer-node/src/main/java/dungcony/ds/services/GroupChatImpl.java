package dungcony.ds.services;

import dungcony.ds.enums.MessageStatus;
import dungcony.ds.interfaces.BootstrapGroupService;
import dungcony.ds.interfaces.GroupChatService;
import dungcony.ds.interfaces.MessageHistoryService;
import dungcony.ds.interfaces.PeerDirectoryService;
import dungcony.ds.model.BootstrapClient;
import dungcony.ds.model.Group;
import dungcony.ds.model.GroupManager;
import dungcony.ds.model.Message;
import dungcony.ds.model.MessageSender;
import dungcony.ds.model.PeerInfo;
import dungcony.ds.utils.Mes;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
// Service xử lý group chat gồm tạo nhóm, gửi tin nhóm, sync membership và đọc lịch sử nhóm
public class GroupChatImpl implements GroupChatService {
    private static final String GROUP_CHAT_PREFIX = "group:";

    private final PeerInfo localPeer;
    private final MessageSender messageSender;
    private final BootstrapClient bootstrapClient;
    private final PeerDirectoryService peerDirectoryService;
    private final MessageHistoryService messageHistoryService;
    private final BootstrapGroupService bootstrapGroupService;
    private final GroupManager groupManager;
    private final Consumer<Message> messageNotifier;
    private final Runnable peerChangeNotifier;

    // Khởi tạo service group chat với các dependency mạng, bootstrap, history và group manager
    public GroupChatImpl(PeerInfo localPeer,
                         MessageSender messageSender,
                         BootstrapClient bootstrapClient,
                         PeerDirectoryService peerDirectoryService,
                         MessageHistoryService messageHistoryService,
                         BootstrapGroupService bootstrapGroupService,
                         GroupManager groupManager,
                         Consumer<Message> messageNotifier,
                         Runnable peerChangeNotifier) {
        this.localPeer = localPeer;
        this.messageSender = messageSender;
        this.bootstrapClient = bootstrapClient;
        this.peerDirectoryService = peerDirectoryService;
        this.messageHistoryService = messageHistoryService;
        this.bootstrapGroupService = bootstrapGroupService;
        this.groupManager = groupManager;
        this.messageNotifier = messageNotifier;
        this.peerChangeNotifier = peerChangeNotifier;
    }

    // Gửi tin nhắn nhóm tới từng member và lưu trạng thái tổng vào history group
    @Override
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
            PeerInfo target = resolveGroupMemberTarget(member);
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
        messageNotifier.accept(message);
        peerChangeNotifier.run();
    }

    // Tạo group mới với local peer là thành viên đầu tiên rồi sync membership trực tiếp
    @Override
    public Group createGroup(String name, Collection<PeerInfo> members) {
        List<PeerInfo> initialMembers = new ArrayList<>();
        initialMembers.add(localPeer);
        if (members != null) {
            initialMembers.addAll(members);
        }
        Group group = groupManager.createGroup(name, initialMembers);
        broadcastGroupMembersSync(group);
        peerChangeNotifier.run();
        return group;
    }

    // Thêm member vào group local, sync bootstrap và gửi snapshot membership tới member khác
    @Override
    public Group addMembersToGroup(String groupId, Collection<PeerInfo> members) {
        Group group = groupManager.addMembers(groupId, members);
        if (group != null) {
            bootstrapGroupService.addMembersToGroup(groupId, members);
            broadcastGroupMembersSync(group);
            peerChangeNotifier.run();
        }
        return group;
    }

    // Lấy toàn bộ group local hiện có để UI hiển thị
    @Override
    public Collection<Group> getGroups() {
        return groupManager.getAllGroups();
    }

    // Lấy history message của group theo groupId
    @Override
    public List<Message> getMessagesWithGroup(String groupId) {
        Group group = groupManager.getGroup(groupId);
        PeerInfo groupPeer = group == null
                ? new PeerInfo(groupId, groupId, GROUP_CHAT_PREFIX + groupId, 0, false)
                : groupConversationPeer(group);
        return messageHistoryService.getMessages(groupPeer, groupHistoryKey(groupId));
    }

    // Lấy message cuối cùng của group theo groupId
    @Override
    public Message getLastGroupMessage(String groupId) {
        Group group = groupManager.getGroup(groupId);
        PeerInfo groupPeer = group == null
                ? new PeerInfo(groupId, groupId, GROUP_CHAT_PREFIX + groupId, 0, false)
                : groupConversationPeer(group);
        return messageHistoryService.getLastMessage(groupPeer, groupHistoryKey(groupId));
    }

    // Tìm target runtime mới nhất cho member hoặc dùng lại member trong group
    private PeerInfo resolveGroupMemberTarget(PeerInfo member) {
        PeerInfo target = peerDirectoryService.findKnownPeerById(member.getId());
        return target == null ? member : target;
    }

    // Tạo key history riêng cho group để không trùng conversation 1-1
    private String groupHistoryKey(String groupId) {
        return GROUP_CHAT_PREFIX + groupId;
    }

    // Tạo peer ảo đại diện cho group khi lưu conversation group
    private PeerInfo groupConversationPeer(Group group) {
        return new PeerInfo(group.getGroupId(), group.getName(), GROUP_CHAT_PREFIX + group.getGroupId(), 0, true);
    }

    // Lưu group message offline lên bootstrap theo receiverId của từng member
    private boolean storeGroupOfflineIfPossible(Message message, PeerInfo member) {
        if (bootstrapClient == null) {
            log.warn("Không thể lưu tin nhắn nhóm offline vì bootstrap đang tắt. member={}", member.getId());
            return false;
        }
        boolean stored = bootstrapClient.storeOffline(Mes.fromMessage(message));
        log.info("Đã lưu fallback tin nhóm offline={}, messageId={}, groupId={}, receiverId={}", stored, message.getId(), message.getGroupId(), member.getId());
        return stored;
    }

    // Gửi snapshot thành viên group trực tiếp tới các member reachable
    private void broadcastGroupMembersSync(Group group) {
        if (group == null) {
            return;
        }
        log.info("Đang sync membership nhóm trực tiếp. groupId={}, sốThànhViên={}", group.getGroupId(), group.getMembers().size());
        for (PeerInfo member : group.getMembers()) {
            PeerInfo target = resolveGroupMemberTarget(member);
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
}
