package dungcony.ds.services.impl.group;

import dungcony.ds.enums.MessageStatus;
import dungcony.ds.model.Group;
import dungcony.ds.model.Message;
import dungcony.ds.model.PeerInfo;
import dungcony.ds.services.interfaces.bootstrap.BootstrapGateway;
import dungcony.ds.services.interfaces.bootstrap.BootstrapGroupService;
import dungcony.ds.services.interfaces.group.GroupChatService;
import dungcony.ds.services.interfaces.messaging.MessageHistoryService;
import dungcony.ds.services.interfaces.messaging.PeerMessageSender;
import dungcony.ds.services.interfaces.peer.PeerDirectoryService;
import dungcony.ds.utils.GroupConversationHelper;
import dungcony.ds.utils.Mes;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
// Service xử lý group chat gồm tạo nhóm, gửi tin nhóm, sync membership và đọc lịch sử nhóm
public class GroupChatImpl implements GroupChatService {

    private final PeerInfo localPeer;
    private final PeerMessageSender messageSender;
    private final BootstrapGateway bootstrapGateway;
    private final PeerDirectoryService peerDirectoryService;
    private final MessageHistoryService messageHistoryService;
    private final BootstrapGroupService bootstrapGroupService;
    private final GroupManager groupManager;
    private final Consumer<Message> messageNotifier;
    private final Runnable peerChangeNotifier;

    // Khởi tạo service group chat với các dependency mạng, bootstrap, history và group manager
    public GroupChatImpl(PeerInfo localPeer,
                         PeerMessageSender messageSender,
                         BootstrapGateway bootstrapGateway,
                         PeerDirectoryService peerDirectoryService,
                         MessageHistoryService messageHistoryService,
                         BootstrapGroupService bootstrapGroupService,
                         GroupManager groupManager,
                         Consumer<Message> messageNotifier,
                         Runnable peerChangeNotifier) {
        this.localPeer = localPeer;
        this.messageSender = messageSender;
        this.bootstrapGateway = bootstrapGateway;
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
        log.info("Đang broadcast tin nhắn nhóm. messageId={} tới nhóm={}, sốThànhViên={}",
                message.getId(), groupId, group.getMembers().size());
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
        messageHistoryService.addAndSave(
                GroupConversationHelper.historyKey(groupId),
                GroupConversationHelper.conversationPeer(group),
                message);
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
                ? new PeerInfo(groupId, groupId, GroupConversationHelper.historyKey(groupId), 0, false)
                : GroupConversationHelper.conversationPeer(group);
        return messageHistoryService.getMessages(groupPeer, GroupConversationHelper.historyKey(groupId));
    }

    // Lấy message cuối cùng của group theo groupId
    @Override
    public Message getLastGroupMessage(String groupId) {
        Group group = groupManager.getGroup(groupId);
        PeerInfo groupPeer = group == null
                ? new PeerInfo(groupId, groupId, GroupConversationHelper.historyKey(groupId), 0, false)
                : GroupConversationHelper.conversationPeer(group);
        return messageHistoryService.getLastMessage(groupPeer, GroupConversationHelper.historyKey(groupId));
    }

    // Tìm target runtime mới nhất cho member hoặc dùng lại member trong group
    private PeerInfo resolveGroupMemberTarget(PeerInfo member) {
        PeerInfo target = peerDirectoryService.findKnownPeerById(member.getId());
        return target == null ? member : target;
    }

    // Lưu group message offline lên bootstrap theo receiverId của từng member
    private boolean storeGroupOfflineIfPossible(Message message, PeerInfo member) {
        if (bootstrapGateway == null) {
            log.warn("Không thể lưu tin nhắn nhóm offline vì bootstrap đang tắt. member={}", member.getId());
            return false;
        }
        boolean stored = bootstrapGateway.storeOffline(Mes.fromMessage(message));
        log.info("Đã lưu fallback tin nhóm offline={}, messageId={}, groupId={}, receiverId={}",
                stored, message.getId(), message.getGroupId(), member.getId());
        return stored;
    }

    // Gửi snapshot thành viên group trực tiếp tới các member reachable
    private void broadcastGroupMembersSync(Group group) {
        if (group == null) {
            return;
        }
        log.info("Đang sync membership nhóm trực tiếp. groupId={}, sốThànhViên={}",
                group.getGroupId(), group.getMembers().size());
        for (PeerInfo member : group.getMembers()) {
            PeerInfo target = resolveGroupMemberTarget(member);
            if (peerDirectoryService.isSelfPeer(target)
                    || target.getHost() == null || target.getHost().isBlank()
                    || target.getPort() <= 0) {
                continue;
            }
            Message syncMessage = Message.groupMembersSync(localPeer, target, group);
            boolean sent = messageSender.send(target, syncMessage);
            target.setOnline(sent);
            member.setOnline(sent);
            log.info("Kết quả sync membership trực tiếp. groupId={}, member={}, sent={}",
                    group.getGroupId(), member.getId(), sent);
        }
    }
}
