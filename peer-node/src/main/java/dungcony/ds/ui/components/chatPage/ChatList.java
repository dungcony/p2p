package dungcony.ds.ui.components.chatPage;

import dungcony.ds.App;
import dungcony.ds.model.Message;
import dungcony.ds.model.PeerInfo;
import dungcony.ds.ui.components.ChatProfile;
import dungcony.ds.ui.components.ModernScrollBarUI;
import dungcony.ds.ui.pages.ChatPage;
import dungcony.ds.ui.utils.ColorPalette;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ChatList extends JPanel {
    private JPanel devicesContainer;
    private JScrollPane scrollPane;
    private ChatPage parentChatPage;

    public ChatList() {
        try {
            if (App.peerNode != null) {
                App.peerNode.addPeerChangeListener(this::renderFriends);
                App.peerNode.addMessageListener(message -> renderFriends());
            }
            initializeComponents();
            add(scrollPane, BorderLayout.CENTER);
            renderFriends();
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to initialize ChatList\nError Message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initializeComponents() {
        this.setBackground(ColorPalette.BACKGROUND);
        this.setLayout(new BorderLayout(0, 15));

        devicesContainer = new JPanel();
        devicesContainer.setLayout(new BoxLayout(devicesContainer, BoxLayout.Y_AXIS));
        devicesContainer.setBackground(ColorPalette.PANEL_BACKGROUND);
        devicesContainer.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        scrollPane = new JScrollPane(devicesContainer);
        scrollPane.setBackground(ColorPalette.BACKGROUND);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getVerticalScrollBar().setBackground(ColorPalette.BACKGROUND);
        scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());
    }

    private void renderFriends() {
        SwingUtilities.invokeLater(() -> {
            try {
                devicesContainer.removeAll();
                if (App.peerNode != null) {
                    for (PeerInfo friend : App.peerNode.getKnownPeers()) {
                        String peerKey = friend.addressKey();
                        Message message = App.peerNode.getLastMessage(peerKey);
                        String lastMessage = "";
                        String lastTime = "";

                        if (message != null) {
                            lastMessage = message.isFromCurrentUser() ? "You: " + message.getContent() : message.getContent();
                            lastTime = message.getFormattedTime();
                        }
                        addProfile(friend.getId(), lastMessage, lastTime, peerKey);
                    }
                }
                devicesContainer.revalidate();
                devicesContainer.repaint();
            } catch (Exception e) {
                System.out.println("[ERROR] Failed to render friends\nError Message: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void addProfile(String userName, String lastMessage, String messageTime, String peerKey) {
        JPanel deviceInfoPanel = createDeviceInfoPanel(userName, lastMessage, messageTime);
        ChatProfile chatProfile = new ChatProfile(10, deviceInfoPanel, ColorPalette.BACKGROUND);
        chatProfile.setBackground(ColorPalette.BACKGROUND);

        MouseAdapter selector = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onChatItemSelected(userName, peerKey);
            }
        };
        deviceInfoPanel.addMouseListener(selector);
        chatProfile.addMouseListener(selector);

        if (devicesContainer.getComponentCount() > 0) {
            devicesContainer.add(Box.createVerticalStrut(8));
        }

        devicesContainer.add(chatProfile);
    }

    private JPanel createDeviceInfoPanel(String userName, String lastMessage, String messageTime) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(ColorPalette.PANEL_BACKGROUND);

        GridBagConstraints gbc = new GridBagConstraints();

        JLabel nameLabel = new JLabel(userName);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        nameLabel.setForeground(ColorPalette.TEXT);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 2, 0);
        panel.add(nameLabel, gbc);

        JLabel messageLabel = new JLabel(lastMessage);
        messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        messageLabel.setForeground(ColorPalette.SECONDARY_TEXT);
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        panel.add(messageLabel, gbc);

        JLabel lastMessageTime = new JLabel(messageTime);
        lastMessageTime.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 20, 0, 0);
        panel.add(lastMessageTime, gbc);

        return panel;
    }

    public void setParentChatPage(ChatPage parent) {
        this.parentChatPage = parent;
    }

    private void onChatItemSelected(String userName, String peerKey) {
        if (parentChatPage != null) {
            parentChatPage.onChatSelected(userName, peerKey);
        }
    }
}
