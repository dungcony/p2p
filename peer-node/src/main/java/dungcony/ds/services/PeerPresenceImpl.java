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
     * Kiem tra peer online qua bootstrap LIST neu co, fallback heartbeat truc tiep neu chay khong co bootstrap.
     */
    @Override
    public boolean checkUserIsOnline(String hostAndMaybePort) {
        PeerInfo peerInfo = peerDirectoryService.resolvePeer(hostAndMaybePort);
        if (peerInfo == null) {
            System.out.println("[WARN] Cannot check online status. Peer address is empty.");
            return false;
        }
        if (peerDirectoryService.isSelfPeer(peerInfo)) {
            System.out.println("[WARN] Refusing online check for local peer: " + peerInfo.addressKey());
            return false;
        }
        boolean online = bootstrapClient == null
                ? checkByDirectHeartbeat(peerInfo)
                : checkByBootstrap(peerInfo);
        peerInfo.setOnline(online);
        peerChangeNotifier.run();
        return online;
    }

    /**
     * Hoi bootstrap-server danh sach peer online va so khop theo id hoac address.
     */
    private boolean checkByBootstrap(PeerInfo targetPeer) {
        System.out.println("[DEBUG] Checking peer online status via bootstrap. target=" + targetPeer.addressKey());
        for (PeerInfo onlinePeer : bootstrapClient.list()) {
            if (onlinePeer == null || peerDirectoryService.isSelfPeer(onlinePeer)) {
                continue;
            }
            peerDirectoryService.put(onlinePeer);
            if (isSamePeer(targetPeer, onlinePeer)) {
                System.out.println("[INFO] Bootstrap reports peer online. target=" + targetPeer.addressKey()
                        + ", matched=" + onlinePeer.addressKey());
                return true;
            }
        }
        System.out.println("[INFO] Bootstrap reports peer offline. target=" + targetPeer.addressKey());
        return false;
    }

    /**
     * Gui heartbeat truc tiep khi peer-node khong cau hinh bootstrap-server.
     */
    private boolean checkByDirectHeartbeat(PeerInfo peerInfo) {
        System.out.println("[DEBUG] Bootstrap disabled. Sending direct heartbeat to " + peerInfo.addressKey());
        boolean online = messageSender.send(peerInfo, Message.heartbeat(localPeer));
        System.out.println("[INFO] Direct heartbeat result. peer=" + peerInfo.addressKey() + ", online=" + online);
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
