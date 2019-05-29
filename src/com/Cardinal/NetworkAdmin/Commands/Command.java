package com.Cardinal.NetworkAdmin.Commands;

public interface Command {

	public Executor[] getExecutors();

	public String[] getAccess();

	public String[] getSteps();

}
