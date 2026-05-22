package dungcony.ds.ui;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.swing.*;
import java.awt.*;

public class LoginDialog extends JDialog {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(LoginDialog.class);
private final JTextField idField;
    private final JTextField nameField;
    private boolean confirmed;

    // Tạo dialog nhập tên peer truoc khi khởi động PeerNode.
    public LoginDialog() {
        this("peer-local", System.getProperty("user.name", "peer"));
    }

    // Tạo dialog nhập peer với gia tri mặc định lay từ cấu hình đã lưu.
    public LoginDialog(String defaultPeerId, String defaultPeerName) {
        this.idField = new JTextField(defaultPeerId == null || defaultPeerId.isBlank() ? "peer-local" : defaultPeerId, 20);
        this.nameField = new JTextField(defaultPeerName == null || defaultPeerName.isBlank() ? "peer" : defaultPeerName, 20);
        setTitle("Khởi động P2P Chat");
        setModal(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(12, 12));

        JPanel form = new JPanel(new GridLayout(1, 2, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(16, 16, 0, 16));
//        form.add(new JLabel("Peer ID"));
//        form.add(idField);
        form.add(new JLabel("Tên peer"));
        form.add(nameField);

        JButton startButton = new JButton("Bắt đầu");
        startButton.addActionListener(e -> confirm());
        JButton cancelButton = new JButton("Hủy");
        cancelButton.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancelButton);
        buttons.add(startButton);
        buttons.setBorder(BorderFactory.createEmptyBorder(0, 16, 16, 16));

        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(null);
    }

    // Đóng dialog khi người dùng xác nhận thông tin profile.
    private void confirm() {
        confirmed = true;
        LOGGER.info("Đã xác nhận đăng nhập. peerId=" + getPeerId()
                + ", tênPeer=" + getPeerName());
        dispose();
    }

    // Cho biết người dùng đã bấm Start hay đã hủy dialog.
    public boolean isConfirmed() {
        return confirmed;
    }

    // Lấy id đăng ký ổn định của peer, dùng làm user_id trên bootstrap-server.
    public String getPeerId() {
        String peerId = idField.getText();
        return peerId == null || peerId.isBlank() ? "peer-local" : peerId.trim();
    }

    // Lấy tên peer từ input, dùng giá trị mặc định nếu người dùng để trống.
    public String getPeerName() {
        String name = nameField.getText();
        return name == null || name.isBlank() ? "peer" : name.trim();
    }

}
