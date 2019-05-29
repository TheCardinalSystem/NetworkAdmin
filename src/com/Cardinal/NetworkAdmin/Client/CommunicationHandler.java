package com.Cardinal.NetworkAdmin.Client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.util.logging.Level;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import com.Cardinal.NetworkAdmin.NetworkConstants;
import com.Cardinal.NetworkAdmin.NetworkHandler;
import com.Cardinal.NetworkAdmin.Security.CryptoManager;

public class CommunicationHandler extends Thread {

	private Socket socket;
	private WriteThread write;
	private ReadThread read;
	private boolean isOpen = false;

	public CommunicationHandler(Socket socket) {
		this.socket = socket;
		this.setName("CommunicationThread" + socket.getInetAddress().getHostAddress());
		write = new WriteThread(socket);
		read = new ReadThread(socket);
	}

	public synchronized boolean isOpen() {
		return isOpen;
	}

	public synchronized Socket getSocket() {
		return socket;
	}

	public synchronized byte[] read(boolean decode)
			throws InterruptedException, InvalidKeyException, InvalidKeySpecException, NoSuchAlgorithmException,
			IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, NoSuchProviderException {
		if (decode) {
			return read.readDecoded();
		} else {
			return read.readRaw();
		}
	}

	public synchronized void write(byte[] data, boolean encode)
			throws InvalidKeyException, InvalidKeySpecException, NoSuchAlgorithmException, NoSuchPaddingException,
			IllegalBlockSizeException, BadPaddingException, IOException {
		if (encode) {
			write.writeEncoded(data);
		} else {
			write.writeRaw(data);
		}
	}

	public synchronized void write(String str, boolean encode)
			throws InvalidKeyException, InvalidKeySpecException, NoSuchAlgorithmException, NoSuchPaddingException,
			IllegalBlockSizeException, BadPaddingException, IOException {
		write(str.getBytes(), encode);
	}

	public synchronized void close(boolean flush) throws IOException {
		isOpen = false;
		NetworkConstants.LOGGER.log(Level.INFO,
				"Closing connection at " + socket.getRemoteSocketAddress().toString().replaceFirst("/", ""));
		CryptoManager.removeKeys(socket.getInetAddress());
		write.close(flush);
		try {
			write.join();
		} catch (InterruptedException e) {
			NetworkConstants.LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		NetworkConstants.LOGGER.log(Level.INFO,
				"Write-Thread closed " + socket.getRemoteSocketAddress().toString().replaceFirst("/", ""));
		CryptoManager.removeKeys(socket.getInetAddress());
		read.close();
		try {
			read.join();
		} catch (InterruptedException e) {
		}
		NetworkConstants.LOGGER.log(Level.INFO,
				"Read-Thread closed " + socket.getRemoteSocketAddress().toString().replaceFirst("/", ""));
		CryptoManager.removeKeys(socket.getInetAddress());
		socket.close();
		SocketHandler.checkLink();
		NetworkConstants.LOGGER.log(Level.INFO,
				"Connection closed " + socket.getRemoteSocketAddress().toString().replaceFirst("/", ""));
	}

	@Override
	public void run() {
		if (isOpen)
			return;

		isOpen = true;
		NetworkConstants.LOGGER.log(Level.INFO, "Opening communication with socket at "
				+ socket.getRemoteSocketAddress().toString().replaceFirst("/", ""));
		read.start();
		write.start();

		InetAddress address = socket.getInetAddress();
		try {
			write.writeRaw(CryptoManager.getPublicKey(address).getEncoded());
		} catch (NoSuchAlgorithmException | NoSuchProviderException e) {
			NetworkConstants.LOGGER.log(Level.WARNING, e.getMessage(), e);
		}

		try {
			byte[] response = read.readRaw();
			CryptoManager.setPublicKey(address, response);
		} catch (InterruptedException | InvalidKeySpecException | NoSuchAlgorithmException e) {
			NetworkConstants.LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		NetworkConstants.LOGGER.log(Level.INFO, "Communication established with socket at "
				+ socket.getRemoteSocketAddress().toString().replaceFirst("/", ""));

		String message = null;
		boolean map2 = false;

		while (isOpen) {
			if (read.dataAvailable()) {
				try {
					byte[] b = read.peekDecoded();
					String s = new String(b);
					if (s.isEmpty())
						continue;

					if (s.startsWith(NetworkConstants.GEN_MAP_REQUEST)) {
						read.pop();
						boolean recurse = false;
						if (s.indexOf(":") > 1) {
							try {
								int i = Integer.parseInt(s.split(":")[1]);
								recurse = i == 1;
							} catch (NumberFormatException e) {
								recurse = false;
							}
						}
						NetPoint map = SocketHandler.generateMap(socket.getInetAddress(), recurse);
						String data = NetworkConstants.GSON.toJson(map, NetPoint.class);
						write.writeEncoded(data.getBytes());
					} else if (s.startsWith(NetworkConstants.UDP_END_MESSAGE)) {
						read.pop();
						if (SocketHandler.getConnections() >= NetworkConstants.CAPACITY) {
							InetAddress a = SocketHandler.BroadcastUDPEnd(socket.getInetAddress());
							write.writeEncoded(
									(NetworkConstants.UDP_TRANSFER_MESSAGE + ":" + a.getHostAddress()).getBytes());
						} else {
							NetworkHandler.discoverServer();
							write.writeEncoded((NetworkConstants.UDP_TRANSFER_MESSAGE + ":" + address).getBytes());
						}
					} else if (s.endsWith(NetworkConstants.UDP_REBROADCAST_MESSAGE)) {
						NetworkHandler.broadcastClient();
						SocketHandler.killConnection(address, false);
					} else if (s.equals(NetworkConstants.SYSTEM_EXIT_MESSAGE)) {
						NetworkHandler.exitSystem();
					} else if (s.equals(NetworkConstants.TEMP)) {
						write(NetworkConstants.SYSTEM_EXIT_MESSAGE, true);
					}
				} catch (InvalidKeyException | InvalidKeySpecException | NoSuchAlgorithmException
						| IllegalBlockSizeException | BadPaddingException | NoSuchPaddingException
						| NoSuchProviderException | InterruptedException | IOException e) {
					NetworkConstants.LOGGER.log(Level.WARNING, e.getMessage(), e);
				}
			}
			if (message != null) {
				try {
					write(message, true);
				} catch (InvalidKeyException | InvalidKeySpecException | NoSuchAlgorithmException
						| NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | IOException e) {
					e.printStackTrace();
				}
				message = null;
			}
			if (map2) {
				map2 = false;
				try {
					System.out.println(SocketHandler.generateMap(null, true));
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof CommunicationHandler
				? ((CommunicationHandler) obj).getSocket().getInetAddress().equals(socket.getInetAddress())
				: false;
	}

}
