package dungcony.ds.bootstrap;

public class HeartbeatMonitor implements Runnable {
    private final PeerRegistry registry;
    private volatile boolean running = true;

    public HeartbeatMonitor(PeerRegistry registry) {
        this.registry = registry;
    }

    /**
     * Vòng lặp nền dành cho việc kiểm tra heartbeat định kỳ của registry.
     */
    @Override
    public void run() {
        System.out.println("[INFO] HeartbeatMonitor started.");
        while (running) {
            try {
                Thread.sleep(10_000);
                System.out.println("[TRACE] HeartbeatMonitor tick. Registry size=" + registry.list().size());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
            }
        }
        System.out.println("[INFO] HeartbeatMonitor stopped.");
    }

    /**
     * Dừng vòng lặp monitor.
     */
    public void stop() {
        running = false;
        System.out.println("[INFO] HeartbeatMonitor stop requested.");
    }

    /**
     * Trả về registry mà monitor đang giám sát.
     */
    public PeerRegistry getRegistry() {
        return registry;
    }
}
