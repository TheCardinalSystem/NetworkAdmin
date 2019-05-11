package com.Cardinal.NetworkAdmin.Client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import com.Cardinal.NetworkAdmin.NetworkConstants;
import com.Cardinal.NetworkAdmin.NetworkHandler;
import com.Cardinal.NetworkAdmin.Server.ServerSocketHandler;

public class SocketHandler {

	public static final HashMap<InetAddress, CommunicationHandler> CONNECTIONS = new HashMap<InetAddress, CommunicationHandler>();
	private static int size = 0;

	public static void checkLink() {
		if (CONNECTIONS.values().stream().noneMatch(CommunicationHandler::isOpen)) {
			CONNECTIONS.clear();
			ServerSocketHandler.allowedConnections.clear();
			size = 0;
			NetworkHandler.startupSequence();
		}
	}

	public static int getConnections() {
		return size;
	}

	public static NetPoint generateMap(InetAddress requestOrigin, boolean recurse) throws UnknownHostException {
		NetPoint map = new NetPoint(InetAddress.getLocalHost());

		for (Entry<InetAddress, CommunicationHandler> entry : CONNECTIONS.entrySet()) {
			if (entry.getKey().equals(requestOrigin))
				continue;
			NetPoint point = new NetPoint(entry.getKey());
			if (recurse) {
				System.out.println("recurse");
				CommunicationHandler handler = entry.getValue();
				if (handler.isOpen()) {
					System.out.println(entry.getKey());
					try {
						handler.write(NetworkConstants.GEN_MAP_REQUEST, true);
						byte[] bytes = handler.read(true);
						NetPoint response = NetworkConstants.GSON.fromJson(new String(bytes), NetPoint.class);
						point.addEntry(response);
					} catch (InvalidKeyException | InvalidKeySpecException | NoSuchAlgorithmException
							| NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | IOException
							| NoSuchProviderException | InterruptedException e) {
						NetworkConstants.LOGGER.log(Level.WARNING, e.getMessage(), e);
					}
				}
			}
			map.addEntry(point);
		}

		return map;
	}

	public static void openConnection(InetAddress address, int port) throws IOException {
		size++;
		if (NetworkHandler.mode == 0) {
			NetworkConstants.LOGGER.log(Level.INFO, "Attempting to connect to " + address.toString() + ":" + port);
			Socket socket = new Socket();
			socket.connect(new InetSocketAddress(address, port), NetworkConstants.TIMEOUT);

			NetworkConstants.LOGGER.log(Level.INFO,
					"Socket connected to " + socket.getRemoteSocketAddress().toString().replaceFirst("/", ""));
			CommunicationHandler handler = new CommunicationHandler(socket);
			CONNECTIONS.put(address, handler);
		}
		if (CONNECTIONS.containsKey(address))
			CONNECTIONS.get(address).start();

		if (size >= NetworkConstants.CAPACITY && NetworkHandler.mode == 1) {
			InetAddress a = BroadcastUDPEnd(InetAddress.getLocalHost());
			NetworkHandler.shutdownUDP();
			if (size > NetworkConstants.CAPACITY && !address.equals(a)) {
				try {
					CONNECTIONS.get(address).write(NetworkConstants.UDP_REBROADCAST_MESSAGE, true);
				} catch (InvalidKeyException | InvalidKeySpecException | NoSuchAlgorithmException
						| NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static InetAddress BroadcastUDPEnd(InetAddress requestOrigin) {
		for (Entry<InetAddress, CommunicationHandler> entry : CONNECTIONS.entrySet()) {
			if (!entry.getKey().equals(requestOrigin)) {
				CommunicationHandler handler = entry.getValue();
				try {
					handler.write(NetworkConstants.UDP_END_MESSAGE, true);
					byte[] response = handler.read(true);
					String s = new String(response);
					if (s.isEmpty() || s.startsWith(NetworkConstants.UDP_CAPACITY_MESSAGE))
						continue;

					if (s.startsWith(NetworkConstants.UDP_TRANSFER_MESSAGE)) {
						return InetAddress.getByName(s.split(":")[1]);
					}
				} catch (InvalidKeyException | InvalidKeySpecException | NoSuchAlgorithmException
						| NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | IOException
						| NoSuchProviderException | InterruptedException e) {
					NetworkConstants.LOGGER.log(Level.WARNING, e.getMessage(), e);
				}
			}
		}
		return null;
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

	public static void broadcast(byte[] data, boolean encode, InetAddress... ignoredAddresses)
			throws InvalidKeyException, InvalidKeySpecException, NoSuchAlgorithmException, NoSuchPaddingException,
			IllegalBlockSizeException, BadPaddingException, IOException {

		List<InetAddress> ignored = Arrays.asList(ignoredAddresses);
		for (Entry<InetAddress, CommunicationHandler> entry : CONNECTIONS.entrySet()) {
			if (!ignored.contains(entry.getKey())) {
				CommunicationHandler handler = entry.getValue();
				if (handler.isOpen()) {
					handler.write(data, encode);
				}
			}
		}
	}

	public static void killConnection(InetAddress address, boolean flush) throws IOException {
		if (CONNECTIONS.containsKey(address)) {
			CONNECTIONS.get(address).close(flush);
			CONNECTIONS.remove(address);
		}
	}

	public static void addConnection(InetAddress address, CommunicationHandler handler) {
		CONNECTIONS.put(address, handler);
		if (handler.isOpen())
			size++;
	}

}
