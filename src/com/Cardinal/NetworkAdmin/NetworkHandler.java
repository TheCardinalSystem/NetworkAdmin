package com.Cardinal.NetworkAdmin;

import java.lang.Thread.State;

import com.Cardinal.NetworkAdmin.Client.ClientBroadcastThread;
import com.Cardinal.NetworkAdmin.Server.ServerDiscoveryThread;
import com.Cardinal.NetworkAdmin.Server.ServerSocketHandler;

public class NetworkHandler {

	private static ServerDiscoveryThread server;
	private static ClientBroadcastThread client;
	private static Thread socket;
	public static int mode = 0;

	public static void shutdownUDP() {
		if (client != null && client.isAlive()) {
			client.kill();
		}
		if (server != null && server.isAlive()) {
			server.kill();
		}
	}

	public static synchronized void restartThread(int flag) throws InterruptedException {
		if (flag == 1) {
			if (client == null) {
				discoverClient();
			} else if (client.isAlive()) {
				client.join();
				discoverClient();
			} else if (client.getState().equals(State.NEW)) {
				client.start();
			}
		} else {
			if (server == null) {
				broadcastServer();
			} else if (server.isAlive()) {
				server.join();
				broadcastServer();
			} else if (server.getState().equals(State.NEW)) {
				server.start();
			}
		}
	}

	public static synchronized void broadcastServer() {
		mode = 1;
		if (socket == null || socket.getState().equals(State.TERMINATED)) {
			socket = new Thread(new ServerSocketHandler());
			socket.start();
		} else if (socket.getState().equals(State.NEW)) {
			socket.start();
		}
		if (server == null || server.getState().equals(State.TERMINATED)) {
			server = new ServerDiscoveryThread();
			server.start();
		}
	}

	public static synchronized void discoverClient() {
		mode = 0;
		if (client == null || client.getState().equals(State.TERMINATED)) {
			client = new ClientBroadcastThread();
			client.start();
		}
	}
}
