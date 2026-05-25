package dungcony.ds.dtos;

/**
 * Payload thành viên group nhận từ hoặc gửi tới bootstrap
 */
public record GroupMemberPayload(String groupId,
                                 String userId,
                                 long joinedAt) {
}
 
