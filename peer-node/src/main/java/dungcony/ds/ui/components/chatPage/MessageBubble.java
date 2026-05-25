package dungcony.ds.ui.components.chatPage;

import lombok.extern.slf4j.Slf4j;


import dungcony.ds.App;
import dungcony.ds.enums.MessageStatus;
import dungcony.ds.enums.MessageType;
import dungcony.ds.model.Message;
import dungcony.ds.ui.utils.ColorPalette;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

// Component bong bóng tin nhắn
//
// @author Shoyeb Ansari
@Slf4j
class MessageBubble extends JPanel {
// Tin nhắn cần hiển thị
    private Message message;

    public MessageBubble(Message message) {
        try {
            this.message = message;
            initializeComponents();
        } catch (Exception e) {
            log.error("Không thể khởi tạo bong bóng tin nhắn\nChi tiết lỗi: {}", e.getMessage());
            log.error("Chi tiết lỗi", e);
        }
    }

    // Phương thức khởi tạo các thành phần
    private void initializeComponents() {
        try {
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            setBackground(ColorPalette.BACKGROUND);

            // Đặt chiều cao tối đa để tránh lãng phí không gian dọc
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

            // Tạo panel nội dung tin nhắn
            JPanel messageContent = createMessageContent();

            // Căn lề dựa trên người gửi
            if (message.isFromCurrentUser()) {
                // Căn phải cho người dùng hiện tại
                add(Box.createHorizontalGlue());
                add(messageContent);
            } else {
                // Căn trái cho người dùng khác
                add(messageContent);
                add(Box.createHorizontalGlue());
            }
        } catch (Exception e) {
            log.error("Không thể khởi tạo component\nChi tiết lỗi: {}", e.getMessage());
            log.error("Chi tiết lỗi", e);
        }
    }

    // Phương thức tạo panel nội dung tin nhắn
    //
    // @return Panel tin nhắn
    private JPanel createMessageContent() {
        try {
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

            // Đặt màu bong bóng dựa trên người gửi
            Color bubbleColor = message.isFromCurrentUser() ?
                    ColorPalette.PRIMARY : ColorPalette.PANEL_BACKGROUND;
            Color textColor = message.isFromCurrentUser() ?
                    ColorPalette.PANEL_BACKGROUND : ColorPalette.TEXT;

            panel.setBackground(bubbleColor);
            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.black),
                    new EmptyBorder(12, 16, 12, 16)
            ));

            // Sử dụng HTML để hiển thị văn bản ổn định
            String content = message.getContent()
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\n", "<br>");

            String htmlContent = String.format(
                    "<html><body style='width: 270px; font-family: Segoe UI; font-size: 12px; margin: 0; padding: 0;'>%s</body></html>",
                    content
            );

            JLabel textArea = new JLabel(htmlContent);
            textArea.setForeground(textColor);
            textArea.setOpaque(false);
            textArea.setVerticalAlignment(SwingConstants.TOP);
            JLabel senderLabel = createBroadcastSenderLabel(textColor);

            // Nhãn thời gian
            JLabel timeLabel = new JLabel(message.getFormattedTime());
            timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            timeLabel.setForeground(message.isFromCurrentUser() ?
                    ColorPalette.PANEL_BACKGROUND.darker() : ColorPalette.SECONDARY_TEXT);
            timeLabel.setBorder(new EmptyBorder(4, 0, 0, 0));
            JLabel statusLabel = createStatusLabel();
            JButton retryButton = createRetryButton();

            // Đặt căn chỉnh
            textArea.setAlignmentX(Component.LEFT_ALIGNMENT);
            if (senderLabel != null) {
                senderLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            }
            timeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            if (statusLabel != null) {
                statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            }
            if (retryButton != null) {
                retryButton.setAlignmentX(Component.LEFT_ALIGNMENT);
            }

            // Giới hạn chiều rộng panel
            int maxWidth = 320; // 300 + some padding
            panel.setMaximumSize(new Dimension(maxWidth, Integer.MAX_VALUE));

            if (senderLabel != null) {
                panel.add(senderLabel);
                panel.add(Box.createVerticalStrut(4));
            }
            panel.add(textArea);
            panel.add(timeLabel);
            if (statusLabel != null) {
                panel.add(statusLabel);
            }
            if (retryButton != null) {
                panel.add(Box.createVerticalStrut(4));
                panel.add(retryButton);
            }

            return panel;
        } catch (Exception e) {
            log.error("Không thể tạo nội dung tin nhắn\nChi tiết lỗi: {}", e.getMessage());
            log.error("Chi tiết lỗi", e);
            return new JPanel();
        }
    }

    private JLabel createBroadcastSenderLabel(Color textColor) {
        if (message.getType() != MessageType.BROADCAST || message.isFromCurrentUser()) {
            return null;
        }
        String sender = message.getSenderId() == null || message.getSenderId().isBlank()
                ? "Peer"
                : message.getSenderId();
        JLabel label = new JLabel(sender);
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
        label.setForeground(textColor);
        return label;
    }

    // Tạo label trạng thái gửi tin cho message của user hiện tại
    private JLabel createStatusLabel() {
        if (!message.isFromCurrentUser()) {
            return null;
        }
        JLabel label = new JLabel(statusText(message.getStatus()));
        label.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        label.setForeground(message.isFromCurrentUser()
                ? ColorPalette.PANEL_BACKGROUND.darker()
                : ColorPalette.SECONDARY_TEXT);
        label.setBorder(new EmptyBorder(2, 0, 0, 0));
        return label;
    }

    // Tạo nút retry thủ công cho tin nhắn 1-1 bị FAILED
    private JButton createRetryButton() {
        if (!message.isFromCurrentUser()
                || message.getStatus() != MessageStatus.FAILED
                || message.getType() == MessageType.BROADCAST
                || (message.getGroupId() != null && !message.getGroupId().isBlank())) {
            return null;
        }
        JButton button = new JButton("Thử lại");
        button.setFocusPainted(false);
        button.setFont(new Font("Segoe UI", Font.BOLD, 11));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.addActionListener(event -> {
            button.setEnabled(false);
            new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() {
                    return App.peerNode != null && App.peerNode.retryMessage(message);
                }
            }.execute();
        });
        return button;
    }

    private String statusText(MessageStatus status) {
        return switch (status) {
            case SENDING -> "Đang gửi";
            case SENT -> "Đã gửi";
            case PENDING -> "Chờ server giao";
            case FAILED -> "Gửi lỗi";
        };
    }

}
