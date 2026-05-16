package dungcony.ds.ui.components.chatPage;

import dungcony.ds.App;
import dungcony.ds.model.Group;
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
import java.util.ArrayList;
import java.util.List;

public class ChatList extends JPanel {
    private JPanel devicesContainer;
    private JScrollPane scrollPane;
    private JButton createGroupButton;
    private ChatPage parentChatPage;

    public ChatList() {
        try {
            if (App.peerNode != null) {
                App.peerNode.addPeerChangeListener(this::renderFriends);
                App.peerNode.addMessageListener(message -> renderFriends());
            }
            initializeComponents();
            add(createGroupButton, BorderLayout.NORTH);
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

        createGroupButton = new JButton("New group");
        createGroupButton.setFocusPainted(false);
        createGroupButton.setBackground(ColorPalette.PRIMARY);
        createGroupButton.setForeground(Color.WHITE);
        createGroupButton.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        createGroupButton.addActionListener(event -> openCreateGroupDialog());
    }

    private void renderFriends() {
        SwingUtilities.invokeLater(() -> {
            try {
                devicesContainer.removeAll();
                if (App.peerNode != null) {
                    for (Group group : App.peerNode.getGroups()) {
                        Message message = App.peerNode.getLastGroupMessage(group.getGroupId());
                        addGroupProfile(group, message);
                    }
                    for (PeerInfo friend : App.peerNode.getChatListPeers()) {
                        addPeerProfile(friend);
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

    /**
     * Them item peer vao danh sach chat.
     */
    private void addPeerProfile(PeerInfo peerInfo) {
        String peerKey = peerInfo.addressKey();
        Message message = App.peerNode.getLastMessage(peerKey);
        String lastMessage = "";
        String lastTime = "";

        if (message != null) {
            lastMessage = message.isFromCurrentUser() ? "You: " + message.getContent() : message.getContent();
            lastTime = message.getFormattedTime();
        } else if (peerInfo.isOnline()) {
            lastMessage = "Online";
        }
        addProfile(peerInfo.getName(), lastMessage, lastTime, peerKey);
    }

    /**
     * Them item group vao danh sach chat.
     */
    private void addGroupProfile(Group group, Message message) {
        String lastMessage = "";
        String lastTime = "";
        if (message != null) {
            lastMessage = message.isFromCurrentUser() ? "You: " + message.getContent() : message.getContent();
            lastTime = message.getFormattedTime();
        }
        JPanel deviceInfoPanel = createDeviceInfoPanel("[Group] " + group.getName(), lastMessage, lastTime);
        ChatProfile chatProfile = new ChatProfile(10, deviceInfoPanel, ColorPalette.BACKGROUND);
        chatProfile.setBackground(ColorPalette.BACKGROUND);

        MouseAdapter selector = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (parentChatPage != null) {
                    parentChatPage.onGroupSelected(group.getName(), group.getGroupId());
                }
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

    /**
     * Hien dialog tao group voi cac peer dang online.
     */
    private void openCreateGroupDialog() {
        if (App.peerNode == null) {
            return;
        }
        createGroupButton.setEnabled(false);
        new SwingWorker<List<PeerInfo>, Void>() {
            @Override
            protected List<PeerInfo> doInBackground() {
                return loadGroupCandidatesWithFreshStatus();
            }

            @Override
            protected void done() {
                try {
                    showCreateGroupDialog(get());
                } catch (Exception e) {
                    System.out.println("[ERROR] Failed to open create group dialog: " + e.getMessage());
                } finally {
                    createGroupButton.setEnabled(true);
                }
            }
        }.execute();
    }

    /**
     * Lay danh sach peer co uid va refresh online/offline truoc khi hien dialog.
     */
    private List<PeerInfo> loadGroupCandidatesWithFreshStatus() {
        List<PeerInfo> groupCandidates = new ArrayList<>();
        boolean bootstrapAvailable = App.peerNode.isBootstrapAvailable();
        for (PeerInfo peerInfo : App.peerNode.getChatListPeers()) {
            if (peerInfo.getId() != null && !peerInfo.getId().isBlank()) {
                if (!bootstrapAvailable) {
                    App.peerNode.checkUserIsOnline(peerInfo.addressKey());
                }
                groupCandidates.add(peerInfo);
            }
        }
        return groupCandidates;
    }

    /**
     * Hien dialog tao group sau khi trang thai peer da duoc refresh.
     */
    private void showCreateGroupDialog(List<PeerInfo> groupCandidates) {
        if (groupCandidates.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No known peers available for group.", "Create group",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JTextField groupNameField = new JTextField("New group");
        JList<PeerInfo> peerList = new JList<>(groupCandidates.toArray(new PeerInfo[0]));
        peerList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        peerList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            String status = value.isOnline() ? "online" : "offline";
            JLabel label = new JLabel(value.getName() + " (" + status + ", " + value.getId() + ")");
            label.setOpaque(true);
            label.setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
            label.setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
            label.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
            return label;
        });

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(groupNameField, BorderLayout.NORTH);
        panel.add(new JScrollPane(peerList), BorderLayout.CENTER);

        int choice = JOptionPane.showConfirmDialog(this, panel, "Create group",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION || peerList.getSelectedValuesList().isEmpty()) {
            return;
        }

        List<PeerInfo> selectedPeers = peerList.getSelectedValuesList();
        boolean bootstrapAvailable = App.peerNode.isBootstrapAvailable();
        if (!bootstrapAvailable && !validateDirectGroupMembers(selectedPeers)) {
            return;
        }

        Group group = App.peerNode.createGroup(groupNameField.getText(), selectedPeers);
        System.out.println("[INFO] UI created group. groupId=" + group.getGroupId()
                + ", members=" + group.getMembers().size());
        renderFriends();
    }

    /**
     * Khi bootstrap-server tat, group chi duoc tao voi peer dang TCP reachable.
     */
    private boolean validateDirectGroupMembers(List<PeerInfo> selectedPeers) {
        List<String> offlinePeers = new ArrayList<>();
        for (PeerInfo peerInfo : selectedPeers) {
            boolean online = App.peerNode.checkUserIsOnline(peerInfo.addressKey());
            if (!online) {
                offlinePeers.add(peerInfo.getName() + " (" + peerInfo.addressKey() + ")");
            }
        }
        if (!offlinePeers.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Bootstrap server is unavailable. These peers are offline and cannot be added:\n"
                            + String.join("\n", offlinePeers),
                    "Offline peers",
                    JOptionPane.WARNING_MESSAGE
            );
            System.out.println("[WARN] Group creation blocked because bootstrap is unavailable and peers are offline: "
                    + offlinePeers);
            return false;
        }
        return true;
    }
}
