package dungcony.ds.ui.pages;

import dungcony.ds.App;
import dungcony.ds.ui.components.chatPage.ChatList;
import dungcony.ds.ui.components.chatPage.ChatScreen;


import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/** 
 * Page for Chat section
 * @author Shoyeb Ansari
 */
public class ChatPage extends JPanel {
    private static final int MEDIUM_WIDTH = 768;
    /** Component to hold all the chat list */
    private ChatList chatList;
    /** Component to hold the current chatscreen */
    private ChatScreen chatScreen;
    /** Split pane for effectively showing both chat screen and chat list */
    private JSplitPane splitPane;
    /** Card Layout for dynamic rendering between the chat screen and chat list in mobile mode */
    private CardLayout cardLayout;
    /** Panel cho chế độ mobile */
    private JPanel mobileView;
    /** Main container to switch between desktop and mobile views */
    private JPanel mainContainer;
    /** Layout on the page will be seen */  
    private CardLayout mainLayout;
    /** Cờ cho biết có đang ở chế độ mobile không */
    private boolean isMobileMode = false;
    
    public ChatPage() {
        try {
            setLayout(new BorderLayout());
            // Initialize components first
            initializeComponents();
            
            // Thiết lập bố cục
            setupLayouts();
            
            // Đặt trạng thái ban đầu
            setInitialState();
            
            // Lắng nghe sự kiện thay đổi kích thước
            addResizeListener();
        } catch (Exception e) {
            System.out.println("[ERROR] Không thể khởi tạo trang chat\nChi tiết lỗi: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Khởi tạo các thành phần */    
    private void initializeComponents() {
        try {
            chatList = new ChatList();
            chatScreen = new ChatScreen();
            
            // Đặt tham chiếu cha
            chatList.setParentChatPage(this);
            chatScreen.setParentChatPage(this);
            
            // Ensure components are visible and have preferred sizes
            chatList.setPreferredSize(new Dimension(300, 400));
            chatScreen.setPreferredSize(new Dimension(500, 400));
        } catch (Exception e) {
            System.out.println("[ERROR] Không thể khởi tạo component\nChi tiết lỗi: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Thiết lập bố cục */
    private void setupLayouts() {
        try {
            // Main container with CardLayout to switch between desktop and mobile
            mainLayout = new CardLayout();
            mainContainer = new JPanel(mainLayout);
            
            // Thiết lập chế độ desktop
            setupDesktopView();
            
            // Thiết lập chế độ mobile  
            setupMobileView();
            
            // Thêm cả hai chế độ vào container
            mainContainer.add(splitPane, "DESKTOP");
            mainContainer.add(mobileView, "MOBILE");
            
            // Thêm container vào trang
            add(mainContainer, BorderLayout.CENTER);
        } catch (Exception e) {
            System.out.println("[ERROR] Không thể thiết lập bố cục\nChi tiết lỗi: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Initial state of the page, i.e., desktop mode */
    private void setInitialState() {
        try {
            // Show desktop view initially
            mainLayout.show(mainContainer, "DESKTOP");
            chatScreen.setMobileMode(false);
            
            // Force initial layout
            SwingUtilities.invokeLater(() -> {
                revalidate();
                repaint();
            });
        } catch (Exception e) {
            System.out.println("[ERROR] Không thể set initial state\nChi tiết lỗi: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Thiết lập bố cục khi thay đổi kích thước */
    private void addResizeListener() {
        try {
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    try {
                        // Use SwingUtilities.invokeLater to ensure proper event handling
                        SwingUtilities.invokeLater(() -> checkAndUpdateLayout());
                    } catch (Exception ex) {
                        System.out.println("[ERROR] Không thể xử lý sự kiện đổi kích thước\nChi tiết lỗi: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            System.out.println("[ERROR] Không thể thêm listener đổi kích thước\nChi tiết lỗi: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Thiết lập chế độ desktop
     */
    private void setupDesktopView() {
        try {
            splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            splitPane.setLeftComponent(chatList);
            splitPane.setRightComponent(chatScreen);
            splitPane.setResizeWeight(0.4); // 40% for chat list
            splitPane.setDividerSize(2);
            splitPane.setContinuousLayout(true);
            splitPane.setOneTouchExpandable(false);
            
            // Đặt kích thước tối thiểu và ưu tiên
            chatList.setMinimumSize(new Dimension(250, 0));
            chatScreen.setMinimumSize(new Dimension(300, 0));
        } catch (Exception e) {
            System.out.println("[ERROR] Không thể thiết lập giao diện desktop\nChi tiết lỗi: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Thiết lập chế độ mobile
     */
    private void setupMobileView() {
        try {
            cardLayout = new CardLayout();
            mobileView = new JPanel(cardLayout);
            
            // Tạo panel bao để tránh chia sẻ component
            JPanel chatListWrapper = new JPanel(new BorderLayout());
            JPanel chatScreenWrapper = new JPanel(new BorderLayout());
            
            mobileView.add(chatListWrapper, "CHAT_LIST");
            mobileView.add(chatScreenWrapper, "CHAT_SCREEN");
        } catch (Exception e) {
            System.out.println("[ERROR] Không thể thiết lập giao diện mobile\nChi tiết lỗi: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Phương thức điều chỉnh bố cục theo kích thước cửa sổ
     */
    private void checkAndUpdateLayout() {
        try {
            if (!isDisplayable()) {
                return; // Don't process if component isn't ready
            }
            
            int currentWidth = getWidth();
            boolean shouldBeMobile = currentWidth < (MEDIUM_WIDTH + 120);
            
            if (shouldBeMobile && !isMobileMode) {
                switchToMobileMode();
            } else if (!shouldBeMobile && isMobileMode) {
                switchToDesktopMode();
            }
        } catch (Exception e) {
            System.out.println("[ERROR] Không thể kiểm tra và cập nhật bố cục\nChi tiết lỗi: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Chuyển sang chế độ mobile */
    private void switchToMobileMode() {
        try {
            isMobileMode = true;
            
            // Xóa component khỏi split pane
            splitPane.remove(chatList);
            splitPane.remove(chatScreen);
            
            // Lấy panel bao và thêm component
            JPanel chatListWrapper = (JPanel) ((JPanel) mobileView.getComponent(0));
            JPanel chatScreenWrapper = (JPanel) ((JPanel) mobileView.getComponent(1));
            
            chatListWrapper.removeAll();
            chatScreenWrapper.removeAll();
            
            chatListWrapper.add(chatList, BorderLayout.CENTER);
            chatScreenWrapper.add(chatScreen, BorderLayout.CENTER);
            
            // Switch to mobile view
            mainLayout.show(mainContainer, "MOBILE");
            
            // Show chat list by default in mobile mode
            showChatList();
            
            // Update components for mobile mode
            chatScreen.setMobileMode(true);
            
            // Force layout update
            SwingUtilities.invokeLater(() -> {
                mobileView.revalidate();
                mobileView.repaint();
                mainContainer.revalidate();
                mainContainer.repaint();
            });
        } catch (Exception e) {
            System.out.println("[ERROR] Không thể chuyển sang chế độ mobile\nChi tiết lỗi: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Chuyển sang chế độ desktop */
    private void switchToDesktopMode() {
        try {
            isMobileMode = false;
            
            // Xóa component khỏi panel mobile
            JPanel chatListWrapper = (JPanel) ((JPanel) mobileView.getComponent(0));
            JPanel chatScreenWrapper = (JPanel) ((JPanel) mobileView.getComponent(1));
            
            chatListWrapper.remove(chatList);
            chatScreenWrapper.remove(chatScreen);
            
            // Thêm lại component vào split pane
            splitPane.setLeftComponent(chatList);
            splitPane.setRightComponent(chatScreen);
            
            // Switch to desktop view
            mainLayout.show(mainContainer, "DESKTOP");
            
            // Update components for desktop mode
            chatScreen.setMobileMode(false);
            
            // Force layout update
            SwingUtilities.invokeLater(() -> {
                splitPane.revalidate();
                splitPane.repaint();
                mainContainer.revalidate();
                mainContainer.repaint();
            });
        } catch (Exception e) {
            System.out.println("[ERROR] Không thể chuyển sang chế độ desktop\nChi tiết lỗi: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Hiển thị danh sách chat
     */
    public void showChatList() {
        try {
            if (isMobileMode) {
                cardLayout.show(mobileView, "CHAT_LIST");
            }
        } catch (Exception e) {
            System.out.println("[ERROR] Không thể hiển thị danh sách chat\nChi tiết lỗi: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Hiển thị màn hình chat
     */
    public void showChatScreen() {
        try {
            if (isMobileMode) {
                cardLayout.show(mobileView, "CHAT_SCREEN");
            }
        } catch (Exception e) {
            System.out.println("[ERROR] Không thể hiển thị màn hình chat\nChi tiết lỗi: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * return if the current window is mobile mode or not
     * @return boolean indicating mobile mode status
     */
    public boolean isMobileMode() {
        try {
            return isMobileMode;
        } catch (Exception e) {
            System.out.println("[ERROR] Không thể kiểm tra trạng thái chế độ mobile\nChi tiết lỗi: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /** Xử lý khi chọn một cuộc chat từ ChatList */
    public void onChatSelected(String username, String ipAddress) {
        try {
            System.out.println("[INFO] UI đã chọn chat. user=" + username + ", peer=" + ipAddress);
            chatScreen.setSelectedUser(username);
            chatScreen.setIpAddress(ipAddress);
            chatScreen.setMessages(App.peerNode == null ? java.util.Collections.emptyList() : App.peerNode.getMessagesWithPeer(ipAddress));
            chatScreen.setUserStatus(false);
            if (App.peerNode != null) {
                new SwingWorker<Boolean, Void>() {
                    @Override
                    protected Boolean doInBackground() {
                        System.out.println("[DEBUG] Đang kiểm tra trạng thái peer đã chọn: " + ipAddress);
                        return App.peerNode.checkUserIsOnline(ipAddress);
                    }

                    @Override
                    protected void done() {
                        try {
                            chatScreen.setUserStatus(get());
                        } catch (Exception e) {
                            chatScreen.setUserStatus(false);
                        }
                    }
                }.execute();
            }
            if (isMobileMode) {
                showChatScreen();
            }
            chatScreen.revalidate();
            chatScreen.repaint();
        } catch (Exception e) {
            System.out.println("[ERROR] Không thể xử lý chọn chat\nChi tiết lỗi: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Xu ly khi chon group chat tu ChatList.
     */
    public void onGroupSelected(String groupName, String groupId) {
        try {
            System.out.println("[INFO] UI đã chọn nhóm. nhóm=" + groupName + ", groupId=" + groupId);
            chatScreen.setSelectedGroup(groupName, groupId);
            chatScreen.setMessages(App.peerNode == null
                    ? java.util.Collections.emptyList()
                    : App.peerNode.getMessagesWithGroup(groupId));
            if (isMobileMode) {
                showChatScreen();
            }
            chatScreen.revalidate();
            chatScreen.repaint();
        } catch (Exception e) {
            System.out.println("[ERROR] Không thể xử lý chọn nhóm\nChi tiết lỗi: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
