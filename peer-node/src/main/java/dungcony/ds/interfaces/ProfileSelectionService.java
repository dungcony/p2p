package dungcony.ds.interfaces;

import dungcony.ds.dtos.ProfileSelection;

// Đại diện contract chọn profile runtime trước khi khởi động peer
public interface ProfileSelectionService {
    // Hiển thị lựa chọn profile
    ProfileSelection selectProfile();
}
