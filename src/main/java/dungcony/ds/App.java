package dungcony.ds;

import dungcony.ds.peer.PeerNode;
import dungcony.ds.ui.LoginDialog;
import dungcony.ds.ui.Main;

import javax.swing.SwingUtilities;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class App {
    public static PeerNode peerNode;

    /**
     * Điểm vào của ứng dụng: lấy thông tin peer từ LoginDialog, khởi động PeerNode,
     * sau đó mở cửa sổ chat chính.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LoginDialog loginDialog = new LoginDialog();
            loginDialog.setVisible(true);
            if (!loginDialog.isConfirmed()) {
                System.out.println("[INFO] Login dialog cancelled. Application will not start PeerNode.");
                return;
            }

            System.out.println("[INFO] Starting PeerNode with name=" + loginDialog.getPeerName()
                    + ", port=" + loginDialog.getPeerPort());
            peerNode = new PeerNode(loginDialog.getPeerName(), loginDialog.getPeerPort());
            peerNode.start();

            System.out.println("[INFO] Opening main chat window.");
            Main mainWindow = new Main();
            mainWindow.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    System.out.println("[INFO] Main window closing. Stopping PeerNode.");
                    peerNode.stop();
                }
            });
            mainWindow.setVisible(true);
        });
    }
}
