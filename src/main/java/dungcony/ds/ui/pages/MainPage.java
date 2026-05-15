package dungcony.ds.ui.pages;


import dungcony.ds.ui.router.RouterManager;

import javax.swing.*;
import java.awt.*;

/**
 * Trang chính chứa thanh điều hướng bên trái và nội dung động bên phải
 */
public class MainPage extends JPanel {

    /** Trang điều hướng của ứng dụng */
    private NavigationPage navigationPage;
    /** Quản lý route, khởi tạo và chuyển trang */
    private RouterManager routerManager;
    /** Panel chứa nội dung chính */
    private JPanel contentPanel;
    
    public MainPage() throws InterruptedException {
        setLayout(new BorderLayout());

        // Tạo trang điều hướng
        navigationPage = new NavigationPage();

        // Tạo panel nội dung với CardLayout
        contentPanel = new JPanel(new CardLayout());

        // Thiết lập router manager
        routerManager = RouterManager.getInstance();
        routerManager.setContentPanel(contentPanel);

        addRoutes();

        // Đặt route mặc định
        routerManager.navigateTo("chats");

        // Thêm các thành phần vào trang chính
        add(navigationPage, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);
    }
   
    /** 
     * Thêm các route
     * @see RouterManager
     */
    private void addRoutes() throws InterruptedException {
        // Thêm tất cả các route
        try {
            routerManager.addRoute("start", new StartPage());
            routerManager.addRoute("chats", new ChatPage());
            routerManager.addRoute("addFriend", new AddFriendPage());
            routerManager.addRoute("scanner", new ScannerPage());
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to add the route in main page\nError Message: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

