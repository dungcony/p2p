package dungcony.ds.ui.components;

import dungcony.ds.ui.utils.ColorPalette;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ModernButton extends JButton {
    /** Cờ hover */
    private boolean isHover = false;
    /** Màu mặc định */
    private Color normalColor = ColorPalette.PRIMARY;
    private Color hoverColor = ColorPalette.SECONDARY;
    
    public ModernButton(String text, Color normalColor, Color hoverColor) {
        super(text);
        this.normalColor = normalColor;
        this.hoverColor = hoverColor;
        
        setForeground(Color.WHITE);
        setFont(new Font("Segoe UI", Font.BOLD, 14));
        setBorderPainted(false);
        setFocusPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setBorder(new EmptyBorder(10, 20, 10, 20));
        
        // Lắng nghe sự kiện chuột
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHover = true;
                repaint();
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                isHover = false;
                repaint();
            }
        });
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Vẽ nền nút
        if (isHover) {
            g2.setColor(hoverColor);
        } else {
            if (isEnabled()) {
                g2.setColor(normalColor);
            } else {
                g2.setColor(hoverColor);
            }
        }
        
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
        
        // Lấy icon và text
        Icon icon = getIcon();
        String text = getText();
        FontMetrics fm = g2.getFontMetrics();
        
        // Tính vị trí icon và text
        int iconWidth = (icon != null) ? icon.getIconWidth() : 0;
        int iconHeight = (icon != null) ? icon.getIconHeight() : 0;
        int textWidth = (text != null) ? fm.stringWidth(text) : 0;
        int gap = (icon != null && text != null && !text.isEmpty()) ? getIconTextGap() : 0;
        
        int totalWidth = iconWidth + gap + textWidth;
        int startX = (getWidth() - totalWidth) / 2;
        
        // Vẽ icon
        if (icon != null) {
            int iconX = startX;
            int iconY = (getHeight() - iconHeight) / 2;
            icon.paintIcon(this, g2, iconX, iconY);
        }
        
        // Vẽ text
        if (text != null && !text.isEmpty()) {
            g2.setColor(Color.WHITE);
            int textX = startX + iconWidth + gap;
            int textY = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(text, textX, textY);
        }
        
        g2.dispose();
    }
}
