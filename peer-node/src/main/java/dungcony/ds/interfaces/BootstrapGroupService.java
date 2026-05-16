package dungcony.ds.interfaces;

import dungcony.ds.model.Group;
import dungcony.ds.model.PeerInfo;

import java.util.Collection;
import java.util.List;

public interface BootstrapGroupService {

    // đẩy group lên server để các peer khác nhận được
    void publishGroup(Group group);

    // thêm peer vào group đã tồn tại trên bootstrap
    void addMembersToGroup(String groupId, Collection<PeerInfo> members);

    // lấy các group peer hiện tại là thành viên
    List<Group> fetchJoinedGroups(Collection<PeerInfo> knownPeers);
}
