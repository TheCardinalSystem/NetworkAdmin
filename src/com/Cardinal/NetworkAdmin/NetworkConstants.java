package com.Cardinal.NetworkAdmin;

import java.io.File;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class NetworkConstants {

	public static final File WORKLOCATION = new File(System.getenv("ProgramFiles"), "Netowrk Admin");
	public static final String UDP_REQUEST_MESSAGE = "CARDINALSERVER_DISCOVER_REQUEST",
			UDP_RESPONSE_MESSAGE = "CARDINALSERVER_DISCOVER_RESPONSE", UDP_END_MESSAGE = "CARDINALSERVER_DISCOVER_END",
			UDP_CAPACITY_MESSAGE = "CARDINALSERVER_DISCOVER_CAPACITY",
			UDP_TRANSFER_MESSAGE = "CARDINALSERVER_DISCOVER_TRANSFER",
			UDP_REBROADCAST_MESSAGE = "CARDINALSERVER_DISCOVER_RETRY", GEN_MAP_REQUEST = "CARDINAL_MAP_REQUEST",
			GEN_MAP_RESPONSE = "CARDINAL_MAP_RESPONSE", SYSTEM_EXIT_MESSAGE = "CARDOMAL_EXIT_SYSTEM",
			TEMP = "CARDINAL_SHUTMEDOWN";

	public static final Logger LOGGER = Logger.getLogger(NetworkConstants.class.getPackage().getName());
	public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static final int PORT = 8888, BUFFER = 500, TIMEOUT = 10000, CAPACITY = 2;

	public static void main(String[] args) {
		NetworkHandler.startupSequence();
	}
}
