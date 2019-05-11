package com.Cardinal.NetworkAdmin.Client;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class NetPoint {

	private InetAddress address;
	private List<NetPoint> connections = new ArrayList<NetPoint>();

	public void addEntry(NetPoint point) {
		connections.add(point);
	}

	public NetPoint(InetAddress address) {
		this.address = address;
	}

	public InetAddress getAddress() {
		return address;
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append(address.getHostAddress());

		if (!connections.isEmpty()) {
			for (NetPoint p : connections) {
				b.append("\n\t|-" + p.toString());
			}
		}
		return b.toString();
	}
}