package com.stringee.chat.ui.kit.view;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import androidx.core.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;

import com.stringee.chat.ui.kit.commons.Constant;
import com.stringee.chat.ui.kit.listener.ChatUIListener;
import com.stringee.chat.ui.kit.listener.ICusKeyboard;
import com.stringee.stringeechatuikit.BaseActivity;
import com.stringee.stringeechatuikit.R;
import com.stringee.stringeechatuikit.common.Common;
import com.stringee.stringeechatuikit.common.Utils;

public class CusKeyboardController implements CusRelativeLayout.IKeyboardChanged, CusKeyboardWidget.OnDrawerOpenListener, CusKeyboardWidget.OnDrawerScrollListener,
        CusKeyboardWidget.OnDrawerCloseListener, OnTouchListener {
    private String TAG = CusKeyboardController.class.getSimpleName();

    private BaseActivity activity;
    private ChatUIListener chatUIListener;
    private ICusKeyboard.ChangeListMessageSizeListener changeListMessageSizeListener;

    private View rootView, vSticker, vChatbar;
    private ImageButton attachButton, stickerButton, sendMessageButton, recordButton;
    private CusKeyboardWidget keyboardWidget;
    private int mChatbarHeight;
    private int mKeyboardHeight;
    private int mTopOffset;
    private int mScreenHeight;
    private int mActionbarHeight;
    private EditText etChat;

    public CusKeyboardController(BaseActivity activity, CusRelativeLayout rootView,
                                 final CusKeyboardWidget keyboardWidget, ChatUIListener listener,
                                 ICusKeyboard.ChangeListMessageSizeListener changeListMessageSizeListener) {
        this.activity = activity;
        this.chatUIListener = listener;
        this.changeListMessageSizeListener = changeListMessageSizeListener;
        this.rootView = rootView;
        this.keyboardWidget = keyboardWidget;
        Display dm = ((WindowManager) activity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        mTopOffset = Common.preferences.getInt(Constant.PREF_CHAT_TOP_OFFSET, dm.getHeight() / 2);
        this.keyboardWidget.setTopOffset(mTopOffset);
        this.keyboardWidget.lock();
        // cac view
        vChatbar = keyboardWidget.findViewById(R.id.v_chat);
        vSticker = keyboardWidget.findViewById(R.id.v_stickers);

        rootView.addKeyboardStateChangedListener(this);
        registerChatbarHeightChange();
        registerKeyboardHeightChange();

        // menu tren chat bar
        etChat = (EditText) keyboardWidget.findViewById(R.id.et_msg);
        stickerButton = keyboardWidget.findViewById(R.id.stickerButton);
        attachButton = keyboardWidget.findViewById(R.id.btn_attach);
        sendMessageButton = keyboardWidget.findViewById(R.id.btn_send_msg);
        recordButton = keyboardWidget.findViewById(R.id.btn_record);

        attachButton.setColorFilter(Color.parseColor("#929395"), PorterDuff.Mode.SRC_IN);
        stickerButton.setColorFilter(Color.parseColor("#929395"), PorterDuff.Mode.SRC_IN);
        recordButton.setColorFilter(Color.parseColor("#929395"), PorterDuff.Mode.SRC_IN);
        sendMessageButton.setColorFilter(ContextCompat.getColor(activity, R.color.stringee_colorPrimary), PorterDuff.Mode.SRC_IN);

        initClick();

        etChat.setOnFocusChangeListener(new OnFocusChangeListener() {

            @Override
            public void onFocusChange(View arg0, boolean arg1) {
                stickerButton.setImageResource(R.drawable.ic_sticker);
                Utils.showSoftKeyboard(CusKeyboardController.this.activity, etChat);
                etChat.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        keyboardWidget.open();
                    }
                }, 300);
            }
        });

        etChat.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.toString().trim().length() > 0) {
                    handleSendAndRecordButtonView(true);
                } else {
                    handleSendAndRecordButtonView(false);
                }
            }
        });

        keyboardWidget.setOnDrawerOpenListener(this);
        keyboardWidget.setOnDrawerCloseListener(this);
        keyboardWidget.setOnDrawerScrollListener(this);

        mChatbarHeight = vChatbar.getHeight();
        mActionbarHeight = (int) activity.getResources().getDimension(R.dimen.actionbar_height);
    }

    @Override
    public void onKeyboardShown() {
        isHiddenKeyboard = false;
        keyboardWidget.open();
        flagHiddenKeyboardByChatbar = false;
        isStickerShow = false;
        stickerButton.setImageResource(R.drawable.ic_sticker);
        chatUIListener.onKeyboardShown();
    }

    @Override
    public void onKeyboardHidden() {
        isHiddenKeyboard = true;
        if (!flagHiddenKeyboardByChatbar) {
            closeCusKeyboardFolowRootView();
        }
    }

    private void registerKeyboardHeightChange() {
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                Rect rect = new Rect();
                rootView.getWindowVisibleDisplayFrame(rect);

                int screenHeight = rootView.getRootView().getHeight();
                int heightDifference = screenHeight - rect.bottom;

                if (heightDifference > 0 && heightDifference != mKeyboardHeight) {
                    mKeyboardHeight = heightDifference;

                    if (mKeyboardHeight > 0.15 * screenHeight) {
                        mTopOffset = rootView.getHeight() - mChatbarHeight;
                        rootView.post(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(50);
                                } catch (Exception ex) {
                                }
                                setOffsetKeyboard();// set keyboard height
                                keyboardWidget.open();
                                // change height list
                                if (keyboardWidget != null) {
                                    changeListMessageSizeListener.onChangeListMessageSize(mTopOffset + mActionbarHeight);
                                }
                            }
                        });

                        Editor editor = Common.preferences.edit();
                        editor.putInt(Constant.PREF_CHAT_TOP_OFFSET, mTopOffset + mChatbarHeight);
                        editor.commit();
                    }
                }

                if (rootView.getHeight() > 0 && mScreenHeight != rootView.getHeight()) {
                    if (keyboardWidget != null && !keyboardWidget.isOpened()) {
                        if (changeListMessageSizeListener != null) {
                            changeListMessageSizeListener.onChangeListMessageSize(rootView.getHeight() - mChatbarHeight
                                    + mActionbarHeight);
                        }
                    }
                    mScreenHeight = rootView.getHeight();
                }
            }
        });
    }

    private void registerChatbarHeightChange() {
        vChatbar.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                if (mChatbarHeight != vChatbar.getHeight()) {
                    int delta = vChatbar.getHeight() - mChatbarHeight;
                    mChatbarHeight = vChatbar.getHeight();
                    mTopOffset = mTopOffset - delta;

                    setOffsetKeyboard();
                    if (isOpened()) {
                        changeListMessageSizeListener.onChangeListMessageSize(mTopOffset + mActionbarHeight);
                    } else {
                        changeListMessageSizeListener.onChangeListMessageSize(mScreenHeight - mChatbarHeight
                                + mActionbarHeight);
                    }
                }
            }
        });
    }

    private void setOffsetKeyboard() {
        keyboardWidget.setTopOffset(mTopOffset);
    }

    public boolean isOpened() {
        return keyboardWidget.isOpened();
    }

    private static final int SLIDE_OFF_CONTENT_AND_CLOSE_CUSTOM_KEYBOARD = 2;
    boolean isHidening = false;
    boolean flagHiddenKeyboardByChatbar = false;
    private boolean isHiddenKeyboard = true;
    private boolean isStickerShow = false;

    public synchronized void hideCusKeyboardController() {
        if (isOpened() && !isHidening) {
            isHidening = true;
            hideKeyboardController();
        }
    }

    private void hideKeyboardController() {
        if (isHiddenKeyboard) {
            startContentSlidingOffAnd(SLIDE_OFF_CONTENT_AND_CLOSE_CUSTOM_KEYBOARD);
        } else {
            Utils.hideSoftKeyboardWithEditText(activity, etChat);
        }
    }

    /**
     * @param isFlag 1: Do nothing; 2: Close Custom Keyboard; 3: Show system
     *               Keyboard
     */
    private synchronized void startContentSlidingOffAnd(final int isFlag) {
        if (isFlag == SLIDE_OFF_CONTENT_AND_CLOSE_CUSTOM_KEYBOARD) {
            if (keyboardWidget != null && keyboardWidget.isOpened()) {
                removeAllViewInContainLayout();
                closeCusKeyboardFolowRootView();
            }
        }
    }

    private void closeCusKeyboardFolowRootView() {
        rootView.post(new Runnable() {
            @Override
            public void run() {
                if (keyboardWidget != null && keyboardWidget.isOpened()) {
                    try {
                        Thread.sleep(50);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    keyboardWidget.animateClose();
                }
            }
        });
    }

    private void removeAllViewInContainLayout() {
        vSticker.setVisibility(View.GONE);
    }

    public void initClick() {
        attachButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                chatUIListener.onButtonAttachClick();
            }
        });

        stickerButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                isStickerShow = !isStickerShow;
                if (isStickerShow) {
                    flagHiddenKeyboardByChatbar = true;
                    keyboardWidget.open();
                    Utils.hideSoftKeyboardWithEditText(activity, etChat);
                    stickerButton.setImageResource(R.drawable.input_keyboard);
                    chatUIListener.onButtonStickerClick(isStickerShow);
                } else {
                    etChat.requestFocus();
                    Utils.showSoftKeyboard(activity, etChat);
                    stickerButton.setImageResource(R.drawable.ic_sticker);
                    chatUIListener.onButtonStickerClick(isStickerShow);
                }
            }
        });

        recordButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                chatUIListener.onButtonVoiceClick();
            }
        });

        sendMessageButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                chatUIListener.onButtonSendClick();
            }
        });

        etChat.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                stickerButton.setImageResource(R.drawable.ic_sticker);
                Utils.showSoftKeyboard(activity, etChat);
                etChat.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        keyboardWidget.open();
                    }
                }, 300);
            }
        });
    }

    @Override
    public void onCusKeyboardOpened(String openCode) {
        if (changeListMessageSizeListener != null) {
            changeListMessageSizeListener.onChangeListMessageSize(mTopOffset + mActionbarHeight);
        }
        isHidening = false;
    }

    @Override
    public void onCusKeyboardScrollStarted() {
    }

    @Override
    public synchronized void onCusKeyboardScrollEnded() {
        flagHiddenKeyboardByChatbar = false;
    }

    @Override
    public synchronized void onCusKeyboardClosed() {
        if (changeListMessageSizeListener != null) {
            changeListMessageSizeListener.onChangeListMessageSize(mScreenHeight - mChatbarHeight + mActionbarHeight);
        }
        isHidening = true;
    }

    @Override
    public boolean onTouch(View v, MotionEvent arg1) {
        if (v.getId() == R.id.et_msg) {
            stickerButton.setImageResource(R.drawable.ic_sticker);
            Utils.showSoftKeyboard(activity, etChat);
            etChat.postDelayed(new Runnable() {
                @Override
                public void run() {
                    keyboardWidget.open();
                }
            }, 300);
        }
        return false;
    }

    public void handleSendAndRecordButtonView(boolean isSendButtonVisible) {
        sendMessageButton.setVisibility(isSendButtonVisible ? View.VISIBLE : View.GONE);
        attachButton.setVisibility(isSendButtonVisible ? View.GONE : View.VISIBLE);
        recordButton.setVisibility(isSendButtonVisible ? View.GONE : View.VISIBLE);
    }

    public boolean isStickerShow() {
        return isStickerShow;
    }

    public void closeSticker() {
        if (keyboardWidget != null && keyboardWidget.isOpened()) {
            keyboardWidget.animateClose();
            stickerButton.setImageResource(R.drawable.ic_sticker);
            isStickerShow = false;
        }
    }
}
