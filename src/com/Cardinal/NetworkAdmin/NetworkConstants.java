package com.Cardinal.NetworkAdmin;

import java.util.logging.Logger;

public class NetworkConstants {

	public static final String REQUEST_MESSAGE = "DISCOVER_CARDINALSERVER_REQUEST",
			RESPONSE_MESSAGE = "DISCOVER_CARDINALSERVER_RESPONSE",
			REJECT_MESSAGE = "CARDINAL_CONNECTION_REQUEST_REJECTED",
			ACCEPT_MESSAGE = "CARDINAL_CONNECTION_REQUEST_ACCEPTED";

	public static final Logger LOGGER = Logger.getLogger(NetworkConstants.class.getPackage().getName());

	public static final int PORT = 8888, BUFFER = 15000, TIMEOUT = 10000, DELAY = 30000;

	public static void main(String[] args) {
		NetworkHandler.discoverClient();
	}
}
