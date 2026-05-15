package dungcony.ds.models;

import dungcony.ds.entities.GroupEntity;
import dungcony.ds.entities.GroupMemberEntity;
import dungcony.ds.entities.OnlinePeerEntity;
import dungcony.ds.entities.OfflineMessageEntity;
import dungcony.ds.entities.UserEntity;
import dungcony.ds.repositories.Conn;
import dungcony.ds.repositories.GroupMemberRepo;
import dungcony.ds.repositories.GroupRepo;
import dungcony.ds.repositories.OfflineMessageRepo;
import dungcony.ds.repositories.OnlinePeerRepo;
import dungcony.ds.repositories.UserRepo;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PeerRegistry {
    private final Map<String, PeerInfo> peers = new ConcurrentHashMap<>();
    private final Conn conn;
    private final UserRepo userRepo;
    private final OnlinePeerRepo onlinePeerRepo;
    private final OfflineMessageRepo offlineMessageRepo;
    private final GroupRepo groupRepo;
    private final GroupMemberRepo groupMemberRepo;

    public PeerRegistry(Conn conn) {
        this.conn = conn;
        this.userRepo = new UserRepo(conn);
        this.onlinePeerRepo = new OnlinePeerRepo(conn);
        this.offlineMessageRepo = new OfflineMessageRepo(conn);
        this.groupRepo = new GroupRepo(conn);
        this.groupMemberRepo = new GroupMemberRepo(conn);
        for (PeerInfo peerInfo : onlinePeerRepo.listOnline()) {
            peers.put(peerInfo.addressKey(), peerInfo);
        }
        System.out.println("[INFO] PeerRegistry loaded online peers from SQLite. count=" + peers.size());
    }

    /**
     * Đăng ký hoặc cập nhật một peer đang online trong tracker.
     */
    public void join(PeerInfo peerInfo) {
        if (peerInfo != null) {
            peerInfo.setOnline(true);
            peers.put(peerInfo.addressKey(), peerInfo);
            saveJoinedPeer(peerInfo);
            System.out.println("[DEBUG] PeerRegistry join: " + peerInfo.addressKey());
        } else {
            System.out.println("[WARN] PeerRegistry join ignored null peer.");
        }
    }

    /**
     * Xóa peer khỏi registry khi peer rời mạng.
     */
    public void leave(String addressKey) {
        peers.remove(addressKey);
        onlinePeerRepo.remove(addressKey);
        System.out.println("[DEBUG] PeerRegistry leave: " + addressKey);
    }

    /**
     * Trả về danh sách peer online hiện được tracker biết.
     */
    public Collection<PeerInfo> list() {
        Collection<PeerInfo> onlinePeers = onlinePeerRepo.listOnline();
        peers.clear();
        for (PeerInfo peerInfo : onlinePeers) {
            peers.put(peerInfo.addressKey(), peerInfo);
        }
        System.out.println("[TRACE] PeerRegistry list size=" + onlinePeers.size());
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
        System.out.println("[INFO] Drained offline messages for receiver=" + receiverId
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
     * Luu user va peer online trong cung transaction.
     */
    private void saveJoinedPeer(PeerInfo peerInfo) {
        long now = System.currentTimeMillis();
        UserEntity userEntity = UserEntity.fromPeerInfo(peerInfo, now);
        OnlinePeerEntity onlinePeerEntity = OnlinePeerEntity.fromPeerInfo(peerInfo, userEntity.getUserId(), now);
        try (Connection connection = conn.getConnection()) {
            connection.setAutoCommit(false);
            try {
                userRepo.upsert(userEntity, connection);
                onlinePeerRepo.upsert(onlinePeerEntity, connection);
                connection.commit();
                System.out.println("[INFO] SQLite saved online peer=" + peerInfo.addressKey());
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] Failed to save joined peer: " + e.getMessage());
        }
    }
}
