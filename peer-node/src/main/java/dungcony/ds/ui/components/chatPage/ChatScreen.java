package dungcony.ds.ui.components.chatPage;

import dungcony.ds.App;
import dungcony.ds.interfaces.MessageListener;
import dungcony.ds.model.Message;
import dungcony.ds.ui.pages.ChatPage;
import dungcony.ds.ui.utils.ColorPalette;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ChatScreen extends JPanel implements MessageListener {
    private String selectedUser = "Chọn một cuộc chat";
    private String ipAddress;
    private String groupId;
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
        emptyStateLabel = new JLabel("Chọn một peer để bắt đầu chat", SwingConstants.CENTER);
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

    /**
     * Chon group chat de UI gui/nhan message theo groupId thay vi host:port.
     */
    public void setSelectedGroup(String groupName, String groupId) {
        this.groupId = groupId;
        this.ipAddress = null;
        this.selectedUser = groupName;
        chatHeader.setUserName(groupName);
        chatHeader.setGroupStatus();
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

    public String getGroupId() {
        return groupId;
    }

    public boolean isGroupChat() {
        return groupId != null && !groupId.isBlank();
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
        this.groupId = null;
        showChatControls(ipAddress != null && !ipAddress.isBlank());
    }

    @Override
    public void onMessageReceived(Message message) {
        if (message == null) {
            return;
        }
        if (isGroupChat()) {
            if (groupId.equals(message.getGroupId())) {
                messages.add(message);
                chatHistory.renderMessage(message);
                revalidate();
                repaint();
            }
            return;
        }
        if (ipAddress == null) {
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
