package dungcony.ds.ui.components.chatPage;

import dungcony.ds.App;
import dungcony.ds.model.Group;
import dungcony.ds.model.Message;
import dungcony.ds.model.PeerInfo;
import dungcony.ds.model.PeerNode;
import dungcony.ds.ui.components.ChatProfile;
import dungcony.ds.ui.components.ModernScrollBarUI;
import dungcony.ds.ui.pages.ChatPage;
import dungcony.ds.ui.utils.ColorPalette;
import lombok.extern.slf4j.Slf4j;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class ChatList extends JPanel {
    private static final Color ONLINE_COLOR = new Color(46, 125, 50);
    private static final Color OFFLINE_COLOR = new Color(211, 47, 47);

    private JPanel devicesContainer;
    private JScrollPane scrollPane;
    private JButton createGroupButton;
    private JButton broadcastButton;
    private ChatPage parentChatPage;

    public ChatList() {
        try {
            if (App.peerNode != null) {
                App.peerNode.addPeerChangeListener(this::renderFriends);
                App.peerNode.addMessageListener(message -> renderFriends());
            }
            initializeComponents();
            add(createActionPanel(), BorderLayout.NORTH);
            add(scrollPane, BorderLayout.CENTER);
            renderFriends();
        } catch (Exception e) {
            log.error("Không thể khởi tạo danh sách chat\nChi tiết lỗi: {}", e.getMessage());
            log.error("Chi tiết lỗi", e);
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

        createGroupButton = new JButton("Nhóm mới");
        createGroupButton.setFocusPainted(false);
        createGroupButton.setBackground(ColorPalette.PRIMARY);
        createGroupButton.setForeground(Color.WHITE);
        createGroupButton.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        createGroupButton.addActionListener(event -> openCreateGroupDialog());

        broadcastButton = new JButton("Phát toàn mạng");
        broadcastButton.setFocusPainted(false);
        broadcastButton.setBackground(ColorPalette.ACCENT);
        broadcastButton.setForeground(Color.WHITE);
        broadcastButton.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        broadcastButton.addActionListener(event -> openBroadcastDialog());
    }

    private JPanel createActionPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 8, 0));
        panel.setBackground(ColorPalette.BACKGROUND);
        panel.add(createGroupButton);
        panel.add(broadcastButton);
        return panel;
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
                log.error("Không thể hiển thị danh sách chat\nChi tiết lỗi: {}", e.getMessage());
                log.error("Chi tiết lỗi", e);
            }
        });
    }

    private void addProfile(String userName, String lastMessage, String messageTime, String peerKey, Boolean online) {
        JPanel deviceInfoPanel = createDeviceInfoPanel(userName, lastMessage, messageTime, online);
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

    // Thêm item peer vào danh sách chat.
    private void addPeerProfile(PeerInfo peerInfo) {
        String peerKey = peerInfo.addressKey();
        Message message = App.peerNode.getLastMessage(peerKey);
        String lastMessage = "";
        String lastTime = "";

        if (message != null) {
            lastMessage = message.isFromCurrentUser() ? "Bạn: " + message.getContent() : message.getContent();
            lastTime = message.getFormattedTime();
        } else {
            lastMessage = peerInfo.isOnline() ? "Trực tuyến" : "Ngoại tuyến";
        }
        addProfile(peerInfo.getName(), lastMessage, lastTime, peerKey, peerInfo.isOnline());
    }

    // Thêm item group vào danh sách chat.
    private void addGroupProfile(Group group, Message message) {
        String lastMessage = "";
        String lastTime = "";
        if (message != null) {
            lastMessage = message.isFromCurrentUser() ? "Bạn: " + message.getContent() : message.getContent();
            lastTime = message.getFormattedTime();
        }
        JPanel deviceInfoPanel = createDeviceInfoPanel("[Nhóm] " + group.getName(), lastMessage, lastTime, null);
        ChatProfile chatProfile = new ChatProfile(10, deviceInfoPanel, ColorPalette.BACKGROUND);
        chatProfile.setBackground(ColorPalette.BACKGROUND);

        JButton addMemberButton = createAddMemberButton(group);
        chatProfile.add(addMemberButton, BorderLayout.EAST);

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

    private JButton createAddMemberButton(Group group) {
        JButton button = new JButton();
        button.setIcon(FontIcon.of(FontAwesome.USER_PLUS, 14, ColorPalette.PRIMARY));
        button.setToolTipText("Thêm peer vào nhóm");
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        button.setContentAreaFilled(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.addActionListener(event -> openAddMembersDialog(group));
        return button;
    }

    private JPanel createDeviceInfoPanel(String userName, String lastMessage, String messageTime, Boolean online) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(ColorPalette.PANEL_BACKGROUND);

        GridBagConstraints gbc = new GridBagConstraints();

        if (online != null) {
            JLabel statusDot = new JLabel("●");
            statusDot.setForeground(online ? ONLINE_COLOR : OFFLINE_COLOR);
            statusDot.setFont(new Font("Segoe UI", Font.BOLD, 12));
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.insets = new Insets(0, 0, 2, 6);
            panel.add(statusDot, gbc);
        }

        JLabel nameLabel = new JLabel(userName);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        nameLabel.setForeground(ColorPalette.TEXT);
        gbc.gridx = online == null ? 0 : 1;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 2, 0);
        panel.add(nameLabel, gbc);

        JLabel messageLabel = new JLabel(lastMessage);
        messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        messageLabel.setForeground(ColorPalette.SECONDARY_TEXT);
        gbc.gridy = 1;
        gbc.gridx = online == null ? 0 : 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        panel.add(messageLabel, gbc);

        JLabel lastMessageTime = new JLabel(messageTime);
        lastMessageTime.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        gbc.gridx = online == null ? 1 : 2;
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

    // Hiển thị dialog tạo group với các peer đang online.
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
                    log.error("Không thể mở hộp thoại tạo nhóm: {}", e.getMessage());
                } finally {
                    createGroupButton.setEnabled(true);
                }
            }
        }.execute();
    }

    private void openBroadcastDialog() {
        if (App.peerNode == null) {
            return;
        }
        JTextArea messageArea = new JTextArea(5, 28);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        JScrollPane messageScrollPane = new JScrollPane(messageArea);
        int choice = JOptionPane.showConfirmDialog(
                this,
                messageScrollPane,
                "Phát tin nhắn toàn mạng",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }
        String content = messageArea.getText();
        if (content == null || content.isBlank()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập nội dung broadcast.", "Broadcast",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        broadcastButton.setEnabled(false);
        new SwingWorker<PeerNode.BroadcastResult, Void>() {
            @Override
            protected PeerNode.BroadcastResult doInBackground() {
                return App.peerNode.broadcastToNetwork(content);
            }

            @Override
            protected void done() {
                try {
                    PeerNode.BroadcastResult result = get();
                    JOptionPane.showMessageDialog(
                            ChatList.this,
                            "Đã gửi tới " + result.delivered() + "/" + result.totalTargets()
                                    + " peer. Thất bại: " + result.failed(),
                            "Broadcast",
                            result.failed() == 0 ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE
                    );
                } catch (Exception e) {
                    log.error("Không thể broadcast toàn mạng: {}", e.getMessage());
                    JOptionPane.showMessageDialog(ChatList.this, "Không thể broadcast toàn mạng.", "Broadcast",
                            JOptionPane.ERROR_MESSAGE);
                } finally {
                    broadcastButton.setEnabled(true);
                    renderFriends();
                }
            }
        }.execute();
    }

    private void openAddMembersDialog(Group group) {
        if (App.peerNode == null || group == null) {
            return;
        }
        new SwingWorker<List<PeerInfo>, Void>() {
            @Override
            protected List<PeerInfo> doInBackground() {
                return loadGroupCandidatesWithFreshStatus(group);
            }

            @Override
            protected void done() {
                try {
                    showAddMembersDialog(group, get());
                } catch (Exception e) {
                    log.error("Không thể mở hộp thoại thêm peer vào nhóm: {}", e.getMessage());
                }
            }
        }.execute();
    }

    // Lấy danh sách peer có uid và refresh online/offline trước khi hiển thị dialog.
    private List<PeerInfo> loadGroupCandidatesWithFreshStatus() {
        return loadGroupCandidatesWithFreshStatus(null);
    }

    private List<PeerInfo> loadGroupCandidatesWithFreshStatus(Group excludedGroup) {
        List<PeerInfo> groupCandidates = new ArrayList<>();
        Set<String> existingMemberIds = collectMemberIds(excludedGroup);
        boolean bootstrapAvailable = App.peerNode.isBootstrapAvailable();
        for (PeerInfo peerInfo : App.peerNode.getChatListPeers()) {
            if (existingMemberIds.contains(peerInfo.getId())) {
                continue;
            }
            if (peerInfo.getId() != null && !peerInfo.getId().isBlank()) {
                if (!bootstrapAvailable) {
                    App.peerNode.checkUserIsOnline(peerInfo.addressKey());
                }
                groupCandidates.add(peerInfo);
            }
        }
        return groupCandidates;
    }

    private Set<String> collectMemberIds(Group group) {
        Set<String> ids = new HashSet<>();
        if (group != null) {
            for (PeerInfo member : group.getMembers()) {
                if (member != null && member.getId() != null) {
                    ids.add(member.getId());
                }
            }
        }
        return ids;
    }

    // Hiển thị dialog tạo group sau khi trạng thái peer đã được refresh.
    private void showCreateGroupDialog(List<PeerInfo> groupCandidates) {
        JTextField groupNameField = new JTextField("Nhóm mới");
        JTextField peerIdField = new JTextField();
        peerIdField.setToolTipText("Nhập peerId, có thể nhập nhiều id cách nhau bằng dấu phẩy hoặc xuống dòng");
        JList<PeerInfo> peerList = new JList<>(groupCandidates.toArray(new PeerInfo[0]));
        peerList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        peerList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            String status = value.isOnline() ? "trực tuyến" : "ngoại tuyến";
            JLabel label = new JLabel(value.getName() + " (" + status + ", " + value.getId() + ")");
            label.setOpaque(true);
            label.setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
            label.setForeground(isSelected ? list.getSelectionForeground() : value.isOnline() ? ONLINE_COLOR : OFFLINE_COLOR);
            label.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
            return label;
        });

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(groupNameField, BorderLayout.NORTH);
        panel.add(new JScrollPane(peerList), BorderLayout.CENTER);
        panel.add(createManualPeerIdPanel(peerIdField), BorderLayout.SOUTH);

        int choice = JOptionPane.showConfirmDialog(this, panel, "Tạo nhóm",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        boolean bootstrapAvailable = App.peerNode.isBootstrapAvailable();
        List<PeerInfo> selectedPeers = mergeSelectedAndManualPeers(
                peerList.getSelectedValuesList(),
                peerIdField.getText(),
                groupCandidates,
                Set.of(),
                bootstrapAvailable
        );
        if (selectedPeers == null || selectedPeers.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn peer hoặc nhập peerId.", "Tạo nhóm",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!bootstrapAvailable && !validateDirectGroupMembers(selectedPeers)) {
            return;
        }

        Group group = App.peerNode.createGroup(groupNameField.getText(), selectedPeers);
        log.info("UI đã tạo nhóm. groupId={}, sốThànhViên={}", group.getGroupId(), group.getMembers().size());
        renderFriends();
    }

    private void showAddMembersDialog(Group group, List<PeerInfo> groupCandidates) {
        JTextField peerIdField = new JTextField();
        peerIdField.setToolTipText("Nhập peerId, có thể nhập nhiều id cách nhau bằng dấu phẩy hoặc xuống dòng");
        JList<PeerInfo> peerList = new JList<>(groupCandidates.toArray(new PeerInfo[0]));
        peerList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        peerList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            String status = value.isOnline() ? "trực tuyến" : "ngoại tuyến";
            JLabel label = new JLabel(value.getName() + " (" + status + ", " + value.getId() + ")");
            label.setOpaque(true);
            label.setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
            label.setForeground(isSelected ? list.getSelectionForeground() : value.isOnline() ? ONLINE_COLOR : OFFLINE_COLOR);
            label.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
            return label;
        });

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(new JScrollPane(peerList), BorderLayout.CENTER);
        panel.add(createManualPeerIdPanel(peerIdField), BorderLayout.SOUTH);

        int choice = JOptionPane.showConfirmDialog(
                this,
                panel,
                "Thêm peer vào nhóm " + group.getName(),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        boolean bootstrapAvailable = App.peerNode.isBootstrapAvailable();
        Set<String> existingMemberIds = collectMemberIds(group);
        List<PeerInfo> selectedPeers = mergeSelectedAndManualPeers(
                peerList.getSelectedValuesList(),
                peerIdField.getText(),
                groupCandidates,
                existingMemberIds,
                bootstrapAvailable
        );
        if (selectedPeers == null || selectedPeers.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn peer hoặc nhập peerId.", "Thêm peer",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!bootstrapAvailable && !validateDirectGroupMembers(selectedPeers)) {
            return;
        }

        App.peerNode.addMembersToGroup(group.getGroupId(), selectedPeers);
        log.info("UI đã thêm peer vào nhóm. groupId={}, sốPeerThêm={}", group.getGroupId(), selectedPeers.size());
        renderFriends();
    }

    private JPanel createManualPeerIdPanel(JTextField peerIdField) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        JLabel label = new JLabel("Nhập peerId thủ công");
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setForeground(ColorPalette.SECONDARY_TEXT);
        panel.add(label, BorderLayout.NORTH);
        panel.add(peerIdField, BorderLayout.CENTER);
        return panel;
    }

    private List<PeerInfo> mergeSelectedAndManualPeers(List<PeerInfo> selectedPeers,
                                                       String manualPeerIds,
                                                       List<PeerInfo> candidates,
                                                       Set<String> excludedIds,
                                                       boolean bootstrapAvailable) {
        List<PeerInfo> result = new ArrayList<>();
        Set<String> addedIds = new HashSet<>();
        for (PeerInfo selectedPeer : selectedPeers) {
            if (selectedPeer == null || selectedPeer.getId() == null || selectedPeer.getId().isBlank()) {
                continue;
            }
            if (excludedIds.contains(selectedPeer.getId()) || !addedIds.add(selectedPeer.getId())) {
                continue;
            }
            result.add(selectedPeer);
        }

        for (String peerId : parsePeerIds(manualPeerIds)) {
            if (excludedIds.contains(peerId) || !addedIds.add(peerId)) {
                continue;
            }
            PeerInfo knownPeer = findPeerById(candidates, peerId);
            if (knownPeer != null) {
                result.add(knownPeer);
                continue;
            }
            if (!bootstrapAvailable) {
                JOptionPane.showMessageDialog(
                        this,
                        "Không tìm thấy peer này: " + peerId,
                        "Không tìm thấy peer",
                        JOptionPane.WARNING_MESSAGE
                );
                return null;
            }
            result.add(new PeerInfo(peerId, peerId, "", 0, false));
        }
        return result;
    }

    private List<String> parsePeerIds(String text) {
        List<String> peerIds = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return peerIds;
        }
        for (String token : text.split("[,;\\r\\n\\t ]+")) {
            String peerId = token.trim();
            if (!peerId.isBlank()) {
                peerIds.add(peerId);
            }
        }
        return peerIds;
    }

    private PeerInfo findPeerById(List<PeerInfo> peers, String peerId) {
        if (peerId == null || peers == null) {
            return null;
        }
        for (PeerInfo peerInfo : peers) {
            if (peerInfo != null && peerId.equals(peerInfo.getId())) {
                return peerInfo;
            }
        }
        return null;
    }

    // Khi bootstrap-server tắt, group chỉ được tạo với peer đang TCP reachable.
    private boolean validateDirectGroupMembers(List<PeerInfo> selectedPeers) {
        List<String> offlinePeers = new ArrayList<>();
        for (PeerInfo peerInfo : selectedPeers) {
            if (peerInfo.getHost() == null || peerInfo.getHost().isBlank() || peerInfo.getPort() <= 0) {
                offlinePeers.add(peerInfo.getName() + " (" + peerInfo.getId() + ")");
                continue;
            }
            boolean online = App.peerNode.checkUserIsOnline(peerInfo.addressKey());
            if (!online) {
                offlinePeers.add(peerInfo.getName() + " (" + peerInfo.addressKey() + ")");
            }
        }
        if (!offlinePeers.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Không tìm thấy peer này hoặc peer không phản hồi ACK:\n"
                            + String.join("\n", offlinePeers),
                    "Không tìm thấy peer",
                    JOptionPane.WARNING_MESSAGE
            );
            log.warn("Đã chặn tạo nhóm vì bootstrap không khả dụng và có peer ngoại tuyến: {}", offlinePeers);
            return false;
        }
        return true;
    }
}
