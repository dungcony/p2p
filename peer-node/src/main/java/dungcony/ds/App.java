package dungcony.ds;

import dungcony.ds.config.PeerConfig;
import dungcony.ds.dtos.ProfileSelection;
import dungcony.ds.interfaces.ProfileSelectionService;
import dungcony.ds.peer.PeerNode;
import dungcony.ds.services.ProfileSelectionImpl;
import dungcony.ds.ui.LoginDialog;
import dungcony.ds.ui.Main;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;

public class App {
    public static PeerNode peerNode;

    /**
     * Điểm vào của ứng dụng: lấy thông tin peer từ LoginDialog, khởi động PeerNode,
     * sau đó mở cửa sổ chat chính.
     */
    public static void main(String[] args) {
        Path dataRoot = resolveDataRoot(args);
        SwingUtilities.invokeLater(() -> {
            ProfileSelectionService profileSelectionService = new ProfileSelectionImpl(dataRoot);
            ProfileSelection selection = profileSelectionService.selectProfile();
            if (selection == null) {
                System.out.println("[INFO] Profile selection cancelled. Application will not start PeerNode.");
                return;
            }

            PeerConfig config = selection.config();
            if (selection.editBeforeStart()) {
                LoginDialog loginDialog = new LoginDialog(config.getPeerId(), config.getPeerName(), config.getPeerPort());
                loginDialog.setVisible(true);
                if (!loginDialog.isConfirmed()) {
                    System.out.println("[INFO] Profile edit cancelled. Application will not start PeerNode.");
                    return;
                }
                config.updateLogin(loginDialog.getPeerId(), loginDialog.getPeerName(), loginDialog.getPeerPort());
                config.save();
            } else {
                System.out.println("[INFO] Starting with existing peer profile without edit. "
                        + config.getDisplayLabel());
                config.save();
            }

            System.out.println("[INFO] Starting PeerNode with name=" + config.getPeerName()
                    + ", port=" + config.getPeerPort()
                    + ", bootstrap=" + config.getBootstrapHost() + ":" + config.getBootstrapPort());
            peerNode = new PeerNode(
                    config.getPeerId(),
                    config.getPeerName(),
                    config.getPeerPort(),
                    config.getBootstrapHost(),
                    config.getBootstrapPort(),
                    config.getDataDir()
            );
            peerNode.start();

            System.out.println("[INFO] Opening main chat window.");
            Main mainWindow = new Main();
            mainWindow.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    System.out.println("[INFO] Main window closing. Stopping PeerNode.");
                    peerNode.stop();
                }
            });
            mainWindow.setVisible(true);
        });
    }

    /**
     * Doc tham so --data-dir de chon data root chua cac folder peer theo UUID.
     */
    private static Path resolveDataRoot(String[] args) {
        Path defaultDataRoot = Path.of("peer-node", "src", "main", "resources", "data");
        if (args == null) {
            return defaultDataRoot;
        }

        for (int index = 0; index < args.length; index++) {
            String arg = args[index];
            if (arg == null || arg.isBlank()) {
                continue;
            }
            if (arg.startsWith("--data-dir=")) {
                Path dataRoot = Path.of(arg.substring("--data-dir=".length()));
                System.out.println("[INFO] Runtime dataRoot=" + dataRoot.toAbsolutePath());
                return dataRoot;
            }
            if ("--data-dir".equals(arg) && index + 1 < args.length) {
                Path dataRoot = Path.of(args[index + 1]);
                System.out.println("[INFO] Runtime dataRoot=" + dataRoot.toAbsolutePath());
                return dataRoot;
            }
        }

        System.out.println("[INFO] Runtime dataRoot=" + defaultDataRoot.toAbsolutePath());
        return defaultDataRoot;
    }
}
