package dungcony.ds.ui.components;


import dungcony.ds.ui.utils.ColorPalette;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import java.awt.*;


/** Component vẽ biểu tượng tròn */
public class CircularIcon extends JPanel {

    /** Biểu tượng */
    private FontIcon userIcon;
    /** Màu nền của biểu tượng */
    private Color circleColor;
    /** Màu biểu tượng */
    private Color iconColor;

    /**
     * @param icon biểu tượng cần hiển thị trong vòng tròn
     */
    public CircularIcon(FontIcon icon) {
        this.userIcon = icon;
        this.circleColor = ColorPalette.PRIMARY;
        this.iconColor = Color.WHITE;
        
        this.setOpaque(false);
        this.setPreferredSize(new Dimension(46, 46));
        this.setMinimumSize(new Dimension(46, 46));
        this.setMaximumSize(new Dimension(46, 46));
    }
    
    public CircularIcon(FontIcon icon, Color circleColor, Color iconColor) {
        this.userIcon = icon;
        this.circleColor = circleColor;
        this.iconColor = iconColor;
        
        this.setOpaque(false);
        this.setPreferredSize(new Dimension(46, 46));
        this.setMinimumSize(new Dimension(46, 46));
        this.setMaximumSize(new Dimension(46, 46));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int diameter = Math.min(getWidth(), getHeight()) - 2; // Leave 1px margin
        int x = (getWidth() - diameter) / 2;
        int y = (getHeight() - diameter) / 2;
        
        // Vẽ bóng
        g2d.setColor(new Color(0, 0, 0, 30));
        g2d.fillOval(x + 1, y + 1, diameter, diameter);
        
        // Vẽ vòng tròn chính
        g2d.setColor(circleColor);
        g2d.fillOval(x, y, diameter, diameter);
        
        // Vẽ viền trong để tạo chiều sâu
        g2d.setColor(new Color(255, 255, 255, 40));
        g2d.drawOval(x + 1, y + 1, diameter - 2, diameter - 2);

        // Vẽ biểu tượng ở giữa
        if (userIcon != null) {
            // Đặt màu biểu tượng
            userIcon.setIconColor(iconColor);
            
            int iconWidth = userIcon.getIconWidth();
            int iconHeight = userIcon.getIconHeight();

            int iconX = (getWidth() - iconWidth) / 2;
            int iconY = (getHeight() - iconHeight) / 2;

            userIcon.paintIcon(this, g2d, iconX, iconY);
        }

        g2d.dispose();
    }
    
    /**
     * Thay đổi màu nền
     * @param color màu mới
     */
    public void setCircleColor(Color color) {
        this.circleColor = color;
        repaint();
    }
    
    /**
     * Thay đổi màu biểu tượng
     * @param color màu mới
     */
    public void setIconColor(Color color) {
        this.iconColor = color;
        repaint();
    }
    
    /**
     * Lấy màu nền
     * @return màu nền
     */
    public Color getCircleColor() {
        return circleColor;
    }

    
    /**
     * Lấy màu biểu tượng
     * @return màu biểu tượng
     */
    public Color getIconColor() {
        return iconColor;
    }
}
