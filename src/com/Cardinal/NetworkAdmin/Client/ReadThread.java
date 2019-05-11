package com.Cardinal.NetworkAdmin.Client;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import com.Cardinal.NetworkAdmin.NetworkConstants;
import com.Cardinal.NetworkAdmin.Crypto.CryptoManager;

public class ReadThread extends Thread {

	private Socket socket;
	private InputStream in;
	private boolean run = true, data = false;
	private LinkedBlockingQueue<byte[]> dataIn = new LinkedBlockingQueue<byte[]>();

	public ReadThread(Socket socket) {
		this.socket = socket;
		this.setName("ReadThread" + socket.getInetAddress().getHostAddress());
	}

	public void pop()
			throws InterruptedException, InvalidKeyException, InvalidKeySpecException, NoSuchAlgorithmException,
			IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, NoSuchProviderException {
		byte[] data = readDecoded();
		NetworkConstants.LOGGER.log(Level.INFO, "Socket Pop: " + Arrays.toString(data) + "\n<< "
				+ socket.getRemoteSocketAddress().toString().replaceFirst("/", ""));
	}

	public byte[] peek() {
		return dataIn.peek();
	}

	public byte[] peekDecoded() throws InvalidKeyException, InvalidKeySpecException, NoSuchAlgorithmException,
			IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, NoSuchProviderException {
		return CryptoManager.decrypt(socket.getInetAddress(), dataIn.peek());
	}

	public synchronized boolean dataAvailable() {
		return data;
	}

	private byte[] read() throws IOException {
		if (in == null)
			in = socket.getInputStream();
		byte[] buffer = new byte[NetworkConstants.BUFFER];
		int size = in.read(buffer);
		NetworkConstants.LOGGER.log(Level.INFO,
				"Data recieved << " + socket.getRemoteSocketAddress().toString().replaceFirst("/", ""));
		return Arrays.copyOfRange(buffer, 0, size);
	}

	private synchronized byte[] take() throws InterruptedException {
		byte[] data = dataIn.take();
		this.data = !dataIn.isEmpty();
		return data;
	}

	/**
	 * Decodes data before returning it.
	 * 
	 * @return
	 * @throws InvalidKeyException
	 * @throws InvalidKeySpecException
	 * @throws NoSuchAlgorithmException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 * @throws NoSuchPaddingException
	 * @throws NoSuchProviderException
	 * @throws InterruptedException
	 */
	public synchronized byte[] readDecoded()
			throws InvalidKeyException, InvalidKeySpecException, NoSuchAlgorithmException, IllegalBlockSizeException,
			BadPaddingException, NoSuchPaddingException, NoSuchProviderException, InterruptedException {
		byte[] data = CryptoManager.decrypt(socket.getInetAddress(), take());
		NetworkConstants.LOGGER.log(Level.INFO, "Socket Read-Decoded: " + Arrays.toString(data) + "\n<< "
				+ socket.getRemoteSocketAddress().toString().replaceFirst("/", ""));
		return data;
	}

	/**
	 * Returns raw (encoded) data.
	 * 
	 * @return
	 * @throws InterruptedException
	 */
	public synchronized byte[] readRaw() throws InterruptedException {
		byte[] data = take();
		NetworkConstants.LOGGER.log(Level.INFO, "Socket Read-Raw: " + Arrays.toString(data) + "\n<< "
				+ socket.getRemoteSocketAddress().toString().replaceFirst("/", ""));
		return data;
	}

	/**
	 * Stops the thread loop. Does not close the socket or input stream.
	 */
	public synchronized void close() {
		interrupt();
	}

	@Override
	public void run() {
		while (run) {
			try {
				data = !dataIn.isEmpty();
				dataIn.add(read());
			} catch (IOException e) {
				NetworkConstants.LOGGER.log(Level.WARNING, e.getMessage(), e);
				if (e instanceof SocketException && e.getMessage().contains("reset")) {
					run = false;
					try {
						SocketHandler.killConnection(socket.getInetAddress(), false);
					} catch (IOException e1) {
						NetworkConstants.LOGGER.log(Level.WARNING, e1.getMessage(), e1);
					}
				}
			}
		}
	}

}
