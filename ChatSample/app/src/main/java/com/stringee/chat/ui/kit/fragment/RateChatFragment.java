package com.stringee.chat.ui.kit.fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.textfield.TextInputEditText;
import com.stringee.exception.StringeeError;
import com.stringee.listener.StatusListener;
import com.stringee.messaging.Conversation;
import com.stringee.messaging.Conversation.Rating;
import com.stringee.stringeechatuikit.BaseActivity;
import com.stringee.stringeechatuikit.R.id;
import com.stringee.stringeechatuikit.R.layout;
import com.stringee.stringeechatuikit.R.string;
import com.stringee.stringeechatuikit.common.Common;
import com.stringee.stringeechatuikit.common.Utils;

public class RateChatFragment extends DialogFragment {

    private TextInputEditText etComment;
    private Rating rating = Rating.GOOD;
    private Conversation conversation;
    private BaseActivity activity;

    public RateChatFragment(Conversation conversation) {
        this.conversation = conversation;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        activity = (BaseActivity) requireActivity();
        View view = LayoutInflater.from(activity).inflate(layout.fragment_rate_chat, null);
        etComment = view.findViewById(id.et_comment);
        RadioGroup rg = view.findViewById(id.rg);
        rg.setOnCheckedChangeListener((radioGroup1, i) -> {
            int checkedId = radioGroup1.getCheckedRadioButtonId();
            if (checkedId == id.btn_good) {
                rating = Rating.GOOD;
            } else {
                rating = Rating.BAD;
            }
        });

        return new Builder(activity)
                .setTitle(string.rating_chat)
                .setPositiveButton(string.rate, (dialogInterface, pos) -> {
                    conversation.rateChat(Common.client, Utils.isStringEmpty(etComment.getText()) ? null : etComment.getText().toString().trim(), rating, new StatusListener() {
                        @Override
                        public void onSuccess() {
                            Utils.reportMessage(activity, string.rate_success);
                            dialogInterface.dismiss();
                        }

                        @Override
                        public void onError(StringeeError stringeeError) {
                            super.onError(stringeeError);
                            Utils.reportMessage(activity, stringeeError.getMessage());
                            dialogInterface.dismiss();
                        }
                    });
                })
                .setNegativeButton(string.cancel, (dialogInterface, i) -> dialogInterface.dismiss())
                .setView(view)
                .create();
    }
}
