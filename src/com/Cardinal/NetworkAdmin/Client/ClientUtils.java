package com.Cardinal.NetworkAdmin.Client;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientUtils {

	public static String[] getAdmins() throws IOException {
		Process p = new ProcessBuilder("cmd", "/c", "net", "localgroup", "administrators").start();

		InputStream stream = p.getInputStream();

		@SuppressWarnings("resource")
		Scanner s = new Scanner(stream).useDelimiter("\\A");
		String result = s.hasNext() ? s.next() : "";

		s.close();

		Matcher matcher = Pattern.compile("(-+\\s+)([\\s\\S]*?)(The command completed successfully\\.)")
				.matcher(result);
		matcher.find();
		return matcher.group(2).split("\\s{2,}");
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
