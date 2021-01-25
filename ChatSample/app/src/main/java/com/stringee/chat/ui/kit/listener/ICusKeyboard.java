package com.stringee.chat.ui.kit.listener;

import android.view.View;

/**
 * Created by huybq7 on 9/18/2014.
 */
public class ICusKeyboard {
	public interface InitKeyboardListener {
		public View onInitLayoutVoiceListener();

		public View onInitLayoutEmotionsListener();

		public View onInitLayoutOptionListener();
	}

	public interface ChangeListMessageSizeListener {
		public void onChangeListMessageSize(int sizeListMessage);
	}

	public interface OpenCusKeyboarListener {
		public void onOpenCusKeyboard();
	}

	public interface CloseCusKeyboarListener {
		public void onCloseCusKeyboard();
	}

	public interface SendSmsClickListener {
		public void onSendSmsClick();
	}

	public interface SendReengClickListener {
		public void onSendReengClick();
	}

}
