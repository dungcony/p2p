package dungcony.ds.ui.components.addFriendPage;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dungcony.ds.App;
import dungcony.ds.model.PeerInfo;
import dungcony.ds.ui.components.ModernButton;
import dungcony.ds.ui.components.RoundedPanel;
import dungcony.ds.ui.pages.ChatPage;
import dungcony.ds.ui.router.RouterManager;
import dungcony.ds.ui.utils.ColorPalette;
import dungcony.ds.ui.utils.Dialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class BottomPanel extends RoundedPanel {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(BottomPanel.class);
private ModernButton checkConnectionButton;
    private InputField nameField;
    private InputField ipField;

    public BottomPanel() {
        super(15, ColorPalette.BACKGROUND);

        nameField = new InputField("Tên", 50);
        ipField = new InputField("Địa chỉ IP hoặc host:port", 50);

        checkConnectionButton = new ModernButton("Chat", ColorPalette.PRIMARY, ColorPalette.SECONDARY);
        checkConnectionButton.setPreferredSize(new Dimension(140, 40));
        checkConnectionButton.addActionListener(e -> connectAndOpenChat());

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        setPreferredSize(new Dimension(400, 200));
        setMinimumSize(new Dimension(350, 180));

        JPanel inputPanel = new JPanel();
        inputPanel.setOpaque(false);
        inputPanel.setLayout(new GridLayout(2, 1, 0, 15));
        nameField.setPreferredSize(new Dimension(350, 50));
        ipField.setPreferredSize(new Dimension(350, 50));
        inputPanel.add(nameField);
        inputPanel.add(ipField);
        inputPanel.setPreferredSize(new Dimension(350, 115));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 0));
        buttonPanel.add(checkConnectionButton);
        buttonPanel.setPreferredSize(new Dimension(350, 50));

        add(inputPanel);
        add(Box.createVerticalStrut(20));
        add(buttonPanel);
    }

    private boolean connectAndOpenChat() {
        String name = nameField.getTextField().getText();
        String address = ipField.getTextField().getText();
        LOGGER.info("Yêu cầu chat trực tiếp. tên=" + name + ", địaChỉ=" + address);

        if (!isValidPeerAddress(address)) {
            LOGGER.warn("Từ chối chat trực tiếp: địa chỉ không hợp lệ=" + address);
            Dialog.showMessageDialog(null, "Vui lòng nhập địa chỉ IP hoặc host:port hợp lệ", "Địa chỉ không hợp lệ", Dialog.ERROR_MESSAGE);
            return false;
        }
        if (App.peerNode != null && App.peerNode.isSelfAddress(address)) {
            LOGGER.warn("Từ chối chat trực tiếp: địa chỉ trỏ về peer hiện tại=" + address);
            Dialog.showMessageDialog(null, "Bạn không thể kết nối tới chính peer hiện tại.", "Peer không hợp lệ", Dialog.WARNING_MESSAGE);
            return false;
        }

        boolean online = App.peerNode != null && App.peerNode.checkUserIsOnline(address);
        if (!online) {
            LOGGER.warn("Chat trực tiếp thất bại heartbeat. địa chỉ=" + address);
            Dialog.showMessageDialog(null, "Peer không phản hồi heartbeat.", "Peer ngoại tuyến", Dialog.WARNING_MESSAGE);
            return false;
        }

        String displayName = name == null || name.isBlank() ? address.trim() : name.trim();
        PeerInfo peerInfo = App.peerNode.addKnownPeer(displayName, address);
        if (peerInfo == null) {
            Dialog.showMessageDialog(null, "Không thể mở cuộc chat với peer.", "Mở chat thất bại", Dialog.WARNING_MESSAGE);
            return false;
        }
        App.peerNode.discoverPeersFromKnownPeer(peerInfo);
        openChat(peerInfo);
        nameField.getTextField().setText("");
        ipField.getTextField().setText("");
        LOGGER.info("Đã mở chat trực tiếp. peer=" + peerInfo.addressKey());
        return true;
    }

    // Chuyển thẳng sẵng trang chat và mở conversation vừa connect.
    private void openChat(PeerInfo peerInfo) {
        RouterManager routerManager = RouterManager.getInstance();
        routerManager.navigateTo("chats");
        Component route = routerManager.getRoute("chats");
        if (route instanceof ChatPage chatPage) {
            chatPage.onChatSelected(peerInfo.getName(), peerInfo.addressKey());
        }
    }

    public static boolean isValidPeerAddress(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String host = value.trim();
        int colonIndex = host.lastIndexOf(':');
        if (colonIndex > 0 && colonIndex < host.length() - 1) {
            try {
                int port = Integer.parseInt(host.substring(colonIndex + 1));
                if (port < 1 || port > 65535) {
                    return false;
                }
                host = host.substring(0, colonIndex);
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return isValidIPAddress(host) || "localhost".equalsIgnoreCase(host);
    }

    public static boolean isValidIPAddress(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }

        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }

        for (String part : parts) {
            try {
                if (part.isEmpty() || (part.length() > 1 && part.startsWith("0"))) {
                    return false;
                }

                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }

        return true;
    }
}
