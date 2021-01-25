package com.stringee.stringeechatuikit.common;

public interface CallBack {
	public void start();

	public void doWork(Object... params);

	public void end(Object[] params);
}
