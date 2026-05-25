package dungcony.ds.ui.components.chatPage;

import dungcony.ds.App;
import dungcony.ds.model.Group;
import dungcony.ds.model.Message;
import dungcony.ds.model.PeerInfo;
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

/**
 * Danh sách conversation, group và hành động nhanh của trang chat
 */
@Slf4j
public class ChatList extends JPanel {
    private static final Color ONLINE_COLOR = new Color(46, 125, 50);
    private static final Color OFFLINE_COLOR = new Color(211, 47, 47);

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

    }

    private JPanel createActionPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 1));
        panel.setBackground(ColorPalette.BACKGROUND);
        panel.add(createGroupButton);
        return panel;
    }

    private void renderFriends() {
        SwingUtilities.invokeLater(() -> {
            try {
                devicesContainer.removeAll();
                if (App.peerNode != null) {
                    addBroadcastProfile(App.peerNode.getLastBroadcastMessage());
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

    // Thêm item peer vào danh sách chat
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

    // Thêm item group vào danh sách chat
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

        chatProfile.add(createGroupActionPanel(group), BorderLayout.EAST);

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

    // Thêm conversation broadcast toàn mạng vào danh sách chat
    private void addBroadcastProfile(Message message) {
        String lastMessage = message == null
                ? "Tin nhắn phát tới peer online"
                : message.isFromCurrentUser() ? "Bạn: " + message.getContent() : message.getContent();
        String lastTime = message == null ? "" : message.getFormattedTime();
        JPanel deviceInfoPanel = createDeviceInfoPanel("[Thế giới]", lastMessage, lastTime, null);
        ChatProfile chatProfile = new ChatProfile(10, deviceInfoPanel, ColorPalette.BACKGROUND);
        chatProfile.setBackground(ColorPalette.BACKGROUND);

        MouseAdapter selector = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (parentChatPage != null) {
                    parentChatPage.onBroadcastSelected();
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

    private JPanel createGroupActionPanel(Group group) {
        JPanel panel = new JPanel(new GridLayout(1, 2, 4, 0));
        panel.setOpaque(false);
        panel.add(createRenameGroupButton(group));
        panel.add(createAddMemberButton(group));
        return panel;
    }

    private JButton createRenameGroupButton(Group group) {
        JButton button = createIconButton(FontAwesome.EDIT, "Đổi tên nhóm");
        button.addActionListener(event -> openRenameGroupDialog(group, button));
        return button;
    }

    private JButton createAddMemberButton(Group group) {
        JButton button = createIconButton(FontAwesome.USER_PLUS, "Thêm peer vào nhóm");
        button.addActionListener(event -> openAddMembersDialog(group, button));
        return button;
    }

    private JButton createIconButton(FontAwesome icon, String tooltip) {
        JButton button = new JButton();
        button.setIcon(FontIcon.of(icon, 14, ColorPalette.PRIMARY));
        button.setToolTipText(tooltip);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        button.setContentAreaFilled(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
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

    // Hiển thị dialog tạo group với các peer đang online
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
                boolean submitted = false;
                try {
                    submitted = showCreateGroupDialog(get());
                } catch (Exception e) {
                    log.error("Không thể mở hộp thoại tạo nhóm: {}", e.getMessage());
                } finally {
                    if (!submitted) {
                        createGroupButton.setEnabled(true);
                    }
                }
            }
        }.execute();
    }


    private void openAddMembersDialog(Group group, JButton sourceButton) {
        if (App.peerNode == null || group == null) {
            return;
        }
        sourceButton.setEnabled(false);
        new SwingWorker<List<PeerInfo>, Void>() {
            @Override
            protected List<PeerInfo> doInBackground() {
                return loadGroupCandidatesWithFreshStatus(group);
            }

            @Override
            protected void done() {
                boolean submitted = false;
                try {
                    submitted = showAddMembersDialog(group, get(), sourceButton);
                } catch (Exception e) {
                    log.error("Không thể mở hộp thoại thêm peer vào nhóm: {}", e.getMessage());
                } finally {
                    if (!submitted) {
                        sourceButton.setEnabled(true);
                    }
                }
            }
        }.execute();
    }

    private void openRenameGroupDialog(Group group, JButton sourceButton) {
        if (App.peerNode == null || group == null) {
            return;
        }
        JTextField nameField = new JTextField(group.getName());
        int choice = JOptionPane.showConfirmDialog(
                this,
                nameField,
                "Đổi tên nhóm",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }
        String newName = nameField.getText();
        if (newName == null || newName.isBlank()) {
            JOptionPane.showMessageDialog(this, "Tên nhóm không được để trống.", "Đổi tên nhóm",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        sourceButton.setEnabled(false);
        new SwingWorker<Group, Void>() {
            @Override
            protected Group doInBackground() {
                return App.peerNode.renameGroup(group.getGroupId(), newName);
            }

            @Override
            protected void done() {
                try {
                    Group renamedGroup = get();
                    if (renamedGroup == null) {
                        throw new IllegalStateException("Không tìm thấy nhóm.");
                    }
                    renderFriends();
                    if (parentChatPage != null) {
                        parentChatPage.refreshSelectedGroupName(renamedGroup);
                    }
                } catch (Exception e) {
                    log.error("Không thể đổi tên nhóm: {}", e.getMessage());
                    JOptionPane.showMessageDialog(ChatList.this, "Không thể đổi tên nhóm.", "Đổi tên nhóm",
                            JOptionPane.ERROR_MESSAGE);
                } finally {
                    sourceButton.setEnabled(true);
                }
            }
        }.execute();
    }

    // Lấy danh sách peer có uid và refresh online/offline trước khi hiển thị dialog
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

    // Hiển thị dialog tạo group sau khi trạng thái peer đã được refresh
    private boolean showCreateGroupDialog(List<PeerInfo> groupCandidates) {
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
            return false;
        }

        createGroupAsync(groupNameField.getText(), peerList.getSelectedValuesList(),
                peerIdField.getText(), groupCandidates);
        return true;
    }

    private boolean showAddMembersDialog(Group group, List<PeerInfo> groupCandidates, JButton sourceButton) {
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
            return false;
        }

        addMembersAsync(group, peerList.getSelectedValuesList(), peerIdField.getText(),
                groupCandidates, sourceButton);
        return true;
    }

    private void createGroupAsync(String groupName,
                                  List<PeerInfo> selectedPeers,
                                  String manualPeerIds,
                                  List<PeerInfo> groupCandidates) {
        createGroupButton.setEnabled(false);
        new SwingWorker<Group, Void>() {
            @Override
            protected Group doInBackground() {
                boolean bootstrapAvailable = App.peerNode.isBootstrapAvailable();
                List<PeerInfo> peers = resolveSelectedPeers(
                        selectedPeers, manualPeerIds, groupCandidates, Set.of(), bootstrapAvailable);
                validateGroupSelection(peers, bootstrapAvailable);
                return App.peerNode.createGroup(groupName, peers);
            }

            @Override
            protected void done() {
                try {
                    Group group = get();
                    log.info("UI đã tạo nhóm. groupId={}, sốThànhViên={}",
                            group.getGroupId(), group.getMembers().size());
                    renderFriends();
                } catch (Exception e) {
                    showGroupOperationError("Tạo nhóm", e);
                } finally {
                    createGroupButton.setEnabled(true);
                }
            }
        }.execute();
    }

    private void addMembersAsync(Group group,
                                 List<PeerInfo> selectedPeers,
                                 String manualPeerIds,
                                 List<PeerInfo> groupCandidates,
                                 JButton sourceButton) {
        sourceButton.setEnabled(false);
        new SwingWorker<Group, Void>() {
            @Override
            protected Group doInBackground() {
                boolean bootstrapAvailable = App.peerNode.isBootstrapAvailable();
                List<PeerInfo> peers = resolveSelectedPeers(
                        selectedPeers, manualPeerIds, groupCandidates, collectMemberIds(group), bootstrapAvailable);
                validateGroupSelection(peers, bootstrapAvailable);
                return App.peerNode.addMembersToGroup(group.getGroupId(), peers);
            }

            @Override
            protected void done() {
                try {
                    Group updatedGroup = get();
                    if (updatedGroup == null) {
                        throw new IllegalStateException("Không tìm thấy nhóm.");
                    }
                    log.info("UI đã thêm peer vào nhóm. groupId={}", updatedGroup.getGroupId());
                    renderFriends();
                    if (parentChatPage != null) {
                        parentChatPage.refreshSelectedGroupName(updatedGroup);
                    }
                } catch (Exception e) {
                    showGroupOperationError("Thêm peer", e);
                } finally {
                    sourceButton.setEnabled(true);
                }
            }
        }.execute();
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

    private List<PeerInfo> resolveSelectedPeers(List<PeerInfo> selectedPeers,
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
                throw new IllegalArgumentException("Không tìm thấy peer này: " + peerId);
            }
            result.add(new PeerInfo(peerId, peerId, "", 0, false));
        }
        return result;
    }

    private void validateGroupSelection(List<PeerInfo> selectedPeers, boolean bootstrapAvailable) {
        if (selectedPeers == null || selectedPeers.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn peer hoặc nhập peerId.");
        }
        if (!bootstrapAvailable) {
            List<String> offlinePeers = findUnreachableDirectGroupMembers(selectedPeers);
            if (!offlinePeers.isEmpty()) {
                throw new IllegalArgumentException("Không tìm thấy peer này hoặc peer không phản hồi ACK:\n"
                        + String.join("\n", offlinePeers));
            }
        }
    }

    private void showGroupOperationError(String title, Exception e) {
        Throwable cause = e;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        String message = cause.getMessage() == null || cause.getMessage().isBlank()
                ? "Không thể thực hiện thao tác nhóm."
                : cause.getMessage();
        log.error("{} thất bại: {}", title, message);
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.WARNING_MESSAGE);
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

    // Khi bootstrap-server tắt, group chỉ được tạo với peer đang TCP reachable
    private List<String> findUnreachableDirectGroupMembers(List<PeerInfo> selectedPeers) {
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
            log.warn("Đã chặn thao tác nhóm vì bootstrap không khả dụng và có peer ngoại tuyến: {}", offlinePeers);
        }
        return offlinePeers;
    }
}
