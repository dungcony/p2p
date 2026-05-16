package dungcony.ds.dtos;

public record GroupPayload(
        String groupId,
        String name,
        String createdBy,
        long createdAt
) {
}
