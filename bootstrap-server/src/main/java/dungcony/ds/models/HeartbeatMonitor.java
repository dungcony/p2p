package dungcony.ds.models;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class HeartbeatMonitor implements Runnable {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(HeartbeatMonitor.class);
private final PeerRegistry registry;
    private volatile boolean running = true;

    public HeartbeatMonitor(PeerRegistry registry) {
        this.registry = registry;
    }

    // Vòng lặp nền dành cho việc kiểm tra heartbeat định kỳ của registry.
    @Override
    public void run() {
        LOGGER.info("HeartbeatMonitor đã khởi động.");
        while (running) {
            try {
                Thread.sleep(10_000);
                LOGGER.trace("HeartbeatMonitor tick. kíchThướcRegistry=" + registry.list().size());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
            }
        }
        LOGGER.info("HeartbeatMonitor đã dừng.");
    }

    // Dừng vòng lặp monitor.
    public void stop() {
        running = false;
        LOGGER.info("Đã yêu cầu dừng HeartbeatMonitor.");
    }

    // Trả về registry mà monitor đang giám sát.
    public PeerRegistry getRegistry() {
        return registry;
    }
}
