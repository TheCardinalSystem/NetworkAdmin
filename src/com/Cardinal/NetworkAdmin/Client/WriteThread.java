package com.Cardinal.NetworkAdmin.Client;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import com.Cardinal.NetworkAdmin.NetworkConstants;
import com.Cardinal.NetworkAdmin.Crypto.CryptoManager;

public class WriteThread extends Thread {

	private Socket socket;
	private OutputStream out;
	private boolean run = true;
	private LinkedBlockingQueue<byte[]> dataOut = new LinkedBlockingQueue<byte[]>();

	public WriteThread(Socket socket) {
		this.socket = socket;
	}

	/**
	 * Encodes data before writing it.
	 * 
	 * @param data
	 * @throws InvalidKeyException
	 * @throws InvalidKeySpecException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 * @throws IOException
	 */
	public synchronized void writeEncoded(byte[] data)
			throws InvalidKeyException, InvalidKeySpecException, NoSuchAlgorithmException, NoSuchPaddingException,
			IllegalBlockSizeException, BadPaddingException, IOException {
		NetworkConstants.LOGGER.log(Level.INFO, "Socket Write-Encoded: " + Arrays.toString(data) + " >> "
				+ socket.getInetAddress().toString() + ":" + socket.getPort());
		dataOut.add(CryptoManager.encrypt(socket.getInetAddress(), data));
	}

	/**
	 * Writes data without encoding it.
	 * 
	 * @param data
	 */
	public synchronized void writeRaw(byte[] data) {
		NetworkConstants.LOGGER.log(Level.INFO, "Socket Write-Raw: " + Arrays.toString(data) + " >> "
				+ socket.getInetAddress().toString() + ":" + socket.getPort());
		dataOut.add(data);
	}

	private void write(byte[] data) throws IOException {
		if (out == null)
			this.out = socket.getOutputStream();
		out.write(data);
	}

	/**
	 * Stops the thread loop. Does not close the socket or output stream.
	 */
	public synchronized void close() {
		run = false;
	}

	private synchronized void doRun() {
		if (!dataOut.isEmpty())
			try {
				write(dataOut.take());
			} catch (IOException | InterruptedException e) {
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
