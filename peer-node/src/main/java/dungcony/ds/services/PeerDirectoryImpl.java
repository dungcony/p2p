package dungcony.ds.services;

import lombok.extern.slf4j.Slf4j;


import dungcony.ds.interfaces.NetworkAddressService;
import dungcony.ds.interfaces.PeerDirectoryService;
import dungcony.ds.model.Message;
import dungcony.ds.model.PeerInfo;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
// Service quản lý danh bạ peer runtime và các thao tác merge resolve peer
public class PeerDirectoryImpl implements PeerDirectoryService {
    private final PeerInfo localPeer;
    private final NetworkAddressService networkAddressService;
    private final Map<String, PeerInfo> peers = new ConcurrentHashMap<>();

    // Khởi tạo danh bạ peer runtime của PeerNode
    public PeerDirectoryImpl(PeerInfo localPeer, NetworkAddressService networkAddressService) {
        this.localPeer = localPeer;
        this.networkAddressService = networkAddressService;
    }

    // Thêm peer vào danh bạ theo addressKey
    @Override
    public void put(PeerInfo peerInfo) {
        if (peerInfo != null) {
            removeSamePeerWithDifferentAddress(peerInfo);
            peers.put(peerInfo.addressKey(), peerInfo);
        }
    }

    // Lấy danh sách peer runtime hiện tại
    @Override
    public Collection<PeerInfo> list() {
        return Collections.unmodifiableCollection(peers.values());
    }

    // Merge danh sách peer nhận từ bootstrap hoặc peer khác vào danh bạ runtime
    @Override
    public int mergeKnownPeers(Collection<PeerInfo> discoveredPeers) {
        int merged = 0;
        if (discoveredPeers == null) {
            return merged;
        }
        for (PeerInfo peerInfo : discoveredPeers) {
            if (peerInfo == null || isSelfPeer(peerInfo) || isGroupPseudoPeer(peerInfo)) {
                continue;
            }
            PeerInfo normalized = new PeerInfo(
                    peerInfo.getId(),
                    peerInfo.getName(),
                    peerInfo.getHost(),
                    peerInfo.getPort(),
                    peerInfo.isOnline()
            );
            put(normalized);
            merged++;
            log.debug("Đã merge peer từ discovery. id={}, địaChỉ={}, online={}", normalized.getId(), normalized.addressKey(), normalized.isOnline());
        }
        log.info("Merge danh sách peer xong. sốPeerMerge={}, knownCount={}", merged, peers.size());
        return merged;
    }

    // Lấy số peer đã biết
    @Override
    public int size() {
        return peers.size();
    }

    // Thêm một peer từ input name và host/port
    @Override
    public PeerInfo addKnownPeer(String name, String hostAndMaybePort) {
        PeerInfo peerInfo = parsePeer(name, hostAndMaybePort);
        if (peerInfo == null) {
            log.warn("Đã bỏ qua thêm peer vì địa chỉ rỗng.");
            return null;
        }
        if (isSelfPeer(peerInfo)) {
            log.warn("Đã bỏ qua thêm peer vì đích là peer local: {}", peerInfo.addressKey());
            return null;
        }
        PeerInfo existingPeer = peers.get(peerInfo.addressKey());
        if (existingPeer != null) {
            log.info("Peer đã tồn tại. Dùng lại peer id={}, địaChỉ={}", existingPeer.getId(), existingPeer.addressKey());
            return existingPeer;
        }
        put(peerInfo);
        log.info("Đã thêm peer đã biết: id={}, địaChỉ={}", peerInfo.getId(), peerInfo.addressKey());
        return peerInfo;
    }

    // Tìm peer đã biết hoặc phân tích địa chỉ đầu vào thành PeerInfo tạm thời
    @Override
    public PeerInfo resolvePeer(String hostAndMaybePort) {
        if (hostAndMaybePort == null || hostAndMaybePort.isBlank()) {
            log.warn("resolvePeer được gọi với địa chỉ rỗng.");
            return null;
        }
        PeerInfo existing = peers.get(hostAndMaybePort.trim());
        if (existing != null) {
            return existing;
        }
        PeerInfo parsed = parsePeer(hostAndMaybePort, hostAndMaybePort);
        return peers.getOrDefault(parsed.addressKey(), parsed);
    }

    // Chuyển chuỗi host hoặc host:port thành PeerInfo với port mặc định nếu không nhập port
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
                log.warn("Cổng peer không hợp lệ trong địa chỉ '{}'. Chuyển về cổng local {}", value, localPeer.getPort());
                port = localPeer.getPort();
            }
        }
        return new PeerInfo(name, host, port);
    }

    // Tìm peer runtime theo user_id ổn định do bootstrap cấp
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

    // Tạo PeerInfo từ message đến và giữ lại tên hiển thị nếu peer đã có trong map
    @Override
    public PeerInfo mergeSenderFromKnownPeers(Message message) {
        String key = message.getSenderHost() + ":" + message.getSenderPort();
        PeerInfo existing = peers.get(key);
        String displayName = existing == null ? message.getSenderId() : existing.getName();
        return new PeerInfo(message.getSenderId(), displayName, message.getSenderHost(), message.getSenderPort());
    }

    // Đồng bộ danh sách online bootstrap trả về, đánh dấu peer vắng mặt là offline
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
            log.debug("Danh bạ đã đồng bộ peer online id={}, địaChỉ={}", peerInfo.getId(), peerInfo.addressKey());
        }
        log.info("Đồng bộ trạng thái online trong danh bạ xong. sốOnline={}, knownCount={}", addedOrUpdated, peers.size());
        return addedOrUpdated;
    }

    // Kiểm tra peer có trỏ về local peer hay không
    @Override
    public boolean isSelfPeer(PeerInfo peerInfo) {
        return networkAddressService.isSelfPeer(localPeer, peerInfo);
    }

    private void removeSamePeerWithDifferentAddress(PeerInfo peerInfo) {
        if (peerInfo.getId() == null || peerInfo.getId().isBlank()) {
            return;
        }
        peers.entrySet().removeIf(entry -> peerInfo.getId().equals(entry.getValue().getId())
                && !peerInfo.addressKey().equals(entry.getKey()));
    }

    private boolean isGroupPseudoPeer(PeerInfo peerInfo) {
        return peerInfo.getHost() != null && peerInfo.getHost().startsWith("group:");
    }
}
