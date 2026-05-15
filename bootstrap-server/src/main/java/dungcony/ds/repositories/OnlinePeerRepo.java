package dungcony.ds.repositories;

import dungcony.ds.entities.OnlinePeerEntity;
import dungcony.ds.models.PeerInfo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public record OnlinePeerRepo(Conn conn)  {
    /**
     * Them moi hoac cap nhat peer online trong bang peers_online.
     */
    public void upsert(OnlinePeerEntity onlinePeerEntity, Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO peers_online(peer_key, user_id, host, port, online, last_seen)
                VALUES(?, ?, ?, ?, 1, ?)
                ON CONFLICT(peer_key) DO UPDATE SET
                    user_id = excluded.user_id,
                    host = excluded.host,
                    port = excluded.port,
                    online = 1,
                    last_seen = excluded.last_seen
                """)) {
            statement.setString(1, onlinePeerEntity.getPeerKey());
            statement.setString(2, onlinePeerEntity.getUserId());
            statement.setString(3, onlinePeerEntity.getHost());
            statement.setInt(4, onlinePeerEntity.getPort());
            statement.setLong(5, onlinePeerEntity.getLastSeen());
            statement.executeUpdate();
        }
    }

    /**
     * Xoa peer online khoi bang peers_online khi peer LEAVE.
     */
    public void remove(String peerKey) {
        try (Connection connection = conn.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM peers_online WHERE peer_key = ?")) {
            statement.setString(1, peerKey);
            int deleted = statement.executeUpdate();
            System.out.println("[INFO] SQLite removed online peer=" + peerKey + ", deleted=" + deleted);
        } catch (SQLException e) {
            System.out.println("[ERROR] Failed to remove online peer: " + e.getMessage());
        }
    }

    /**
     * Doc danh sach peer dang online tu SQLite.
     */
    public Collection<PeerInfo> listOnline() {
        List<PeerInfo> peers = new ArrayList<>();
        try (Connection connection = conn.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT u.display_name, p.host, p.port, p.online
                     FROM peers_online p
                     JOIN users u ON u.user_id = p.user_id
                     WHERE p.online = 1
                     ORDER BY p.last_seen DESC
                     """);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                OnlinePeerEntity onlinePeerEntity = new OnlinePeerEntity(
                        resultSet.getString("host") + ":" + resultSet.getInt("port"),
                        resultSet.getString("display_name"),
                        resultSet.getString("host"),
                        resultSet.getInt("port"),
                        resultSet.getInt("online") == 1,
                        0L
                );
                peers.add(onlinePeerEntity.toPeerInfo());
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] Failed to list online peers: " + e.getMessage());
        }
        return peers;
    }
}
