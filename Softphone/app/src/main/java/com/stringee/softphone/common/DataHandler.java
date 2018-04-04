package com.stringee.softphone.common;

import android.content.Context;
import android.os.AsyncTask;

public class DataHandler extends AsyncTask<Object, Object[], Object[]> {

	private CallBack mCallBack;

	public DataHandler(Context context, CallBack callBack) {
		mCallBack = callBack;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		mCallBack.start();
	}

	@Override
	protected Object[] doInBackground(Object... params) {
		mCallBack.doWork(params);
		return params;
	}

	@Override
	protected void onPostExecute(Object[] params) {
		super.onPostExecute(params);
		mCallBack.end(params);
	}
}
