package com.Cardinal.NetworkAdmin;

import java.io.IOException;
import java.lang.Thread.State;
import java.util.logging.Level;

import com.Cardinal.NetworkAdmin.Client.ClientBroadcastThread;
import com.Cardinal.NetworkAdmin.Server.ServerDiscoveryThread;
import com.Cardinal.NetworkAdmin.Server.ServerSocketHandler;

public class NetworkHandler {

	private static ServerDiscoveryThread server;
	private static ClientBroadcastThread client;
	private static ServerSocketHandler socket;

	/**
	 * 1 - Server 0 - Client
	 */
	public static int mode = 0;

	public static void exitSystem() {
		NetworkConstants.LOGGER.log(Level.INFO, "Exiting system...");
		shutdownUDP();
		if (server != null && server.isAlive()) {
			try {
				server.join();
			} catch (InterruptedException e) {
			}
		}
		if (client != null && client.isAlive()) {
			try {
				client.join();
			} catch (InterruptedException e) {
			}
		}
		if (socket != null && socket.isAlive()) {
			try {
				socket.close();
			} catch (IOException e1) {
				NetworkConstants.LOGGER.log(Level.WARNING, e1.getMessage(), e1);
			}
			try {
				socket.join();
			} catch (InterruptedException e) {
			}
		}
		NetworkConstants.LOGGER.log(Level.INFO, "Terminating JVM...");
		System.exit(0);
	}

	public static void killThread(int flag) {
		if (flag == 0 && client != null) {
			client.kill();
			client = null;
		} else if (flag == 1 && server != null) {
			server.kill();
			server = null;
			try {
				socket.close();
			} catch (IOException e) {
				NetworkConstants.LOGGER.log(Level.WARNING, e.getMessage(), e);
			}
		}
	}

	public static void shutdownUDP() {
		if (client != null && client.isAlive()) {
			killThread(0);
		}
		if (server != null && server.isAlive()) {
			killThread(1);
		}
	}

	public static void startupSequence() {
		shutdownUDP();
		if (socket != null && socket.isAlive()) {
			try {
				socket.close();
			} catch (IOException e1) {
				NetworkConstants.LOGGER.log(Level.WARNING, e1.getMessage(), e1);
			}
		}

		ThreadLock lock = new ThreadLock();
		broadcastClient(lock);
		synchronized (client) {
			try {
				client.wait();
			} catch (InterruptedException e) {
				NetworkConstants.LOGGER.log(Level.WARNING, e.getMessage(), e);
			}
			discoverServer(lock);
		}
		mode = 0;
		synchronized (lock) {
			try {
				lock.wait();
			} catch (InterruptedException e) {
				NetworkConstants.LOGGER.log(Level.WARNING, e.getMessage(), e);
			}
			if (lock.getTrigger() == 1) { // Server connection
				mode = 1;
				killThread(0);
			} else if (lock.getTrigger() == 0) { // Client connection
				mode = 0;
				killThread(1);
			} else if (lock.getTrigger() == 3) { // Client timeout
				mode = 1;
				killThread(0);
			}
		}
	}

	private static synchronized void discoverServer(ThreadLock lock) {
		if (socket == null || socket.getState().equals(State.TERMINATED)) {
			socket = new ServerSocketHandler();
			socket.start();
		} else if (socket.getState().equals(State.NEW)) {
			socket.start();
		}
		if (server == null || server.getState().equals(State.TERMINATED)) {
			server = new ServerDiscoveryThread(lock);
			server.start();
		} else if (server.getState().equals(State.NEW)) {
			server.start();
		}
	}

	public static synchronized void discoverServer() {
		mode = 1;
		if (socket == null || socket.getState().equals(State.TERMINATED)) {
			socket = new ServerSocketHandler();
			socket.start();
		} else if (socket.getState().equals(State.NEW)) {
			socket.start();
		}
		if (server == null || server.getState().equals(State.TERMINATED)) {
			server = new ServerDiscoveryThread();
			server.start();
		} else if (server.getState().equals(State.NEW)) {
			server.start();
		}
	}

	private static synchronized void broadcastClient(ThreadLock lock) {
		if (client == null || client.getState().equals(State.TERMINATED)) {
			client = new ClientBroadcastThread(lock);
			client.start();
		} else if (client.getState().equals(State.NEW)) {
			client.start();
		}
	}

	public static synchronized void broadcastClient() {
		mode = 0;
		if (client == null || client.getState().equals(State.TERMINATED)) {
			client = new ClientBroadcastThread();
			client.start();
		} else if (client.getState().equals(State.NEW)) {
			client.start();
		}
	}

	public static class ThreadLock {
		private int trigger = -1;

		public synchronized void setTrigger(int trigger) {
			if (this.trigger != -1)
				return;
			this.trigger = trigger;
		}

		public synchronized int getTrigger() {
			return trigger;
		}
	}
}
