package dungcony.ds.models;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class HeartbeatMonitor implements Runnable {
private final PeerRegistry registry;
    private volatile boolean running = true;

    public HeartbeatMonitor(PeerRegistry registry) {
        this.registry = registry;
    }

    // Vòng lặp nền dành cho việc kiểm tra heartbeat định kỳ của registry.
    @Override
    public void run() {
        log.info("HeartbeatMonitor đã khởi động.");
        while (running) {
            try {
                Thread.sleep(10_000);
                log.trace("HeartbeatMonitor tick. kíchThướcRegistry=" + registry.list().size());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
            }
        }
        log.info("HeartbeatMonitor đã dừng.");
    }

    // Dừng vòng lặp monitor.
    public void stop() {
        running = false;
        log.info("Đã yêu cầu dừng HeartbeatMonitor.");
    }

    // Trả về registry mà monitor đang giám sát.
    public PeerRegistry getRegistry() {
        return registry;
    }
}
