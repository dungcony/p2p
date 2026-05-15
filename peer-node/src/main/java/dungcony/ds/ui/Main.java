package dungcony.ds.ui;

import dungcony.ds.ui.pages.MainPage;

import javax.swing.*;

public class Main extends JFrame {
    /**
     * Khởi tạo cửa sổ chính và gắn MainPage vào frame.
     */
    public Main() {
        setTitle("P2P Chat");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        try {
            setContentPane(new MainPage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            setContentPane(new JLabel("Failed to start UI"));
        }
    }
}
