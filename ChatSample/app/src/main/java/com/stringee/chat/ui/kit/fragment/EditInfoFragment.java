package com.stringee.chat.ui.kit.fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.textfield.TextInputEditText;
import com.stringee.chat.ui.kit.activity.LiveChatActivity;
import com.stringee.exception.StringeeError;
import com.stringee.listener.StatusListener;
import com.stringee.messaging.User;
import com.stringee.stringeechatuikit.BaseActivity;
import com.stringee.stringeechatuikit.R.id;
import com.stringee.stringeechatuikit.R.layout;
import com.stringee.stringeechatuikit.R.string;
import com.stringee.stringeechatuikit.common.Common;
import com.stringee.stringeechatuikit.common.Utils;

public class EditInfoFragment extends DialogFragment {

    private TextInputEditText etName;
    private TextInputEditText etPhone;
    private TextInputEditText etEmail;
    private BaseActivity activity;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        activity = (BaseActivity) requireActivity();
        View view = LayoutInflater.from(activity).inflate(layout.fragment_edit_info, null);
        etName = view.findViewById(id.et_name);
        etPhone = view.findViewById(id.et_phone);
        etEmail = view.findViewById(id.et_email);
        etName.setText(LiveChatActivity.name);
        etPhone.setText(LiveChatActivity.phone);
        etEmail.setText(LiveChatActivity.email);

        return new Builder(activity)
                .setTitle(string.edit_info)
                .setPositiveButton(string.submit, (dialogInterface, pos) -> {
                    if (getActivity() == null) {
                        return;
                    }
                    if (Utils.isStringEmpty(etName.getText())) {
                        Utils.reportMessage(activity, "Name cannot empty");
                        return;
                    }
                    User user = new User();
                    user.setName(etName.getText().toString().trim());
                    if (!Utils.isStringEmpty(etPhone.getText())) {
                        user.setPhone(etPhone.getText().toString().trim());
                    }
                    if (!Utils.isStringEmpty(etEmail.getText())) {
                        user.setEmail(etEmail.getText().toString().trim());
                    }
                    Common.client.updateUser(user, new StatusListener() {
                        @Override
                        public void onSuccess() {
                            LiveChatActivity.name = etName.getText().toString().trim();
                            LiveChatActivity.phone = Utils.isStringEmpty(etPhone.getText()) ? "" : etPhone.getText().toString();
                            LiveChatActivity.email = Utils.isStringEmpty(etEmail.getText()) ? "" : etEmail.getText().toString();
                            Utils.reportMessage(activity, string.edit_info_success);
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
