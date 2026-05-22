package dungcony.ds.ui.components.chatPage;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dungcony.ds.model.Message;
import dungcony.ds.ui.components.ModernScrollBarUI;
import dungcony.ds.ui.utils.ColorPalette;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

// Component lịch sử chat với bong bóng tin nhắn hiện đại
public class ChatHistory extends JPanel {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatHistory.class);
private JPanel messagesPanel;
    private JScrollPane scrollPane;

    public ChatHistory() {
        try {
            initializeComponents();
            setupLayout();
        } catch (Exception e) {
            LOGGER.error("Không thể khởi tạo lịch sử chat\nChi tiết lỗi: " + e.getMessage());
            LOGGER.error("Chi tiết lỗi", e);
        }
    }

    private void initializeComponents() {
        try {
            setBackground(ColorPalette.BACKGROUND);

            // Container chứa tin nhắn
            messagesPanel = new JPanel();
            messagesPanel.setLayout(new BoxLayout(messagesPanel, BoxLayout.Y_AXIS));
            messagesPanel.setBackground(ColorPalette.BACKGROUND);
            messagesPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

            // ScrollPane với giao diện hiện đại
            scrollPane = new JScrollPane(messagesPanel);
            scrollPane.setBackground(ColorPalette.BACKGROUND);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scrollPane.getVerticalScrollBar().setUnitIncrement(16);
            // Trang trí thanh cuộn
            scrollPane.getVerticalScrollBar().setBackground(ColorPalette.BACKGROUND);
            scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        } catch (Exception e) {
            LOGGER.error("Không thể khởi tạo component\nChi tiết lỗi: " + e.getMessage());
            LOGGER.error("Chi tiết lỗi", e);
        }
    }

    private void setupLayout() {
        try {
            setLayout(new BorderLayout());
            add(scrollPane, BorderLayout.CENTER);
        } catch (Exception e) {
            LOGGER.error("Không thể thiết lập bố cục\nChi tiết lỗi: " + e.getMessage());
            LOGGER.error("Chi tiết lỗi", e);
        }
    }

    // Hiển thị tất cả tin nhắn cùng lúc
    public void renderAllMessages(List<Message> messages) {
        try {
            // Xóa tin nhắn cũ
            messagesPanel.removeAll();

            // Thêm tất cả tin nhắn
            for (int i = 0; i < messages.size(); i++) {
                MessageBubble bubble = new MessageBubble(messages.get(i));

                // Căn chỉnh bong bóng
                bubble.setAlignmentX(JPanel.LEFT_ALIGNMENT);

                // Khoảng cách giữa các tin nhắn
                if (i > 0) {
                    messagesPanel.add(Box.createVerticalStrut(8));
                }

                messagesPanel.add(bubble);
            }

            // Đẩy tin nhắn lên trên
            messagesPanel.add(Box.createVerticalGlue());

            // Cập nhật giao diện
            messagesPanel.revalidate();
            messagesPanel.repaint();

            // Tự động cuộn xuống đáy
            SwingUtilities.invokeLater(() -> {
                try {
                    JScrollBar vertical = scrollPane.getVerticalScrollBar();
                    vertical.setValue(vertical.getMaximum());
                } catch (Exception ex) {
                    LOGGER.error("Không thể tự cuộn xuống cuối\nChi tiết lỗi: " + ex.getMessage());
                    LOGGER.error("Chi tiết lỗi", ex);
                }
            });
        } catch (Exception e) {
            LOGGER.error("Không thể hiển thị tất cả tin nhắn\nChi tiết lỗi: " + e.getMessage());
            LOGGER.error("Chi tiết lỗi", e);
        }
    }

    // Thêm từng tin nhắn (tin nhắn mới)
    public void renderMessage(Message message) {
        try {
            MessageBubble bubble = new MessageBubble(message);
            bubble.setAlignmentX(JPanel.LEFT_ALIGNMENT);

            // Khoảng cách giữa các tin nhắn
            if (messagesPanel.getComponentCount() > 0) {
                // Xóa glue nếu có
                int componentCount = messagesPanel.getComponentCount();
                if (componentCount > 0 && messagesPanel.getComponent(componentCount - 1) instanceof Box.Filler) {
                    messagesPanel.remove(componentCount - 1);
                }
                messagesPanel.add(Box.createVerticalStrut(8));
            }

            messagesPanel.add(bubble);
            // Thêm glue ở cuối
            messagesPanel.add(Box.createVerticalGlue());

            messagesPanel.revalidate();
            messagesPanel.repaint();

            // Tự động cuộn xuống đáy
            SwingUtilities.invokeLater(() -> {
                try {
                    JScrollBar vertical = scrollPane.getVerticalScrollBar();
                    vertical.setValue(vertical.getMaximum());
                } catch (Exception ex) {
                    LOGGER.error("Không thể tự cuộn xuống cuối\nChi tiết lỗi: " + ex.getMessage());
                    LOGGER.error("Chi tiết lỗi", ex);
                }
            });
        } catch (Exception e) {
            LOGGER.error("Không thể hiển thị tin nhắn\nChi tiết lỗi: " + e.getMessage());
            LOGGER.error("Chi tiết lỗi", e);
        }
    }

    public void clearMessages() {
        try {
            messagesPanel.removeAll();
            messagesPanel.revalidate();
            messagesPanel.repaint();
        } catch (Exception e) {
            LOGGER.error("Không thể xóa danh sách tin nhắn\nChi tiết lỗi: " + e.getMessage());
            LOGGER.error("Chi tiết lỗi", e);
        }
    }
}
