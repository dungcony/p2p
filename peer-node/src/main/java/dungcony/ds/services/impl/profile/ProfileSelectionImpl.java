package dungcony.ds.services.impl.profile;

import dungcony.ds.config.PeerProfile;
import dungcony.ds.config.PeerProfileRepository;
import dungcony.ds.dtos.ProfileSelection;
import dungcony.ds.services.interfaces.profile.ProfileSelectionService;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.nio.file.Path;
import java.util.List;

@Slf4j
// Service hiển thị luồng chọn profile peer trước khi khởi động node
public class ProfileSelectionImpl implements ProfileSelectionService {
    private final Path dataRoot;
    private final PeerProfileRepository profileRepository;

    // Khởi tạo service chọn profile với data root và repository I/O
    public ProfileSelectionImpl(Path dataRoot, PeerProfileRepository profileRepository) {
        this.dataRoot = dataRoot;
        this.profileRepository = profileRepository;
    }

    // Chọn profile cũ hoặc tạo profile mới cho peer local
    @Override
    public ProfileSelection selectProfile() {
        List<PeerProfile> profiles = profileRepository.listProfiles(dataRoot);
        if (profiles.isEmpty()) {
            log.info("Không tìm thấy profile cũ. Đang tạo profile UUID mới.");
            return new ProfileSelection(profileRepository.createNew(dataRoot), true, true);
        }

        Object[] options = {"Tạo profile mới", "Dùng profile có sẵn", "Hủy"};
        int choice = JOptionPane.showOptionDialog(
                null,
                "Chọn cách khởi động peer này.",
                "Profile peer",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[1]
        );

        if (choice == 0) {
            return new ProfileSelection(profileRepository.createNew(dataRoot), true, true);
        }
        if (choice == 1) {
            return selectExistingProfile(profiles);
        }
        return null;
    }


    // ------------------------- PRIVATE -----------------------------//

    // Cho người dùng click profile cũ, Start trực tiếp hoặc Sửa nếu muốn sửa name/port
    private ProfileSelection selectExistingProfile(List<PeerProfile> profiles) {
        DefaultListModel<PeerProfile> listModel = new DefaultListModel<>();
        profiles.forEach(listModel::addElement);

        JList<PeerProfile> profileList = new JList<>(listModel);
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

        Object[] options = {"Bắt đầu", "Sửa", "Hủy"};
        int choice = JOptionPane.showOptionDialog(
                null,
                new JScrollPane(profileList),
                "Dùng profile có sẵn",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[0]
        );

        PeerProfile selectedProfile = profileList.getSelectedValue();
        if (selectedProfile == null || choice == 2 || choice == JOptionPane.CLOSED_OPTION) {
            return null;
        }
        return new ProfileSelection(selectedProfile, choice == 1, false);
    }
}
