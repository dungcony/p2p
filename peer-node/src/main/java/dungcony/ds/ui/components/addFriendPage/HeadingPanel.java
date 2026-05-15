package dungcony.ds.ui.components.addFriendPage;


import dungcony.ds.ui.utils.ColorPalette;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class HeadingPanel extends JPanel {
    private static final int SMALL_WIDTH = 576;
    private static final int MEDIUM_WIDTH = 768;
    /** Nhãn chứa chữ */
    private JLabel textLabel;
    
    public HeadingPanel(String text) {
        setBackground(ColorPalette.BACKGROUND);
        setLayout(new FlowLayout());
        textLabel = new JLabel(text, SwingConstants.CENTER);
        textLabel.setFont(new Font("Segoe UI", Font.BOLD, 38));
        textLabel.setForeground(ColorPalette.PRIMARY);
        
        setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        
        this.add(textLabel, BorderLayout.CENTER);
        // Lắng nghe sự kiện thay đổi kích thước
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                adjustFontSize(getWidth());
            }
        });
    }

    /**
     * Điều chỉnh cỡ chữ theo chiều rộng
     * @param width chiều rộng cửa sổ
     */
    private void adjustFontSize(int width) {
        int fontSize;
        if (width < SMALL_WIDTH) {         // sm
            fontSize = 30;
        } else if (width < MEDIUM_WIDTH) {  // md
            fontSize = 35;
        } else {                   // xl
            fontSize = 40;
        }

        textLabel.setFont(new Font("Arial", Font.BOLD, fontSize));
        revalidate();
        repaint();
    }
}
