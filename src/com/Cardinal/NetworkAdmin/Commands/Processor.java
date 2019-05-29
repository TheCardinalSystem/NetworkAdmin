package com.Cardinal.NetworkAdmin.Commands;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.Cardinal.NetworkAdmin.Security.SecurityManager;

public class Processor {

	public void processCommand(byte[] uuid, Command command) throws IllegalAccessError, ClassNotFoundException {
		checkPermission(uuid, "Access denied to", command.getAccess());
		checkPermission(uuid, "No permission to use", command.getExecutors());
		String[] steps = command.getSteps();

		Object[] obj = null;
		for (String s : steps) {
			if (obj != null) {
				obj = executeStep(s, command.getAccess(), obj);
			} else {
				obj = executeStep(s, command.getAccess());
			}
		}
	}

	/**
	 * Executes the given step. If an object is returned, then it is required to
	 * complete the next step.
	 * 
	 * @param step
	 * @param access
	 * @return
	 * @throws ClassNotFoundException
	 */
	private Object[] executeStep(String step, String[] access, Object... args) throws ClassNotFoundException {
		String action = getAction(step);
		switch (action) {
		case "Access": {
			int arg = getAccess(step);
			String target = access[arg];
			Class<?> clazz = Class.forName(target);
			return new Class[] { clazz };
		}
		case "Invoke": {
			if (args != null && args.length > 0) {
				Class<?> accessClass = (Class<?>) args[0];
				String target = getAction(step);
			}
		}
		}
		return null;
	}

	private String getTarget(String step) {
		Matcher matcher = Pattern.compile("(?<=\\}\\s*)\\S+").matcher(step);
		matcher.find();
		return matcher.group();
	}

	/**
	 * Gets the index for the access array which the given step requires.
	 * 
	 * @param step
	 * @return
	 */
	private int getAccess(String step) {
		return Integer.parseInt(getTarget(step));
	}

	/**
	 * Gets the given step's action.
	 * 
	 * @param step
	 * @return
	 */
	private String getAction(String step) {
		Matcher matcher = Pattern.compile("(?<=\\{)(\\w*)(?=\\})").matcher(step);
		matcher.find();
		return matcher.group();
	}

	private void checkPermission(byte[] uuid, String message, Object[] request) throws IllegalAccessError {
		if (request instanceof String[]) {
			String[] access = SecurityManager.checkAccess(uuid, (String[]) request);
			if (access.length > 0) {
				throw new IllegalAccessError(message + " " + Arrays.toString(access));
			}
		} else if (request instanceof Executor[]) {
			Executor[] access = SecurityManager.checkExecutors(uuid, (Executor[]) request);
			if (access.length > 0) {
				throw new IllegalAccessError(message + " " + Arrays.toString(access));
			}
		}
	}
}
