package com.Cardinal.NetworkAdmin.Commands;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.Cardinal.NetworkAdmin.Security.SecurityManager;

public class Processor {

	public void processCommand(byte[] uuid, Command command)
			throws IllegalAccessError, ClassNotFoundException, NoSuchMethodException, SecurityException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationException {
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
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	private Object[] executeStep(String step, String[] access, Object... args)
			throws ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, InstantiationException {
		String action = getAction(step).toLowerCase();
		int argumentCount = countArguments(step);

		switch (action) {
		case "access": {
			int arg = getAccess(step);
			String target = access[arg];
			Class<?> clazz = Class.forName(target);
			if (args != null && args.length > 1) {
				List<Object> list = new ArrayList<Object>();
				list.add(clazz);
				list.addAll(Arrays.asList(args));
				return list.toArray(new Object[list.size()]);
			} else if (args.length == 1) {
				return new Object[] { clazz, args[0] };
			} else {
				return new Object[] { clazz };
			}
		}
		case "invoke": {
			if (args != null && args.length > 0) {
				Class<?> accessClass = (Class<?>) args[0];
				String target = getTarget(step);
				if (argumentCount > 0) {
					List<String> arguments = new ArrayList<String>();
					for (int i = 0; i < argumentCount; i++) {
						arguments.add(getArgument(step, i).toLowerCase());
					}
					Class<?>[] types = null;
					Object[] params = null;
					boolean reset = false, get = false;
					if (arguments.contains("reset"))
						reset = true;
					if (arguments.contains("return"))
						get = true;
					if (arguments.contains("param")) {
						params = getParam(arguments.get(arguments.indexOf("param") + 1), args);
						types = Arrays.stream(params).map(t -> t instanceof Integer ? int.class
								: t instanceof Long ? long.class
										: t instanceof Double ? double.class
												: t instanceof Float ? float.class
														: t instanceof Byte ? byte.class
																: t instanceof Short ? short.class
																		: t instanceof Boolean ? boolean.class
																				: t instanceof Character ? char.class
																						: t.getClass())
								.toArray(Class[]::new);
					}
					Method method = types != null ? accessClass.getMethod(target, types)
							: accessClass.getMethod(target);
					if (get && reset) {
						if (params != null) {
							return new Object[] { method.invoke(null, params) };
						} else {
							return new Object[] { method.invoke(null) };
						}
					} else if (reset) {
						if (params != null) {
							method.invoke(null, params);
						} else {
							method.invoke(null);
						}
						return null;
					} else if (get) {
						Object obj = params != null ? method.invoke(null, params) : method.invoke(null);
						List<Object> list = new ArrayList<Object>(Arrays.asList(args));
						list.remove(0);
						list.add(obj);
						return list.toArray(new Object[list.size()]);
					} else {
						if (params != null)
							method.invoke(null, params);
						else
							method.invoke(null);
						if (args.length > 1) {
							List<Object> list = new ArrayList<Object>(Arrays.asList(args));
							list.remove(0);
							return list.toArray(new Object[list.size()]);
						} else {
							return null;
						}
					}
				} else {
					Method method = accessClass.getMethod(target);
					method.invoke(null);
					if (args.length > 1) {
						List<Object> list = new ArrayList<Object>(Arrays.asList(args));
						list.remove(0);
						return list.toArray(new Object[list.size()]);
					} else {
						return null;
					}
				}
			}
		}
		case "param": {
			if (argumentCount > 0) {
				String[] arguments = new String[argumentCount];
				for (int i = 0; i < arguments.length; i++) {
					arguments[i] = getArgument(step, i);
				}
				Object param = createParam(arguments, args);
				List<Object> list = new ArrayList<Object>(Arrays.asList(args));
				list.remove(0);
				list.add(param);
				return list.toArray(new Object[list.size()]);
			} else {
				Object param = createParam(new String[] { getArgument(step, 0) }, args);
				List<Object> list = new ArrayList<Object>(Arrays.asList(args));
				list.remove(0);
				list.add(param);
				return list.toArray(new Object[list.size()]);
			}
		}
		case "reset": {
			if (argumentCount > 0) {
				int flag = Integer.parseInt(getArgument(step, 0));
				int index = args.length - flag;
				return Arrays.copyOfRange(args, index, args.length);
			}
			return null;
		}
		}
		return null;
	}

	private Object[] getParam(String arg, Object... args) {
		if (arg.indexOf(",") > 0) {
			int[] parts = Arrays.stream(arg.split(",")).mapToInt(Integer::parseInt).toArray();
			Object[] paramVals = Arrays.stream(parts).mapToObj(x -> args[x + 1]).toArray(Object[]::new);
			return paramVals;
		} else {
			int param = Integer.parseInt(arg);
			Object paramVal = args[param + 1];
			return new Object[] { paramVal };
		}
	}

	private Object createParam(String[] args, Object... args2) throws NoSuchMethodException, SecurityException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationException {
		int i = 0;
		String type = args[i].toLowerCase();
		Class<?> context = (Class<?>) args2[i];
		switch (type) {
		case "primitive": {
			String val = args[i += 1];
			String parse = context.equals(Integer.class) ? "parseInt" : "parse" + context.getSimpleName();
			Method method = context.getMethod(parse, String.class);
			return method.invoke(null, val);
		}
		case "object": {
			String val = args[i += 1];
			if (val.matches("(\\d,|\\d)+")) {
				int[] params = Arrays.stream(val.split(",")).mapToInt(Integer::parseInt).toArray();
				Object[] paramVals = Arrays.stream(params).mapToObj(x -> args2[x + 1]).toArray(Object[]::new);
				Class<?>[] paramTypes = Arrays.stream(paramVals).map(o -> o.getClass()).toArray(Class[]::new);
				Constructor<?> constructor = context.getConstructor(paramTypes);
				return constructor.newInstance(paramVals);
			} else {
				if (val.matches("\\d+")) {
					Object paramVal = args2[Integer.parseInt(val) + 1];
					Class<?> paramType = paramVal.getClass();
					Constructor<?> constructor = context.getConstructor(paramType);
					return constructor.newInstance(paramVal);
				} else {
					Constructor<?> constructor = context.getConstructor(String.class);
					return constructor.newInstance(val);
				}
			}
		}
		}
		return null;
	}

	private String getTarget(String step) {
		step = step.replaceAll("(^\\s+|(?<=\\s{1})\\s+)", "");
		Matcher matcher = Pattern.compile("(?<=\\})\\s*\\w+").matcher(step);
		matcher.find();
		String target = matcher.group();
		while (Character.isWhitespace(target.charAt(0)))
			target = target.substring(1);
		return target;
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

	private String getArgument(String step, int argument) {
		Matcher matcher = Pattern.compile("(?<=\\}\\{)(\\w|,|\\s)+(?=\\})").matcher(step);
		argument++;
		for (int i = 0; i < argument; i++) {
			matcher.find();
		}
		return matcher.group();
	}

	private int countArguments(String step) {
		Matcher matcher = Pattern.compile("(?<=\\}\\{)(\\w|,)+(?=\\})").matcher(step);
		int count = 0;
		if (matcher.find()) {
			Matcher matcher2 = Pattern.compile("(?<=\\}\\{)(\\w|,|\\s)+(?=\\})").matcher(step);
			while (matcher2.find())
				count++;
		}
		return count;
	}

	/**
	 * Gets the given step's action.
	 * 
	 * @param step
	 * @return
	 */
	private String getAction(String step) {
		Matcher matcher = Pattern.compile("(?<=\\{)(\\w+)(?=\\})").matcher(step);
		matcher.find();
		return matcher.group(0);
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
