package dungcony.ds.services.impl.peer;

import dungcony.ds.enums.MessageType;
import dungcony.ds.model.Message;
import dungcony.ds.model.PeerInfo;
import dungcony.ds.services.interfaces.messaging.PeerMessageSender;
import dungcony.ds.services.interfaces.peer.PeerDirectoryService;
import dungcony.ds.services.interfaces.peer.PeerDiscoverService;
import dungcony.ds.utils.GroupConverstation;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
// Service xử lý peer discovery trực tiếp bằng PEER_LIST_REQUEST giữa các peer
public class PeerDiscoverImpl implements PeerDiscoverService {

    private final PeerInfo localPeer;
    private final PeerMessageSender messageSender;
    private final PeerDirectoryService peerDirectoryService;
    private final Runnable peerChangeNotifier;

    // Khởi tạo service discovery với peer local, sender và danh bạ runtime
    public PeerDiscoverImpl(PeerInfo localPeer,
                            PeerMessageSender messageSender,
                            PeerDirectoryService peerDirectoryService,
                            Runnable peerChangeNotifier) {
        this.localPeer = localPeer;
        this.messageSender = messageSender;
        this.peerDirectoryService = peerDirectoryService;
        this.peerChangeNotifier = peerChangeNotifier;
    }

    // Hỏi một peer đã biết danh sách peer mà nó đang biết để fallback khi bootstrap không sẵn sàng
    @Override
    public List<PeerInfo> discoverPeersFromKnownPeer(PeerInfo knownPeer) {
        if (knownPeer == null || peerDirectoryService.isSelfPeer(knownPeer)) {
            return List.of();
        }
        log.info("Đang hỏi peer đã biết danh sách peer khác. peer={}", knownPeer.addressKey());
        Message request = Message.peerListRequest(localPeer, knownPeer);
        Message response = messageSender.sendForResponse(knownPeer, request, MessageType.PEER_LIST_RESPONSE);
        if (response == null) {
            knownPeer.setOnline(false);
            log.warn("Không nhận được PEER_LIST_RESPONSE từ peer={}", knownPeer.addressKey());
            peerChangeNotifier.run();
            return List.of();
        }
        knownPeer.setOnline(true);
        int merged = onPeerListResponse(response);
        List<PeerInfo> discoveredPeers = extractDiscoverablePeers(response);
        log.info("Đã nhận danh sách peer từ peer={}. sốPeer={}, sốPeerMerge={}",
                knownPeer.addressKey(), discoveredPeers.size(), merged);
        peerChangeNotifier.run();
        return discoveredPeers;
    }

    // Tạo response gồm peer local và toàn bộ danh bạ runtime hiện tại
    @Override
    public Message buildPeerListResponse(Message request) {
        List<PeerInfo> knownPeers = new ArrayList<>();
        knownPeers.add(localPeer);
        knownPeers.addAll(peerDirectoryService.list());
        log.info("Trả danh sách peer cho request id={}, sốPeer={}", request.getId(), knownPeers.size());
        return Message.peerListResponse(localPeer, request, knownPeers);
    }

    // Xử lý PEER_LIST_RESPONSE bằng cách merge sender và danh sách peer vào danh bạ
    @Override
    public int onPeerListResponse(Message response) {
        if (response == null || response.getType() != MessageType.PEER_LIST_RESPONSE) {
            return 0;
        }
        PeerInfo sender = peerDirectoryService.mergeSenderFromKnownPeers(response);
        sender.setOnline(true);
        peerDirectoryService.put(sender);
        int merged = peerDirectoryService.mergeKnownPeers(response.getPeers());
        log.info("Đã xử lý PEER_LIST_RESPONSE. sender={}, sốPeerMerge={}", sender.addressKey(), merged);
        peerChangeNotifier.run();
        return merged;
    }

    // Lọc bỏ peer null, peer local và peer ảo đại diện group khỏi kết quả discovery
    private List<PeerInfo> extractDiscoverablePeers(Message response) {
        if (response == null || response.getPeers() == null) {
            return List.of();
        }
        List<PeerInfo> discoveredPeers = new ArrayList<>();
        for (PeerInfo peerInfo : response.getPeers()) {
            if (peerInfo == null
                    || peerDirectoryService.isSelfPeer(peerInfo)
                    || GroupConverstation.isGroupHost(peerInfo.getHost())) {
                continue;
            }
            discoveredPeers.add(peerInfo);
        }
        return discoveredPeers;
    }
}
