package com.Cardinal.NetworkAdmin;

import java.lang.Thread.State;

import com.Cardinal.NetworkAdmin.Client.ClientDiscoveryThread;
import com.Cardinal.NetworkAdmin.Server.ServerBroadcastThread;
import com.Cardinal.NetworkAdmin.Server.ServerSocketHandler;

public class NetworkHandler {

	private static Thread client, server, socket;

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
		if (socket == null || socket.getState().equals(State.TERMINATED)) {
			socket = new Thread(new ServerSocketHandler());
			socket.start();
		} else if (socket.getState().equals(State.NEW)) {
			socket.start();
		}
		if (server == null || server.getState().equals(State.TERMINATED)) {
			server = new Thread(new ServerBroadcastThread());
			server.start();
		}
	}

	public static synchronized void discoverClient() {
		if (client == null || client.getState().equals(State.TERMINATED)) {
			client = new Thread(new ClientDiscoveryThread());
			client.start();
		}
	}
}
