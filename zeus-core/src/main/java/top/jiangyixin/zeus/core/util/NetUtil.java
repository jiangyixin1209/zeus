package top.jiangyixin.zeus.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;


/**
 * 网络工具类
 * @author jiangyixin
 */
public class NetUtil {
    private static final Logger logger = LoggerFactory.getLogger(NetUtil.class);

    /**
     * 获取已激活网卡的列表IP地址
     * @param interfaceName     可指定网卡名称,null则获取全部
     * @return List<String>     IP 地址
     * @throws SocketException  SocketException
     */
    private static List<String> getHostAddress(String interfaceName) throws SocketException {
        List<String> ipList = new ArrayList<String>(5);
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface ni = interfaces.nextElement();
            Enumeration<InetAddress> allAddress = ni.getInetAddresses();
            while (allAddress.hasMoreElements()) {
                InetAddress address = allAddress.nextElement();
                if (address.isLoopbackAddress()) {
                    // skip the loopback addr
                    continue;
                }
                if (address instanceof Inet6Address) {
                    // skip the IPv6 addr
                    continue;
                }
                String hostAddress = address.getHostAddress();
                if (null == interfaceName) {
                    ipList.add(hostAddress);
                } else if (interfaceName.equals(ni.getDisplayName())) {
                    ipList.add(hostAddress);
                }
            }
        }
        return ipList;
    }

    /**
     * 获取第一个激活网卡的IP的地址
     * @return        IP地址
     */
    public static String getIp() {
        String ip;
        try {
            List<String> ipList = getHostAddress(null);
            // 默认获取第一个激活的网卡IP
            ip = !ipList.isEmpty() ? ipList.get(0) : "";
        } catch (Exception e) {
            ip = "";
            logger.error("NetUtil get IP Error");
        }
        return ip;
    }

    /**
     * 获取指定网卡的IP的地址
     * @return        IP地址
     */
    public static String getIp(String interfaceName) {
        String ip;
        interfaceName = interfaceName.trim();
        try {
            List<String> ipList = getHostAddress(interfaceName);
            // 默认获取第一个激活的网卡IP
            ip = !ipList.isEmpty() ? ipList.get(0) : "";
        } catch (SocketException e) {
            ip = "";
            logger.error("NetUtil get interfaceName <{}> IP Error", interfaceName);
        }
        return ip;
    }
}
