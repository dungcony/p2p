package dungcony.ds.ui.components.chatPage;

import dungcony.ds.App;
import dungcony.ds.model.Message;
import dungcony.ds.interfaces.MessageListener;
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
    private JLabel emptyStateLabel;
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
        emptyStateLabel = new JLabel("Select a peer to start chatting", SwingConstants.CENTER);
        emptyStateLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        emptyStateLabel.setForeground(ColorPalette.SECONDARY_TEXT);
    }

    private void setupLayout() {
        setLayout(new BorderLayout());
        add(chatHeader, BorderLayout.NORTH);
        add(emptyStateLabel, BorderLayout.CENTER);
        sendMessageBox.setVisible(false);
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
        showChatControls(true);
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
        showChatControls(ipAddress != null && !ipAddress.isBlank());
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

    private void showChatControls(boolean hasSelectedPeer) {
        remove(emptyStateLabel);
        remove(chatHistory);
        remove(sendMessageBox);

        if (hasSelectedPeer) {
            add(chatHistory, BorderLayout.CENTER);
            add(sendMessageBox, BorderLayout.SOUTH);
            sendMessageBox.setVisible(true);
        } else {
            add(emptyStateLabel, BorderLayout.CENTER);
            sendMessageBox.setVisible(false);
        }

        revalidate();
        repaint();
    }
}
