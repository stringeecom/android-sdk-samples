package com.stringee.chat.ui.kit.fragment;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog.Builder;
import androidx.fragment.app.DialogFragment;

import com.stringee.chat.ui.kit.activity.ConversationActivity;
import com.stringee.exception.StringeeError;
import com.stringee.messaging.Conversation;
import com.stringee.messaging.ConversationOptions;
import com.stringee.messaging.User;
import com.stringee.messaging.listeners.CallbackListener;
import com.stringee.stringeechatuikit.BaseActivity;
import com.stringee.stringeechatuikit.R;
import com.stringee.stringeechatuikit.common.Common;
import com.stringee.stringeechatuikit.common.Utils;

import java.util.ArrayList;
import java.util.List;

public class ChatWithDialogFragment extends DialogFragment {

    private EditText inputEditText;
    private BaseActivity activity;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        activity = (BaseActivity) getActivity();
        inputEditText = new EditText(activity);
        inputEditText.setHint(R.string.enter_user_id_info);
        return new Builder(activity).setTitle(R.string.chat_with_user)
                .setPositiveButton(R.string.start, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialogInterface, int pos) {
                        final String editTextValue = inputEditText.getText().toString().toLowerCase().trim();
                        if (TextUtils.isEmpty(editTextValue) || inputEditText.getText().toString().trim().length() == 0) {
                            Toast.makeText(activity, R.string.empty_user_id_info, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        activity.showProgress(getString(R.string.loading));
                        String userId = editTextValue;
                        List<User> participants = new ArrayList<>();
                        User user = new User(userId);
                        participants.add(user);
                        ConversationOptions options = new ConversationOptions();
                        options.setDistinct(true);
                        options.setGroup(false);
                        Common.client.createConversation(participants, new CallbackListener<Conversation>() {
                            @Override
                            public void onSuccess(final Conversation conv) {
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        activity.dismissProgress();
                                        Intent intent = new Intent(activity, ConversationActivity.class);
                                        intent.putExtra("conversation", conv);
                                        activity.startActivity(intent);
                                        dialogInterface.dismiss();
                                    }
                                });
                            }

                            @Override
                            public void onError(final StringeeError error) {
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        activity.dismissProgress();
                                        Utils.reportMessage(activity, error.getMessage());
                                    }
                                });
                            }
                        });
                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                }).setView(inputEditText).create();
    }
}
