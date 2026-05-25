package dungcony.ds.dtos;

import dungcony.ds.config.PeerProfile;

/**
 * Kết quả chọn profile trước khi khởi động peer
 */
public record ProfileSelection(
        PeerProfile config,
        boolean editBeforeStart,
        boolean newProfile) {
}
