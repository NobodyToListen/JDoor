package com.jdoor.session;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;

public final class NetworkAddresses {
    private NetworkAddresses() {}

    public static String preferredLanAddress() {
        try {
            List<InetAddress> candidates = new ArrayList<>();
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isUp() || networkInterface.isLoopback() || networkInterface.isVirtual()) {
                    continue;
                }
                networkInterface.getInetAddresses().asIterator().forEachRemaining(candidates::add);
            }
            return candidates.stream()
                    .filter(address -> !address.isLoopbackAddress() && !address.isLinkLocalAddress())
                    .sorted(Comparator.comparingInt(NetworkAddresses::score))
                    .map(InetAddress::getHostAddress)
                    .findFirst()
                    .orElse("127.0.0.1");
        } catch (SocketException unavailable) {
            return "127.0.0.1";
        }
    }

    private static int score(InetAddress address) {
        if (address instanceof Inet4Address && address.isSiteLocalAddress()) {
            return 0;
        }
        if (address instanceof Inet4Address) {
            return 1;
        }
        if (address.isSiteLocalAddress()) {
            return 2;
        }
        return 3;
    }
}
