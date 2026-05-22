package dungcony.ds.ui.components.chatPage;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dungcony.ds.App;
import dungcony.ds.ui.components.ModernButton;
import dungcony.ds.ui.components.RoundedBorder;
import dungcony.ds.ui.utils.ColorPalette;

import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

// Hộp gửi tin nhắn gồm ô nhập và nút gửi
class SendMessageBox extends JPanel {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SendMessageBox.class);
// Ô nhập tin nhắn
    private SendMessageTextField messageField;
    // Nút gửi tin nhắn
    private ModernButton sendButton;
    // Tham chiếu tới màn hình cha
    private ChatScreen parentScreen;

    public SendMessageBox(ChatScreen parentScreen) {
        try {
            this.parentScreen = parentScreen;
            initializeComponents();
            setupLayout();
            setupEventHandlers();
        } catch (Exception e) {
            LOGGER.error("Không thể khởi tạo ô gửi tin nhắn\nChi tiết lỗi: " + e.getMessage());
            LOGGER.error("Chi tiết lỗi", e);
        }
    }

    private void initializeComponents() {
        try {
            setBackground(ColorPalette.PANEL_BACKGROUND);
            setBorder(new EmptyBorder(10, 15, 10, 15));
    
            // Ô nhập tin nhắn với giao diện hiện đại
            messageField = new SendMessageTextField("Nhập tin nhắn...");
            messageField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            messageField.setBackground(ColorPalette.BACKGROUND);
            messageField.setForeground(ColorPalette.TEXT);
            messageField.setBorder(BorderFactory.createCompoundBorder(
                    new RoundedBorder(20, ColorPalette.BACKGROUND),
                    new EmptyBorder(12, 16, 12, 16)));
            messageField.setPlaceholderColor(ColorPalette.SECONDARY_TEXT);
    
            // Nút gửi với giao diện hiện đại
            sendButton = new ModernButton("", ColorPalette.PRIMARY, ColorPalette.SECONDARY);
            FontIcon icon = FontIcon.of(FontAwesome.SEND, 16);
            icon.setIconColor(ColorPalette.PANEL_BACKGROUND);
            sendButton.setIcon(icon);
            sendButton.setPreferredSize(new Dimension(50, 44));
            sendButton.setBorder(new RoundedBorder(22, ColorPalette.PRIMARY));
        } catch (Exception e) {
            LOGGER.error("Không thể khởi tạo component\nChi tiết lỗi: " + e.getMessage());
            LOGGER.error("Chi tiết lỗi", e);
        }
    }

    private void setupLayout() {
        try {
            setLayout(new BorderLayout(12, 0));
            add(messageField, BorderLayout.CENTER);
            add(sendButton, BorderLayout.EAST);
        } catch (Exception e) {
            LOGGER.error("Không thể thiết lập bố cục\nChi tiết lỗi: " + e.getMessage());
            LOGGER.error("Chi tiết lỗi", e);
        }
    }

    // Thêm sự kiện click nút và nhấn Enter
    private void setupEventHandlers() {
        try {
            // Sự kiện nút gửi
            sendButton.addActionListener(e -> {
                try {
                    sendMessage();
                } catch (Exception ex) {
                    LOGGER.error("Không thể xử lý nút gửi\nChi tiết lỗi: " + ex.getMessage());
                    LOGGER.error("Chi tiết lỗi", ex);
                }
            });

            // Sự kiện phím Enter
            messageField.addActionListener(e -> {
                try {
                    sendMessage();
                } catch (Exception ex) {
                    LOGGER.error("Không thể xử lý phím Enter\nChi tiết lỗi: " + ex.getMessage());
                    LOGGER.error("Chi tiết lỗi", ex);
                }
            });
        } catch (Exception e) {
            LOGGER.error("Không thể thiết lập xử lý sự kiện\nChi tiết lỗi: " + e.getMessage());
            LOGGER.error("Chi tiết lỗi", e);
        }
    }

    // Gửi tin nhắn qua PeerNode. Lịch sử và ACK do tầng peer/network xử lý.
    private void sendMessage() {
        try {
            String messageText = messageField.getText();

            if (!messageText.isBlank()) {
                String ip = parentScreen.getIpAddress();
                String groupId = parentScreen.getGroupId();
                if (!parentScreen.isGroupChat() && (ip == null || ip.isBlank())) {
                    LOGGER.warn("Bỏ qua gửi tin vì chưa chọn peer.");
                    return;
                }
                if (!parentScreen.isGroupChat() && App.peerNode != null && App.peerNode.isSelfAddress(ip)) {
                    LOGGER.warn("Bỏ qua gửi tin vì peer được chọn là peer hiện tại: " + ip);
                    return;
                }
                LOGGER.info("UI yêu cầu gửi tin. đích=" + (parentScreen.isGroupChat() ? groupId : ip)
                        + ", độDài=" + messageText.length());

                // Xóa ô nhập ngay để cải thiện trải nghiệm
                messageField.setText("");

                // Tắt nút gửi để tránh gửi nhiều lần
                sendButton.setEnabled(false);
                messageField.setEnabled(false);

                // Dùng SwingWorker để chạy thao tác mạng trong nền
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
                                LOGGER.warn("Không thể gửi tin vì App.peerNode đang null.");
                            }
                        } catch (Exception ex) {
                            LOGGER.error("Không thể gửi tin trong nền\nChi tiết lỗi: " + ex.getMessage());
                            LOGGER.error("Chi tiết lỗi", ex);
                        }
                        return null;
                    }

                    @Override
                    protected void done() {
                        try {
                            // Chạy trên EDT khi tác vụ nền hoàn thành
                            get(); // Check if any exception occurred
                        } catch (Exception e) {
                            LOGGER.error("Không thể hoàn tất gửi tin\nChi tiết lỗi: " + e.getMessage());
                            LOGGER.error("Chi tiết lỗi", e);
                        } finally {
                            // Bật lại các component UI
                            sendButton.setEnabled(true);
                            messageField.setEnabled(true);
                            messageField.requestFocus();
                        }
                    }
                }.execute();
            }
            else {
                LOGGER.debug("Đã bỏ qua tin nhắn rỗng.");
            }
        } catch (Exception e) {
            LOGGER.error("Không thể gửi tin nhắn\nChi tiết lỗi: " + e.getMessage());
            LOGGER.error("Chi tiết lỗi", e);
        }
    }
}
