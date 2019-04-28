package com.Cardinal.NetworkAdmin.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;

public class MachineUtils {

	private static final SystemInfo INFO = new SystemInfo();

	public static Map<String, Long> getLinkSpeeds() throws IOException {
		String result = getProcessResult(
				new ProcessBuilder("wmic.exe", "NIC", "where", "NetEnabled=true", "get", "Name,", "Speed").start());

		Matcher matcher = Pattern.compile("(?<name>\\s*.+(?=\\s{2,}))(?<speed>\\s{2,}\\d+)").matcher(result);

		Map<String, Long> linkSpeeds = new HashMap<String, Long>();
		while (matcher.find()) {
			linkSpeeds.put(matcher.group("name").trim(), Long.parseLong(matcher.group("speed").trim()));
		}
		return linkSpeeds;
	}

	public static double getRAMUsagePercentage() throws IOException {
		GlobalMemory mem = INFO.getHardware().getMemory();
		long tot = mem.getTotal(), av = mem.getAvailable(), free = tot - av;
		return (((double) free) / ((double) tot)) * 100;
	}

	public static int getCPULoadPercentage() throws IOException {
		Matcher matcher = Pattern.compile("\\d{1,3}")
				.matcher(getProcessResult(new ProcessBuilder("wmic.exe", "cpu", "get", "loadpercentage").start()));
		matcher.find();
		return Integer.parseInt(matcher.group());
	}

	public static NetworkInterface getCurrentInterface() throws SocketException, UnknownHostException {
		Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
		InetAddress myAddr = InetAddress.getLocalHost();
		while (interfaces.hasMoreElements()) {
			NetworkInterface face = interfaces.nextElement();

			if (Collections.list(face.getInetAddresses()).contains(myAddr))
				return face;
		}
		return null;
	}

	public static String[] getAdmins() throws IOException {
		Matcher matcher = Pattern.compile("(-+\\s+)([\\s\\S]*?)(The command completed successfully\\.)")
				.matcher(getProcessResult(new ProcessBuilder("net.exe", "localgroup", "administrators").start()));
		matcher.find();
		return matcher.group(2).split("\\s{2,}");
	}

	private static String getProcessResult(Process p) {
		InputStream stream = p.getInputStream();

		@SuppressWarnings("resource")
		Scanner s = new Scanner(stream).useDelimiter("\\A");
		String result = s.hasNext() ? s.next() : "";
		System.err.println(result);
		return result;
	}

	public static String getComputerName() {
		Map<String, String> env = System.getenv();
		if (env.containsKey("COMPUTERNAME"))
			return env.get("COMPUTERNAME");
		else if (env.containsKey("HOSTNAME"))
			return env.get("HOSTNAME");
		else {
			try {
				InetAddress addr;
				addr = InetAddress.getLocalHost();
				String hostname = addr.getHostName();
				return hostname;
			} catch (UnknownHostException ex) {
				return null;
			}
		}
	}

}
