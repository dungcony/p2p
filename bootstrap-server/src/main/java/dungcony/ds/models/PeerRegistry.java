package dungcony.ds.models;

import dungcony.ds.entities.GroupEntity;
import dungcony.ds.entities.GroupMemberEntity;
import dungcony.ds.entities.OfflineMessageEntity;
import dungcony.ds.entities.UserEntity;
import dungcony.ds.repositories.Conn;
import dungcony.ds.repositories.GroupMemberRepo;
import dungcony.ds.repositories.GroupRepo;
import dungcony.ds.repositories.OfflineMessageRepo;
import dungcony.ds.repositories.UserRepo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PeerRegistry {
    private static final long ONLINE_TTL_MS = 15_000;

    private final Map<String, PeerInfo> peers = new ConcurrentHashMap<>();
    private final Map<String, Long> lastSeenByPeerKey = new ConcurrentHashMap<>();
    private final UserRepo userRepo;
    private final OfflineMessageRepo offlineMessageRepo;
    private final GroupRepo groupRepo;
    private final GroupMemberRepo groupMemberRepo;

    public PeerRegistry(Conn conn) {
        this.userRepo = new UserRepo(conn);
        this.offlineMessageRepo = new OfflineMessageRepo(conn);
        this.groupRepo = new GroupRepo(conn);
        this.groupMemberRepo = new GroupMemberRepo(conn);
        System.out.println("[INFO] PeerRegistry khởi tạo cache peer online trong RAM.");
    }

    /**
     * Dang ky hoac cap nhat thong tin user/peer trong bang users, chua danh dau online.
     */
    public void register(PeerInfo peerInfo) {
        if (peerInfo == null) {
            System.out.println("[WARN] PeerRegistry bỏ qua register vì peer null.");
            return;
        }
        UserEntity userEntity = saveUser(peerInfo);
        System.out.println("[INFO] PeerRegistry đã register userId=" + userEntity.getUserId()
                + ", tênHiểnThị=" + userEntity.getDisplayName());
    }

    /**
     * Đăng ký hoặc cập nhật một peer đang online trong tracker.
     */
    public void join(PeerInfo peerInfo) {
        if (peerInfo != null) {
            saveUser(peerInfo);
            peerInfo.setOnline(true);
            peers.put(peerInfo.addressKey(), peerInfo);
            lastSeenByPeerKey.put(peerInfo.addressKey(), System.currentTimeMillis());
            System.out.println("[DEBUG] PeerRegistry peer join/cache heartbeat: " + peerInfo.addressKey());
        } else {
            System.out.println("[WARN] PeerRegistry bỏ qua join vì peer null.");
        }
    }

    /**
     * Xóa peer khỏi registry khi peer rời mạng.
     */
    public void leave(String addressKey) {
        peers.remove(addressKey);
        lastSeenByPeerKey.remove(addressKey);
        System.out.println("[DEBUG] PeerRegistry peer rời mạng: " + addressKey);
    }

    /**
     * Trả về danh sách peer online hiện được tracker biết.
     */
    public Collection<PeerInfo> list() {
        evictExpiredPeers();
        List<PeerInfo> onlinePeers = new ArrayList<>(peers.values());
        onlinePeers.sort(Comparator.comparingLong(
                (PeerInfo peerInfo) -> lastSeenByPeerKey.getOrDefault(peerInfo.addressKey(), 0L)
        ).reversed());
        System.out.println("[TRACE] PeerRegistry list cache online sốLượng=" + onlinePeers.size());
        return onlinePeers;
    }

    /**
     * Luu message offline khi sender khong gui truc tiep duoc cho receiver.
     */
    public void storeOfflineMessage(OfflineMessageEntity message) {
        offlineMessageRepo.save(message);
    }

    /**
     * Lay message offline cua peer vua JOIN va danh dau da giao.
     */
    public Collection<OfflineMessageEntity> drainOfflineMessages(String receiverId) {
        Collection<OfflineMessageEntity> messages = offlineMessageRepo.findPendingByReceiver(receiverId);
        offlineMessageRepo.markDelivered(messages);
        System.out.println("[INFO] Đã lấy tin nhắn offline cho receiver=" + receiverId
                + ", count=" + messages.size());
        return messages;
    }

    /**
     * Luu group metadata vao tracker.
     */
    public void createGroup(GroupEntity groupEntity) {
        groupRepo.upsert(groupEntity);
    }

    /**
     * Lay danh sach group metadata tu tracker.
     */
    public Collection<GroupEntity> listGroups() {
        return groupRepo.listAll();
    }

    /**
     * Them peer/user vao group.
     */
    public void addGroupMember(GroupMemberEntity memberEntity) {
        groupMemberRepo.add(memberEntity);
    }

    /**
     * Xoa peer/user khoi group.
     */
    public void removeGroupMember(String groupId, String userId) {
        groupMemberRepo.remove(groupId, userId);
    }

    /**
     * Lay danh sach thanh vien cua group.
     */
    public Collection<GroupMemberEntity> listGroupMembers(String groupId) {
        return groupMemberRepo.listByGroup(groupId);
    }

    /**
     * Luu/cap nhat user vao SQLite, khong luu trang thai online vao DB.
     */
    private UserEntity saveUser(PeerInfo peerInfo) {
        long now = System.currentTimeMillis();
        UserEntity userEntity = UserEntity.fromPeerInfo(peerInfo, now);
        userRepo.upsert(userEntity);
        return userEntity;
    }

    /**
     * Loai bo peer qua han heartbeat khoi cache online runtime.
     */
    private void evictExpiredPeers() {
        long cutoff = System.currentTimeMillis() - ONLINE_TTL_MS;
        int expired = 0;
        for (Map.Entry<String, Long> entry : lastSeenByPeerKey.entrySet()) {
            if (entry.getValue() < cutoff) {
                lastSeenByPeerKey.remove(entry.getKey());
                peers.remove(entry.getKey());
                expired++;
            }
        }
        if (expired > 0) {
            System.out.println("[INFO] PeerRegistry đã xóa peer quá hạn khỏi cache online. sốLượng=" + expired);
        }
    }
}
