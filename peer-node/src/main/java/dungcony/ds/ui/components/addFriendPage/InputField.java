package dungcony.ds.ui.components.addFriendPage;

import dungcony.ds.ui.utils.ColorPalette;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

// Ô nhập liệu với nhãn và góc bo tròn
public class InputField extends JPanel {
    private JTextField textField;
    private JLabel label;

    public InputField(String placeholder, int height) {
        setLayout(new BorderLayout(5, 5));
        setOpaque(false);

        // Nhãn
        label = new JLabel(placeholder);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(ColorPalette.SECONDARY_TEXT);

        // Ô nhập
        textField = new JTextField();
        textField.setPreferredSize(new Dimension(300, height)); // Fixed width
        textField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        textField.setForeground(ColorPalette.TEXT);
        textField.setBackground(ColorPalette.PANEL_BACKGROUND);
        textField.setBorder(new EmptyBorder(5, 10, 5, 10));
        
        // Thêm các component
        add(label, BorderLayout.NORTH);
        add(textField, BorderLayout.CENTER);
        
        // Cố định kích thước
        setPreferredSize(new Dimension(300, height));
        setMinimumSize(new Dimension(250, height));
    }

    public JTextField getTextField() {
        return textField;
    }
}
