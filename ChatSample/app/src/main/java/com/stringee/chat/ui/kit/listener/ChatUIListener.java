package com.stringee.chat.ui.kit.listener;

public interface ChatUIListener {

    public void onButtonSendClick();

    public void onButtonVoiceClick();

    public void onButtonAttachClick();

    public void onButtonStickerClick(boolean isShow);

    public void onKeyboardShown();

}
