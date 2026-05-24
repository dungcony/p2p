package dungcony.ds.services.impl.peer;

import dungcony.ds.model.Message;
import dungcony.ds.model.PeerInfo;
import dungcony.ds.services.interfaces.bootstrap.BootstrapGateway;
import dungcony.ds.services.interfaces.messaging.PeerMessageSender;
import dungcony.ds.services.interfaces.peer.PeerDirectoryService;
import dungcony.ds.services.interfaces.peer.PeerPresenceService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
// Service kiểm tra và cập nhật trạng thái online/offline của peer
public class PeerPresenceImpl implements PeerPresenceService {

    private final PeerInfo localPeer;
    private final PeerMessageSender messageSender;
    private final BootstrapGateway bootstrapGateway;
    private final PeerDirectoryService peerDirectoryService;
    private final Runnable peerChangeNotifier;

    // Khởi tạo service quản lý trạng thái online/offline của peer
    public PeerPresenceImpl(PeerInfo localPeer, PeerMessageSender messageSender, BootstrapGateway bootstrapGateway,
                            PeerDirectoryService peerDirectoryService, Runnable peerChangeNotifier) {
        this.localPeer = localPeer;
        this.messageSender = messageSender;
        this.bootstrapGateway = bootstrapGateway;
        this.peerDirectoryService = peerDirectoryService;
        this.peerChangeNotifier = peerChangeNotifier;
    }

    // Kiểm tra peer online qua bootstrap LIST nếu có, fallback heartbeat trực tiếp khi bootstrap không thấy peer
    @Override
    public boolean checkUserIsOnline(String hostAndMaybePort) {
        PeerInfo peerInfo = peerDirectoryService.resolvePeer(hostAndMaybePort);
        if (peerInfo == null) {
            log.warn("Không thể kiểm tra trạng thái online. Địa chỉ peer rỗng.");
            return false;
        }
        if (peerDirectoryService.isSelfPeer(peerInfo)) {
            log.warn("Từ chối kiểm tra online với peer local: {}", peerInfo.addressKey());
            return false;
        }
        boolean online = bootstrapGateway != null && checkByBootstrap(peerInfo);
        if (!online) {
            online = checkByDirectHeartbeat(peerInfo);
        }
        peerInfo.setOnline(online);
        peerChangeNotifier.run();
        return online;
    }

    // Hỏi bootstrap-server danh sách peer online và so khớp theo id hoặc address
    private boolean checkByBootstrap(PeerInfo targetPeer) {
        log.debug("Đang kiểm tra trạng thái online qua bootstrap. target={}", targetPeer.addressKey());
        java.util.Collection<PeerInfo> onlinePeers = bootstrapGateway.listOrNull();
        if (onlinePeers == null) {
            log.warn("Bootstrap không khả dụng khi kiểm tra online. Chuyển sang heartbeat trực tiếp. target={}",
                    targetPeer.addressKey());
            return false;
        }
        for (PeerInfo onlinePeer : onlinePeers) {
            if (onlinePeer == null || peerDirectoryService.isSelfPeer(onlinePeer)) {
                continue;
            }
            peerDirectoryService.put(onlinePeer);
            if (isSamePeer(targetPeer, onlinePeer)) {
                log.info("Bootstrap xác nhận peer online. target={}, matched={}",
                        targetPeer.addressKey(), onlinePeer.addressKey());
                return true;
            }
        }
        log.info("Bootstrap chưa xác nhận peer online. Chuyển sang heartbeat trực tiếp. target={}",
                targetPeer.addressKey());
        return false;
    }

    // Gửi heartbeat trực tiếp tới host:port để xác minh peer có TCP reachable không
    private boolean checkByDirectHeartbeat(PeerInfo peerInfo) {
        log.debug("Đang gửi heartbeat trực tiếp tới {}", peerInfo.addressKey());
        boolean online = messageSender.send(peerInfo, Message.heartbeat(localPeer));
        if (online) {
            peerInfo.setOnline(true);
            peerDirectoryService.put(peerInfo);
        }
        log.info("Kết quả heartbeat trực tiếp. peer={}, online={}", peerInfo.addressKey(), online);
        return online;
    }

    // So khớp peer theo user_id nếu có, nếu không thì so khớp bằng host:port
    private boolean isSamePeer(PeerInfo targetPeer, PeerInfo onlinePeer) {
        if (targetPeer.getId() != null && !targetPeer.getId().isBlank()
                && targetPeer.getId().equals(onlinePeer.getId())) {
            return true;
        }
        return targetPeer.addressKey().equals(onlinePeer.addressKey());
    }
}
