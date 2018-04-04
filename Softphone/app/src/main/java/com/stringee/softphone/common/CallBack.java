package com.stringee.softphone.common;

public interface CallBack {
	public void start();

	public void doWork(Object... params);

	public void end(Object[] params);
}
