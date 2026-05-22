package dungcony.ds.ui.pages;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dungcony.ds.ui.components.ModernScrollBarUI;
import dungcony.ds.ui.components.addFriendPage.BottomPanel;
import dungcony.ds.ui.components.addFriendPage.CenterPanel;
import dungcony.ds.ui.components.addFriendPage.HeadingPanel;
import dungcony.ds.ui.utils.ColorPalette;

import javax.swing.*;
import java.awt.*;

// Trang mở chat trực tiếp bằng địa chỉ IP
// @author Shoyeb Ansari
public class AddFriendPage extends JPanel {

    
    private static final Logger LOGGER = LoggerFactory.getLogger(AddFriendPage.class);
// Panel trên cùng chứa tiêu đề
    private HeadingPanel topPanel;
    // Panel giữa chứa logo, địa chỉ IP của người dùng hiện tại và chức năng sao chép
    private CenterPanel centerPanel;
    // Panel dưới dùng để thêm người dùng
    private BottomPanel bottomPanel;
    // Cung cấp thanh cuộn khi kích thước hiển thị nhỏ
    private JScrollPane scrollPane;
    private ScrollablePanel contentPanel;

    public AddFriendPage() {
        try {
            // Panel chính dùng BorderLayout
            setLayout(new BorderLayout());
            setBackground(ColorPalette.BACKGROUND);

            // Tạo panel cuộn chứa các thành phần
            contentPanel = new ScrollablePanel();
            contentPanel.setBackground(ColorPalette.BACKGROUND);
            contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

            // Khởi tạo component với ràng buộc kích thước phù hợp
            topPanel = new HeadingPanel("Bắt đầu chat trực tiếp");
            topPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

            centerPanel = new CenterPanel();
            centerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

            bottomPanel = new BottomPanel();
            centerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 400));

            // Thêm các component
            contentPanel.add(topPanel);
            contentPanel.add(centerPanel);
            contentPanel.add(bottomPanel);

            // Tạo thanh cuộn
            scrollPane = new JScrollPane(contentPanel);
            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            scrollPane.getVerticalScrollBar().setUnitIncrement(16); // Cuộn mượt hơn
            scrollPane.setBackground(ColorPalette.BACKGROUND);
            scrollPane.getViewport().setBackground(ColorPalette.BACKGROUND);

            // Đảm bảo thanh cuộn không làm xô lệch layout khi ẩn/hiện
            scrollPane.getVerticalScrollBar().setPreferredSize(
                new Dimension(scrollPane.getVerticalScrollBar().getPreferredSize().width, 0));

            // Tùy chỉnh thanh cuộn
            scrollPane.getVerticalScrollBar().setBackground(ColorPalette.BACKGROUND);
            scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());

            // Thêm vào trang với margin
            JPanel wrapperPanel = new JPanel(new BorderLayout());
            wrapperPanel.setBackground(ColorPalette.BACKGROUND);
            wrapperPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
            wrapperPanel.add(scrollPane, BorderLayout.CENTER);

            add(wrapperPanel, BorderLayout.CENTER);

            // Lắng nghe sự kiện thay đổi kích thước
            addComponentListener(new java.awt.event.ComponentAdapter() {
                public void componentResized(java.awt.event.ComponentEvent evt) {
                    try {
                        // Đảm bảo layout cập nhật đúng sau khi đổi kích thước
                        SwingUtilities.invokeLater(() -> {
                            contentPanel.revalidate();
                            scrollPane.revalidate();
                        });
                    } catch (Exception e) {
                        LOGGER.error("Chi tiết lỗi", e);
                    }
                }
            });
        } catch (Exception e) {
            LOGGER.error("Chi tiết lỗi", e);
        }
    }
    
    // Panel cuộn tùy chỉnh xử lý thay đổi chiều rộng
    // đồng thời vẫn cho phép cuộn dọc
    private class ScrollablePanel extends JPanel implements Scrollable {
        
        public ScrollablePanel() {
            super();
        }
        
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            try {
                return getPreferredSize();
            } catch (Exception e) {
                LOGGER.error("Chi tiết lỗi", e);
                return null;
            }
        }
        
        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            try {
                return 16;
            } catch (Exception e) {
                LOGGER.error("Chi tiết lỗi", e);
                return 0;
            }
        }
        
        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            try {
                return 100;
            } catch (Exception e) {
                LOGGER.error("Chi tiết lỗi", e);
                return 0;
            }
        }
        
        @Override
        public boolean getScrollableTracksViewportWidth() {
            try {
                return true;
            } catch (Exception e) {
                LOGGER.error("Chi tiết lỗi", e);
                return false;
            }
        }
        
        @Override
        public boolean getScrollableTracksViewportHeight() {
            try {
                return false;
            } catch (Exception e) {
                LOGGER.error("Chi tiết lỗi", e);
                return false;
            }
        }
    }
}
