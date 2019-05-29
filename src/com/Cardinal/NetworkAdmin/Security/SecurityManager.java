package com.Cardinal.NetworkAdmin.Security;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import com.Cardinal.NetworkAdmin.NetworkConstants;
import com.Cardinal.NetworkAdmin.Commands.Executor;
import com.Cardinal.NetworkAdmin.FileSystem.FileManager;

public class SecurityManager {

	private static HashMap<UUID, List<String>> accessEntries = new HashMap<UUID, List<String>>();
	private static HashMap<UUID, List<Executor>> executorEntries = new HashMap<UUID, List<Executor>>();

	public static void init() {
		File file = FileManager.PERMISSIONS_DIRECTORY.getFile("Perms");
		if (file.exists()) {
			File[] entries = file.listFiles((FilenameFilter) (dir, name) -> name.endsWith(".json"));
			for (File f : entries) {
				String s = f.getName();
				String uuid = s.substring(0, s.lastIndexOf("."));
				UUID id = UUID.fromString(uuid);
				FileManager manager = new FileManager(f);
				try {
					accessEntries.put(id, Arrays.asList(manager.getEntry("Access", String[].class)));
				} catch (IOException e) {
					NetworkConstants.LOGGER.log(Level.WARNING, e.getMessage(), e);
				}
				try {
					executorEntries.put(id, Arrays.asList(manager.getEntry("Executor", Executor[].class)));
				} catch (IOException e) {
					NetworkConstants.LOGGER.log(Level.WARNING, e.getMessage(), e);
				}
			}
		}
	}

	public static String[] checkAccess(byte[] uuid, String[] access) {
		UUID id = UUID.nameUUIDFromBytes(uuid);
		if (accessEntries.containsKey(id)) {
			return Arrays.stream(access).filter(a -> !accessEntries.get(id).contains(a)).toArray(String[]::new);
		}
		return access;
	}

	public static Executor[] checkExecutors(byte[] uuid, Executor[] executors) {
		UUID id = UUID.nameUUIDFromBytes(uuid);
		if (executorEntries.containsKey(id)) {
			return Arrays.stream(executors).filter(a -> !executorEntries.get(id).contains(a)).toArray(Executor[]::new);
		}
		return executors;
	}

}
