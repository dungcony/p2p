package dungcony.ds.ui.router;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Lớp singleton quản lý điều hướng trang, giúp tạo trải nghiệm Single Page Application.
 * @author Shoyeb Ansari
 */
public class RouterManager {
    /** Đối tượng duy nhất trong toàn bộ ứng dụng */
    private static RouterManager instance;
    /** Panel nội dung của ứng dụng */
    private Container contentPanel;
    /** CardLayout quản lý việc chuyển đổi trang động */
    private CardLayout cardLayout;
    /** Map lưu các component theo tên route */
    private Map<String, Component> routes = new HashMap<>();
    /** Route hiện tại của ứng dụng */
    private String currentRoute;

    private RouterManager() {
        // Constructor riêng cho singleton
    }

    /**
     * Lấy đối tượng {@code RouterManager}
     * @return Đối tượng RouterManager duy nhất
     */
    public static RouterManager getInstance() {
        if (instance == null) {
            instance = new RouterManager();
        }
        return instance;
    }

    /**
     * Gán panel vào {@code contentPanel}
     * @param panel Panel cần gán
     */
    public void setContentPanel(Container panel) {
        this.contentPanel = panel;
        this.cardLayout = (CardLayout) panel.getLayout();
    }

    /**
     * Đăng ký route mới
     * @param routeName Tên route
     * @param component Component cần gán cho route
     * 
     * <p>Ví dụ:</p> 
     * <pre>
     *      RouterManager.getInstance().addRoute("name", component);
     * </pre>
     */
    public void addRoute(String routeName, Component component) {
        routes.put(routeName, component);
        contentPanel.add(component, routeName);
    }

    /**
     * Điều hướng đến route chỉ định
     * @param routeName Tên route đích
     */
    public void navigateTo(String routeName) {
        if (routes.containsKey(routeName)) {
            cardLayout.show(contentPanel, routeName);
            currentRoute = routeName;
        } else {
            System.err.println("[Error]Route not found: " + routeName);
        }
    }

    /**
     * Lấy route hiện tại
     * @return Route hiện tại
     */
    public String getCurrentRoute() {
        return currentRoute;
    }
}
