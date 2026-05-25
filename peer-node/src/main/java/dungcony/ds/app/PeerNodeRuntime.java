package dungcony.ds.app;

import dungcony.ds.model.PeerInfo;
import dungcony.ds.network.TCPServer;
import dungcony.ds.services.interfaces.bootstrap.PeerBootstrapGateway;
import dungcony.ds.services.interfaces.bootstrap.BootstrapSyncService;
import lombok.extern.slf4j.Slf4j;

/**
 * Quản lý vòng đời TCP server và đồng bộ bootstrap của peer
 */
@Slf4j
public class PeerNodeRuntime {
    private static final long BOOTSTRAP_REFRESH_INTERVAL_MS = 5000;

    private final PeerInfo localPeer;
    private final TCPServer tcpServer;
    private final PeerBootstrapGateway bootstrapGateway;
    private final BootstrapSyncService bootstrapSyncService;
    private volatile boolean running;

    public PeerNodeRuntime(PeerInfo localPeer,
                           TCPServer tcpServer,
                           PeerBootstrapGateway bootstrapGateway,
                           BootstrapSyncService bootstrapSyncService) {
        this.localPeer = localPeer;
        this.tcpServer = tcpServer;
        this.bootstrapGateway = bootstrapGateway;
        this.bootstrapSyncService = bootstrapSyncService;
    }

    public void start() {
        log.info("Đang khởi động bộ lắng nghe TCP cho peer local {}", localPeer.addressKey());
        running = true;
        Thread serverThread = new Thread(tcpServer::listen, "PeerNode-TCPServer-" + localPeer.getPort());
        serverThread.setDaemon(true);
        serverThread.start();
        if (bootstrapSyncService != null) {
            Thread bootstrapThread = new Thread(this::runBootstrapSyncLoop, "PeerNode-Bootstrap");
            bootstrapThread.setDaemon(true);
            bootstrapThread.start();
        }
    }

    public void stop() {
        log.info("Đang dừng PeerNode {}", localPeer.addressKey());
        running = false;
        if (bootstrapGateway != null) {
            bootstrapGateway.leave(localPeer.addressKey());
        }
        tcpServer.stop();
    }

    private void runBootstrapSyncLoop() {
        bootstrapSyncService.registerAndJoinBootstrap();
        while (running) {
            try {
                Thread.sleep(BOOTSTRAP_REFRESH_INTERVAL_MS);
                bootstrapSyncService.refreshFromBootstrap();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (RuntimeException e) {
                log.error("Vòng refresh bootstrap lỗi: {}", e.getMessage());
            }
        }
    }
}
