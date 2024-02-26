package com.stringee.chat.ui.kit.fragment;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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

public class CreateGroupFragment extends DialogFragment {

    private EditText nameEditText;
    private EditText userIdsEditText;
    private BaseActivity activity;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        activity = (BaseActivity) getActivity();
        View view = activity.getLayoutInflater().inflate(R.layout.fragment_create_group, null);
        nameEditText = view.findViewById(R.id.et_name);
        userIdsEditText = view.findViewById(R.id.et_user_id);
        return new Builder(activity).setTitle(R.string.create_group)
                .setPositiveButton(R.string.create, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialogInterface, int pos) {
                        final String groupName = nameEditText.getText().toString().trim();
                        String userIds = userIdsEditText.getText().toString().toLowerCase().trim();
                        if (userIds.length() == 0) {
                            Toast.makeText(activity, R.string.empty_user_id_info, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String[] userIdsList = userIds.split(",");
                        List<User> participants = new ArrayList<>();
                        for (String s : userIdsList) {
                            if (!s.trim().equals(Common.client.getUserId())) {
                                User user = new User(s);
                                participants.add(user);
                            }
                        }

                        if (participants.size() <= 1) {
                            Toast.makeText(activity, R.string.create_conversation_fail, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        activity.showProgress(getString(R.string.loading));
                        ConversationOptions options = new ConversationOptions();
                        options.setName(groupName);
                        options.setDistinct(false);
                        options.setGroup(true);
                        Common.client.createConversation(participants, options, new CallbackListener<Conversation>() {
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
                }).setView(view).create();
    }
}
