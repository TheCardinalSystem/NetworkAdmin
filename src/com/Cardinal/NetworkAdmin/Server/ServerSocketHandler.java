package com.Cardinal.NetworkAdmin.Server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;

import com.Cardinal.NetworkAdmin.NetworkConstants;
import com.Cardinal.NetworkAdmin.Client.CommunicationHandler;

public class ServerSocketHandler implements Runnable {

	public static final HashMap<InetAddress, CommunicationHandler> CONNECTIONS = new HashMap<InetAddress, CommunicationHandler>();
	public static HashSet<InetAddress> allowedConnections = new HashSet<InetAddress>();

	private static ServerSocket server;

	public static void readyServer() throws IOException {
		if (server == null || server.isClosed()) {
			NetworkConstants.LOGGER.log(Level.INFO, "ServerSocket initialized.");
			server = new ServerSocket(7000); // Temporary Port
		}
	}

	public static void shutdown() throws IOException {
		if (!server.isClosed()) {
			NetworkConstants.LOGGER.log(Level.INFO, "ServerSocketHanlder shutdown...");
			server.close();
		}
	}

	public static void allowConnection(InetAddress address) {
		NetworkConstants.LOGGER.log(Level.INFO, "Whitelisted IP: " + address.toString());
		allowedConnections.add(address);
	}

	@Override
	public void run() {
		try {
			readyServer();
		} catch (IOException e) {
			NetworkConstants.LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		NetworkConstants.LOGGER.log(Level.INFO,
				"Accepting sockets at " + server.getLocalSocketAddress());
		while (!server.isClosed()) {
			try {
				Socket socket = server.accept();
				NetworkConstants.LOGGER.log(Level.INFO,
						"Connected to socket at " + socket.getInetAddress().toString() + ":" + socket.getPort());
				CommunicationHandler handler = new CommunicationHandler(socket);
				if (allowedConnections.contains(socket.getInetAddress())) {
					handler.open();
				}
				CONNECTIONS.put(socket.getInetAddress(), handler);
			} catch (IOException e) {
				NetworkConstants.LOGGER.log(Level.WARNING, e.getMessage(), e);
			}
		}
	}

}
