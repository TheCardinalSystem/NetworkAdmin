package com.Cardinal.NetworkAdmin.Client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.logging.Level;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import com.Cardinal.NetworkAdmin.NetworkConstants;
import com.Cardinal.NetworkAdmin.NetworkHandler;

public class SocketHandler {

	public static final HashMap<InetAddress, CommunicationHandler> CONNECTIONS = new HashMap<InetAddress, CommunicationHandler>();
	private static int size = 0;

	public static void openConnection(InetAddress address, int port) throws IOException {
		size++;
		if (NetworkHandler.mode == 0) {
			NetworkConstants.LOGGER.log(Level.INFO, "Attempting to connect to " + address.toString() + ":" + port);
			Socket socket = new Socket(address, port);
			NetworkConstants.LOGGER.log(Level.INFO,
					"Socket connected to " + socket.getRemoteSocketAddress().toString().replaceFirst("/", ""));
			CommunicationHandler handler = new CommunicationHandler(socket);
			CONNECTIONS.put(address, handler);
		}
		if (CONNECTIONS.containsKey(address))
			CONNECTIONS.get(address).open();
		if (size >= NetworkConstants.CAPACITY) {
			NetworkHandler.shutdownUDP();
			try {
				broadcast(NetworkConstants.UDP_END_MESSAGE.getBytes(), false);
			} catch (InvalidKeyException | InvalidKeySpecException | NoSuchAlgorithmException | NoSuchPaddingException
					| IllegalBlockSizeException | BadPaddingException e) {
				NetworkConstants.LOGGER.log(Level.WARNING, e.getMessage(), e);
			}
		}
	}

	public static void broadcast(byte[] data, boolean encode)
			throws InvalidKeyException, InvalidKeySpecException, NoSuchAlgorithmException, NoSuchPaddingException,
			IllegalBlockSizeException, BadPaddingException, IOException {
		for (CommunicationHandler handler : CONNECTIONS.values()) {
			if (handler.isOpen()) {
				handler.write(data, encode);
			}
		}
	}

	public static void killConnection(InetAddress address) throws IOException {
		if (CONNECTIONS.containsKey(address)) {
			CONNECTIONS.get(address).close();
			CONNECTIONS.remove(address);
		}
	}

	public static void addConnection(InetAddress address, CommunicationHandler handler) {
		CONNECTIONS.put(address, handler);
		if (handler.isOpen())
			size++;
	}

}
