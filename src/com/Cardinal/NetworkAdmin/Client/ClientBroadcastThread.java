package com.Cardinal.NetworkAdmin.Client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.logging.Level;

import com.Cardinal.NetworkAdmin.NetworkConstants;
import com.Cardinal.NetworkAdmin.NetworkHandler;
import com.Cardinal.NetworkAdmin.NetworkHandler.ThreadLock;
import com.Cardinal.NetworkAdmin.Utils.NetworkUtils;

public class ClientBroadcastThread extends Thread {

	private DatagramSocket c;
	private boolean run = true;
	private ThreadLock lock;

	public ClientBroadcastThread() {
		lock = null;
	}

	public ClientBroadcastThread(ThreadLock lock) {
		this.lock = lock;
	}

	public void kill() {
		NetworkConstants.LOGGER.log(Level.INFO, "Closing client UDP thread...");
		run = false;
		c.close();
	}

	@Override
	public void run() {
		// Find the server using UDP broadcast
		try {
			// Open a random port to send the package
			c = new DatagramSocket();
			c.setBroadcast(true);

			byte[] sendData = NetworkConstants.UDP_REQUEST_MESSAGE.getBytes();

			Collection<String> netmasks = NetworkUtils.getAllNetmasks();
			for (String netmask : netmasks) {
				try {
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
							InetAddress.getByName(netmask), NetworkConstants.PORT);
					c.send(sendPacket);
				} catch (Exception e) {
				}
			}

			NetworkConstants.LOGGER.log(Level.INFO, "Request packet sent to: "
					+ Arrays.toString(netmasks.toArray(new String[netmasks.size()])) + " (Default)");

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
			c.setSoTimeout(NetworkConstants.TIMEOUT);
			synchronized (this) {
				notifyAll();
			}
			do {
				c.receive(receivePacket);

				String message = new String(receivePacket.getData()).trim();
				NetworkConstants.LOGGER.log(Level.INFO, "Broadcast response from server: "
						+ receivePacket.getAddress().getHostAddress() + "\nData: " + message);

				// Check if the message is correct
				if (message.startsWith(NetworkConstants.UDP_RESPONSE_MESSAGE)) {
					// Make connection
					SocketHandler.openConnection(receivePacket.getAddress(),
							Integer.parseInt(message.substring(message.lastIndexOf(":") + 1)));
					c.close();
					if (lock != null)
						lock.setTrigger(0);
					break;
				}
			} while (receivePacket.getAddress().equals(InetAddress.getLocalHost()));
		} catch (SocketTimeoutException e) {
			NetworkConstants.LOGGER.log(Level.INFO, "Packet discovery timed out.");
			c.close();
			if (lock != null)
				lock.setTrigger(3);
			else
				NetworkHandler.startupSequence();
		} catch (IOException ex) {
			if (run && !(ex instanceof SocketException))
				NetworkConstants.LOGGER.log(Level.WARNING, ex.getMessage(), ex);
		} finally {
			if (!c.isClosed())
				c.close();
			if (lock != null) {
				synchronized (lock) {
					lock.notifyAll();
				}
			}
		}
		NetworkConstants.LOGGER.log(Level.INFO, "Client UDP thread closed.");
	}

}
