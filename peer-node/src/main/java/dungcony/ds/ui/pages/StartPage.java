package dungcony.ds.ui.pages;

import javax.swing.*;

/**
 * Trang bắt đầu khi người dùng mở ứng dụng lần đầu.
 * Chào mừng và giới thiệu ứng dụng
 */
public class StartPage extends JPanel {

    public StartPage() {
        JLabel label = new JLabel("Đây là trang bắt đầu");
        label.setBounds(0, 0, 303, 303);
        this.add(label);
    }

}

