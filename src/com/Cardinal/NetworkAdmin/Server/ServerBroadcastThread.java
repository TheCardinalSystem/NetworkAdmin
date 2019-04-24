package com.Cardinal.NetworkAdmin.Server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.logging.Level;

import com.Cardinal.NetworkAdmin.NetworkConstants;

public class ServerBroadcastThread implements Runnable {

	private DatagramSocket c;

	@Override
	public void run() {
		// Find the server using UDP broadcast
		try {
			// Open a random port to send the package
			c = new DatagramSocket();
			c.setBroadcast(true);

			byte[] sendData = NetworkConstants.REQUEST_MESSAGE.getBytes();
			// Try the 255.255.255.255 first
			try {
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
						InetAddress.getByName("255.255.255.255"), NetworkConstants.PORT);
				c.send(sendPacket);
				NetworkConstants.LOGGER.log(Level.INFO, "Request packet sent to: 255.255.255.255 (DEFAULT)");
			} catch (Exception e) {
			}

			// Broadcast the message over all the network interfaces
			Enumeration<?> interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface networkInterface = (NetworkInterface) interfaces.nextElement();

				if (networkInterface.isLoopback() || !networkInterface.isUp()) {
					continue; // Don't want to broadcast to the loopback interface
				}

				for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
					InetAddress broadcast = interfaceAddress.getBroadcast();
					if (broadcast == null) {
						continue;
					}

					// Send the broadcast package!
					try {
						DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, broadcast,
								NetworkConstants.PORT);
						c.send(sendPacket);
					} catch (Exception e) {
					}

					NetworkConstants.LOGGER.log(Level.INFO, "Request packet sent to: " + broadcast.getHostAddress()
							+ "; Interface: " + networkInterface.getDisplayName());
				}
			}

			NetworkConstants.LOGGER.log(Level.INFO, "Awaiting reply...");

			// Wait for a response
			byte[] recvBuf = new byte[NetworkConstants.BUFFER];
			DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);

			do {
				c.receive(receivePacket);

				// We have a response
				NetworkConstants.LOGGER.log(Level.INFO,
						"Broadcast response from server: " + receivePacket.getAddress().getHostAddress());

				// Check if the message is correct
				String message = new String(receivePacket.getData()).trim();
				if (message.equals(NetworkConstants.RESPONSE_MESSAGE)) {
					// DO SOMETHING WITH THE SERVER'S IP (for example, store it in your controller)
					ServerSocketHandler.allowConnection(receivePacket.getAddress());
				}
			} while (receivePacket.getAddress().equals(InetAddress.getLocalHost()));
		} catch (IOException ex) {
			NetworkConstants.LOGGER.log(Level.WARNING, ex.getMessage(), ex);
		} finally {
			c.close();
		}
	}

}
