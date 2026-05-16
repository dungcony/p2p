package dungcony.ds.ui.pages;

import dungcony.ds.ui.components.ModernScrollBarUI;
import dungcony.ds.ui.components.addFriendPage.BottomPanel;
import dungcony.ds.ui.components.addFriendPage.CenterPanel;
import dungcony.ds.ui.components.addFriendPage.HeadingPanel;
import dungcony.ds.ui.utils.ColorPalette;

import javax.swing.*;
import java.awt.*;

/** Trang mo chat truc tiep bang dia chi IP
 * @author Shoyeb Ansari
 */
public class AddFriendPage extends JPanel {

    /** Top panel which contains heading */
    private HeadingPanel topPanel;
    /** Center panel which contains logo and the curent users' ip address and providing copy functionality */
    private CenterPanel centerPanel;
    /** Bottom panel where we can add user */
    private BottomPanel bottomPanel;
    /** To provide the functionality of scroll bar when the dimensions are low */
    private JScrollPane scrollPane;
    private ScrollablePanel contentPanel;

    public AddFriendPage() {
        try {
            // Main panel uses BorderLayout
            setLayout(new BorderLayout());
            setBackground(ColorPalette.BACKGROUND);

            // Tạo panel cuộn chứa các thành phần
            contentPanel = new ScrollablePanel();
            contentPanel.setBackground(ColorPalette.BACKGROUND);
            contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

            // Initialize components with proper constraints
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
            scrollPane.getVerticalScrollBar().setUnitIncrement(16); // Smoother scrolling
            scrollPane.setBackground(ColorPalette.BACKGROUND);
            scrollPane.getViewport().setBackground(ColorPalette.BACKGROUND);

            // Ensure scrollbar doesn't cause layout thrashing when appearing/disappearing
            scrollPane.getVerticalScrollBar().setPreferredSize(
                new Dimension(scrollPane.getVerticalScrollBar().getPreferredSize().width, 0));

            // Style the scrollbar
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
                        // Ensure layout updates properly after resize
                        SwingUtilities.invokeLater(() -> {
                            contentPanel.revalidate();
                            scrollPane.revalidate();
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Panel cuộn tùy chỉnh xử lý thay đổi chiều rộng
     * while allowing vertical scrolling
     */
    private class ScrollablePanel extends JPanel implements Scrollable {
        
        public ScrollablePanel() {
            super();
        }
        
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            try {
                return getPreferredSize();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        
        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            try {
                return 16;
            } catch (Exception e) {
                e.printStackTrace();
                return 0;
            }
        }
        
        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            try {
                return 100;
            } catch (Exception e) {
                e.printStackTrace();
                return 0;
            }
        }
        
        @Override
        public boolean getScrollableTracksViewportWidth() {
            try {
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        
        @Override
        public boolean getScrollableTracksViewportHeight() {
            try {
                return false;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }
}
