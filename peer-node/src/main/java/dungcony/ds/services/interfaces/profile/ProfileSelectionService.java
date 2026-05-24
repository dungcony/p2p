package dungcony.ds.services.interfaces.profile;

import dungcony.ds.dtos.ProfileSelection;

// Đại diện contract chọn profile runtime trước khi khởi động peer
public interface ProfileSelectionService {
    // Hiển thị lựa chọn profile
    ProfileSelection selectProfile();
}
