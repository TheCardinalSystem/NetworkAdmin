package com.Cardinal.NetworkAdmin.Commands;

public class CommandClose implements Command {

	@Override
	public Executor[] getExecutors() {
		return new Executor[] { Executor.REFLECTION };
	}

	@Override
	public String[] getAccess() {
		return new String[] { "com.Cardinal.NetworkAdmin.NetworkHandler" };
	}

	@Override
	public String[] getSteps() {
		return new String[] { "{Access} 0", "{Invoke} exitSystem" };
	}

}
