package dungcony.ds.ui.components.chatPage;

import dungcony.ds.model.Message;
import dungcony.ds.ui.utils.ColorPalette;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Component bong bóng tin nhắn
 *
 * @author Shoyeb Ansari
 */
class MessageBubble extends JPanel {
    /**
     * Tin nhắn cần hiển thị
     */
    private Message message;

    public MessageBubble(Message message) {
        try {
            this.message = message;
            initializeComponents();
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to initialize MessageBubble\nError Message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Phương thức khởi tạo các thành phần
     */
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
            System.out.println("[ERROR] Failed to initialize components\nError Message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Phương thức tạo panel nội dung tin nhắn
     *
     * @return Panel tin nhắn
     */
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

            // Time label
            JLabel timeLabel = new JLabel(message.getFormattedTime());
            timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            timeLabel.setForeground(message.isFromCurrentUser() ?
                    ColorPalette.PANEL_BACKGROUND.darker() : ColorPalette.SECONDARY_TEXT);
            timeLabel.setBorder(new EmptyBorder(4, 0, 0, 0));

            // Set alignment
            textArea.setAlignmentX(Component.LEFT_ALIGNMENT);
            timeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            // Constrain the panel width
            int maxWidth = 320; // 300 + some padding
            panel.setMaximumSize(new Dimension(maxWidth, Integer.MAX_VALUE));

            panel.add(textArea);
            panel.add(timeLabel);

            return panel;
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to create message content\nError Message: " + e.getMessage());
            e.printStackTrace();
            return new JPanel();
        }
    }

}
