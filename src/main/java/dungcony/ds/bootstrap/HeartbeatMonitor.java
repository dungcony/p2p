package dungcony.ds.bootstrap;

public class HeartbeatMonitor implements Runnable {
    private final PeerRegistry registry;
    private volatile boolean running = true;

    public HeartbeatMonitor(PeerRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void run() {
        while (running) {
            try {
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
            }
        }
    }

    public void stop() {
        running = false;
    }

    public PeerRegistry getRegistry() {
        return registry;
    }
}
