package dungcony.ds.ui.pages;

import lombok.extern.slf4j.Slf4j;


import dungcony.ds.App;
import dungcony.ds.ui.components.chatPage.ChatList;
import dungcony.ds.ui.components.chatPage.ChatScreen;


import javax.swing.*;
import java.awt.*;

// Trang khu vực chat
// @author Shoyeb Ansari
@Slf4j
public class ChatPage extends JPanel {
    // Component chứa toàn bộ danh sách chat
    private ChatList chatList;
    // Component chứa màn hình chat hiện tại
    private ChatScreen chatScreen;
    // SplitPane hiển thị đồng thời màn hình chat và danh sách chat
    private JSplitPane splitPane;
    
    public ChatPage() {
        try {
            setLayout(new BorderLayout());
            // Khởi tạo component trước
            initializeComponents();
            
            // Thiết lập bố cục
            setupLayouts();
        } catch (Exception e) {
            log.error("Không thể khởi tạo trang chat\nChi tiết lỗi: " + e.getMessage());
            log.error("Chi tiết lỗi", e);
        }
    }

    // Khởi tạo các thành phần
    private void initializeComponents() {
        try {
            chatList = new ChatList();
            chatScreen = new ChatScreen();
            
            // Đặt tham chiếu cha
            chatList.setParentChatPage(this);
            
            // Đảm bảo component hiển thị và có kích thước ưu tiên
            chatList.setPreferredSize(new Dimension(300, 400));
            chatScreen.setPreferredSize(new Dimension(500, 400));
        } catch (Exception e) {
            log.error("Không thể khởi tạo component\nChi tiết lỗi: " + e.getMessage());
            log.error("Chi tiết lỗi", e);
        }
    }

    // Thiết lập bố cục
    private void setupLayouts() {
        try {
            setupSplitPane();
            add(splitPane, BorderLayout.CENTER);
        } catch (Exception e) {
            log.error("Không thể thiết lập bố cục\nChi tiết lỗi: " + e.getMessage());
            log.error("Chi tiết lỗi", e);
        }
    }

    // Thiết lập bố cục chia đôi cho ứng dụng Windows
    private void setupSplitPane() {
        try {
            splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            splitPane.setLeftComponent(chatList);
            splitPane.setRightComponent(chatScreen);
            splitPane.setResizeWeight(0.4);
            splitPane.setDividerSize(2);
            splitPane.setContinuousLayout(true);
            splitPane.setOneTouchExpandable(false);
            
            // Đặt kích thước tối thiểu và ưu tiên
            chatList.setMinimumSize(new Dimension(250, 0));
            chatScreen.setMinimumSize(new Dimension(300, 0));
        } catch (Exception e) {
            log.error("Không thể thiết lập bố cục chia đôi\nChi tiết lỗi: " + e.getMessage());
            log.error("Chi tiết lỗi", e);
        }
    }
    
    // Xử lý khi chọn một cuộc chat từ ChatList
    public void onChatSelected(String username, String ipAddress) {
        try {
            log.info("UI đã chọn chat. user=" + username + ", peer=" + ipAddress);
            chatScreen.setSelectedUser(username);
            chatScreen.setIpAddress(ipAddress);
            chatScreen.setMessages(App.peerNode == null ? java.util.Collections.emptyList() : App.peerNode.getMessagesWithPeer(ipAddress));
            chatScreen.setUserStatus(false);
            if (App.peerNode != null) {
                new SwingWorker<Boolean, Void>() {
                    @Override
                    protected Boolean doInBackground() {
                        log.debug("Đang kiểm tra trạng thái peer đã chọn: " + ipAddress);
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
            chatScreen.revalidate();
            chatScreen.repaint();
        } catch (Exception e) {
            log.error("Không thể xử lý chọn chat\nChi tiết lỗi: " + e.getMessage());
            log.error("Chi tiết lỗi", e);
        }
    }

    // Xử lý khi chọn group chat từ ChatList.
    public void onGroupSelected(String groupName, String groupId) {
        try {
            log.info("UI đã chọn nhóm. nhóm=" + groupName + ", groupId=" + groupId);
            chatScreen.setSelectedGroup(groupName, groupId);
            chatScreen.setMessages(App.peerNode == null
                    ? java.util.Collections.emptyList()
                    : App.peerNode.getMessagesWithGroup(groupId));
            chatScreen.revalidate();
            chatScreen.repaint();
        } catch (Exception e) {
            log.error("Không thể xử lý chọn nhóm\nChi tiết lỗi: " + e.getMessage());
            log.error("Chi tiết lỗi", e);
        }
    }
}
