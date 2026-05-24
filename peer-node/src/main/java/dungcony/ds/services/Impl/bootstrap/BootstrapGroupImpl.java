package dungcony.ds.services.impl.bootstrap;

import dungcony.ds.dtos.GroupMemberPayload;
import dungcony.ds.dtos.GroupPayload;
import dungcony.ds.model.Group;
import dungcony.ds.model.PeerInfo;
import dungcony.ds.network.BootstrapClient;
import dungcony.ds.services.interfaces.bootstrap.BootstrapGroupService;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Slf4j
// Service đồng bộ group chat với bootstrap-server gồm tạo group, thêm member và nạp group đã tham gia
public class BootstrapGroupImpl implements BootstrapGroupService {

    private final BootstrapClient bootstrapClient;
    private final PeerInfo localPeer;

    // Khởi tạo service group bootstrap với client tracker và peer local
    public BootstrapGroupImpl(BootstrapClient bootstrapClient, PeerInfo localPeer) {
        this.bootstrapClient = bootstrapClient;
        this.localPeer = localPeer;
    }

    // Đẩy group mới lên bootstrap để peer khác có thể nạp membership
    @Override
    public void publishGroup(Group group) {
        if (bootstrapClient == null) {
            log.warn("Không thể publish nhóm vì bootstrap đang tắt. groupId={}", group.getGroupId());
            return;
        }
        boolean created = bootstrapClient.createGroup(group, localPeer.getId());
        if (!created) {
            log.warn("Bootstrap CREATE_GROUP thất bại. groupId={}", group.getGroupId());
            return;
        }
        bootstrapClient.addGroupMember(group.getGroupId(), localPeer.getId());
        for (PeerInfo member : group.getMembers()) {
            bootstrapClient.addGroupMember(group.getGroupId(), member.getId());
        }
        log.info("Đã publish nhóm lên bootstrap. groupId={}, sốThànhViên={}",
                group.getGroupId(), group.getMembers().size());
    }

    // Thêm các peer mới vào group đã tồn tại trên bootstrap
    @Override
    public void addMembersToGroup(String groupId, Collection<PeerInfo> members) {
        if (bootstrapClient == null) {
            log.warn("Không thể thêm thành viên nhóm lên bootstrap vì bootstrap đang tắt. groupId={}", groupId);
            return;
        }
        int added = 0;
        if (members != null) {
            for (PeerInfo member : members) {
                if (member == null || member.getId() == null || member.getId().isBlank()) {
                    continue;
                }
                if (bootstrapClient.addGroupMember(groupId, member.getId())) {
                    added++;
                }
            }
        }
        log.info("Đã đồng bộ thêm thành viên nhóm lên bootstrap. groupId={}, sốThànhViênThêm={}", groupId, added);
    }

    // Lấy các group mà peer local hiện là thành viên từ bootstrap
    @Override
    public List<Group> fetchJoinedGroups(Collection<PeerInfo> knownPeers) {
        List<Group> joinedGroups = new ArrayList<>();
        if (bootstrapClient == null) {
            return joinedGroups;
        }
        for (GroupPayload groupPayload : bootstrapClient.listGroups()) {
            Collection<GroupMemberPayload> memberPayloads = bootstrapClient.listGroupMembers(groupPayload.groupId());
            boolean localPeerIsMember = memberPayloads.stream()
                    .anyMatch(member -> localPeer.getId().equals(member.userId()));
            if (!localPeerIsMember) {
                continue;
            }
            joinedGroups.add(toGroup(groupPayload, memberPayloads, knownPeers));
        }
        log.info("Đồng bộ nhóm từ bootstrap lấy được số nhóm đã tham gia={}", joinedGroups.size());
        return joinedGroups;
    }

    // Chuyển DTO bootstrap thành Group runtime của peer-node
    private Group toGroup(GroupPayload groupPayload, Collection<GroupMemberPayload> memberPayloads,
                          Collection<PeerInfo> knownPeers) {
        List<PeerInfo> members = new ArrayList<>();
        for (GroupMemberPayload memberPayload : memberPayloads) {
            if (localPeer.getId().equals(memberPayload.userId())) {
                members.add(localPeer);
                continue;
            }
            PeerInfo knownPeer = findKnownPeerById(knownPeers, memberPayload.userId());
            members.add(knownPeer == null
                    ? new PeerInfo(memberPayload.userId(), memberPayload.userId(), "", 0, false)
                    : knownPeer);
        }
        return new Group(groupPayload.groupId(), groupPayload.name(), members);
    }

    // Tìm peer runtime theo user_id ổn định do bootstrap cấp
    private PeerInfo findKnownPeerById(Collection<PeerInfo> knownPeers, String peerId) {
        if (peerId == null || peerId.isBlank() || knownPeers == null) {
            return null;
        }
        for (PeerInfo peerInfo : knownPeers) {
            if (peerId.equals(peerInfo.getId())) {
                return peerInfo;
            }
        }
        return null;
    }
}
