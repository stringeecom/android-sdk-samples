package com.stringee.chat.ui.kit.activity;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.stringee.chat.ui.kit.adapter.ParticipantAdapter;
import com.stringee.chat.ui.kit.commons.Notify;
import com.stringee.chat.ui.kit.commons.utils.AlphaNumberColorUtil;
import com.stringee.chat.ui.kit.fragment.AddParticipantsFragment;
import com.stringee.messaging.Conversation;
import com.stringee.messaging.User;
import com.stringee.messaging.listeners.CallbackListener;
import com.stringee.stringeechatuikit.BaseActivity;
import com.stringee.stringeechatuikit.R;
import com.stringee.stringeechatuikit.common.Common;

import java.util.ArrayList;
import java.util.List;

public class ConversationInfoActivity extends BaseActivity {

    private Conversation mConversation;
    private ParticipantAdapter adapter;
    private List<User> participants;

    private ImageButton btnMore;
    private androidx.recyclerview.widget.RecyclerView participantListview;
    private PopupWindow popupWindow;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conv_info);

        mConversation = (Conversation) getIntent().getSerializableExtra("conversation");

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(this);

        btnMore = findViewById(R.id.btn_more);
        btnMore.setOnClickListener(this);

        String convName = mConversation.getName();
        if (convName == null || convName.length() == 0) {
            convName = "";
            List<User> participants = mConversation.getParticipants();
            for (int j = 0; j < participants.size(); j++) {
                String userId = participants.get(j).getUserId();
                if (!mConversation.isGroup() && Common.client.getUserId() != null && Common.client.getUserId().equals(userId)) {
                    continue;
                } else {
                    String name = participants.get(j).getName();
                    if (name == null || name.trim().length() == 0) {
                        name = participants.get(j).getUserId();
                    }
                    convName = convName + name + ",";
                }
            }
            if (convName.length() > 0) {
                convName = convName.substring(0, convName.length() - 1);
            }
        }

        TextView tvAvatar = findViewById(R.id.tv_avatar);
        char firstLetter = convName.toUpperCase().charAt(0);
        GradientDrawable bgShape = (GradientDrawable) tvAvatar.getBackground();
        bgShape.setColor(getResources().getColor(AlphaNumberColorUtil.alphabetBackgroundColorMap.get("0")));
        tvAvatar.setText(String.valueOf(firstLetter));

        TextView tvName = findViewById(R.id.tv_name);
        tvName.setText(convName);

        TextView tvMembers = findViewById(R.id.tv_members);
        tvMembers.setText(convName);
        tvMembers.setText(String.valueOf(mConversation.getParticipants().size()) + " " + getString(R.string.members));

        participantListview = findViewById(R.id.participantList);
        androidx.recyclerview.widget.LinearLayoutManager linearLayoutManager = new androidx.recyclerview.widget.LinearLayoutManager(this);
        participantListview.setLayoutManager(linearLayoutManager);
        participantListview.setHasFixedSize(true);
        participants = mConversation.getParticipants();
        adapter = new ParticipantAdapter(this, mConversation, participants);
        participantListview.setAdapter(adapter);

        if (!mConversation.isGroup()) {
            btnMore.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_more:
                if (popupWindow == null) {
                    LayoutInflater inflater = getLayoutInflater();
                    View popupView = inflater.inflate(R.layout.conv_info_popup, null);
                    TextView tvAddMembers = popupView.findViewById(R.id.tv_add_member);
                    tvAddMembers.setOnClickListener(this);
                    TextView tvLeave = popupView.findViewById(R.id.tv_leave);
                    tvLeave.setOnClickListener(this);
                    popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    popupWindow.setBackgroundDrawable(new ColorDrawable(Color.WHITE));
                    popupWindow.setOutsideTouchable(true);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        popupWindow.setElevation(20);
                    }
                }
                popupWindow.showAsDropDown(btnMore);
                break;
            case R.id.btn_back:
                finish();
                break;
            case R.id.tv_add_member:
                popupWindow.dismiss();
                androidx.fragment.app.FragmentManager supportFragmentManager = getSupportFragmentManager();
                AddParticipantsFragment fragment = new AddParticipantsFragment();
                Bundle args = new Bundle();
                args.putSerializable("conversation", mConversation);
                fragment.setArguments(args);
                androidx.fragment.app.FragmentTransaction fragmentTransaction = supportFragmentManager
                        .beginTransaction();
                androidx.fragment.app.Fragment prev = getSupportFragmentManager().findFragmentByTag("AddParticipantsDialogFragment");
                if (prev != null) {
                    fragmentTransaction.remove(prev);
                }
                fragmentTransaction.addToBackStack(null);
                fragment.show(fragmentTransaction, "AddParticipantsDialogFragment");
                break;
            case R.id.tv_leave:
                List<User> users = new ArrayList<>();
                users.add(new User(Common.client.getUserId()));

                mConversation.removeParticipants(Common.client, users, new CallbackListener<List<User>>() {
                    @Override
                    public void onSuccess(List<User> users) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(ConversationInfoActivity.this).sendBroadcast(new Intent(Notify.CONVERSATION_DELETED.getValue()));
                                popupWindow.dismiss();
                                finish();
                            }
                        });
                    }
                });
                break;
        }
    }

    public void updateParticipants(List<User> users) {
        participants.addAll(users);
        adapter.notifyDataSetChanged();
    }
}
