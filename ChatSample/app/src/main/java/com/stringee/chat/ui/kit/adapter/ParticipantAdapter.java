package com.stringee.chat.ui.kit.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.GradientDrawable;
import android.util.DisplayMetrics;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog.Builder;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;
import com.stringee.chat.ui.kit.commons.utils.AlphaNumberColorUtil;
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

import de.hdodenhof.circleimageview.CircleImageView;

public class ParticipantAdapter extends Adapter {

    private LayoutInflater mInflater;
    private List<User> participants;
    private Context context;
    private Conversation conversation;

    public ParticipantAdapter(Context context, Conversation conversation, List<User> participants) {
        this.context = context;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.conversation = conversation;
        this.participants = participants;
    }

    @androidx.annotation.NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@androidx.annotation.NonNull ViewGroup parent, int viewType) {
        LayoutInflater mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (mInflater == null) {
            return null;
        }

        View v = mInflater.inflate(R.layout.participant_row, parent, false);
        return new ParticipantViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@androidx.annotation.NonNull RecyclerView.ViewHolder holder, int position) {
        ParticipantViewHolder viewHolder = (ParticipantViewHolder) holder;
        User user = participants.get(position);
        String name = user.getName();
        if (name == null || name.length() == 0) {
            name = user.getUserId();
        }
        char firstLetter = name.toUpperCase().charAt(0);
        GradientDrawable bgShape = (GradientDrawable) viewHolder.tvAlphabet.getBackground();
        int pos = position % 10;
        bgShape.setColor(context.getResources().getColor(AlphaNumberColorUtil.alphabetBackgroundColorMap.get(String.valueOf(pos))));
        viewHolder.tvTitle.setText(name);

        String avatar = user.getAvatarUrl();
        if (avatar != null && avatar.length() > 0) {
            viewHolder.imAvatar.setVisibility(View.VISIBLE);
            viewHolder.tvAlphabet.setVisibility(View.GONE);
            DisplayImageOptions options = new DisplayImageOptions.Builder()
                    .imageScaleType(ImageScaleType.EXACTLY)
                    .displayer(new RoundedBitmapDisplayer(Math.round(3 * (context.getResources().getDisplayMetrics().xdpi / DisplayMetrics.DENSITY_DEFAULT))))
                    .cacheInMemory(true)
                    .cacheOnDisk(true)
                    .build();
            ImageLoader.getInstance().displayImage(avatar, viewHolder.imAvatar, options, new com.nostra13.universalimageloader.core.listener.ImageLoadingListener() {
                @Override
                public void onLoadingStarted(String imageUri, android.view.View view) {

                }

                @Override
                public void onLoadingFailed(String imageUri, android.view.View view, com.nostra13.universalimageloader.core.assist.FailReason failReason) {

                }

                @Override
                public void onLoadingComplete(String imageUri, android.view.View view, android.graphics.Bitmap loadedImage) {
                    Utils.runOnUiThread(() -> {
                        viewHolder.imAvatar.setImageBitmap(loadedImage);
                    });
                }

                @Override
                public void onLoadingCancelled(String imageUri, android.view.View view) {

                }
            });
        } else {
            viewHolder.tvAlphabet.setVisibility(View.VISIBLE);
            viewHolder.imAvatar.setVisibility(View.GONE);
            viewHolder.tvAlphabet.setText(String.valueOf(firstLetter));
        }
    }

    @Override
    public int getItemCount() {
        return participants.size();
    }

    class ParticipantViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {
        RelativeLayout rootView;
        TextView tvTitle, tvAlphabet;
        CircleImageView imAvatar;

        public ParticipantViewHolder(View itemView) {
            super(itemView);

            rootView = itemView.findViewById(R.id.v_root);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvAlphabet = itemView.findViewById(R.id.tv_alphabet);
            imAvatar = itemView.findViewById(R.id.im_avatar);

            rootView.setOnCreateContextMenuListener(this);
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
            if (!conversation.isGroup()) {
                return;
            }
            final int position = getLayoutPosition();
            if (participants.size() <= position) {
                return;
            }
            User user = participants.get(position);
            if (user.getUserId().equals(Common.client.getUserId())) {
                return;
            }
            String name = user.getName();
            if (name == null || name.length() == 0) {
                name = user.getUserId();
            }
            menu.setHeaderTitle(name);
            String[] menuItems = context.getResources().getStringArray(R.array.participants_options_menu);
            for (int i = 0; i < menuItems.length; i++) {
                MenuItem item = menu.add(Menu.NONE, i, i, menuItems[i]);
                item.setOnMenuItemClickListener(onMenuItemClickListener);
            }
        }

        private final MenuItem.OnMenuItemClickListener onMenuItemClickListener = new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                final int position = getLayoutPosition();
                if (participants.size() <= position) {
                    return true;
                }
                final User user = participants.get(position);
                final List<User> users = new ArrayList<>();
                users.add(user);
                switch (item.getItemId()) {
                    case 0:
                        Builder builder = new Builder(context);
                        builder.setMessage(R.string.confirm_delete_participant);
                        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ((BaseActivity) context).showProgress(context.getString(R.string.loading));
                                conversation.removeParticipants(Common.client, users, new CallbackListener<List<User>>() {
                                    @Override
                                    public void onSuccess(List<User> addedUsers) {
                                        ((BaseActivity) context).runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                ((BaseActivity) context).dismissProgress();
                                                participants.remove(position);
                                                notifyDataSetChanged();
                                            }
                                        });
                                    }

                                    @Override
                                    public void onError(final StringeeError error) {
                                        ((BaseActivity) context).runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                ((BaseActivity) context).dismissProgress();
                                                Utils.reportMessage(context, error.getMessage());
                                            }
                                        });
                                    }
                                });
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
}
