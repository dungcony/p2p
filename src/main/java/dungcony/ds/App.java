package dungcony.ds;

import dungcony.ds.peer.PeerNode;
import dungcony.ds.ui.LoginDialog;
import dungcony.ds.ui.Main;

import javax.swing.SwingUtilities;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class App {
    public static PeerNode peerNode;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LoginDialog loginDialog = new LoginDialog();
            loginDialog.setVisible(true);
            if (!loginDialog.isConfirmed()) {
                return;
            }

            peerNode = new PeerNode(loginDialog.getPeerName(), loginDialog.getPeerPort());
            peerNode.start();

            Main mainWindow = new Main();
            mainWindow.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    peerNode.stop();
                }
            });
            mainWindow.setVisible(true);
        });
    }
}
