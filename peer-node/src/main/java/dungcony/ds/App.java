package dungcony.ds;

import dungcony.ds.config.PeerConfig;
import dungcony.ds.dtos.ProfileSelection;
import dungcony.ds.interfaces.ProfileSelectionService;
import dungcony.ds.peer.PeerNode;
import dungcony.ds.services.ProfileSelectionImpl;
import dungcony.ds.ui.LoginDialog;
import dungcony.ds.ui.Main;
import dungcony.ds.ui.PeerPortDialog;

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
        RuntimeOptions runtimeOptions = resolveRuntimeOptions(args);
        Path dataRoot = runtimeOptions.dataRoot();
        SwingUtilities.invokeLater(() -> {
            ProfileSelectionService profileSelectionService = new ProfileSelectionImpl(dataRoot);
            ProfileSelection selection = profileSelectionService.selectProfile();
            if (selection == null) {
                System.out.println("[INFO] Profile selection cancelled. Application will not start PeerNode.");
                return;
            }

            PeerConfig config = selection.config();
            config.applyRuntimePeerPort(runtimeOptions.peerPort());
            if (selection.newProfile() && runtimeOptions.peerPort() == null) {
                PeerPortDialog peerPortDialog = new PeerPortDialog(config.getPeerPort(), config.getBootstrapPort());
                peerPortDialog.setVisible(true);
                if (!peerPortDialog.isConfirmed()) {
                    System.out.println("[INFO] Peer port selection cancelled. Application will not start PeerNode.");
                    return;
                }
                if (!config.updatePeerPort(peerPortDialog.getPeerPort())) {
                    System.out.println("[WARN] Peer port selection failed validation. Application will not start PeerNode.");
                    return;
                }
                config.save();
            }
            if (selection.editBeforeStart()) {
                LoginDialog loginDialog = new LoginDialog(config.getPeerId(), config.getPeerName());
                loginDialog.setVisible(true);
                if (!loginDialog.isConfirmed()) {
                    System.out.println("[INFO] Profile edit cancelled. Application will not start PeerNode.");
                    return;
                }
                config.updateIdentity(loginDialog.getPeerId(), loginDialog.getPeerName());
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
     * Doc tham so runtime: --data-dir va --peer-port.
     */
    private static RuntimeOptions resolveRuntimeOptions(String[] args) {
        Path defaultDataRoot = Path.of("peer-node", "src", "main", "resources", "data");
        Path dataRoot = defaultDataRoot;
        Integer peerPort = null;
        if (args == null) {
            return new RuntimeOptions(dataRoot, peerPort);
        }

        for (int index = 0; index < args.length; index++) {
            String arg = args[index];
            if (arg == null || arg.isBlank()) {
                continue;
            }
            if (arg.startsWith("--data-dir=")) {
                dataRoot = Path.of(arg.substring("--data-dir=".length()));
                System.out.println("[INFO] Runtime dataRoot=" + dataRoot.toAbsolutePath());
                continue;
            }
            if ("--data-dir".equals(arg) && index + 1 < args.length) {
                dataRoot = Path.of(args[index + 1]);
                System.out.println("[INFO] Runtime dataRoot=" + dataRoot.toAbsolutePath());
                index++;
                continue;
            }
            if (arg.startsWith("--peer-port=") || arg.startsWith("--port=")) {
                String value = arg.contains("--peer-port=")
                        ? arg.substring("--peer-port=".length())
                        : arg.substring("--port=".length());
                peerPort = parsePeerPort(value);
                continue;
            }
            if (("--peer-port".equals(arg) || "--port".equals(arg)) && index + 1 < args.length) {
                peerPort = parsePeerPort(args[index + 1]);
                index++;
            }
        }

        System.out.println("[INFO] Runtime dataRoot=" + dataRoot.toAbsolutePath());
        if (peerPort != null) {
            System.out.println("[INFO] Runtime peerPort=" + peerPort);
        }
        return new RuntimeOptions(dataRoot, peerPort);
    }

    /**
     * Parse peer port tu CLI, tra null neu value khong hop le.
     */
    private static Integer parsePeerPort(String value) {
        try {
            int port = Integer.parseInt(value);
            if (port < 1 || port > 65535) {
                throw new NumberFormatException("Port out of range");
            }
            return port;
        } catch (NumberFormatException e) {
            System.out.println("[WARN] Ignored invalid --peer-port value=" + value);
            return null;
        }
    }

    private record RuntimeOptions(Path dataRoot, Integer peerPort) {
    }
}
