package dungcony.ds.services;

import dungcony.ds.interfaces.NetworkAddressService;
import dungcony.ds.interfaces.PeerDirectoryService;
import dungcony.ds.model.Message;
import dungcony.ds.model.PeerInfo;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PeerDirectoryImpl implements PeerDirectoryService {
    private final PeerInfo localPeer;
    private final NetworkAddressService networkAddressService;
    private final Map<String, PeerInfo> peers = new ConcurrentHashMap<>();

    /**
     * Khoi tao danh ba peer runtime cua PeerNode.
     */
    public PeerDirectoryImpl(PeerInfo localPeer, NetworkAddressService networkAddressService) {
        this.localPeer = localPeer;
        this.networkAddressService = networkAddressService;
    }

    /**
     * Them peer vao danh ba theo addressKey.
     */
    @Override
    public void put(PeerInfo peerInfo) {
        if (peerInfo != null) {
            peers.put(peerInfo.addressKey(), peerInfo);
        }
    }

    /**
     * Lay danh sach peer runtime hien tai.
     */
    @Override
    public Collection<PeerInfo> list() {
        return Collections.unmodifiableCollection(peers.values());
    }

    /**
     * Lay so peer da biet.
     */
    @Override
    public int size() {
        return peers.size();
    }

    /**
     * Them mot peer tu input name va host/port.
     */
    @Override
    public PeerInfo addKnownPeer(String name, String hostAndMaybePort) {
        PeerInfo peerInfo = parsePeer(name, hostAndMaybePort);
        if (peerInfo == null) {
            System.out.println("[WARN] Ignored addKnownPeer because address is blank.");
            return null;
        }
        if (isSelfPeer(peerInfo)) {
            System.out.println("[WARN] Ignored addKnownPeer because target is local peer: " + peerInfo.addressKey());
            return null;
        }
        PeerInfo existingPeer = peers.get(peerInfo.addressKey());
        if (existingPeer != null) {
            System.out.println("[INFO] Known peer already exists. Reusing peer id=" + existingPeer.getId()
                    + ", address=" + existingPeer.addressKey());
            return existingPeer;
        }
        put(peerInfo);
        System.out.println("[INFO] Added known peer: id=" + peerInfo.getId()
                + ", address=" + peerInfo.addressKey());
        return peerInfo;
    }

    /**
     * Tìm peer đã biết hoặc phân tích địa chỉ đầu vào thành PeerInfo tạm thời.
     */
    @Override
    public PeerInfo resolvePeer(String hostAndMaybePort) {
        if (hostAndMaybePort == null || hostAndMaybePort.isBlank()) {
            System.out.println("[WARN] resolvePeer called with blank address.");
            return null;
        }
        PeerInfo existing = peers.get(hostAndMaybePort.trim());
        if (existing != null) {
            return existing;
        }
        PeerInfo parsed = parsePeer(hostAndMaybePort, hostAndMaybePort);
        return peers.getOrDefault(parsed.addressKey(), parsed);
    }

    /**
     * Chuyển chuỗi host hoặc host:port thành PeerInfo với port mặc định nếu không nhập port.
     */
    @Override
    public PeerInfo parsePeer(String name, String hostAndMaybePort) {
        String value = hostAndMaybePort == null ? "" : hostAndMaybePort.trim();
        if (value.isBlank()) {
            return null;
        }
        String host = value;
        int port = localPeer.getPort();
        int colonIndex = value.lastIndexOf(':');
        if (colonIndex > 0 && colonIndex < value.length() - 1) {
            host = value.substring(0, colonIndex);
            try {
                port = Integer.parseInt(value.substring(colonIndex + 1));
            } catch (NumberFormatException ignored) {
                System.out.println("[WARN] Invalid peer port in address '" + value
                        + "'. Falling back to local port " + localPeer.getPort());
                port = localPeer.getPort();
            }
        }
        return new PeerInfo(name, host, port);
    }

    /**
     * Tim peer runtime theo user_id on dinh do bootstrap cap.
     */
    @Override
    public PeerInfo findKnownPeerById(String peerId) {
        if (peerId == null || peerId.isBlank()) {
            return null;
        }
        for (PeerInfo peerInfo : peers.values()) {
            if (peerId.equals(peerInfo.getId())) {
                return peerInfo;
            }
        }
        return null;
    }

    /**
     * Tao PeerInfo tu message den va giu lai ten hien thi neu peer da co trong map.
     */
    @Override
    public PeerInfo mergeSenderFromKnownPeers(Message message) {
        String key = message.getSenderHost() + ":" + message.getSenderPort();
        PeerInfo existing = peers.get(key);
        String displayName = existing == null ? message.getSenderId() : existing.getName();
        return new PeerInfo(message.getSenderId(), displayName, message.getSenderHost(), message.getSenderPort());
    }

    /**
     * Dong bo danh sach online bootstrap tra ve, danh dau peer vang mat la offline.
     */
    @Override
    public int syncOnlinePeers(Collection<PeerInfo> onlinePeers) {
        peers.values().forEach(peerInfo -> peerInfo.setOnline(false));
        int addedOrUpdated = 0;
        if (onlinePeers == null) {
            return addedOrUpdated;
        }
        for (PeerInfo peerInfo : onlinePeers) {
            if (peerInfo == null || isSelfPeer(peerInfo)) {
                continue;
            }
            peerInfo.setOnline(true);
            put(peerInfo);
            addedOrUpdated++;
            System.out.println("[DEBUG] Directory synced online peer id=" + peerInfo.getId()
                    + ", address=" + peerInfo.addressKey());
        }
        System.out.println("[INFO] Directory online sync completed. onlineCount=" + addedOrUpdated
                + ", knownCount=" + peers.size());
        return addedOrUpdated;
    }

    /**
     * Kiem tra peer co tro ve local peer hay khong.
     */
    @Override
    public boolean isSelfPeer(PeerInfo peerInfo) {
        return networkAddressService.isSelfPeer(localPeer, peerInfo);
    }
}
