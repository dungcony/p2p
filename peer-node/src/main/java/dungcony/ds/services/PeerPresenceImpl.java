package dungcony.ds.services;

import dungcony.ds.interfaces.PeerDirectoryService;
import dungcony.ds.interfaces.PeerPresenceService;
import dungcony.ds.model.BootstrapClient;
import dungcony.ds.model.Message;
import dungcony.ds.model.PeerInfo;
import dungcony.ds.peer.MessageSender;

public class PeerPresenceImpl implements PeerPresenceService {
    private final PeerInfo localPeer;
    private final MessageSender messageSender;
    private final BootstrapClient bootstrapClient;
    private final PeerDirectoryService peerDirectoryService;
    private final Runnable peerChangeNotifier;

    /**
     * Khoi tao service quan ly trang thai online/offline cua peer.
     */
    public PeerPresenceImpl(PeerInfo localPeer, MessageSender messageSender, BootstrapClient bootstrapClient,
                            PeerDirectoryService peerDirectoryService, Runnable peerChangeNotifier) {
        this.localPeer = localPeer;
        this.messageSender = messageSender;
        this.bootstrapClient = bootstrapClient;
        this.peerDirectoryService = peerDirectoryService;
        this.peerChangeNotifier = peerChangeNotifier;
    }

    /**
     * Kiem tra peer online qua bootstrap LIST neu co, fallback heartbeat truc tiep khi bootstrap khong thay peer.
     */
    @Override
    public boolean checkUserIsOnline(String hostAndMaybePort) {
        PeerInfo peerInfo = peerDirectoryService.resolvePeer(hostAndMaybePort);
        if (peerInfo == null) {
            System.out.println("[WARN] Không thể kiểm tra trạng thái online. Địa chỉ peer rỗng.");
            return false;
        }
        if (peerDirectoryService.isSelfPeer(peerInfo)) {
            System.out.println("[WARN] Từ chối kiểm tra online với peer local: " + peerInfo.addressKey());
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

    /**
     * Hoi bootstrap-server danh sach peer online va so khop theo id hoac address.
     */
    private boolean checkByBootstrap(PeerInfo targetPeer) {
        System.out.println("[DEBUG] Đang kiểm tra trạng thái online qua bootstrap. target=" + targetPeer.addressKey());
        java.util.Collection<PeerInfo> onlinePeers = bootstrapClient.listOrNull();
        if (onlinePeers == null) {
            System.out.println("[WARN] Bootstrap không khả dụng khi kiểm tra online. Chuyển sang heartbeat trực tiếp. target="
                    + targetPeer.addressKey());
            return false;
        }
        for (PeerInfo onlinePeer : onlinePeers) {
            if (onlinePeer == null || peerDirectoryService.isSelfPeer(onlinePeer)) {
                continue;
            }
            peerDirectoryService.put(onlinePeer);
            if (isSamePeer(targetPeer, onlinePeer)) {
                System.out.println("[INFO] Bootstrap xác nhận peer online. target=" + targetPeer.addressKey()
                        + ", matched=" + onlinePeer.addressKey());
                return true;
            }
        }
        System.out.println("[INFO] Bootstrap chưa xác nhận peer online. Chuyển sang heartbeat trực tiếp. target="
                + targetPeer.addressKey());
        return false;
    }

    /**
     * Gui heartbeat truc tiep toi host:port de xac minh peer co TCP reachable khong.
     */
    private boolean checkByDirectHeartbeat(PeerInfo peerInfo) {
        System.out.println("[DEBUG] Đang gửi heartbeat trực tiếp tới " + peerInfo.addressKey());
        boolean online = messageSender.send(peerInfo, Message.heartbeat(localPeer));
        if (online) {
            peerInfo.setOnline(true);
            peerDirectoryService.put(peerInfo);
        }
        System.out.println("[INFO] Kết quả heartbeat trực tiếp. peer=" + peerInfo.addressKey() + ", online=" + online);
        return online;
    }

    /**
     * So khop peer theo user_id neu co, neu khong thi so khop bang host:port.
     */
    private boolean isSamePeer(PeerInfo targetPeer, PeerInfo onlinePeer) {
        if (targetPeer.getId() != null && !targetPeer.getId().isBlank()
                && targetPeer.getId().equals(onlinePeer.getId())) {
            return true;
        }
        return targetPeer.addressKey().equals(onlinePeer.addressKey());
    }
}
