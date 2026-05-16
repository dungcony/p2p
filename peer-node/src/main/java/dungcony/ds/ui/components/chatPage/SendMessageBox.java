package dungcony.ds.ui.components.chatPage;

import dungcony.ds.App;
import dungcony.ds.ui.components.ModernButton;
import dungcony.ds.ui.components.RoundedBorder;
import dungcony.ds.ui.utils.ColorPalette;

import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Send message box with Text Field Input and Button 
 */
class SendMessageBox extends JPanel {
    /** Message Field */
    private SendMessageTextField messageField;
    /** Nút gửi tin nhắn */
    private ModernButton sendButton;
    /** reference to parent Screen  */
    private ChatScreen parentScreen;

    public SendMessageBox(ChatScreen parentScreen) {
        try {
            this.parentScreen = parentScreen;
            initializeComponents();
            setupLayout();
            setupEventHandlers();
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to initialize SendMessageBox\nError Message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initializeComponents() {
        try {
            setBackground(ColorPalette.PANEL_BACKGROUND);
            setBorder(new EmptyBorder(10, 15, 10, 15));
    
            // Message input field with modern styling
            messageField = new SendMessageTextField("Type your message here...");
            messageField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            messageField.setBackground(ColorPalette.BACKGROUND);
            messageField.setForeground(ColorPalette.TEXT);
            messageField.setBorder(BorderFactory.createCompoundBorder(
                    new RoundedBorder(20, ColorPalette.BACKGROUND),
                    new EmptyBorder(12, 16, 12, 16)));
            messageField.setPlaceholderColor(ColorPalette.SECONDARY_TEXT);
    
            // Send button with modern styling
            sendButton = new ModernButton("", ColorPalette.PRIMARY, ColorPalette.SECONDARY);
            FontIcon icon = FontIcon.of(FontAwesome.SEND, 16);
            icon.setIconColor(ColorPalette.PANEL_BACKGROUND);
            sendButton.setIcon(icon);
            sendButton.setPreferredSize(new Dimension(50, 44));
            sendButton.setBorder(new RoundedBorder(22, ColorPalette.PRIMARY));
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to initialize components\nError Message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupLayout() {
        try {
            setLayout(new BorderLayout(12, 0));
            add(messageField, BorderLayout.CENTER);
            add(sendButton, BorderLayout.EAST);
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to set up layout\nError Message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Thêm sự kiện click nút và nhấn Enter */
    private void setupEventHandlers() {
        try {
            // Send button action
            sendButton.addActionListener(e -> {
                try {
                    sendMessage();
                } catch (Exception ex) {
                    System.out.println("[ERROR] Failed to handle send button action\nError Message: " + ex.getMessage());
                    ex.printStackTrace();
                }
            });

            // Enter key action
            messageField.addActionListener(e -> {
                try {
                    sendMessage();
                } catch (Exception ex) {
                    System.out.println("[ERROR] Failed to handle enter key action\nError Message: " + ex.getMessage());
                    ex.printStackTrace();
                }
            });
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to set up event handlers\nError Message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** 
     * Gửi tin nhắn qua PeerNode. Lịch sử và ACK do tầng peer/network xử lý.
     */
    private void sendMessage() {
        try {
            String messageText = messageField.getText();

            if (!messageText.isBlank()) {
                String ip = parentScreen.getIpAddress();
                String groupId = parentScreen.getGroupId();
                if (!parentScreen.isGroupChat() && (ip == null || ip.isBlank())) {
                    System.out.println("[WARN] UI send ignored because no peer is selected.");
                    return;
                }
                if (!parentScreen.isGroupChat() && App.peerNode != null && App.peerNode.isSelfAddress(ip)) {
                    System.out.println("[WARN] UI send ignored because selected peer is local peer: " + ip);
                    return;
                }
                System.out.println("[INFO] UI send message requested. target=" + (parentScreen.isGroupChat() ? groupId : ip)
                        + ", length=" + messageText.length());

                // Clear the field immediately for better UX
                messageField.setText("");

                // Disable send button to prevent multiple sends
                sendButton.setEnabled(false);
                messageField.setEnabled(false);

                // Use SwingWorker for background network operation
                new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        try {
                            if (App.peerNode != null) {
                                if (parentScreen.isGroupChat()) {
                                    App.peerNode.sendGroupMessage(groupId, messageText);
                                } else {
                                    App.peerNode.sendMessage(messageText, ip);
                                }
                            } else {
                                System.out.println("[WARN] Cannot send message because App.peerNode is null.");
                            }
                        } catch (Exception ex) {
                            System.out.println("[ERROR] Failed to send message in background\nError Message: " + ex.getMessage());
                            ex.printStackTrace();
                        }
                        return null;
                    }

                    @Override
                    protected void done() {
                        try {
                            // Chạy trên EDT khi tác vụ nền hoàn thành
                            get(); // Check if any exception occurred
                        } catch (Exception e) {
                            System.out.println("[ERROR] Failed to complete message sending\nError Message: " + e.getMessage());
                            e.printStackTrace();
                        } finally {
                            // Re-enable UI components
                            sendButton.setEnabled(true);
                            messageField.setEnabled(true);
                            messageField.requestFocus();
                        }
                    }
                }.execute();
            }
            else {
                System.out.println("[DEBUG] Ignored blank message submit.");
            }
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to send message\nError Message: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
