package com.Cardinal.NetworkAdmin.Client;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
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
	private boolean run = true;
	private LinkedBlockingQueue<byte[]> dataIn = new LinkedBlockingQueue<byte[]>();

	public ReadThread(Socket socket) {
		this.socket = socket;
	}

	private byte[] read() throws IOException {
		if (in == null)
			in = socket.getInputStream();
		byte[] buffer = new byte[NetworkConstants.BUFFER];
		int size = in.read(buffer);
		return Arrays.copyOfRange(buffer, 0, size);
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
		byte[] data = CryptoManager.decrypt(socket.getInetAddress(), dataIn.take());
		NetworkConstants.LOGGER.log(Level.INFO, "Socket Read-Decoded: " + Arrays.toString(data) + " >> "
				+ socket.getInetAddress().toString() + ":" + socket.getPort());
		return data;
	}

	/**
	 * Returns raw (encoded) data.
	 * 
	 * @return
	 * @throws InterruptedException
	 */
	public synchronized byte[] readRaw() throws InterruptedException {
		byte[] data = dataIn.take();
		NetworkConstants.LOGGER.log(Level.INFO, "Socket Read-Raw: " + Arrays.toString(data) + " >> "
				+ socket.getInetAddress().toString() + ":" + socket.getPort());
		return data;
	}

	/**
	 * Stops the thread loop. Does not close the socket or input stream.
	 */
	public synchronized void close() {
		run = false;
	}

	private synchronized void doRun() {
		try {
			dataIn.add(read());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		while (run) {
			doRun();
		}
	}

}
