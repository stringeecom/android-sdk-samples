package com.stringee.chat.ui.kit.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;

import androidx.appcompat.app.AlertDialog.Builder;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView.Adapter;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.stringee.chat.ui.kit.activity.ConversationActivity;
import com.stringee.chat.ui.kit.commons.utils.AlphaNumberColorUtil;
import com.stringee.listener.StatusListener;
import com.stringee.messaging.Conversation;
import com.stringee.messaging.Message;
import com.stringee.messaging.User;
import com.stringee.messaging.listeners.CallbackListener;
import com.stringee.stringeechatuikit.R;
import com.stringee.stringeechatuikit.common.Common;
import com.stringee.stringeechatuikit.common.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ConversationAdapter extends Adapter {

    private LayoutInflater mInflater;
    private List<Conversation> conversationList;
    private Context context;
    private Map<String, Conversation> selectedMap = new HashMap<>();

    public ConversationAdapter(Context context, List<Conversation> conversations) {
        this.context = context;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        conversationList = conversations;
    }

    @androidx.annotation.NonNull
    @Override
    public androidx.recyclerview.widget.RecyclerView.ViewHolder onCreateViewHolder(@androidx.annotation.NonNull ViewGroup parent, int viewType) {
        View v = mInflater.inflate(R.layout.conversation_row, parent, false);
        return new ConversationViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@androidx.annotation.NonNull androidx.recyclerview.widget.RecyclerView.ViewHolder holder, int position) {
        ConversationViewHolder viewHolder = (ConversationViewHolder) holder;
        final Conversation conversation = conversationList.get(position);
        String text = conversation.getText();
        switch (conversation.getLastMsgType()) {
            case TEXT:
                viewHolder.subTitleTextView.setTextColor(ContextCompat.getColor(context, R.color.stringee_conv_time));
                text = conversation.getText();
                break;
            case CREATE_CONVERSATION:
                viewHolder.subTitleTextView.setTextColor(ContextCompat.getColor(context, R.color.stringee_conv_time));
                if (conversation.isGroup()) {
                    text = context.getString(R.string.create_conversation, Utils.getCreator(conversation));
                } else {
                    text = context.getString(R.string.create_chat, Utils.getCreator(conversation));
                }
                break;
            case LOCATION:
                text = context.getString(R.string.location);
                viewHolder.subTitleTextView.setTextColor(ContextCompat.getColor(context, R.color.stringee_colorPrimary));
                break;
            case AUDIO:
                text = context.getString(R.string.audio);
                viewHolder.subTitleTextView.setTextColor(ContextCompat.getColor(context, R.color.stringee_colorPrimary));
                break;
            case FILE:
                text = context.getString(R.string.file);
                viewHolder.subTitleTextView.setTextColor(ContextCompat.getColor(context, R.color.stringee_colorPrimary));
                break;
            case PHOTO:
                text = context.getString(R.string.photo);
                viewHolder.subTitleTextView.setTextColor(ContextCompat.getColor(context, R.color.stringee_colorPrimary));
                break;
            case VIDEO:
                text = context.getString(R.string.video);
                viewHolder.subTitleTextView.setTextColor(ContextCompat.getColor(context, R.color.stringee_colorPrimary));
                break;
            case CONTACT:
                text = context.getString(R.string.contact);
                viewHolder.subTitleTextView.setTextColor(ContextCompat.getColor(context, R.color.stringee_colorPrimary));
                break;
            case STICKER:
                text = context.getString(R.string.sticker);
                viewHolder.subTitleTextView.setTextColor(ContextCompat.getColor(context, R.color.stringee_colorPrimary));
                break;
            case NOTIFICATION:
                text = Utils.getNotificationText(context, conversation, conversation.getText());
        }
        String datetime = Utils.getFormattedDateAndTime(conversation.getUpdateAt());

        viewHolder.alphabeticTextView.setVisibility(View.VISIBLE);
        viewHolder.avatarImageView.setVisibility(View.GONE);
        int pos = position % 10;
        GradientDrawable bgShape = (GradientDrawable) viewHolder.alphabeticTextView.getBackground();
        bgShape.setColor(context.getResources().getColor(AlphaNumberColorUtil.alphabetBackgroundColorMap.get(String.valueOf(pos))));
        String convName = Utils.getConversationName(context, conversation);
        String[] chars = convName.trim().split(" ");
        String avaText = "";
        if (chars.length > 1) {
            avaText = String.valueOf(chars[0].charAt(0)) + String.valueOf(chars[1].charAt(0));
        } else if (chars.length > 0) {
            avaText = String.valueOf(chars[0].charAt(0));
        }
        viewHolder.alphabeticTextView.setText(avaText.toUpperCase());

        viewHolder.titleTextView.setText(convName);
        viewHolder.subTitleTextView.setText(text);
        viewHolder.timeTextView.setText(datetime);

        int totalUnread = conversation.getTotalUnread();
        if (totalUnread > 0) {
            viewHolder.unReadTextView.setVisibility(View.VISIBLE);
            viewHolder.unReadTextView.setText(String.valueOf(totalUnread));
        } else {
            viewHolder.unReadTextView.setVisibility(View.GONE);
        }

        Conversation selectedConv = selectedMap.get(conversation.getId());
        if (selectedConv != null) {
            viewHolder.vSelect.setVisibility(View.VISIBLE);
        } else {
            viewHolder.vSelect.setVisibility(View.GONE);
        }
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public int getItemCount() {
        return conversationList.size();
    }

    class ConversationViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder implements View.OnClickListener {
        RelativeLayout rootView;
        TextView titleTextView, subTitleTextView, timeTextView, alphabeticTextView, unReadTextView;
        CircleImageView avatarImageView;
        View vSelect;

        public ConversationViewHolder(View view) {
            super(view);

            rootView = view.findViewById(R.id.rootView);
            avatarImageView = (CircleImageView) view.findViewById(R.id.avatarImage);
            titleTextView = (TextView) view.findViewById(R.id.title);
            titleTextView.setTypeface(Common.boldType);
            subTitleTextView = (TextView) view.findViewById(R.id.subTitle);
            timeTextView = (TextView) view.findViewById(R.id.datetime);
            alphabeticTextView = (TextView) view.findViewById(R.id.alphabeticImage);
            unReadTextView = (TextView) view.findViewById(R.id.totalUnread);
            vSelect = view.findViewById(R.id.v_select);

            rootView.setOnClickListener(this);
            rootView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    int position = getLayoutPosition();
                    if (selectedMap.size() == 0) {
                        ((ConversationActivity) context).showSelectedMenu(1);
                        Conversation conversation = conversationList.get(position);
                        selectedMap.put(conversation.getId(), conversation);
                        notifyDataSetChanged();
                    }
                    return false;
                }
            });
        }

        @Override
        public void onClick(View view) {
            int position = this.getLayoutPosition();

            if (conversationList.size() <= position) {
                return;
            }

            Conversation conversation = conversationList.get(position);
            if (selectedMap.size() > 0) {
                if (selectedMap.get(conversation.getId()) != null) {
                    selectedMap.remove(conversation.getId());
                    if (selectedMap.size() == 0) {
                        // Hide menu
                        ((ConversationActivity) context).hideSelectedMenu();
                    }
                } else {
                    selectedMap.put(conversation.getId(), conversation);
                }
                ((ConversationActivity) context).showSelectedNo(selectedMap.size());
                notifyDataSetChanged();
            } else {
                Intent intent = new Intent(context, ConversationActivity.class);
                intent.putExtra("conversation", conversation);
                context.startActivity(intent);
            }
        }

        private final MenuItem.OnMenuItemClickListener onMenuItemClickListener = new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                final int position = getLayoutPosition();
                if (conversationList.size() <= position) {
                    return true;
                }
                final Conversation conversation = conversationList.get(position);
                switch (item.getItemId()) {
                    case 0:
                        Builder builder = new Builder(context);
                        builder.setMessage(R.string.confirm_delete_conversations);
                        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (conversation.isGroup()) {
                                    List<User> participants = new ArrayList<>();
                                    participants.add(new User(Common.client.getUserId()));
                                    conversation.removeParticipants(Common.client, participants, new CallbackListener<List<User>>() {
                                        @Override
                                        public void onSuccess(List<User> users) {
                                            conversation.delete(Common.client, new StatusListener() {
                                                @Override
                                                public void onSuccess() {
                                                    ((ConversationActivity) context).runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            conversationList.remove(position);
                                                            notifyDataSetChanged();
                                                        }
                                                    });
                                                }
                                            });
                                        }
                                    });
                                } else {
                                    conversation.delete(Common.client, new StatusListener() {
                                        @Override
                                        public void onSuccess() {
                                            ((ConversationActivity) context).runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    conversationList.remove(position);
                                                    notifyDataSetChanged();
                                                }
                                            });
                                        }
                                    });
                                }
                            }
                        });
                        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        });
                        androidx.appcompat.app.AlertDialog dialog = builder.create();
                        dialog.show();
                        break;
                    default:
                }
                return true;
            }
        };
    }

    public void clearSelected() {
        selectedMap.clear();
        notifyDataSetChanged();
    }

    public Map<String, Conversation> getSelectedConversations() {
        return selectedMap;
    }

    public void onRemoveConversation(Conversation conversation) {
        selectedMap.remove(conversation.getId());
        for (int i = 0; i < conversationList.size(); i++) {
            Conversation conv = conversationList.get(i);
            if (conv.getId().equals(conversation.getId())) {
                conversationList.remove(i);
                break;
            }
        }
        notifyDataSetChanged();
    }
}
