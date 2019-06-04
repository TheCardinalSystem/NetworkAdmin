package com.Cardinal.NetworkAdmin.Commands;

public class CommandExit implements Command {

	@Override
	public Executor[] getExecutors() {
		return new Executor[] { Executor.REFLECTION };
	}

	@Override
	public String[] getAccess() {
		return new String[] { "java.lang.Integer", "java.lang.System" };
	}

	@Override
	public String[] getSteps() {
		return new String[] { "{Access} 0", "{Param}{Primitive}{0}", "{Access} 1", "{Invoke}{Param}{0} exit" };
	}

}