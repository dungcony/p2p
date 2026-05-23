package dungcony.ds.interfaces;

import dungcony.ds.model.Group;
import dungcony.ds.model.PeerInfo;

import java.util.Collection;
import java.util.List;

// Đại diện contract đồng bộ group giữa peer-node và bootstrap-server
public interface BootstrapGroupService {

    // Đẩy group lên server để các peer khác nhận được
    void publishGroup(Group group);

    // Thêm peer vào group đã tồn tại trên bootstrap
    void addMembersToGroup(String groupId, Collection<PeerInfo> members);

    // Lấy các group peer hiện tại là thành viên
    List<Group> fetchJoinedGroups(Collection<PeerInfo> knownPeers);
}
