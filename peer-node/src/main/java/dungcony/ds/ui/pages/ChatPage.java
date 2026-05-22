package dungcony.ds.ui.pages;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dungcony.ds.App;
import dungcony.ds.ui.components.chatPage.ChatList;
import dungcony.ds.ui.components.chatPage.ChatScreen;


import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

// Trang khu vực chat
// @author Shoyeb Ansari
public class ChatPage extends JPanel {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatPage.class);
private static final int MEDIUM_WIDTH = 768;
    // Component chứa toàn bộ danh sách chat
    private ChatList chatList;
    // Component chứa màn hình chat hiện tại
    private ChatScreen chatScreen;
    // SplitPane hiển thị đồng thời màn hình chat và danh sách chat
    private JSplitPane splitPane;
    // CardLayout chuyển đổi động giữa màn hình chat và danh sách chat ở chế độ mobile
    private CardLayout cardLayout;
    // Panel cho chế độ mobile
    private JPanel mobileView;
    // Container chính để chuyển giữa giao diện desktop và mobile
    private JPanel mainContainer;
    // Layout hiển thị trên trang
    private CardLayout mainLayout;
    // Cờ cho biết có đang ở chế độ mobile không
    private boolean isMobileMode = false;
    
    public ChatPage() {
        try {
            setLayout(new BorderLayout());
            // Khởi tạo component trước
            initializeComponents();
            
            // Thiết lập bố cục
            setupLayouts();
            
            // Đặt trạng thái ban đầu
            setInitialState();
            
            // Lắng nghe sự kiện thay đổi kích thước
            addResizeListener();
        } catch (Exception e) {
            LOGGER.error("Không thể khởi tạo trang chat\nChi tiết lỗi: " + e.getMessage());
            LOGGER.error("Chi tiết lỗi", e);
        }
    }

    // Khởi tạo các thành phần
    private void initializeComponents() {
        try {
            chatList = new ChatList();
            chatScreen = new ChatScreen();
            
            // Đặt tham chiếu cha
            chatList.setParentChatPage(this);
            chatScreen.setParentChatPage(this);
            
            // Đảm bảo component hiển thị và có kích thước ưu tiên
            chatList.setPreferredSize(new Dimension(300, 400));
            chatScreen.setPreferredSize(new Dimension(500, 400));
        } catch (Exception e) {
            LOGGER.error("Không thể khởi tạo component\nChi tiết lỗi: " + e.getMessage());
            LOGGER.error("Chi tiết lỗi", e);
        }
    }

    // Thiết lập bố cục
    private void setupLayouts() {
        try {
            // Container chính dùng CardLayout để chuyển giữa desktop và mobile
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
            LOGGER.error("Không thể thiết lập bố cục\nChi tiết lỗi: " + e.getMessage());
            LOGGER.error("Chi tiết lỗi", e);
        }
    }

    // Trạng thái ban đầu của trang là chế độ desktop
    private void setInitialState() {
        try {
            // Hiển thị desktop lúc khởi tạo
            mainLayout.show(mainContainer, "DESKTOP");
            chatScreen.setMobileMode(false);
            
            // Ép layout ban đầu cập nhật
            SwingUtilities.invokeLater(() -> {
                revalidate();
                repaint();
            });
        } catch (Exception e) {
            LOGGER.error("Không thể set initial state\nChi tiết lỗi: " + e.getMessage());
            LOGGER.error("Chi tiết lỗi", e);
        }
    }

    // Thiết lập bố cục khi thay đổi kích thước
    private void addResizeListener() {
        try {
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    try {
                        // Use SwingUtilities.invokeLater to ensure proper event handling
                        SwingUtilities.invokeLater(() -> checkAndUpdateLayout());
                    } catch (Exception ex) {
                        LOGGER.error("Không thể xử lý sự kiện đổi kích thước\nChi tiết lỗi: " + ex.getMessage());
                        LOGGER.error("Chi tiết lỗi", ex);
                    }
                }
            });
        } catch (Exception e) {
            LOGGER.error("Không thể thêm listener đổi kích thước\nChi tiết lỗi: " + e.getMessage());
            LOGGER.error("Chi tiết lỗi", e);
        }
    }

    // Thiết lập chế độ desktop
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
            LOGGER.error("Không thể thiết lập giao diện desktop\nChi tiết lỗi: " + e.getMessage());
            LOGGER.error("Chi tiết lỗi", e);
        }
    }

    // Thiết lập chế độ mobile
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
            LOGGER.error("Không thể thiết lập giao diện mobile\nChi tiết lỗi: " + e.getMessage());
            LOGGER.error("Chi tiết lỗi", e);
        }
    }

    // Phương thức điều chỉnh bố cục theo kích thước cửa sổ
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
            LOGGER.error("Không thể kiểm tra và cập nhật bố cục\nChi tiết lỗi: " + e.getMessage());
            LOGGER.error("Chi tiết lỗi", e);
        }
    }

    // Chuyển sẵng chế độ mobile
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
            
            // Chuyển sang giao diện mobile
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
            LOGGER.error("Không thể chuyển sang chế độ mobile\nChi tiết lỗi: " + e.getMessage());
            LOGGER.error("Chi tiết lỗi", e);
        }
    }

    // Chuyển sẵng chế độ desktop
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
            
            // Chuyển sang giao diện desktop
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
            LOGGER.error("Không thể chuyển sang chế độ desktop\nChi tiết lỗi: " + e.getMessage());
            LOGGER.error("Chi tiết lỗi", e);
        }
    }
    
    // Hiển thị danh sách chat
    public void showChatList() {
        try {
            if (isMobileMode) {
                cardLayout.show(mobileView, "CHAT_LIST");
            }
        } catch (Exception e) {
            LOGGER.error("Không thể hiển thị danh sách chat\nChi tiết lỗi: " + e.getMessage());
            LOGGER.error("Chi tiết lỗi", e);
        }
    }
    
    // Hiển thị màn hình chat
    public void showChatScreen() {
        try {
            if (isMobileMode) {
                cardLayout.show(mobileView, "CHAT_SCREEN");
            }
        } catch (Exception e) {
            LOGGER.error("Không thể hiển thị màn hình chat\nChi tiết lỗi: " + e.getMessage());
            LOGGER.error("Chi tiết lỗi", e);
        }
    }
    
    // Trả về trạng thái cửa sổ hiện tại có đang ở chế độ mobile hay không
    // @return boolean indicating mobile mode status
    public boolean isMobileMode() {
        try {
            return isMobileMode;
        } catch (Exception e) {
            LOGGER.error("Không thể kiểm tra trạng thái chế độ mobile\nChi tiết lỗi: " + e.getMessage());
            LOGGER.error("Chi tiết lỗi", e);
            return false;
        }
    }
    
    // Xử lý khi chọn một cuộc chat từ ChatList
    public void onChatSelected(String username, String ipAddress) {
        try {
            LOGGER.info("UI đã chọn chat. user=" + username + ", peer=" + ipAddress);
            chatScreen.setSelectedUser(username);
            chatScreen.setIpAddress(ipAddress);
            chatScreen.setMessages(App.peerNode == null ? java.util.Collections.emptyList() : App.peerNode.getMessagesWithPeer(ipAddress));
            chatScreen.setUserStatus(false);
            if (App.peerNode != null) {
                new SwingWorker<Boolean, Void>() {
                    @Override
                    protected Boolean doInBackground() {
                        LOGGER.debug("Đang kiểm tra trạng thái peer đã chọn: " + ipAddress);
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
            LOGGER.error("Không thể xử lý chọn chat\nChi tiết lỗi: " + e.getMessage());
            LOGGER.error("Chi tiết lỗi", e);
        }
    }

    // Xử lý khi chọn group chat từ ChatList.
    public void onGroupSelected(String groupName, String groupId) {
        try {
            LOGGER.info("UI đã chọn nhóm. nhóm=" + groupName + ", groupId=" + groupId);
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
            LOGGER.error("Không thể xử lý chọn nhóm\nChi tiết lỗi: " + e.getMessage());
            LOGGER.error("Chi tiết lỗi", e);
        }
    }
}
