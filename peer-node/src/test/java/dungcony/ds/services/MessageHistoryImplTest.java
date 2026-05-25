package dungcony.ds.services;

import dungcony.ds.enums.MessageStatus;
import dungcony.ds.model.Message;
import dungcony.ds.model.PeerInfo;
import dungcony.ds.repositories.LocalMessageRepo;
import dungcony.ds.services.impl.messaging.MessageHistoryImpl;
import dungcony.ds.utils.BroadcastConversation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageHistoryImplTest {
    @TempDir
    Path tempDir;

    @Test
    void updatingSameMessageIdDoesNotDuplicateLocalHistory() {
        PeerInfo alice = new PeerInfo("alice", "Alice", "127.0.0.1", 5011);
        PeerInfo bob = new PeerInfo("bob", "Bob", "127.0.0.1", 5012);
        MessageHistoryImpl history = new MessageHistoryImpl(new LocalMessageRepo(tempDir));
        Message message = Message.chat(alice, bob, "retry me");

        history.addAndSave(bob, message);
        message.setStatus(MessageStatus.FAILED);
        history.updateAndSave(bob, message);

        MessageHistoryImpl reloadedHistory = new MessageHistoryImpl(new LocalMessageRepo(tempDir));
        List<Message> messages = reloadedHistory.getMessages(bob, bob.addressKey());

        assertEquals(1, messages.size());
        assertEquals("retry me", messages.getFirst().getContent());
        assertEquals(MessageStatus.FAILED, messages.getFirst().getStatus());
    }

    @Test
    void broadcastHistoryIsSeparateFromDirectConversations() {
        PeerInfo alice = new PeerInfo("alice", "Alice", "127.0.0.1", 5011);
        PeerInfo bob = new PeerInfo("bob", "Bob", "127.0.0.1", 5012);
        MessageHistoryImpl history = new MessageHistoryImpl(new LocalMessageRepo(tempDir));

        history.addAndSave(bob, Message.chat(alice, bob, "private"));
        history.addAndSave(bob, Message.broadcast(alice, bob, "legacy world"));
        history.addAndSave(
                BroadcastConversation.historyKey(),
                BroadcastConversation.conversationPeer(),
                Message.broadcast(alice, BroadcastConversation.conversationPeer(), "world"));

        MessageHistoryImpl reloadedHistory = new MessageHistoryImpl(new LocalMessageRepo(tempDir));

        assertEquals(1, reloadedHistory.getDirectConversationPeers().size());
        assertEquals("bob", reloadedHistory.getDirectConversationPeers().getFirst().getId());
        assertEquals(1, reloadedHistory.getMessages(bob, bob.addressKey()).size());
        List<Message> broadcastMessages = reloadedHistory.getMessages(
                BroadcastConversation.conversationPeer(),
                BroadcastConversation.historyKey());
        assertEquals(1, broadcastMessages.size());
        assertEquals("world", broadcastMessages.getFirst().getContent());
    }
}
