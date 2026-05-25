package dungcony.ds;

import dungcony.ds.config.PeerProfile;
import dungcony.ds.config.RuntimeOption;
import dungcony.ds.dtos.ProfileSelection;
import dungcony.ds.repositories.PeerProfileRepository;
import dungcony.ds.app.PeerNode;
import dungcony.ds.services.impl.profile.ProfileSelectionImpl;
import dungcony.ds.services.interfaces.profile.ProfileSelectionService;
import dungcony.ds.ui.LoginDialog;
import dungcony.ds.ui.Main;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;

import static dungcony.ds.config.RuntimeOptionParser.resolveRuntimeOptions;

/**
 * Điểm khởi chạy ứng dụng desktop và điều phối vòng đời PeerNode
 */
@Slf4j
public class App {
    public static PeerNode peerNode;
    private static String startupErrorMessage;

    // Điểm vào của ứng dụng: lấy profile từ CLI hoặc dialog, khởi động PeerNode,
    // sau đó mở cửa sổ chat chính.
    public static void main(String[] args) {
        startupErrorMessage = null;
        RuntimeOption runtimeOptions = resolveRuntimeOptions(args);

        SwingUtilities.invokeLater(() -> {
            PeerProfileRepository profileRepository = new PeerProfileRepository();
            PeerProfile config = resolveStartupProfile(runtimeOptions, profileRepository);
            if (config == null) {
                if (startupErrorMessage != null) {
                    showUsage();
                }
                return;
            }
            profileRepository.save(config);

            log.info("Đang khởi động PeerNode với tên={}, cổng={}, bootstrap={}:{}", config.getPeerName(),
                    config.getPeerPort(), config.getBootstrapHost(), config.getBootstrapPort());
            peerNode = new PeerNode(
                    config.getPeerId(),
                    config.getPeerName(),
                    config.getPeerPort(),
                    config.getBootstrapHost(),
                    config.getBootstrapPort(),
                    config.getDataDir());
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

    private static PeerProfile resolveStartupProfile(RuntimeOption runtimeOptions,
            PeerProfileRepository profileRepository) {
        Path dataRoot = runtimeOptions.dataRoot();
        if (runtimeOptions.profileId() != null) {
            PeerProfile profile = profileRepository.loadById(dataRoot, runtimeOptions.profileId()).orElse(null);
            if (profile == null) {
                startupErrorMessage = "Không tìm thấy profile '%s'. Hãy tạo profile mới bằng tên hiển thị."
                        .formatted(runtimeOptions.profileId());
                log.error(startupErrorMessage);
                return null;
            }
            profile.applyRuntimePeerPort(runtimeOptions.peerPort());
            log.info("Đang khởi động bằng profile runtime. {}", profile.getDisplayLabel());
            return profile;
        }

        if (runtimeOptions.peerName() != null) {
            PeerProfile existingProfile = profileRepository.findByName(dataRoot, runtimeOptions.peerName())
                    .orElse(null);
            if (existingProfile != null) {
                existingProfile.applyRuntimePeerPort(runtimeOptions.peerPort());
                log.info("Đang khởi động bằng profile có sẵn theo tên. {}", existingProfile.getDisplayLabel());
                return existingProfile;
            }
            PeerProfile newProfile = profileRepository.createNewWithName(dataRoot, runtimeOptions.peerName());
            newProfile.applyRuntimePeerPort(runtimeOptions.peerPort());
            return newProfile;
        }

        return selectStartupProfileFromUi(runtimeOptions, profileRepository);
    }

    private static PeerProfile selectStartupProfileFromUi(RuntimeOption runtimeOptions,
            PeerProfileRepository profileRepository) {
        ProfileSelectionService profileSelectionService = new ProfileSelectionImpl(
                runtimeOptions.dataRoot(),
                profileRepository);
        ProfileSelection selection = profileSelectionService.selectProfile();
        if (selection == null) {
            log.info("Đã hủy chọn profile. Ứng dụng sẽ không khởi động PeerNode.");
            return null;
        }

        PeerProfile config = selection.config();
        config.applyRuntimePeerPort(runtimeOptions.peerPort());

        if (selection.editBeforeStart()) {
            LoginDialog loginDialog = new LoginDialog(config.getPeerId(), config.getPeerName());
            loginDialog.setVisible(true);
            if (!loginDialog.isConfirmed()) {
                log.info("Đã hủy sửa profile. Ứng dụng sẽ không khởi động PeerNode.");
                return null;
            }
            config.updateIdentity(loginDialog.getPeerId(), loginDialog.getPeerName());
        } else {
            log.info("Đang khởi động bằng profile đã chọn. {}", config.getDisplayLabel());
        }
        return config;
    }

    private static void showUsage() {
        String errorMessage = startupErrorMessage == null ? "Thiếu tham số khởi động peer." : startupErrorMessage;
        JOptionPane.showMessageDialog(
                null,
                """
                        %s

                        Ví dụ dùng profile có sẵn:
                          run.bat --profile=alice

                        Ví dụ tạo/chạy bằng tên:
                          run.bat --peer-name=Dave
                        """.formatted(errorMessage),
                "P2P Chat",
                JOptionPane.ERROR_MESSAGE);
    }

}
