package com.Cardinal.NetworkAdmin.Client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.util.logging.Level;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import com.Cardinal.NetworkAdmin.NetworkConstants;
import com.Cardinal.NetworkAdmin.Crypto.CryptoManager;

public class CommunicationHandler {

	private Socket socket;
	private WriteThread write;
	private ReadThread read;
	private boolean isOpen = false;

	public CommunicationHandler(Socket socket) {
		this.socket = socket;
		write = new WriteThread(socket);
		read = new ReadThread(socket);
	}

	public boolean isOpen() {
		return isOpen;
	}

	public Socket getSocket() {
		return socket;
	}

	public byte[] read(boolean decode)
			throws InterruptedException, InvalidKeyException, InvalidKeySpecException, NoSuchAlgorithmException,
			IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, NoSuchProviderException {
		if (decode) {
			return read.readDecoded();
		} else {
			return read.readRaw();
		}
	}

	public void write(byte[] data, boolean encode)
			throws InvalidKeyException, InvalidKeySpecException, NoSuchAlgorithmException, NoSuchPaddingException,
			IllegalBlockSizeException, BadPaddingException, IOException {
		if (encode) {
			write.writeEncoded(data);
		} else {
			write.writeRaw(data);
		}
	}

	public void write(String str, boolean encode)
			throws InvalidKeyException, InvalidKeySpecException, NoSuchAlgorithmException, NoSuchPaddingException,
			IllegalBlockSizeException, BadPaddingException, IOException {
		write(str.getBytes(), encode);
	}

	public void close() throws IOException {
		NetworkConstants.LOGGER.log(Level.INFO,
				"Closing connection at " + socket.getRemoteSocketAddress().toString().replaceFirst("/", ""));
		CryptoManager.removeKeys(socket.getInetAddress());
		write.close();
		read.close();
		socket.close();
	}

	public void open() {
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
			;
		}
		NetworkConstants.LOGGER.log(Level.INFO, "Communication established with socket at "
				+ socket.getRemoteSocketAddress().toString().replaceFirst("/", ""));
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof CommunicationHandler
				? ((CommunicationHandler) obj).getSocket().getInetAddress().equals(socket.getInetAddress())
				: false;
	}

}
