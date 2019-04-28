package com.Cardinal.NetworkAdmin.Server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.logging.Level;

import com.Cardinal.NetworkAdmin.NetworkConstants;
import com.Cardinal.NetworkAdmin.Client.CommunicationHandler;
import com.Cardinal.NetworkAdmin.Client.SocketHandler;

public class ServerSocketHandler implements Runnable {

	public static HashSet<InetAddress> allowedConnections = new HashSet<InetAddress>();

	private static ServerSocket server;
	private static boolean initializing = false;

	public static synchronized int getPort() {
		while (initializing)
			;

		if (server != null && !server.isClosed()) {
			return server.getLocalPort();
		}
		return -1;
	}

	public static synchronized void readyServer() throws IOException {
		if (server == null || server.isClosed()) {
			initializing = true;
			try {
				server = new ServerSocket(0);
			} catch (IOException e) {
				initializing = false;
				throw e;
			}
			NetworkConstants.LOGGER.log(Level.INFO, "ServerSocket initialized.");
		}
		initializing = false;
	}

	public static synchronized void shutdown() throws IOException {
		if (!server.isClosed()) {
			NetworkConstants.LOGGER.log(Level.INFO, "ServerSocketHanlder shutdown...");
			server.close();
		}
	}

	public static synchronized void allowConnection(InetAddress address) throws IOException {
		NetworkConstants.LOGGER.log(Level.INFO, "Whitelisted IP: " + address.toString().replaceFirst("/", ""));
		allowedConnections.add(address);
		SocketHandler.openConnection(address, 0);
	}

	@Override
	public void run() {
		try {
			readyServer();
		} catch (IOException e) {
			NetworkConstants.LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		NetworkConstants.LOGGER.log(Level.INFO, "Accepting sockets at " + server.getLocalSocketAddress());
		while (!server.isClosed()) {
			try {
				Socket socket = server.accept();
				NetworkConstants.LOGGER.log(Level.INFO,
						"Connected to socket at " + socket.getInetAddress().toString() + ":" + socket.getPort());
				CommunicationHandler handler = new CommunicationHandler(socket);
				if (allowedConnections.contains(socket.getInetAddress())) {
					handler.open();
				} else {
					NetworkConstants.LOGGER.log(Level.INFO, "Connection not allowed for " + socket.getInetAddress());
				}
				SocketHandler.addConnection(socket.getInetAddress(), handler);
			} catch (IOException e) {
				NetworkConstants.LOGGER.log(Level.WARNING, e.getMessage(), e);
			}
		}
	}

}
