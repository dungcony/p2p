package dungcony.ds;

import dungcony.ds.config.Config;
import dungcony.ds.models.BootstrapServer;

public class App {
    // Entry point riêng của module bootstrap-server.
    public static void main(String[] args) {
        Config config = Config.load();
        int port = args.length > 0 ? Integer.parseInt(args[0]) : config.getServerPort();
        new BootstrapServer(port, config.getDatabasePath()).start();
    }
}
