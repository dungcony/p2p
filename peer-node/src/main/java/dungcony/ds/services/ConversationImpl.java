package dungcony.ds.services;

import dungcony.ds.interfaces.ConversationService;
import dungcony.ds.interfaces.MessageHistoryService;
import dungcony.ds.interfaces.PeerDirectoryService;
import dungcony.ds.model.Message;
import dungcony.ds.model.PeerInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
// Service gom dữ liệu hội thoại từ danh bạ runtime và lịch sử tin nhắn local
public class ConversationImpl implements ConversationService {
    private final PeerDirectoryService peerDirectoryService;
    private final MessageHistoryService messageHistoryService;

    // Khởi tạo service đọc hội thoại với danh bạ peer và kho lịch sử tin nhắn
    public ConversationImpl(PeerDirectoryService peerDirectoryService,
                            MessageHistoryService messageHistoryService) {
        this.peerDirectoryService = peerDirectoryService;
        this.messageHistoryService = messageHistoryService;
    }

    // Lấy danh sách chat gồm peer online và peer offline từng có lịch sử message
    @Override
    public Collection<PeerInfo> getChatListPeers() {
        Map<String, PeerInfo> conversations = new LinkedHashMap<>();
        for (PeerInfo peerInfo : peerDirectoryService.list()) {
            if (peerDirectoryService.isSelfPeer(peerInfo)) {
                continue;
            }
            if (peerInfo.isOnline()) {
                conversations.put(peerInfo.getId(), peerInfo);
            }
        }
        for (PeerInfo historyPeer : messageHistoryService.getDirectConversationPeers()) {
            if (peerDirectoryService.isSelfPeer(historyPeer)) {
                log.debug("Bỏ qua conversation trỏ về peer local. peerId={}", historyPeer.getId());
                continue;
            }
            PeerInfo runtimePeer = peerDirectoryService.findKnownPeerById(historyPeer.getId());
            if (runtimePeer == null) {
                peerDirectoryService.put(historyPeer);
                conversations.put(historyPeer.getId(), historyPeer);
            } else if (!peerDirectoryService.isSelfPeer(runtimePeer)) {
                conversations.put(historyPeer.getId(), runtimePeer);
            } else {
                log.debug("Bỏ qua runtime peer local trong danh sách chat. peerId={}", runtimePeer.getId());
            }
        }
        log.debug("Đã tạo danh sách chat. sốLượng={}", conversations.size());
        return conversations.values();
    }

    // Lấy lịch sử tin nhắn với peer theo địa chỉ nhập từ UI
    @Override
    public List<Message> getMessagesWithPeer(String hostAndMaybePort) {
        PeerInfo peerInfo = peerDirectoryService.resolvePeer(hostAndMaybePort);
        return messageHistoryService.getMessages(peerInfo, hostAndMaybePort);
    }

    // Lấy message cuối cùng với peer theo địa chỉ nhập từ UI
    @Override
    public Message getLastMessage(String hostAndMaybePort) {
        PeerInfo peerInfo = peerDirectoryService.resolvePeer(hostAndMaybePort);
        return messageHistoryService.getLastMessage(peerInfo, hostAndMaybePort);
    }
}
