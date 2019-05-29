package com.Cardinal.NetworkAdmin.FileSystem;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import com.Cardinal.NetworkAdmin.NetworkConstants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

public class FileManager {

	public static final FileManager PERMISSIONS_DIRECTORY = new FileManager(
			new File(NetworkConstants.WORKLOCATION, "Perms"));

	private File concerning;
	private Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private String data;

	public FileManager(File file) {
		concerning = file;
	}

	public FileManager getChild(String name) {
		File f = new File(concerning, name);
		if (f.exists())
			return new FileManager(f);
		else
			return null;
	}

	public File getFile(String name) {
		return new File(concerning, name);
	}

	public File getFile() {
		return concerning;
	}

	public <T> T getEntry(String path, Class<T> type) throws IOException {
		checkData();
		if (data.isEmpty())
			return null;

		JsonObject el = gson.fromJson(data, JsonObject.class);
		String[] array = path.split(".");
		for (String s : array) {
			el = el.get(s).getAsJsonObject();
		}

		return gson.fromJson(el, type);
	}

	private void checkData() throws IOException {
		if (data == null) {
			StringBuilder builder = new StringBuilder();

			BufferedReader reader = new BufferedReader(new FileReader(concerning));
			String s;
			while ((s = reader.readLine()) != null) {
				builder.append(s + "\n");
			}
			reader.close();
			data = builder.toString();
		}
	}
}
