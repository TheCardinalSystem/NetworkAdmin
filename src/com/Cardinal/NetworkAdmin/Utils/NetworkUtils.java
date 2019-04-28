package com.Cardinal.NetworkAdmin.Utils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;

public class NetworkUtils {
	private static final HashMap<Integer, String> NETMASK_ADDRESSES = new HashMap<Integer, String>();

	static {
		NETMASK_ADDRESSES.put(4, "240.0.0.0");
		NETMASK_ADDRESSES.put(5, "248.0.0.0");
		NETMASK_ADDRESSES.put(6, "252.0.0.0");
		NETMASK_ADDRESSES.put(7, "254.0.0.0");
		NETMASK_ADDRESSES.put(8, "255.0.0.0");
		NETMASK_ADDRESSES.put(9, "255.128.0.0");
		NETMASK_ADDRESSES.put(10, "255.192.0.0");
		NETMASK_ADDRESSES.put(11, "255.224.0.0");
		NETMASK_ADDRESSES.put(12, "255.240.0.0");
		NETMASK_ADDRESSES.put(13, "255.248.0.0");
		NETMASK_ADDRESSES.put(14, "255.252.0.0");
		NETMASK_ADDRESSES.put(15, "255.254.0.0");
		NETMASK_ADDRESSES.put(16, "255.255.0.0");
		NETMASK_ADDRESSES.put(17, "255.255.128.0");
		NETMASK_ADDRESSES.put(18, "255.255.192.0");
		NETMASK_ADDRESSES.put(19, "255.255.224.0");
		NETMASK_ADDRESSES.put(20, "255.255.240.0");
		NETMASK_ADDRESSES.put(21, "255.255.248.0");
		NETMASK_ADDRESSES.put(22, "255.255.252.0");
		NETMASK_ADDRESSES.put(23, "255.255.254.0");
		NETMASK_ADDRESSES.put(24, "255.255.255.0");
		NETMASK_ADDRESSES.put(25, "255.255.255.128");
		NETMASK_ADDRESSES.put(26, "255.255.255.192");
		NETMASK_ADDRESSES.put(27, "255.255.255.224");
		NETMASK_ADDRESSES.put(28, "255.255.255.240");
		NETMASK_ADDRESSES.put(29, "255.255.255.248");
		NETMASK_ADDRESSES.put(30, "255.255.255.252");
		NETMASK_ADDRESSES.put(32, "255.255.255.255");
	}

	private static String subnetmask;

	public static Collection<String> getAllNetmasks() {
		return NETMASK_ADDRESSES.values();
	}

	public static String getSubnetMask() throws SocketException, UnknownHostException {
		if (subnetmask == null) {
			InetAddress localHost = Inet4Address.getLocalHost();
			NetworkInterface networkInterface = NetworkInterface.getByInetAddress(localHost);
			int i = networkInterface.getInterfaceAddresses().get(0).getNetworkPrefixLength();
			subnetmask = NETMASK_ADDRESSES.get(i);
		}
		return subnetmask;
	}

}
