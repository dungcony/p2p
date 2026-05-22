package dungcony.ds.services;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dungcony.ds.interfaces.LanDiscoveryService;
import dungcony.ds.interfaces.PeerDirectoryService;
import dungcony.ds.model.Message;
import dungcony.ds.model.MessageSender;
import dungcony.ds.model.PeerInfo;

import java.util.ArrayList;
import java.util.List;

public class LanDiscoveryImpl implements LanDiscoveryService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(LanDiscoveryImpl.class);
private final PeerInfo localPeer;
    private final MessageSender messageSender;
    private final PeerDirectoryService peerDirectoryService;

    // Khởi tạo service scan LAN bằng heartbeat.
    public LanDiscoveryImpl(PeerInfo localPeer, MessageSender messageSender, PeerDirectoryService peerDirectoryService) {
        this.localPeer = localPeer;
        this.messageSender = messageSender;
        this.peerDirectoryService = peerDirectoryService;
    }

    // Quét subnet LAN hiện tại bằng heartbeat để tìm các peer đang chạy cùng port.
    @Override
    public List<PeerInfo> discoverPeersOnLocalNetwork() {
        List<PeerInfo> discovered = new ArrayList<>();
        String localHost = localPeer.getHost();
        int lastDot = localHost.lastIndexOf('.');
        if (lastDot < 0) {
            LOGGER.warn("Không thể khám phá peer. Host local không phải địa chỉ IPv4 LAN: " + localHost);
            return discovered;
        }

        String prefix = localHost.substring(0, lastDot + 1);
        LOGGER.info("Đang khởi động khám phá LAN trên subnet " + prefix + "0/24 bằng cổng " + localPeer.getPort());
        List<Thread> probes = new ArrayList<>();
        for (int i = 1; i <= 254; i++) {
            String host = prefix + i;
            if (host.equals(localHost)) {
                continue;
            }
            Thread probe = new Thread(() -> probeHost(host, discovered), "PeerProbe-" + host);
            probe.setDaemon(true);
            probes.add(probe);
            probe.start();
        }

        for (Thread probe : probes) {
            try {
                probe.join(3500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        LOGGER.info("Quét LAN xong. sốPeerTìmThấy=" + discovered.size());
        return discovered;
    }

    // Gửi heartbeat đến một host trong subnet.
    private void probeHost(String host, List<PeerInfo> discovered) {
        PeerInfo peerInfo = new PeerInfo(host, host, host, localPeer.getPort());
        if (messageSender.send(peerInfo, Message.heartbeat(localPeer))) {
            peerDirectoryService.put(peerInfo);
            LOGGER.info("Đã phát hiện peer " + peerInfo.addressKey());
            synchronized (discovered) {
                discovered.add(peerInfo);
            }
        }
    }
}
