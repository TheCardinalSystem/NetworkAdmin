package com.Cardinal.NetworkAdmin.Client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.logging.Level;

import com.Cardinal.NetworkAdmin.NetworkHandler;
import com.Cardinal.NetworkAdmin.NetworkConstants;

public class ClientDiscoveryThread implements Runnable {

	DatagramSocket socket;

	@Override
	public void run() {
		try {
			// Keep a socket open to listen to all the UDP traffic that is destined for this
			// port
			socket = new DatagramSocket(NetworkConstants.PORT, InetAddress.getByName("0.0.0.0"));
			socket.setSoTimeout(NetworkConstants.TIMEOUT);
			socket.setBroadcast(true);

			while (true) {
				NetworkConstants.LOGGER.log(Level.INFO, "Ready for broadcast packets...");

				// Receive a packet
				byte[] recvBuf = new byte[NetworkConstants.BUFFER];
				DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
				socket.receive(packet);

				// Packet received
				NetworkConstants.LOGGER.log(Level.INFO,
						"Discovery packet received from: " + packet.getAddress().getHostAddress());
				NetworkConstants.LOGGER.log(Level.INFO, "Packet received; data: " + new String(packet.getData()));

				// See if the packet holds the right command (message)
				String message = new String(packet.getData()).trim();
				if (message.equals(NetworkConstants.REQUEST_MESSAGE)) {

					byte[] sendData = NetworkConstants.RESPONSE_MESSAGE.getBytes();

					// Send a response
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, packet.getAddress(),
							packet.getPort());
					socket.send(sendPacket);

					NetworkConstants.LOGGER.log(Level.INFO,
							"Sent packet to: " + sendPacket.getAddress().getHostAddress());

					SocketHandler.makeConnection(packet.getAddress(), packet.getPort());
				}
			}
		} catch (SocketTimeoutException e) {
			NetworkHandler.broadcastServer();
		} catch (IOException ex) {
			NetworkConstants.LOGGER.log(Level.WARNING, ex.getMessage(), ex);
		}
	}
}
