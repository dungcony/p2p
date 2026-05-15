package dungcony.ds.ui.components.chatPage;

import dungcony.ds.App;
import dungcony.ds.model.Message;
import dungcony.ds.peer.MessageListener;
import dungcony.ds.ui.pages.ChatPage;
import dungcony.ds.ui.utils.ColorPalette;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ChatScreen extends JPanel implements MessageListener {
    private String selectedUser = "Select a chat";
    private String ipAddress;
    private ChatHistory chatHistory;
    private SendMessageBox sendMessageBox;
    private ChatHeader chatHeader;
    private final List<Message> messages;
    private ChatPage parentChatPage;

    public ChatScreen() {
        this.messages = new ArrayList<>();
        if (App.peerNode != null) {
            App.peerNode.addMessageListener(this);
        }
        initializeComponents();
        setupLayout();
        renderAllMessages();
    }

    private void initializeComponents() {
        setBackground(ColorPalette.BACKGROUND);
        chatHeader = new ChatHeader(selectedUser, ipAddress);
        chatHeader.getBackButton().addActionListener(e -> goToChatListPage());
        chatHistory = new ChatHistory();
        sendMessageBox = new SendMessageBox(this);
    }

    private void setupLayout() {
        setLayout(new BorderLayout());
        add(chatHeader, BorderLayout.NORTH);
        add(chatHistory, BorderLayout.CENTER);
        add(sendMessageBox, BorderLayout.SOUTH);
    }

    private void renderAllMessages() {
        chatHistory.clearMessages();
        chatHistory.renderAllMessages(messages);
    }

    public void setMessages(List<Message> messages) {
        this.messages.clear();
        chatHistory.clearMessages();
        if (messages != null) {
            this.messages.addAll(messages);
        }
        renderAllMessages();
        revalidate();
        repaint();
    }

    public void setSelectedUser(String userName) {
        this.selectedUser = userName;
        chatHeader.setUserName(userName);
        messages.clear();
        chatHistory.clearMessages();
    }

    public void setMobileMode(boolean isMobile) {
        chatHeader.setMobileMode(isMobile);
    }

    public void setParentChatPage(ChatPage parent) {
        this.parentChatPage = parent;
    }

    private void goToChatListPage() {
        if (parentChatPage != null) {
            parentChatPage.showChatList();
        }
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    @Override
    public void onMessageReceived(Message message) {
        if (ipAddress == null || message == null) {
            return;
        }
        if (ipAddress.equals(message.getSenderIp()) || ipAddress.equals(message.getReceiverHost() + ":" + message.getReceiverPort())) {
            messages.add(message);
            chatHistory.renderMessage(message);
            revalidate();
            repaint();
        }
    }

    public void setUserStatus(boolean isOnline) {
        chatHeader.setStatus(isOnline);
    }
}
