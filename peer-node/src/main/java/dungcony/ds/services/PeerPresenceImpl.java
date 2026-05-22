package dungcony.ds.services;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dungcony.ds.interfaces.PeerDirectoryService;
import dungcony.ds.interfaces.PeerPresenceService;
import dungcony.ds.model.BootstrapClient;
import dungcony.ds.model.Message;
import dungcony.ds.model.MessageSender;
import dungcony.ds.model.PeerInfo;

public class PeerPresenceImpl implements PeerPresenceService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PeerPresenceImpl.class);
private final PeerInfo localPeer;
    private final MessageSender messageSender;
    private final BootstrapClient bootstrapClient;
    private final PeerDirectoryService peerDirectoryService;
    private final Runnable peerChangeNotifier;

    // Khởi tạo service quản lý trạng thái online/offline của peer.
    public PeerPresenceImpl(PeerInfo localPeer, MessageSender messageSender, BootstrapClient bootstrapClient,
                            PeerDirectoryService peerDirectoryService, Runnable peerChangeNotifier) {
        this.localPeer = localPeer;
        this.messageSender = messageSender;
        this.bootstrapClient = bootstrapClient;
        this.peerDirectoryService = peerDirectoryService;
        this.peerChangeNotifier = peerChangeNotifier;
    }

    // Kiểm tra peer online qua bootstrap LIST nếu có, fallback heartbeat trực tiếp khi bootstrap không thấy peer.
    @Override
    public boolean checkUserIsOnline(String hostAndMaybePort) {
        PeerInfo peerInfo = peerDirectoryService.resolvePeer(hostAndMaybePort);
        if (peerInfo == null) {
            LOGGER.warn("Không thể kiểm tra trạng thái online. Địa chỉ peer rỗng.");
            return false;
        }
        if (peerDirectoryService.isSelfPeer(peerInfo)) {
            LOGGER.warn("Từ chối kiểm tra online với peer local: " + peerInfo.addressKey());
            return false;
        }
        boolean online = bootstrapClient != null && checkByBootstrap(peerInfo);
        if (!online) {
            online = checkByDirectHeartbeat(peerInfo);
        }
        peerInfo.setOnline(online);
        peerChangeNotifier.run();
        return online;
    }

    // Hỏi bootstrap-server danh sách peer online và so khớp theo id hoặc address.
    private boolean checkByBootstrap(PeerInfo targetPeer) {
        LOGGER.debug("Đang kiểm tra trạng thái online qua bootstrap. target=" + targetPeer.addressKey());
        java.util.Collection<PeerInfo> onlinePeers = bootstrapClient.listOrNull();
        if (onlinePeers == null) {
            LOGGER.warn("Bootstrap không khả dụng khi kiểm tra online. Chuyển sang heartbeat trực tiếp. target="
                    + targetPeer.addressKey());
            return false;
        }
        for (PeerInfo onlinePeer : onlinePeers) {
            if (onlinePeer == null || peerDirectoryService.isSelfPeer(onlinePeer)) {
                continue;
            }
            peerDirectoryService.put(onlinePeer);
            if (isSamePeer(targetPeer, onlinePeer)) {
                LOGGER.info("Bootstrap xác nhận peer online. target=" + targetPeer.addressKey()
                        + ", matched=" + onlinePeer.addressKey());
                return true;
            }
        }
        LOGGER.info("Bootstrap chưa xác nhận peer online. Chuyển sang heartbeat trực tiếp. target="
                + targetPeer.addressKey());
        return false;
    }

    // Gửi heartbeat trực tiếp tới host:port để xác minh peer có TCP reachable không.
    private boolean checkByDirectHeartbeat(PeerInfo peerInfo) {
        LOGGER.debug("Đang gửi heartbeat trực tiếp tới " + peerInfo.addressKey());
        boolean online = messageSender.send(peerInfo, Message.heartbeat(localPeer));
        if (online) {
            peerInfo.setOnline(true);
            peerDirectoryService.put(peerInfo);
        }
        LOGGER.info("Kết quả heartbeat trực tiếp. peer=" + peerInfo.addressKey() + ", online=" + online);
        return online;
    }

    // So khớp peer theo user_id nếu có, nếu không thì so khớp bằng host:port.
    private boolean isSamePeer(PeerInfo targetPeer, PeerInfo onlinePeer) {
        if (targetPeer.getId() != null && !targetPeer.getId().isBlank()
                && targetPeer.getId().equals(onlinePeer.getId())) {
            return true;
        }
        return targetPeer.addressKey().equals(onlinePeer.addressKey());
    }
}
