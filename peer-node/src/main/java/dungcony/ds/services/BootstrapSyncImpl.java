package dungcony.ds.services;

import dungcony.ds.dtos.JoinResponse;
import dungcony.ds.dtos.OfflineMessage;
import dungcony.ds.enums.MessageType;
import dungcony.ds.interfaces.BootstrapGroupService;
import dungcony.ds.interfaces.BootstrapSyncService;
import dungcony.ds.interfaces.MessageHistoryService;
import dungcony.ds.interfaces.PeerDirectoryService;
import dungcony.ds.model.BootstrapClient;
import dungcony.ds.model.Group;
import dungcony.ds.model.Message;
import dungcony.ds.model.PeerInfo;
import dungcony.ds.peer.GroupManager;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

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

    /**
     * Khoi tao service xu ly REGISTER/JOIN/offline/group sync voi bootstrap.
     */
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

    /**
     * Dang ky user voi bootstrap, join vao mang, nap peer/group va offline message.
     */
    @Override
    public void registerAndJoinBootstrap() {
        System.out.println("[INFO] Registering local peer with bootstrap. peerId=" + localPeer.getId()
                + ", name=" + localPeer.getName());
        boolean registered = bootstrapClient.register(localPeer);
        if (!registered) {
            System.out.println("[WARN] Bootstrap REGISTER failed. Peer still runs in direct TCP mode.");
            return;
        }

        JoinResponse joinResponse = bootstrapClient.join(localPeer);
        int added = peerDirectoryService.syncOnlinePeers(joinResponse.getOnlinePeers());
        syncGroupsFromBootstrap();
        handleOfflineMessages(joinResponse);
        peerChangeNotifier.run();
        System.out.println("[INFO] Bootstrap sync completed. addedPeers=" + added
                + ", knownPeers=" + peerDirectoryService.size());
    }

    /**
     * Lam moi danh sach peer online va group tu bootstrap-server.
     */
    @Override
    public void refreshFromBootstrap() {
        Collection<PeerInfo> onlinePeers = bootstrapClient.listOrNull();
        if (onlinePeers == null) {
            System.out.println("[WARN] Bootstrap refresh skipped because tracker is unavailable. "
                    + "Keeping current local peer state.");
            return;
        }
        int onlineCount = peerDirectoryService.syncOnlinePeers(onlinePeers);
        syncGroupsFromBootstrap();
        peerChangeNotifier.run();
        System.out.println("[INFO] Bootstrap refresh completed. onlinePeers=" + onlineCount
                + ", knownPeers=" + peerDirectoryService.size());
    }

    /**
     * Load group membership tu bootstrap va cache lai vao groups.json cua profile hien tai.
     */
    private void syncGroupsFromBootstrap() {
        List<Group> joinedGroups = bootstrapGroupService.fetchJoinedGroups(peerDirectoryService.list());
        groupManager.replaceAll(joinedGroups);
        System.out.println("[INFO] Bootstrap group sync completed. joinedGroups=" + joinedGroups.size());
    }

    /**
     * Dua cac tin offline bootstrap tra ve vao history neu tim duoc peer gui trong danh sach da biet.
     */
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
            System.out.println("[INFO] Offline message loaded. messageId=" + offlineMessage.messageId()
                    + ", senderId=" + offlineMessage.senderId()
                    + ", historyKey=" + historyKey);
        }
    }

    /**
     * Tao conversation peer dai dien group de message offline group duoc luu dung history.
     */
    private PeerInfo groupConversationPeer(String groupId) {
        Group group = groupManager.getGroup(groupId);
        String name = group == null ? groupId : group.getName();
        return new PeerInfo(groupId, name, GROUP_CHAT_PREFIX + groupId, 0, false);
    }
}
