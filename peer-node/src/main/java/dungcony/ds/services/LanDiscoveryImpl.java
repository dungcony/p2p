package dungcony.ds.services;

import lombok.extern.slf4j.Slf4j;


import dungcony.ds.interfaces.LanDiscoveryService;
import dungcony.ds.interfaces.PeerDirectoryService;
import dungcony.ds.model.Message;
import dungcony.ds.model.MessageSender;
import dungcony.ds.model.PeerInfo;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class LanDiscoveryImpl implements LanDiscoveryService {
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
            log.warn("Không thể khám phá peer. Host local không phải địa chỉ IPv4 LAN: " + localHost);
            return discovered;
        }

        String prefix = localHost.substring(0, lastDot + 1);
        log.info("Đang khởi động khám phá LAN trên subnet " + prefix + "0/24 bằng cổng " + localPeer.getPort());
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
        log.info("Quét LAN xong. sốPeerTìmThấy=" + discovered.size());
        return discovered;
    }

    // Gửi heartbeat đến một host trong subnet.
    private void probeHost(String host, List<PeerInfo> discovered) {
        PeerInfo peerInfo = new PeerInfo(host, host, host, localPeer.getPort());
        if (messageSender.send(peerInfo, Message.heartbeat(localPeer))) {
            peerDirectoryService.put(peerInfo);
            log.info("Đã phát hiện peer " + peerInfo.addressKey());
            synchronized (discovered) {
                discovered.add(peerInfo);
            }
        }
    }
}
