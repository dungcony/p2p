package dungcony.ds;

import dungcony.ds.config.PeerConfig;
import dungcony.ds.peer.PeerNode;
import dungcony.ds.ui.LoginDialog;
import dungcony.ds.ui.Main;

import javax.swing.SwingUtilities;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.util.List;

public class App {
    public static PeerNode peerNode;

    /**
     * Điểm vào của ứng dụng: lấy thông tin peer từ LoginDialog, khởi động PeerNode,
     * sau đó mở cửa sổ chat chính.
     */
    public static void main(String[] args) {
        Path dataRoot = resolveDataRoot(args);
        SwingUtilities.invokeLater(() -> {
            ProfileSelection selection = selectProfile(dataRoot);
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
     * Hien thi lua chon tao profile moi hoac dung profile cu trong data root.
     */
    private static ProfileSelection selectProfile(Path dataRoot) {
        List<PeerConfig> profiles = PeerConfig.listProfiles(dataRoot);
        if (profiles.isEmpty()) {
            System.out.println("[INFO] No existing profile found. Creating a new UUID profile.");
            return new ProfileSelection(PeerConfig.createNew(dataRoot), true);
        }

        Object[] options = {"Create new profile", "Use existing profile", "Cancel"};
        int choice = JOptionPane.showOptionDialog(
                null,
                "Select how to start this peer.",
                "Peer Profile",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[1]
        );

        if (choice == 0) {
            return new ProfileSelection(PeerConfig.createNew(dataRoot), true);
        }
        if (choice == 1) {
            return selectExistingProfile(profiles);
        }
        return null;
    }

    /**
     * Cho nguoi dung click profile cu, Start truc tiep hoac Edit neu muon sua name/port.
     */
    private static ProfileSelection selectExistingProfile(List<PeerConfig> profiles) {
        DefaultListModel<PeerConfig> listModel = new DefaultListModel<>();
        profiles.forEach(listModel::addElement);

        JList<PeerConfig> profileList = new JList<>(listModel);
        profileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        profileList.setSelectedIndex(0);
        profileList.setVisibleRowCount(Math.min(8, Math.max(1, profiles.size())));
        profileList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            javax.swing.JLabel label = new javax.swing.JLabel(value.getDisplayLabel());
            label.setOpaque(true);
            label.setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
            label.setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
            label.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 8, 6, 8));
            return label;
        });

        Object[] options = {"Start", "Edit", "Cancel"};
        int choice = JOptionPane.showOptionDialog(
                null,
                new JScrollPane(profileList),
                "Use Existing Profile",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[0]
        );

        PeerConfig selectedProfile = profileList.getSelectedValue();
        if (selectedProfile == null || choice == 2 || choice == JOptionPane.CLOSED_OPTION) {
            return null;
        }
        return new ProfileSelection(selectedProfile, choice == 1);
    }

    private record ProfileSelection(PeerConfig config, boolean editBeforeStart) {
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
