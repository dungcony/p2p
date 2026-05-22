package dungcony.ds.services;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dungcony.ds.interfaces.NetworkAddressService;
import dungcony.ds.model.PeerInfo;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;

public class NetworkAddressImpl implements NetworkAddressService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkAddressImpl.class);
// Tìm địa chỉ IPv4 LAN phù hợp nhất của máy hiện tại để peer khác có thể kết nối.
    @Override
    public String resolveLocalHost() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isUp() || networkInterface.isLoopback() || networkInterface.isVirtual()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address instanceof Inet4Address && !address.isLoopbackAddress() && !address.isLinkLocalAddress()) {
                        LOGGER.info("Đã tìm thấy địa chỉ IPv4 local: " + address.getHostAddress());
                        return address.getHostAddress();
                    }
                }
            }
            String fallback = InetAddress.getLocalHost().getHostAddress();
            LOGGER.warn("Chuyển sang dùng InetAddress.getLocalHost(): " + fallback);
            return fallback;
        } catch (IOException e) {
            LOGGER.warn("Không thể xác định host local. Chuyển sang 127.0.0.1. Lỗi=" + e.getMessage());
            return "127.0.0.1";
        }
    }

    // So sánh PeerInfo với localPeer để chặn self-chat trong mọi luồng logic.
    @Override
    public boolean isSelfPeer(PeerInfo localPeer, PeerInfo peerInfo) {
        if (peerInfo == null) {
            return false;
        }
        if (localPeer.getId().equals(peerInfo.getId())) {
            return true;
        }
        if (peerInfo.getPort() != localPeer.getPort()) {
            return false;
        }
        return isSameHost(peerInfo.getHost(), localPeer.getHost());
    }

    // So sánh host theo literal, localhost/loopback và địa chỉ IP resolve được.
    @Override
    public boolean isSameHost(String candidateHost, String localHost) {
        if (candidateHost == null || candidateHost.isBlank()) {
            return false;
        }
        if (candidateHost.equalsIgnoreCase(localHost)
                || "localhost".equalsIgnoreCase(candidateHost)
                || "127.0.0.1".equals(candidateHost)) {
            return true;
        }
        try {
            InetAddress candidateAddress = InetAddress.getByName(candidateHost);
            InetAddress localAddress = InetAddress.getByName(localHost);
            return candidateAddress.isLoopbackAddress() || candidateAddress.equals(localAddress);
        } catch (UnknownHostException e) {
            LOGGER.warn("Không thể so sánh host với peer local. host="
                    + candidateHost + ", lỗi=" + e.getMessage());
            return false;
        }
    }
}
