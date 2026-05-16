package dungcony.ds.services;

import dungcony.ds.interfaces.LanDiscoveryService;
import dungcony.ds.interfaces.PeerDirectoryService;
import dungcony.ds.model.Message;
import dungcony.ds.model.PeerInfo;
import dungcony.ds.peer.MessageSender;

import java.util.ArrayList;
import java.util.List;

public class LanDiscoveryImpl implements LanDiscoveryService {
    private final PeerInfo localPeer;
    private final MessageSender messageSender;
    private final PeerDirectoryService peerDirectoryService;

    /**
     * Khoi tao service scan LAN bang heartbeat.
     */
    public LanDiscoveryImpl(PeerInfo localPeer, MessageSender messageSender, PeerDirectoryService peerDirectoryService) {
        this.localPeer = localPeer;
        this.messageSender = messageSender;
        this.peerDirectoryService = peerDirectoryService;
    }

    /**
     * Quét subnet LAN hiện tại bằng heartbeat để tìm các peer đang chạy cùng port.
     */
    @Override
    public List<PeerInfo> discoverPeersOnLocalNetwork() {
        List<PeerInfo> discovered = new ArrayList<>();
        String localHost = localPeer.getHost();
        int lastDot = localHost.lastIndexOf('.');
        if (lastDot < 0) {
            System.out.println("[WARN] Cannot discover peers. Local host is not an IPv4 LAN address: " + localHost);
            return discovered;
        }

        String prefix = localHost.substring(0, lastDot + 1);
        System.out.println("[INFO] Starting LAN discovery on subnet " + prefix + "0/24 using port " + localPeer.getPort());
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
        System.out.println("[INFO] LAN discovery completed. Found peers=" + discovered.size());
        return discovered;
    }

    /**
     * Gui heartbeat den mot host trong subnet.
     */
    private void probeHost(String host, List<PeerInfo> discovered) {
        PeerInfo peerInfo = new PeerInfo(host, host, host, localPeer.getPort());
        if (messageSender.send(peerInfo, Message.heartbeat(localPeer))) {
            peerDirectoryService.put(peerInfo);
            System.out.println("[INFO] Discovered peer " + peerInfo.addressKey());
            synchronized (discovered) {
                discovered.add(peerInfo);
            }
        }
    }
}
