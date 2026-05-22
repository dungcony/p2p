package dungcony.ds.models;

import lombok.extern.slf4j.Slf4j;


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

@Slf4j
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
        log.info("PeerRegistry khởi tạo cache peer online trong RAM.");
    }

    // Đăng ký hoặc cập nhật thông tin user/peer trong bảng users, chưa đánh dấu online.
    public void register(PeerInfo peerInfo) {
        if (peerInfo == null) {
            log.warn("PeerRegistry bỏ qua register vì peer null.");
            return;
        }
        UserEntity userEntity = saveUser(peerInfo);
        log.info("PeerRegistry đã register userId={}, tênHiểnThị={}", userEntity.getUserId(), userEntity.getDisplayName());
    }

    // Đăng ký hoặc cập nhật một peer đang online trong tracker.
    public void join(PeerInfo peerInfo) {
        if (peerInfo != null) {
            saveUser(peerInfo);
            peerInfo.setOnline(true);
            peers.put(peerInfo.addressKey(), peerInfo);
            lastSeenByPeerKey.put(peerInfo.addressKey(), System.currentTimeMillis());
            log.debug("PeerRegistry peer join/cache heartbeat: {}", peerInfo.addressKey());
        } else {
            log.warn("PeerRegistry bỏ qua join vì peer null.");
        }
    }

    // Xóa peer khỏi registry khi peer rời mạng.
    public void leave(String addressKey) {
        peers.remove(addressKey);
        lastSeenByPeerKey.remove(addressKey);
        log.debug("PeerRegistry peer rời mạng: {}", addressKey);
    }

    // Trả về danh sách peer online hiện được tracker biết.
    public Collection<PeerInfo> list() {
        evictExpiredPeers();
        List<PeerInfo> onlinePeers = new ArrayList<>(peers.values());
        onlinePeers.sort(Comparator.comparingLong(
                (PeerInfo peerInfo) -> lastSeenByPeerKey.getOrDefault(peerInfo.addressKey(), 0L)
        ).reversed());
        log.trace("PeerRegistry list cache online sốLượng={}", onlinePeers.size());
        return onlinePeers;
    }

    // Lưu message offline khi sender không gửi trực tiếp được cho receiver.
    public void storeOfflineMessage(OfflineMessageEntity message) {
        offlineMessageRepo.save(message);
    }

    // Lấy message offline của peer vừa JOIN và đánh dấu đã giao.
    public Collection<OfflineMessageEntity> drainOfflineMessages(String receiverId) {
        Collection<OfflineMessageEntity> messages = offlineMessageRepo.findPendingByReceiver(receiverId);
        offlineMessageRepo.markDelivered(messages);
        log.info("Đã lấy tin nhắn offline cho receiver={}, count={}", receiverId, messages.size());
        return messages;
    }

    // Lưu group metadata vào tracker.
    public void createGroup(GroupEntity groupEntity) {
        groupRepo.upsert(groupEntity);
    }

    // Lấy danh sách group metadata từ tracker.
    public Collection<GroupEntity> listGroups() {
        return groupRepo.listAll();
    }

    // Thêm peer/user vào group.
    public void addGroupMember(GroupMemberEntity memberEntity) {
        if (memberEntity == null || memberEntity.getUserId() == null || memberEntity.getUserId().isBlank()) {
            log.warn("Bỏ qua thêm thành viên nhóm vì userId rỗng.");
            return;
        }
        long now = System.currentTimeMillis();
        userRepo.upsert(new UserEntity(memberEntity.getUserId(), memberEntity.getUserId(), now, now));
        groupMemberRepo.add(memberEntity);
    }

    // Xóa peer/user khỏi group.
    public void removeGroupMember(String groupId, String userId) {
        groupMemberRepo.remove(groupId, userId);
    }

    // Lấy danh sách thành viên của group.
    public Collection<GroupMemberEntity> listGroupMembers(String groupId) {
        return groupMemberRepo.listByGroup(groupId);
    }

    // Lưu/cập nhật user vào SQLite, không lưu trạng thái online vào DB.
    private UserEntity saveUser(PeerInfo peerInfo) {
        long now = System.currentTimeMillis();
        UserEntity userEntity = UserEntity.fromPeerInfo(peerInfo, now);
        userRepo.upsert(userEntity);
        return userEntity;
    }

    // Loại bỏ peer quá hạn heartbeat khỏi cache online runtime.
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
            log.info("PeerRegistry đã xóa peer quá hạn khỏi cache online. sốLượng={}", expired);
        }
    }
}
