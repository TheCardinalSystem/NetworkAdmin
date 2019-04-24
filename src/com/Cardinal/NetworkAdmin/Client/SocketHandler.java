package com.Cardinal.NetworkAdmin.Client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.logging.Level;

import com.Cardinal.NetworkAdmin.NetworkConstants;

public class SocketHandler {

	public static final HashMap<InetAddress, CommunicationHandler> CONNECTIONS = new HashMap<InetAddress, CommunicationHandler>();

	public static void makeConnection(InetAddress address, int port) throws IOException {
		port = 7000; //Temporary
		NetworkConstants.LOGGER.log(Level.INFO, "Attempting to connect to " + address.toString() + ":" + port);
		Socket socket = new Socket(address, port);
		NetworkConstants.LOGGER.log(Level.INFO, "Socket connected at " + socket.getLocalSocketAddress());
		CommunicationHandler handler = new CommunicationHandler(socket);
		handler.open();
		CONNECTIONS.put(address, handler);
	}

	public static void killConnection(InetAddress address) throws IOException {
		CONNECTIONS.get(address).close();
	}

}
