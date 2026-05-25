package dungcony.ds.services.impl.messaging;

import dungcony.ds.dtos.BroadcastResult;
import dungcony.ds.model.Message;
import dungcony.ds.model.PeerInfo;
import dungcony.ds.services.interfaces.bootstrap.PeerBootstrapGateway;
import dungcony.ds.services.interfaces.messaging.NetworkBroadcastService;
import dungcony.ds.services.interfaces.messaging.PeerMessageSender;
import dungcony.ds.services.interfaces.peer.PeerDirectoryService;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
// Service xử lý broadcast toàn mạng dựa trên bootstrap và danh bạ peer runtime
public class NetworkBroadcastImpl implements NetworkBroadcastService {

    private final PeerInfo localPeer;
    private final PeerMessageSender messageSender;
    private final PeerBootstrapGateway bootstrapGateway;
    private final PeerDirectoryService peerDirectoryService;
    private final Runnable peerChangeNotifier;

    // Khởi tạo service broadcast với peer local, sender, bootstrap và danh bạ peer
    public NetworkBroadcastImpl(PeerInfo localPeer,
                                PeerMessageSender messageSender,
                                PeerBootstrapGateway bootstrapGateway,
                                PeerDirectoryService peerDirectoryService,
                                Runnable peerChangeNotifier) {
        this.localPeer = localPeer;
        this.messageSender = messageSender;
        this.bootstrapGateway = bootstrapGateway;
        this.peerDirectoryService = peerDirectoryService;
        this.peerChangeNotifier = peerChangeNotifier;
    }

    // Gửi một nội dung tới toàn bộ peer online và trả kết quả tổng hợp
    @Override
    public BroadcastResult broadcastToNetwork(String content) {
        if (content == null || content.isBlank()) {
            log.warn("Từ chối broadcast tin nhắn rỗng.");
            return new BroadcastResult(0, 0, 0);
        }
        List<PeerInfo> targets = collectOnlineBroadcastTargets();
        int delivered = 0;
        int failed = 0;
        log.info("Đang broadcast toàn mạng. sốPeerĐích={}", targets.size());
        for (PeerInfo target : targets) {
            Message message = Message.broadcast(localPeer, target, content);
            boolean sent = messageSender.send(target, message);
            target.setOnline(sent);
            if (sent) {
                delivered++;
            } else {
                failed++;
            }
            log.info("Kết quả broadcast toàn mạng. receiver={}, sent={}", target.addressKey(), sent);
        }
        peerChangeNotifier.run();
        BroadcastResult result = new BroadcastResult(targets.size(), delivered, failed);
        log.info("Broadcast toàn mạng hoàn tất. total={}, delivered={}, failed={}",
                result.totalTargets(), result.delivered(), result.failed());
        return result;
    }

    // Lấy peer online từ bootstrap trước rồi hợp nhất với peer đã discover trực tiếp
    private List<PeerInfo> collectOnlineBroadcastTargets() {
        Map<String, PeerInfo> targets = new LinkedHashMap<>();
        Collection<PeerInfo> bootstrapPeers = bootstrapGateway == null ? null : bootstrapGateway.listOrNull();
        if (bootstrapPeers != null) {
            peerDirectoryService.mergeKnownPeers(bootstrapPeers);
            appendBroadcastTargets(targets, bootstrapPeers);
        }
        appendBroadcastTargets(targets, peerDirectoryService.list());
        return new ArrayList<>(targets.values());
    }

    // Thêm peer hợp lệ vào target map và tránh trùng theo id hoặc address
    private void appendBroadcastTargets(Map<String, PeerInfo> targets, Collection<PeerInfo> candidates) {
        if (candidates == null) {
            return;
        }
        for (PeerInfo candidate : candidates) {
            if (candidate == null
                    || peerDirectoryService.isSelfPeer(candidate)
                    || !candidate.isOnline()
                    || candidate.getHost() == null || candidate.getHost().isBlank()
                    || candidate.getPort() <= 0) {
                continue;
            }
            String key = (candidate.getId() == null || candidate.getId().isBlank())
                    ? candidate.addressKey()
                    : candidate.getId();
            targets.put(key, candidate);
        }
    }
}
