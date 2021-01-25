package com.stringee.chat.ui.kit.fragment;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog.Builder;
import androidx.fragment.app.DialogFragment;

import com.stringee.chat.ui.kit.activity.ConversationInfoActivity;
import com.stringee.exception.StringeeError;
import com.stringee.messaging.Conversation;
import com.stringee.messaging.User;
import com.stringee.messaging.listeners.CallbackListener;
import com.stringee.stringeechatuikit.BaseActivity;
import com.stringee.stringeechatuikit.R;
import com.stringee.stringeechatuikit.common.Common;
import com.stringee.stringeechatuikit.common.Utils;

import java.util.ArrayList;
import java.util.List;

public class AddParticipantsFragment extends DialogFragment {

    private EditText inputEditText;
    private BaseActivity activity;
    private Conversation conversation;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        conversation = (Conversation) getArguments().getSerializable("conversation");
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        activity = (BaseActivity) getActivity();
        inputEditText = new EditText(activity);
        return new Builder(activity).setTitle(R.string.add_participants).setMessage(R.string.enter_user_id_info)
                .setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialogInterface, int pos) {
                        final String editTextValue = inputEditText.getText().toString().trim();
                        if (TextUtils.isEmpty(editTextValue) || inputEditText.getText().toString().trim().length() == 0) {
                            Utils.reportMessage(activity, getString(R.string.empty_user_id_info));
                            return;
                        }

                        activity.showProgress(getString(R.string.loading));
                        String[] pars = editTextValue.split(",");
                        List<User> participants = new ArrayList<>();
                        for (int i = 0; i < pars.length; i++) {
                            User identity = new User(pars[i]);
                            participants.add(identity);
                        }
                        conversation.addParticipants(Common.client, participants, new CallbackListener<List<User>>() {
                            @Override
                            public void onSuccess(final List<User> users) {
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        activity.dismissProgress();
                                        ((ConversationInfoActivity) activity).updateParticipants(users);
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
