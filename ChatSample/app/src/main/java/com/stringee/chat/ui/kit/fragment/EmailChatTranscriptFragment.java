package com.stringee.chat.ui.kit.fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.core.util.PatternsCompat;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.textfield.TextInputEditText;
import com.stringee.chat.ui.kit.activity.LiveChatActivity;
import com.stringee.exception.StringeeError;
import com.stringee.listener.StatusListener;
import com.stringee.messaging.Conversation;
import com.stringee.stringeechatuikit.BaseActivity;
import com.stringee.stringeechatuikit.R.id;
import com.stringee.stringeechatuikit.R.layout;
import com.stringee.stringeechatuikit.R.string;
import com.stringee.stringeechatuikit.common.Common;
import com.stringee.stringeechatuikit.common.Utils;

public class EmailChatTranscriptFragment extends DialogFragment {

    private TextInputEditText etEmail;
    private TextInputEditText etDomain;
    private Conversation conversation;
    private BaseActivity activity;

    public EmailChatTranscriptFragment(Conversation conversation) {
        this.conversation = conversation;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        activity = (BaseActivity) requireActivity();
        View view = LayoutInflater.from(activity).inflate(layout.fragment_email_chat_transcript, null);
        etEmail = view.findViewById(id.et_email);
        etDomain = view.findViewById(id.et_domain);
        etEmail.setText(LiveChatActivity.email);
        etDomain.setText("https://stringeex.com");

        return new Builder(activity)
                .setTitle(string.email_chat_transcript)
                .setPositiveButton(string.submit, (dialogInterface, pos) -> {
                    if (Utils.isStringEmpty(etEmail.getText()) || !PatternsCompat.EMAIL_ADDRESS.matcher(etEmail.getText()).matches()) {
                        Utils.reportMessage(activity, "Email is empty or invalid");
                        return;
                    }
                    if (Utils.isStringEmpty(etDomain.getText()) || !PatternsCompat.WEB_URL.matcher(etDomain.getText()).matches()) {
                        Utils.reportMessage(activity, "Domain is empty or invalid");
                        return;
                    }
                    conversation.sendChatTranscriptTo(Common.client, etEmail.getText().toString().trim(), etDomain.getText().toString().trim(), new StatusListener() {
                        @Override
                        public void onSuccess() {
                            Utils.reportMessage(activity, string.email_chat_transcript_success);
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
