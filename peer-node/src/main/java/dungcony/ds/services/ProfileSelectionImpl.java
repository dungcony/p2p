package dungcony.ds.services;

import dungcony.ds.config.PeerConfig;
import dungcony.ds.dtos.ProfileSelection;
import dungcony.ds.interfaces.ProfileSelectionService;

import javax.swing.*;
import java.nio.file.Path;
import java.util.List;

public class ProfileSelectionImpl implements ProfileSelectionService {
    private final Path dataRoot;

    public ProfileSelectionImpl(Path dataRoot) {
        this.dataRoot = dataRoot;
    }

    @Override
    public ProfileSelection selectProfile() {
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


    // ------------------------- PRIVATE -----------------------------//

    /**
     * Cho nguoi dung click profile cu, Start truc tiep hoac Edit neu muon sua name/port.
     */
    private ProfileSelection selectExistingProfile(List<PeerConfig> profiles) {
        DefaultListModel<PeerConfig> listModel = new DefaultListModel<>();
        profiles.forEach(listModel::addElement);

        JList<PeerConfig> profileList = new JList<>(listModel);
        profileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        profileList.setSelectedIndex(0);
        profileList.setVisibleRowCount(Math.min(8, Math.max(1, profiles.size())));
        profileList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel(value.getDisplayLabel());
            label.setOpaque(true);
            label.setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
            label.setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
            label.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
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
}
