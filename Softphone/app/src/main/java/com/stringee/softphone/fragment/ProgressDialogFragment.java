package com.stringee.softphone.fragment;

import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.stringee.softphone.R;

public class ProgressDialogFragment extends DialogFragment {
    private static final String TAG = "ProgressDialogFragment";
    private TextView mTvtitle, mTvMessgae;
    protected String mStringTitle, mStringMessage;
    protected ProgressBar mProgress;

    public static ProgressDialogFragment newInstance(String title, String message) {
        ProgressDialogFragment f = new ProgressDialogFragment();
        f.mStringTitle = title;
        f.mStringMessage = message;
        f.setCancelable(true);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        View v = initControl(inflater, container);
        initEvent();
        return v;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        try {
            mProgress.setEnabled(false);
            mProgress.setVisibility(View.INVISIBLE);
        } catch (Exception e) {
            Log.e(TAG, "dismiss", e);
        }
        super.onDismiss(dialog);
    }

    /**
     * init controller
     *
     * @param: n/a
     * @return: n/a
     * @throws: n/a
     */
    private View initControl(LayoutInflater inflater, ViewGroup container) {
        if (mStringTitle != null) {
            return null;
        } else {
            View v = inflater.inflate(R.layout.progress_dialog, container, false);
            mProgress = (ProgressBar) v.findViewById(R.id.pr_loading);
            mTvMessgae = (TextView) v.findViewById(R.id.tv_message);
            return v;
        }
    }

    /**
     * init event for controller
     *
     * @param: n/a
     * @return: n/a
     * @throws: n/a
     */
    private void initEvent() {
        try {
            if (mTvtitle != null) {
                mTvtitle.setText(mStringTitle);
            }
            if (mTvMessgae != null) {
                mTvMessgae.setText(mStringMessage);
            }
            mProgress.setEnabled(true);
            mProgress.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            Log.e(TAG, "initEvent", e);
        }
    }
}
