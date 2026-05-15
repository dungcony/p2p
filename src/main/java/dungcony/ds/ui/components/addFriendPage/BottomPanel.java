package dungcony.ds.ui.components.addFriendPage;

import dungcony.ds.App;
import dungcony.ds.ui.components.ModernButton;
import dungcony.ds.ui.components.RoundedPanel;
import dungcony.ds.ui.utils.ColorPalette;
import dungcony.ds.ui.utils.Dialog;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

public class BottomPanel extends RoundedPanel {
    private ModernButton checkConnectionButton;
    private ModernButton addFriendButton;
    private InputField nameField;
    private InputField ipField;

    public BottomPanel() {
        super(15, ColorPalette.BACKGROUND);

        nameField = new InputField("Name", 50);
        ipField = new InputField("IP Address or host:port", 50);

        checkConnectionButton = new ModernButton("Connect", ColorPalette.PRIMARY, ColorPalette.SECONDARY);
        checkConnectionButton.setPreferredSize(new Dimension(120, 40));

        addFriendButton = new ModernButton("Add", ColorPalette.PRIMARY, ColorPalette.SECONDARY);
        addFriendButton.setPreferredSize(new Dimension(120, 40));

        FontIcon plusIcon = FontIcon.of(FontAwesome.PLUS, 14);
        plusIcon.setIconColor(Color.WHITE);
        addFriendButton.setIcon(plusIcon);
        checkConnectionButton.addActionListener(e -> connectFriend(false));
        addFriendButton.addActionListener(this::addFriend);

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
        buttonPanel.add(addFriendButton);
        buttonPanel.setPreferredSize(new Dimension(350, 50));

        add(inputPanel);
        add(Box.createVerticalStrut(20));
        add(buttonPanel);
    }

    private void addFriend(ActionEvent e) {
        String name = nameField.getTextField().getText();
        String address = ipField.getTextField().getText();
        System.out.println("[INFO] AddFriend requested. name=" + name + ", address=" + address);

        if (name == null || name.isBlank()) {
            System.out.println("[WARN] AddFriend rejected: empty name.");
            Dialog.showMessageDialog(null, "Please enter name of your friend", "Empty input fields", Dialog.ERROR_MESSAGE);
            return;
        }

        if (!isValidPeerAddress(address)) {
            System.out.println("[WARN] AddFriend rejected: invalid address=" + address);
            Dialog.showMessageDialog(null, "Please provide valid IP address or host:port", "Invalid address", Dialog.ERROR_MESSAGE);
            return;
        }
        if (App.peerNode != null && App.peerNode.isSelfAddress(address)) {
            System.out.println("[WARN] AddFriend rejected: address points to local peer=" + address);
            Dialog.showMessageDialog(null, "You cannot add your own peer address.", "Invalid peer", Dialog.WARNING_MESSAGE);
            return;
        }

        if (App.peerNode != null) {
            if (App.peerNode.addKnownPeer(name, address) == null) {
                Dialog.showMessageDialog(null, "Peer was not added.", "Add Friend Failed", Dialog.WARNING_MESSAGE);
                return;
            }
        } else {
            System.out.println("[WARN] AddFriend skipped because App.peerNode is null.");
        }

        nameField.getTextField().setText("");
        ipField.getTextField().setText("");
        Dialog.showConfirmDialog(null, "Your friend added successfully", "Add Friend Success", Dialog.CLOSED_OPTION, Dialog.INFORMATION_MESSAGE);
    }

    private boolean connectFriend(boolean isFromAddFriend) {
        String address = ipField.getTextField().getText();
        System.out.println("[INFO] ConnectFriend requested. address=" + address
                + ", fromAddFriend=" + isFromAddFriend);
        if (!isValidPeerAddress(address)) {
            System.out.println("[WARN] ConnectFriend rejected: invalid address=" + address);
            Dialog.showMessageDialog(null, "Please provide valid IP address or host:port", "Invalid address", Dialog.ERROR_MESSAGE);
            return false;
        }
        if (App.peerNode != null && App.peerNode.isSelfAddress(address)) {
            System.out.println("[WARN] ConnectFriend rejected: address points to local peer=" + address);
            Dialog.showMessageDialog(null, "You cannot connect to your own peer address.", "Invalid peer", Dialog.WARNING_MESSAGE);
            return false;
        }

        boolean online = App.peerNode != null && App.peerNode.checkUserIsOnline(address);
        if (!online) {
            System.out.println("[WARN] ConnectFriend failed heartbeat. address=" + address);
            Dialog.showMessageDialog(null, "Peer did not respond to heartbeat.", "Offline peer", Dialog.WARNING_MESSAGE);
            return false;
        }

        System.out.println("[INFO] ConnectFriend succeeded. address=" + address);
        if (!isFromAddFriend) {
            Dialog.showConfirmDialog(null, "Peer is online. You can add it to your friend list.", "Peer connected", Dialog.CLOSED_OPTION, Dialog.INFORMATION_MESSAGE);
        }
        return true;
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
