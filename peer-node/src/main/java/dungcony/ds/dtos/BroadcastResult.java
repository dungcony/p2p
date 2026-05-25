package dungcony.ds.dtos;

/**
 * Kết quả gửi broadcast tới các peer đích
 */
public record BroadcastResult(int totalTargets, int delivered, int failed) {
}
