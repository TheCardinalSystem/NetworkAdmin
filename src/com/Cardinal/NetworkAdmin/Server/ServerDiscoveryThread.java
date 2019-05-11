package com.Cardinal.NetworkAdmin.Server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.logging.Level;

import com.Cardinal.NetworkAdmin.NetworkConstants;
import com.Cardinal.NetworkAdmin.Client.SocketHandler;
import com.Cardinal.NetworkAdmin.NetworkHandler.ThreadLock;

public class ServerDiscoveryThread extends Thread {

	private DatagramSocket socket;
	private boolean run = true;
	private ThreadLock lock;

	public ServerDiscoveryThread() {
		lock = null;
	}

	public ServerDiscoveryThread(ThreadLock lock) {
		this.lock = lock;
	}

	public void kill() {
		NetworkConstants.LOGGER.log(Level.INFO, "Closing server UDP thread...");
		run = false;
		socket.close();
	}

	@Override
	public void run() {
		try {
			// Keep a socket open to listen to all the UDP traffic that is destined for this
			// port
			socket = new DatagramSocket(NetworkConstants.PORT, InetAddress.getByName("0.0.0.0"));
			socket.setBroadcast(true);

			while (run) {
				NetworkConstants.LOGGER.log(Level.INFO, "Ready for broadcast packets...");

				// Receive a packet
				byte[] recvBuf = new byte[NetworkConstants.BUFFER];
				DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
				socket.receive(packet);

				// Packet received
				InetAddress packetAddress = packet.getAddress();
				if (SocketHandler.CONNECTIONS.containsKey(packetAddress)
						|| ServerSocketHandler.allowedConnections.contains(packetAddress)) {
					NetworkConstants.LOGGER.log(Level.INFO, "Ignoring packet from " + packetAddress.getHostAddress());
					continue;
				}

				String message = new String(packet.getData()).trim();
				NetworkConstants.LOGGER.log(Level.INFO,
						"Discovery packet received from: " + packetAddress.getHostAddress() + "\nData: " + message);

				// See if the packet holds the right command (message)
				if (message.equals(NetworkConstants.UDP_REQUEST_MESSAGE)) {

					if (lock != null && lock.getTrigger() == -1) {
						lock.setTrigger(1);
						synchronized (lock) {
							lock.notifyAll();
						}
					}

					byte[] sendData = new StringBuilder().append(NetworkConstants.UDP_RESPONSE_MESSAGE).append(":")
							.append(ServerSocketHandler.getPort()).toString().getBytes();

					// Send a response
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, packetAddress,
							packet.getPort());
					socket.send(sendPacket);

					NetworkConstants.LOGGER.log(Level.INFO, "Sent packet to: " + packetAddress.getHostAddress());

					// Whitelist
					ServerSocketHandler.allowConnection(packetAddress);
				}
			}
		} catch (IOException ex) {
			if (run && !(ex instanceof SocketException)) {
				NetworkConstants.LOGGER.log(Level.WARNING, ex.getMessage(), ex);
				socket.close();
			}
		}
		NetworkConstants.LOGGER.log(Level.INFO, "Server UDP thread closed.");
	}
}
