package dungcony.ds.dtos;

public record GroupMemberPayload(String groupId,
                                 String userId,
                                 long joinedAt) {
}
 