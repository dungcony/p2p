package dungcony.ds.dtos;

/**
 * Payload metadata group dùng khi đồng bộ với bootstrap
 */
public record GroupPayload(
        String groupId,
        String name,
        String createdBy,
        long createdAt
) {
}
