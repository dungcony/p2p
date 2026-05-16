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
                System.out.println("[INFO] Đã hủy chọn profile. Ứng dụng sẽ không khởi động PeerNode.");
                return;
            }

            PeerConfig config = selection.config();
            config.applyRuntimePeerPort(runtimeOptions.peerPort());
            if (selection.newProfile() && runtimeOptions.peerPort() == null) {
                PeerPortDialog peerPortDialog = new PeerPortDialog(config.getPeerPort(), config.getBootstrapPort());
                peerPortDialog.setVisible(true);
                if (!peerPortDialog.isConfirmed()) {
                    System.out.println("[INFO] Đã hủy chọn cổng peer. Ứng dụng sẽ không khởi động PeerNode.");
                    return;
                }
                if (!config.updatePeerPort(peerPortDialog.getPeerPort())) {
                    System.out.println("[WARN] Cổng peer không hợp lệ. Ứng dụng sẽ không khởi động PeerNode.");
                    return;
                }
                config.save();
            }
            if (selection.editBeforeStart()) {
                LoginDialog loginDialog = new LoginDialog(config.getPeerId(), config.getPeerName());
                loginDialog.setVisible(true);
                if (!loginDialog.isConfirmed()) {
                    System.out.println("[INFO] Đã hủy sửa profile. Ứng dụng sẽ không khởi động PeerNode.");
                    return;
                }
                config.updateIdentity(loginDialog.getPeerId(), loginDialog.getPeerName());
                config.save();
            } else {
                System.out.println("[INFO] Đang khởi động bằng profile đã chọn, không chỉnh sửa. "
                        + config.getDisplayLabel());
                config.save();
            }

            System.out.println("[INFO] Đang khởi động PeerNode với tên=" + config.getPeerName()
                    + ", cổng=" + config.getPeerPort()
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

            System.out.println("[INFO] Đang mở cửa sổ chat chính.");
            Main mainWindow = new Main();
            mainWindow.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    System.out.println("[INFO] Cửa sổ chính đang đóng. Đang dừng PeerNode.");
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
                System.out.println("[INFO] Thư mục dữ liệu runtime=" + dataRoot.toAbsolutePath());
                continue;
            }
            if ("--data-dir".equals(arg) && index + 1 < args.length) {
                dataRoot = Path.of(args[index + 1]);
                System.out.println("[INFO] Thư mục dữ liệu runtime=" + dataRoot.toAbsolutePath());
                index++;
                continue;
            }
            if (arg.startsWith("--peer-cổng=") || arg.startsWith("--cổng=")) {
                String value = arg.contains("--peer-cổng=")
                        ? arg.substring("--peer-cổng=".length())
                        : arg.substring("--cổng=".length());
                peerPort = parsePeerPort(value);
                continue;
            }
            if (("--peer-port".equals(arg) || "--port".equals(arg)) && index + 1 < args.length) {
                peerPort = parsePeerPort(args[index + 1]);
                index++;
            }
        }

        System.out.println("[INFO] Thư mục dữ liệu runtime=" + dataRoot.toAbsolutePath());
        if (peerPort != null) {
            System.out.println("[INFO] Cổng peer runtime=" + peerPort);
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
            System.out.println("[WARN] Đã bỏ qua giá trị --peer-port không hợp lệ=" + value);
            return null;
        }
    }

    private record RuntimeOptions(Path dataRoot, Integer peerPort) {
    }
}
