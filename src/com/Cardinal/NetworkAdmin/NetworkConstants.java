package com.Cardinal.NetworkAdmin;

import java.io.IOException;
import java.util.logging.Logger;

import com.Cardinal.NetworkAdmin.Utils.MachineUtils;

public class NetworkConstants {

	public static final String REQUEST_MESSAGE = "CARDINALSERVER_DISCOVER_REQUEST",
			RESPONSE_MESSAGE = "CARDINALSERVER_DISCOVER_RESPONSE", UDP_END_MESSAGE = "CARDINALSERVER_DISCOVER_END";

	public static final Logger LOGGER = Logger.getLogger(NetworkConstants.class.getPackage().getName());

	public static final int PORT = 8888, BUFFER = 500, TIMEOUT = 10000, CAPACITY = 2;

	public static void main(String[] args) throws IOException {
		// NetworkHandler.discoverClient();
		System.out.println(MachineUtils.getNetworkUsage());
	}
}
