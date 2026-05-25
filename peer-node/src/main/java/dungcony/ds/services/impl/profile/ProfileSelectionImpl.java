package dungcony.ds.services.impl.profile;

import dungcony.ds.config.PeerProfile;
import dungcony.ds.dtos.ProfileSelection;
import dungcony.ds.repositories.PeerProfileRepository;
import dungcony.ds.services.interfaces.profile.ProfileSelectionService;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.Set;
import java.util.List;

@Slf4j
// Service hiển thị luồng chọn profile peer trước khi khởi động node
public class ProfileSelectionImpl implements ProfileSelectionService {
    private static final Set<String> DEMO_PROFILE_IDS = Set.of("alice", "bob", "carol");

    private final Path dataRoot;
    private final PeerProfileRepository profileRepository;

    // Khởi tạo service chọn profile với data root và repository I/O
    public ProfileSelectionImpl(Path dataRoot, PeerProfileRepository profileRepository) {
        this.dataRoot = dataRoot;
        this.profileRepository = profileRepository;
    }

    // Dùng profile người dùng thật nếu có; bỏ qua profile demo và chỉ hỏi tên khi cần tạo mới.
    @Override
    public ProfileSelection selectProfile() {
        List<PeerProfile> profiles = profileRepository.listProfiles(dataRoot);
        for (PeerProfile profile : profiles) {
            if (isDemoProfile(profile)) {
                continue;
            }
            log.info("Đang dùng profile người dùng đã lưu. {}", profile.getDisplayLabel());
            return new ProfileSelection(profile, false, false);
        }
        log.info("Chưa có profile người dùng thật. Đang tạo profile mới và yêu cầu nhập tên.");
        return new ProfileSelection(profileRepository.createNew(dataRoot), true, true);
    }

    private boolean isDemoProfile(PeerProfile profile) {
        if (profile == null) {
            return false;
        }
        return isDemoValue(profile.getPeerId()) || isDemoValue(profile.getPeerName());
    }

    private boolean isDemoValue(String value) {
        return value != null && DEMO_PROFILE_IDS.contains(value.trim().toLowerCase());
    }
}
