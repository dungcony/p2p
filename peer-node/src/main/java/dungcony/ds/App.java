package dungcony.ds;

import dungcony.ds.config.PeerConfig;
import dungcony.ds.dtos.ProfileSelection;
import dungcony.ds.model.PeerNode;
import dungcony.ds.services.impl.profile.ProfileSelectionImpl;
import dungcony.ds.services.interfaces.profile.ProfileSelectionService;
import dungcony.ds.ui.LoginDialog;
import dungcony.ds.ui.Main;
import dungcony.ds.ui.PeerPortDialog;
import dungcony.ds.utils.RuntimeOption;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;

import static dungcony.ds.utils.ProfileRead.resolveRuntimeOptions;

@Slf4j
public class App {
    public static PeerNode peerNode;

    // Điểm vào của ứng dụng: lấy thông tin peer từ LoginDialog, khởi động PeerNode,
    // sau đó mở cửa sổ chat chính
    public static void main(String[] args) {
        RuntimeOption runtimeOptions = resolveRuntimeOptions(args);
        Path dataRoot = runtimeOptions.dataRoot();

        SwingUtilities.invokeLater(() -> {
            ProfileSelectionService profileSelectionService = new ProfileSelectionImpl(dataRoot);
            ProfileSelection selection = profileSelectionService.selectProfile();
            if (selection == null) {
                log.info("Đã hủy chọn profile. Ứng dụng sẽ không khởi động PeerNode.");
                return;
            }

            PeerConfig config = selection.config();
            config.applyRuntimePeerPort(runtimeOptions.peerPort());
            if (selection.newProfile() && runtimeOptions.peerPort() == null) {
                PeerPortDialog peerPortDialog = new PeerPortDialog(config.getPeerPort(), config.getBootstrapPort());
                peerPortDialog.setVisible(true);
                if (!peerPortDialog.isConfirmed()) {
                    log.info("Đã hủy chọn cổng peer. Ứng dụng sẽ không khởi động PeerNode.");
                    return;
                }
                if (!config.updatePeerPort(peerPortDialog.getPeerPort())) {
                    log.warn("Cổng peer không hợp lệ. Ứng dụng sẽ không khởi động PeerNode.");
                    return;
                }
                config.save();
            }
            if (selection.editBeforeStart()) {
                LoginDialog loginDialog = new LoginDialog(config.getPeerId(), config.getPeerName());
                loginDialog.setVisible(true);
                if (!loginDialog.isConfirmed()) {
                    log.info("Đã hủy sửa profile. Ứng dụng sẽ không khởi động PeerNode.");
                    return;
                }
                config.updateIdentity(loginDialog.getPeerId(), loginDialog.getPeerName());
                config.save();
            } else {
                log.info("Đang khởi động bằng profile đã chọn, không chỉnh sửa. {}", config.getDisplayLabel());
                config.save();
            }

            log.info("Đang khởi động PeerNode với tên={}, cổng={}, bootstrap={}:{}", config.getPeerName(), config.getPeerPort(), config.getBootstrapHost(), config.getBootstrapPort());
            peerNode = new PeerNode(
                    config.getPeerId(),
                    config.getPeerName(),
                    config.getPeerPort(),
                    config.getBootstrapHost(),
                    config.getBootstrapPort(),
                    config.getDataDir()
            );
            peerNode.start();

            log.info("Đang mở cửa sổ chat chính.");
            Main mainWindow = new Main();
            mainWindow.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    log.info("Cửa sổ chính đang đóng. Đang dừng PeerNode.");
                    peerNode.stop();
                }
            });
            mainWindow.setVisible(true);
        });
    }


}
