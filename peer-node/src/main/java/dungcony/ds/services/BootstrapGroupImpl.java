package dungcony.ds.services;

import dungcony.ds.dtos.GroupMemberPayload;
import dungcony.ds.dtos.GroupPayload;
import dungcony.ds.interfaces.BootstrapGroupService;
import dungcony.ds.model.BootstrapClient;
import dungcony.ds.model.Group;
import dungcony.ds.model.PeerInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BootstrapGroupImpl implements BootstrapGroupService {
    private final BootstrapClient bootstrapClient;
    private final PeerInfo localPeer;

    public BootstrapGroupImpl(BootstrapClient bootstrapClient, PeerInfo localPeer) {
        this.bootstrapClient = bootstrapClient;
        this.localPeer = localPeer;
    }

    @Override
    public void publishGroup(Group group) {
        if (bootstrapClient == null) {
            System.out.println("[WARN] Cannot publish group because bootstrap is disabled. groupId="
                    + group.getGroupId());
            return;
        }
        boolean created = bootstrapClient.createGroup(group, localPeer.getId());
        if (!created) {
            System.out.println("[WARN] Bootstrap CREATE_GROUP failed. groupId=" + group.getGroupId());
            return;
        }
        bootstrapClient.addGroupMember(group.getGroupId(), localPeer.getId());
        for (PeerInfo member : group.getMembers()) {
            bootstrapClient.addGroupMember(group.getGroupId(), member.getId());
        }
        System.out.println("[INFO] Group published to bootstrap. groupId=" + group.getGroupId()
                + ", members=" + group.getMembers().size());
    }

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

        System.out.println("[INFO] Bootstrap group sync fetched joinedGroups=" + joinedGroups.size());
        return joinedGroups;
    }


    //----------------------------------------- PRIVATE -----------------------------------//

    /**
     * Chuyen DTO bootstrap thanh Group runtime cua peer-node.
     */
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

    /**
     * Tim peer runtime theo user_id on dinh do bootstrap cap.
     */
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
