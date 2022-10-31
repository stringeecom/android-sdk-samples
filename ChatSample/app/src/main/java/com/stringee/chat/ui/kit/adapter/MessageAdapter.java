package com.stringee.chat.ui.kit.adapter;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView.Adapter;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;
import com.stringee.chat.ui.kit.activity.ConversationActivity;
import com.stringee.chat.ui.kit.activity.ImageFullScreenActivity;
import com.stringee.chat.ui.kit.commons.Constant;
import com.stringee.chat.ui.kit.commons.LocalImageLoader;
import com.stringee.chat.ui.kit.commons.MediaPlayerManager;
import com.stringee.chat.ui.kit.commons.utils.AlphaNumberColorUtil;
import com.stringee.chat.ui.kit.commons.utils.FileUtils;
import com.stringee.chat.ui.kit.commons.utils.FileUtils.FileType;
import com.stringee.chat.ui.kit.commons.utils.LocationUtils;
import com.stringee.chat.ui.kit.commons.utils.PermissionsUtils;
import com.stringee.chat.ui.kit.commons.utils.StringeePermissions;
import com.stringee.chat.ui.kit.contact.STContactData;
import com.stringee.chat.ui.kit.contact.STContactParser;
import com.stringee.listener.StatusListener;
import com.stringee.messaging.Conversation;
import com.stringee.messaging.Message;
import com.stringee.messaging.Message.MsgType;
import com.stringee.messaging.Message.Type;
import com.stringee.stringeechatuikit.R;
import com.stringee.stringeechatuikit.common.Common;
import com.stringee.stringeechatuikit.common.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageAdapter extends Adapter {

    private List<Message> messageList;
    private Context mContext;
    private Drawable sentIcon;
    private Drawable deliveredIcon;
    private Drawable pendingIcon;
    private Drawable readIcon;
    private Drawable sentIconDark;
    private Drawable deliveredIconDark;
    private Drawable pendingIconDark;
    private Drawable readIconDark;
    private View view;
    private Handler mHandler = new Handler();
    private int screenWidth;
    private int screenHeight;
    private Conversation conversation;
    private Map<String, Message> selectedMap = new HashMap<>();

    public MessageAdapter(Context context, List<Message> data, Conversation conversation) {
        mContext = context;
        messageList = data;
        this.conversation = conversation;

        sentIcon = context.getResources().getDrawable(R.drawable.stringee_ic_action_message_sent_w);
        deliveredIcon = context.getResources().getDrawable(R.drawable.stringee_ic_action_message_delivered_w);
        pendingIcon = context.getResources().getDrawable(R.drawable.stringee_ic_action_message_pending_w);
        readIcon = context.getResources().getDrawable(R.drawable.stringee_ic_action_message_read_w);

        sentIconDark = context.getResources().getDrawable(R.drawable.stringee_ic_action_message_sent);
        deliveredIconDark = context.getResources().getDrawable(R.drawable.stringee_ic_action_message_delivered);
        pendingIconDark = context.getResources().getDrawable(R.drawable.stringee_ic_action_message_pending);
        readIconDark = context.getResources().getDrawable(R.drawable.stringee_ic_action_message_read);

        Display dm = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        screenWidth = dm.getWidth();
        screenHeight = dm.getHeight();
    }

    @Override
    public androidx.recyclerview.widget.RecyclerView.ViewHolder onCreateViewHolder(@androidx.annotation.NonNull ViewGroup parent, int viewType) {
        LayoutInflater mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (mInflater == null) {
            return null;
        }

        if (viewType == 1) {
            View v1 = mInflater.inflate(R.layout.stringee_received_message_list_view, parent, false);
            return new MessageHolder(v1);
        } else if (viewType == 2) {
            View v2 = mInflater.inflate(R.layout.stringee_conversation_custom_message, parent, false);
            return new MessageHolder2(v2);
        } else if (viewType == 3) {
            View v3 = mInflater.inflate(R.layout.stringee_date_layout, parent, false);
            return new MessageHolder3(v3);
        }
        view = mInflater.inflate(R.layout.stringee_sent_message_list_view, parent, false);
        return new MessageHolder(view);
    }

    @SuppressLint("NewApi")
    @Override
    public void onBindViewHolder(@androidx.annotation.NonNull androidx.recyclerview.widget.RecyclerView.ViewHolder holder, int position) {
        int itemViewType = getItemViewType(position);
        final Message message = messageList.get(position);

        boolean isSelected = false;
        String id = message.getId();
        if (id == null) {
            id = message.getLocalId();
        }
        Message message1 = selectedMap.get(id);
        if (message1 != null) {
            isSelected = true;
        } else {
            isSelected = false;
        }

        if (itemViewType == 2) {
            MessageHolder2 messageHolder = (MessageHolder2) holder;
            String text = "";
            if (message.getType() == Message.Type.CREATE_CONVERSATION) {
                String creator = message.getSenderName();
                if (Utils.isStringEmpty(creator)) {
                    creator = message.getSenderId();
                }
                if (conversation.isGroup()) {
                    text = mContext.getString(R.string.create_conversation, creator);
                } else {
                    text = mContext.getString(R.string.create_chat, creator);
                }
            } else if (message.getType() == Message.Type.NOTIFICATION) {
                text = Utils.getNotificationText(mContext, conversation, message.getText());
            } else if (message.getType() == Type.RATING) {
                text = Utils.getRatingText(mContext, conversation, message);
            }

            messageHolder.customMessageTextView.setText(text);
            if (selectedMap.size() > 0) {
                messageHolder.vSelect.setVisibility(View.VISIBLE);
                if (isSelected) {
                    messageHolder.vSelect.setBackgroundResource(R.drawable.list_check_bg);
                    messageHolder.rootView.setBackgroundColor(Color.parseColor("#55b2cee3"));
                } else {
                    messageHolder.vSelect.setBackgroundResource(R.drawable.list_uncheck_bg);
                    messageHolder.rootView.setBackgroundColor(Color.TRANSPARENT);
                }
            } else {
                messageHolder.vSelect.setVisibility(View.GONE);
                messageHolder.rootView.setBackgroundColor(Color.TRANSPARENT);
            }
        } else if (itemViewType == 3) {
            MessageHolder3 messageHolder3 = (MessageHolder3) holder;
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd MMM");
            SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("dd MMM, yyyy");
            Date date = new Date(message.getCreatedAt());

            if (Utils.isSameYear(message.getCreatedAt())) {
                messageHolder3.timeTextView.setText(simpleDateFormat.format(date));
            } else {
                messageHolder3.timeTextView.setText(simpleDateFormat2.format(date));
            }
        } else {
            final MessageHolder messageHolder = (MessageHolder) holder;
            if (selectedMap.size() > 0) {
                messageHolder.vSelect.setVisibility(View.VISIBLE);
                if (isSelected) {
                    messageHolder.vSelect.setBackgroundResource(R.drawable.list_check_bg);
                    messageHolder.rootView.setBackgroundColor(Color.parseColor("#55b2cee3"));
                } else {
                    messageHolder.vSelect.setBackgroundResource(R.drawable.list_uncheck_bg);
                    messageHolder.rootView.setBackgroundColor(Color.TRANSPARENT);
                }
            } else {
                messageHolder.vSelect.setVisibility(View.GONE);
                messageHolder.rootView.setBackgroundColor(Color.TRANSPARENT);
            }
            messageHolder.timeTextView.setText(Utils.getFormattedDate(message.getCreatedAt()));
            String text = message.getText();
            Type type = message.getType();
            MsgType msgType = message.getMsgType();

            switch (type) {
                case TEXT:
                    messageHolder.messageTextView.setVisibility(View.VISIBLE);
                    messageHolder.chatLocationLayout.setVisibility(View.GONE);
                    messageHolder.mainContactShareLayout.setVisibility(View.GONE);
                    messageHolder.attachmentPreviewLayout.setVisibility(View.GONE);
                    messageHolder.attachmentAudioLayout.setVisibility(View.GONE);
                    messageHolder.messageTextView.setText(text);
                    if (messageHolder.messageLayout != null) {
                        messageHolder.messageLayout.setVisibility(View.VISIBLE);
                    }
                    break;
                case LOCATION:
                    messageHolder.messageTextView.setVisibility(View.GONE);
                    if (conversation.isGroup()) {
                        messageHolder.chatLocationLayout.setLayoutParams(new RelativeLayout.LayoutParams((int) (0.6 * screenWidth), (int) (0.25 * screenHeight)));
                    } else {
                        messageHolder.chatLocationLayout.setLayoutParams(new RelativeLayout.LayoutParams((int) (0.7 * screenWidth), (int) (0.25 * screenHeight)));
                    }
                    messageHolder.chatLocationLayout.setVisibility(View.VISIBLE);
                    messageHolder.mainContactShareLayout.setVisibility(View.GONE);
                    messageHolder.attachmentPreviewLayout.setVisibility(View.GONE);
                    messageHolder.attachmentAudioLayout.setVisibility(View.GONE);
                    if (messageHolder.messageLayout != null) {
                        messageHolder.messageLayout.setVisibility(View.GONE);
                    }
                    messageHolder.locationTimeTextView.setText(Utils.getFormattedDate(message.getCreatedAt()));

                    //Default image while loading image.
                    messageHolder.mapImageView.setVisibility(View.VISIBLE);
                    DisplayImageOptions options = new DisplayImageOptions.Builder()
                            .showImageOnLoading(R.drawable.stringee_map_offline_thumbnail)
                            .showImageForEmptyUri(R.drawable.stringee_map_offline_thumbnail)
                            .showImageOnFail(R.drawable.stringee_map_offline_thumbnail)
                            .displayer(new RoundedBitmapDisplayer(Math.round(10 * (mContext.getResources().getDisplayMetrics().xdpi / DisplayMetrics.DENSITY_DEFAULT))))
                            .cacheInMemory(true)
                            .cacheOnDisk(true)
                            .build();
                    ImageLoader.getInstance().displayImage(LocationUtils.loadStaticMap(message, mContext), messageHolder.mapImageView, options);

                    messageHolder.chatLocationLayout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String uri = "geo:" + message.getLatitude() + "," + message.getLongitude();
                            Uri gmmIntentUri = Uri.parse(uri);
                            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                            mapIntent.setPackage("com.google.android.apps.maps");
                            ((androidx.appcompat.app.AppCompatActivity) mContext).startActivity(mapIntent);
                        }
                    });
                    break;
                case CONTACT:
                    messageHolder.messageTextView.setVisibility(View.GONE);
                    messageHolder.chatLocationLayout.setVisibility(View.GONE);
                    messageHolder.mainContactShareLayout.setVisibility(View.VISIBLE);
                    messageHolder.attachmentPreviewLayout.setVisibility(View.GONE);
                    messageHolder.attachmentAudioLayout.setVisibility(View.GONE);
                    if (messageHolder.messageLayout != null) {
                        messageHolder.messageLayout.setVisibility(View.GONE);
                    }
                    messageHolder.contactTimeTextView.setText(Utils.getFormattedDate(message.getCreatedAt()));

                    setupContactShareView(message, messageHolder);
                    break;
                case PHOTO:
                    messageHolder.messageTextView.setVisibility(View.GONE);
                    messageHolder.chatLocationLayout.setVisibility(View.GONE);
                    messageHolder.mainContactShareLayout.setVisibility(View.GONE);
                    messageHolder.attachmentPreviewLayout.setVisibility(View.VISIBLE);
                    messageHolder.attachmentAudioLayout.setVisibility(View.GONE);
                    messageHolder.videoIconImageView.setVisibility(View.GONE);
                    if (messageHolder.messageLayout != null) {
                        messageHolder.messageLayout.setVisibility(View.GONE);
                    }
                    messageHolder.previewTimeTextView.setText(Utils.getFormattedDate(message.getCreatedAt()));

                    float ratio = message.getImageRatio();
                    int width;
                    int height;
                    Display dm = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
                    if (ratio > 1) {
                        width = (int) (dm.getWidth() * 0.65);
                        height = (int) (width / ratio);
                    } else {
                        height = (int) (dm.getHeight() * 0.5);
                        width = (int) (ratio * height);
                    }

                    messageHolder.attachmentPreviewLayout.setLayoutParams(new RelativeLayout.LayoutParams(width, height));

                    messageHolder.previewImageView.setLayoutParams(new RelativeLayout.LayoutParams(width, height));
                    String filepath = message.getFilePath();
                    DisplayImageOptions photoOptions = new DisplayImageOptions.Builder()
                            .imageScaleType(ImageScaleType.EXACTLY)
                            .displayer(new RoundedBitmapDisplayer(Math.round(10 * (mContext.getResources().getDisplayMetrics().xdpi / DisplayMetrics.DENSITY_DEFAULT))))
                            .cacheInMemory(true)
                            .cacheOnDisk(true)
                            .build();
                    if (filepath != null && filepath.trim().length() > 0) {
                        LocalImageLoader.getInstance().displayImage("file://" + filepath, messageHolder.previewImageView, photoOptions);
                    } else {
                        ImageLoader.getInstance().displayImage(message.getFileUrl(), messageHolder.previewImageView, photoOptions);
                    }

                    messageHolder.previewImageView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(mContext, ImageFullScreenActivity.class);
                            intent.putExtra("message", message);
                            mContext.startActivity(intent);
                        }
                    });
                    break;

                case VIDEO:
                    messageHolder.messageTextView.setVisibility(View.GONE);
                    messageHolder.chatLocationLayout.setVisibility(View.GONE);
                    messageHolder.mainContactShareLayout.setVisibility(View.GONE);
                    messageHolder.attachmentPreviewLayout.setVisibility(View.VISIBLE);
                    messageHolder.attachmentAudioLayout.setVisibility(View.GONE);
                    messageHolder.videoIconImageView.setVisibility(View.VISIBLE);
                    if (messageHolder.messageLayout != null) {
                        messageHolder.messageLayout.setVisibility(View.GONE);
                    }
                    messageHolder.previewTimeTextView.setText(Utils.getFormattedDate(message.getCreatedAt()));
                    float ratio1 = message.getImageRatio();
                    int width1;
                    int height1;
                    Display dm1 = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
                    if (ratio1 > 1) {
                        width1 = (int) (dm1.getWidth() * 0.65);
                        height1 = (int) (width1 / ratio1);
                    } else {
                        height1 = (int) (dm1.getHeight() * 0.5);
                        width1 = (int) (ratio1 * height1);
                    }

                    messageHolder.attachmentPreviewLayout.setLayoutParams(new RelativeLayout.LayoutParams(width1, height1));

                    messageHolder.previewImageView.setLayoutParams(new RelativeLayout.LayoutParams(width1, height1));
                    String thumbnail = message.getThumbnail();
                    DisplayImageOptions videoOptions = new DisplayImageOptions.Builder()
                            .imageScaleType(ImageScaleType.EXACTLY)
                            .displayer(new RoundedBitmapDisplayer(Math.round(10 * (mContext.getResources().getDisplayMetrics().xdpi / DisplayMetrics.DENSITY_DEFAULT))))
                            .cacheInMemory(true)
                            .cacheOnDisk(true)
                            .build();
                    if (thumbnail != null && thumbnail.trim().length() > 0) {
                        LocalImageLoader.getInstance().displayImage("file://" + thumbnail, messageHolder.previewImageView, videoOptions);
                    } else {
                        ImageLoader.getInstance().displayImage(message.getThumbnailUrl(), messageHolder.previewImageView, videoOptions);
                    }
                    final String videoFilePath = message.getFilePath();
                    if (videoFilePath != null && videoFilePath.trim().length() > 0) {
                        messageHolder.videoIconImageView.setImageResource(R.drawable.play_big);
                    } else {
                        messageHolder.videoIconImageView.setImageResource(R.drawable.load_big);
                    }
                    messageHolder.videoIconImageView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (videoFilePath != null && videoFilePath.trim().length() > 0) {
                                if (Build.VERSION.SDK_INT >= 24) {
                                    File videoFile = new File(videoFilePath);
                                    Uri fileUri = FileProvider.getUriForFile(mContext, mContext.getPackageName() + ".provider", videoFile);
                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    intent.setDataAndType(fileUri, "*/*");
                                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    mContext.startActivity(intent);
                                } else {
                                    Uri uri = Uri.parse(videoFilePath);
                                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                    intent.setDataAndType(uri, "*/*");
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    mContext.startActivity(intent);
                                }
                            } else {
                                if (Utils.hasMarshmallow() && PermissionsUtils.checkSelfForStoragePermission((ConversationActivity) mContext)) {
                                    new StringeePermissions((ConversationActivity) mContext).requestStoragePermissions();
                                } else {
                                    messageHolder.progressBar.setVisibility(View.VISIBLE);
                                    messageHolder.videoIconImageView.setVisibility(View.GONE);
                                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                                    String filename = "video_" + timeStamp + ".mp4";
                                    File destFile = FileUtils.getFilePath(filename, mContext, FileType.VIDEO);
                                    final String dest = destFile.getAbsolutePath();
                                    Utils.downloadAttachment(message.getFileUrl(), dest, new StatusListener() {
                                        @Override
                                        public void onSuccess() {
                                            messageHolder.videoIconImageView.setVisibility(View.VISIBLE);
                                            messageHolder.videoIconImageView.setImageResource(R.drawable.play_big);
                                            messageHolder.progressBar.setVisibility(View.GONE);
                                            message.setFilePath(dest);
                                            Common.client.updateAttachment(message);
                                        }
                                    });
                                }
                            }
                        }
                    });
                    break;
                case AUDIO:
                    messageHolder.messageTextView.setVisibility(View.GONE);
                    messageHolder.chatLocationLayout.setVisibility(View.GONE);
                    messageHolder.mainContactShareLayout.setVisibility(View.GONE);
                    messageHolder.attachmentPreviewLayout.setVisibility(View.GONE);
                    messageHolder.attachmentAudioLayout.setVisibility(View.VISIBLE);
                    messageHolder.attachmentAudioLayout.setLayoutParams(new RelativeLayout.LayoutParams((int) (0.6 * screenWidth), ViewGroup.LayoutParams.WRAP_CONTENT));
                    if (messageHolder.messageLayout != null) {
                        messageHolder.messageLayout.setVisibility(View.GONE);
                    }
                    messageHolder.audioSeekBar.setVisibility(View.VISIBLE);
                    messageHolder.audioTimeTextView.setVisibility(View.VISIBLE);
                    if (msgType == Message.MsgType.SEND) {
                        messageHolder.audioSeekBar.getProgressDrawable().setColorFilter(ContextCompat.getColor(mContext, R.color.stringee_time_sent), PorterDuff.Mode.MULTIPLY);
                        messageHolder.audioSeekBar.getThumb().setTint(ContextCompat.getColor(mContext, R.color.stringee_time_sent));
//                        messageHolder.audioTimeTextView.setTextColor(ContextCompat.getColor(mContext, R.color.stringee_time_sent));
                    } else {
                        messageHolder.audioSeekBar.getProgressDrawable().setColorFilter(ContextCompat.getColor(mContext, R.color.stringee_receive_text), PorterDuff.Mode.MULTIPLY);
                        messageHolder.audioSeekBar.getThumb().setTint(ContextCompat.getColor(mContext, R.color.stringee_receive_text));
//                        messageHolder.audioTimeTextView.setTextColor(ContextCompat.getColor(mContext, R.color.stringee_time_received));
                    }
                    messageHolder.audioTimeTextView.setText(Utils.getFormattedDate(message.getCreatedAt()));
                    messageHolder.playImageView.setImageResource(R.drawable.ic_play_circle_outline);
                    messageHolder.fileNameTextView.setVisibility(View.GONE);
                    messageHolder.fileTimeTextView.setVisibility(View.GONE);
                    messageHolder.durationTextView.setVisibility(View.VISIBLE);
                    messageHolder.durationTextView.setText(Utils.getAudioTime(message.getDuration()));
                    messageHolder.imAttachIcon.setVisibility(View.GONE);
                    String audioFilePath = message.getFilePath();
                    if (audioFilePath == null || audioFilePath.trim().length() == 0) {
                        messageHolder.downloadLayout.setVisibility(View.VISIBLE);
                        messageHolder.playImageView.setVisibility(View.GONE);
                    } else {
                        messageHolder.downloadLayout.setVisibility(View.GONE);
                        messageHolder.playImageView.setVisibility(View.VISIBLE);
                    }

                    if (msgType == Message.MsgType.SEND) {
                        messageHolder.durationTextView.setTextColor(ContextCompat.getColor(mContext, R.color.stringee_time_sent));
                        messageHolder.playImageView.setColorFilter(ContextCompat.getColor(mContext, R.color.stringee_time_sent));
                        messageHolder.downloadImageView.setColorFilter(ContextCompat.getColor(mContext, R.color.stringee_time_sent));
                    } else {
                        messageHolder.durationTextView.setTextColor(ContextCompat.getColor(mContext, R.color.stringee_time_received));
                        messageHolder.playImageView.setColorFilter(ContextCompat.getColor(mContext, R.color.stringee_receive_text));
                        messageHolder.downloadImageView.setColorFilter(ContextCompat.getColor(mContext, R.color.stringee_receive_text));
                    }

                    messageHolder.downloadLayout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (Utils.hasMarshmallow() && PermissionsUtils.checkSelfForStoragePermission((ConversationActivity) mContext)) {
                                new StringeePermissions((ConversationActivity) mContext).requestStoragePermissions();
                            } else {
                                messageHolder.audioProgressBar.setVisibility(View.VISIBLE);
                                messageHolder.downloadImageView.setVisibility(View.GONE);
                                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                                String filename = "audio_" + timeStamp + ".m4a";
                                File destFile = FileUtils.getFilePath(filename, mContext, FileType.AUDIO);
                                final String dest = destFile.getAbsolutePath();
                                Utils.downloadAttachment(message.getFileUrl(), dest, new StatusListener() {
                                    @Override
                                    public void onSuccess() {
                                        messageHolder.audioProgressBar.setVisibility(View.GONE);
                                        messageHolder.downloadLayout.setVisibility(View.GONE);
                                        messageHolder.playImageView.setVisibility(View.VISIBLE);
                                        message.setFilePath(dest);
                                        Common.client.updateAttachment(message);
                                    }
                                });
                            }
                        }
                    });

                    messageHolder.playImageView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            playAudio(message, messageHolder);
                        }
                    });
                    break;
                case FILE:
                    messageHolder.messageTextView.setVisibility(View.GONE);
                    messageHolder.chatLocationLayout.setVisibility(View.GONE);
                    messageHolder.mainContactShareLayout.setVisibility(View.GONE);
                    messageHolder.attachmentPreviewLayout.setVisibility(View.GONE);
                    messageHolder.attachmentAudioLayout.setVisibility(View.VISIBLE);
                    messageHolder.attachmentAudioLayout.setLayoutParams(new RelativeLayout.LayoutParams((int) (0.6 * screenWidth), RelativeLayout.LayoutParams.WRAP_CONTENT));
                    if (messageHolder.messageLayout != null) {
                        messageHolder.messageLayout.setVisibility(View.GONE);
                    }
                    messageHolder.audioSeekBar.setVisibility(View.GONE);
                    messageHolder.audioTimeTextView.setVisibility(View.GONE);
                    messageHolder.fileNameTextView.setVisibility(View.VISIBLE);
                    messageHolder.fileTimeTextView.setVisibility(View.VISIBLE);

                    messageHolder.fileTimeTextView.setText(Utils.getFormattedDate(message.getCreatedAt()));

                    if (msgType == Message.MsgType.SEND) {
                        messageHolder.downloadImageView.setColorFilter(ContextCompat.getColor(mContext, R.color.stringee_time_sent));
                    } else {
                        messageHolder.downloadImageView.setColorFilter(ContextCompat.getColor(mContext, R.color.stringee_receive_text));
                    }

                    String path = message.getFilePath();
                    if (path == null || path.trim().length() == 0) {
                        messageHolder.downloadLayout.setVisibility(View.VISIBLE);
                        messageHolder.playImageView.setVisibility(View.GONE);
                        messageHolder.imAttachIcon.setVisibility(View.GONE);
                    } else {
                        messageHolder.downloadLayout.setVisibility(View.GONE);
                        messageHolder.playImageView.setVisibility(View.VISIBLE);
                        messageHolder.imAttachIcon.setVisibility(View.VISIBLE);
                    }

                    messageHolder.durationTextView.setVisibility(View.GONE);
                    messageHolder.fileNameTextView.setVisibility(View.VISIBLE);

                    String fileName = message.getFileName();
                    messageHolder.fileNameTextView.setText(fileName);

                    messageHolder.downloadLayout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (Utils.hasMarshmallow() && PermissionsUtils.checkSelfForStoragePermission((ConversationActivity) mContext)) {
                                new StringeePermissions((ConversationActivity) mContext).requestStoragePermissions();
                            } else {
                                messageHolder.audioProgressBar.setVisibility(View.VISIBLE);
                                messageHolder.downloadImageView.setVisibility(View.GONE);
                                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                                String url = message.getFileUrl();
                                String[] paths = null;
                                if (url != null) {
                                    paths = url.split("/");
                                }
                                String fileName = "";
                                if (paths != null && paths.length > 0) {
                                    fileName = timeStamp + "_" + paths[paths.length - 1];
                                }
                                File destFile = FileUtils.getFilePath(fileName, mContext, FileType.OTHER);
                                final String dest = destFile.getAbsolutePath();
                                Utils.downloadAttachment(message.getFileUrl(), dest, new StatusListener() {
                                    @Override
                                    public void onSuccess() {
                                        messageHolder.audioProgressBar.setVisibility(View.GONE);
                                        messageHolder.downloadLayout.setVisibility(View.GONE);
                                        messageHolder.playImageView.setVisibility(View.VISIBLE);
                                        message.setFilePath(dest);
                                        Common.client.updateAttachment(message);
                                    }
                                });
                            }
                        }
                    });

                    messageHolder.playImageView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            openFile(mContext, message);
                        }
                    });

                    messageHolder.attachmentAudioLayout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            openFile(mContext, message);
                        }
                    });

                    break;

                case STICKER:
                    messageHolder.messageTextView.setVisibility(View.GONE);
                    messageHolder.chatLocationLayout.setVisibility(View.GONE);
                    messageHolder.mainContactShareLayout.setVisibility(View.GONE);
                    messageHolder.attachmentPreviewLayout.setVisibility(View.VISIBLE);
                    messageHolder.attachmentAudioLayout.setVisibility(View.GONE);
                    messageHolder.videoIconImageView.setVisibility(View.GONE);
                    if (messageHolder.messageLayout != null) {
                        messageHolder.messageLayout.setVisibility(View.GONE);
                    }
                    messageHolder.previewTimeTextView.setText(Utils.getFormattedDate(message.getCreatedAt()));

                    int width2;
                    int height2;
                    Display dm2 = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
                    width2 = (int) (dm2.getWidth() * 0.4);
                    height2 = (int) (width2 * 1.3);

                    messageHolder.attachmentPreviewLayout.setLayoutParams(new RelativeLayout.LayoutParams(width2, height2));
                    messageHolder.previewImageView.setLayoutParams(new RelativeLayout.LayoutParams(width2, height2));

                    String stickerPath = Constant.STICKER_BASE_URL + message.getStickerCategory() + "/" + message.getStickerName();
                    boolean displayFromLocal = false;
                    if (isStickerDownloaded(message.getStickerCategory())) {
                        if (Utils.hasMarshmallow()) {
                            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.READ_EXTERNAL_STORAGE)
                                    == PackageManager.PERMISSION_GRANTED) {
                                displayFromLocal = true;
                                stickerPath = "file:/" + FileUtils.getCacheDir(mContext, FileType.STICKER) + "/" + message.getStickerCategory() + "/" + message.getStickerName();
                            }
                        } else {
                            displayFromLocal = true;
                            stickerPath = "file:/" + FileUtils.getCacheDir(mContext, FileType.STICKER) + "/" + message.getStickerCategory() + "/" + message.getStickerName();
                        }
                    }
                    DisplayImageOptions stickerOptions = new DisplayImageOptions.Builder()
                            .cacheInMemory(true)
                            .cacheOnDisk(true)
                            .build();
                    if (displayFromLocal) {
                        LocalImageLoader.getInstance().displayImage(stickerPath, messageHolder.previewImageView, stickerOptions);
                    } else {
                        ImageLoader.getInstance().displayImage(stickerPath, messageHolder.previewImageView, stickerOptions);
                    }
                    break;
            }

            if (message.getMsgType() == Message.MsgType.SEND) {
                Drawable statusIcon;
                if (type == Message.Type.PHOTO || type == Message.Type.LOCATION || type == Message.Type.VIDEO || type == Message.Type.STICKER) {
                    statusIcon = pendingIcon;
                    if (message.getState() == Message.State.SENT) {
                        statusIcon = sentIcon;
                    } else if (message.getState() == Message.State.DELIVERED) {
                        statusIcon = deliveredIcon;
                    } else if (message.getState() == Message.State.READ) {
                        statusIcon = readIcon;
                    }
                } else {
                    statusIcon = pendingIconDark;
                    if (message.getState() == Message.State.SENT) {
                        statusIcon = sentIconDark;
                    } else if (message.getState() == Message.State.DELIVERED) {
                        statusIcon = deliveredIconDark;
                    } else if (message.getState() == Message.State.READ) {
                        statusIcon = readIconDark;
                    }
                }

                if (type == Message.Type.AUDIO) {
                    messageHolder.mediaLayout.setBackgroundResource(R.drawable.stringee_sent_message);
                    messageHolder.audioTimeTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, statusIcon, null);
                }
                if (type == Message.Type.FILE) {
                    messageHolder.mediaLayout.setBackgroundResource(R.drawable.stringee_sent_message);
                    messageHolder.fileTimeTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, statusIcon, null);
                }
                if (type == Message.Type.CONTACT) {
                    messageHolder.mediaLayout.setBackgroundResource(R.drawable.stringee_sent_message);
                    messageHolder.contactTimeTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, statusIcon, null);
                }
                if (type == Message.Type.LOCATION) {
                    messageHolder.mediaLayout.setBackgroundResource(R.drawable.stringee_sent_message);
                    messageHolder.locationTimeTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, statusIcon, null);
                }
                if (type == Message.Type.TEXT) {
                    messageHolder.timeTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, statusIcon, null);
                }
                if (type == Message.Type.PHOTO || type == Message.Type.VIDEO) {
                    messageHolder.mediaLayout.setBackgroundResource(R.drawable.stringee_sent_message);
                    messageHolder.previewTimeTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, statusIcon, null);
                }
                if (type == Message.Type.STICKER) {
                    messageHolder.mediaLayout.setBackgroundResource(R.color.stringee_transparent_color);
                    messageHolder.previewTimeTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, statusIcon, null);
                }
            } else {
                if (type == Message.Type.STICKER) {
                    messageHolder.mediaLayout.setBackgroundResource(R.color.stringee_transparent_color);
                } else {
                    messageHolder.mediaLayout.setBackgroundResource(R.drawable.stringee_received_message);
                }
                if (conversation.isGroup()) {
                    messageHolder.vAvatar.setVisibility(View.VISIBLE);
                    boolean isShowAva = true;
                    if (position < messageList.size() - 1) {
                        Message nextMessage = messageList.get(position + 1);
                        if (message.getSenderId().equals(nextMessage.getSenderId())) {
                            isShowAva = false;
                        } else {
                            isShowAva = true;
                        }
                    }
                    if (isShowAva) {
                        messageHolder.alphabeticTextView.setVisibility(View.VISIBLE);
                        displayImage(message, messageHolder.contactImageView, messageHolder.alphabeticTextView);
                    } else {
                        messageHolder.alphabeticTextView.setVisibility(View.GONE);
                    }
                } else {
                    messageHolder.vAvatar.setVisibility(View.GONE);
                }
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        Message message = getItem(position);
        Type type = message.getType();
        MsgType msgType = message.getMsgType();
        if (type == Message.Type.CREATE_CONVERSATION || type == Message.Type.NOTIFICATION || type == Type.RATING) {
            return 2;
        } else if (type == Message.Type.TEMP_DATE) {
            return 3;
        }
        if (msgType == Message.MsgType.SEND) {
            return 0;
        }
        if (msgType == Message.MsgType.RECEIVE) {
            return 1;
        }
        return 0;
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    private Message getItem(int position) {
        return messageList.get(position);
    }

    class MessageHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {

        TextView messageTextView, timeTextView, alphabeticTextView, shareContactName, shareContactNo, durationTextView, fileNameTextView, contactAlphabeticTextView, contactTimeTextView, audioTimeTextView, fileTimeTextView, locationTimeTextView, previewTimeTextView;
        ImageView contactImageView, mapImageView, shareContactImage, previewImageView, playImageView, videoIconImageView, downloadImageView, imAttachIcon, checkImageView;
        Button addContactButton;
        LinearLayout mainContactShareLayout;
        RelativeLayout chatLocationLayout;
        RelativeLayout attachmentPreviewLayout, attachmentAudioLayout, downloadLayout, mediaLayout;
        ProgressBar progressBar, audioProgressBar;
        SeekBar audioSeekBar;
        LinearLayout messageLayout;
        FrameLayout vAvatar;
        View rootView;
        View vSelect;

        public MessageHolder(View itemView) {
            super(itemView);

            rootView = itemView.findViewById(R.id.rootView);
            messageLayout = itemView.findViewById(R.id.messageLayout);
            messageTextView = itemView.findViewById(R.id.message);
            messageTextView.setMaxWidth((int) (0.65 * screenWidth));
            timeTextView = itemView.findViewById(R.id.createdAtTime);
            alphabeticTextView = itemView.findViewById(R.id.alphabeticImage);
            contactImageView = itemView.findViewById(R.id.contactImage);
            vAvatar = itemView.findViewById(R.id.v_avatar_received);
            mapImageView = itemView.findViewById(R.id.static_mapview);
            mainContactShareLayout = itemView.findViewById(R.id.contact_share_layout);
            chatLocationLayout = itemView.findViewById(R.id.chat_location);
            attachmentPreviewLayout = itemView.findViewById(R.id.attachment_preview_layout);
            attachmentAudioLayout = itemView.findViewById(R.id.attach_audio_layout);
            mediaLayout = itemView.findViewById(R.id.mediaLayout);
            vSelect = itemView.findViewById(R.id.v_select);
            checkImageView = itemView.findViewById(R.id.checkImageView);


            contactAlphabeticTextView = mainContactShareLayout.findViewById(R.id.tv_contact_alphabetic);
            shareContactImage = mainContactShareLayout.findViewById(R.id.contact_share_image);
            shareContactName = mainContactShareLayout.findViewById(R.id.tv_contact_name);
            shareContactNo = mainContactShareLayout.findViewById(R.id.tv_contact_phone);
            addContactButton = mainContactShareLayout.findViewById(R.id.contact_share_add_btn);
            contactTimeTextView = mainContactShareLayout.findViewById(R.id.contactTime);

            previewImageView = attachmentPreviewLayout.findViewById(R.id.preview);
            videoIconImageView = attachmentPreviewLayout.findViewById(R.id.video_icon);
            progressBar = attachmentPreviewLayout.findViewById(R.id.progressBar);
            previewTimeTextView = attachmentPreviewLayout.findViewById(R.id.previewTime);

            playImageView = attachmentAudioLayout.findViewById(R.id.playImageView);
            durationTextView = attachmentAudioLayout.findViewById(R.id.durationTextView);
            audioSeekBar = attachmentAudioLayout.findViewById(R.id.audioSeekbar);
            downloadLayout = attachmentAudioLayout.findViewById(R.id.downloadLayout);
            audioProgressBar = attachmentAudioLayout.findViewById(R.id.audioProgressBar);
            downloadImageView = attachmentAudioLayout.findViewById(R.id.downloadImageView);
            audioTimeTextView = attachmentAudioLayout.findViewById(R.id.audioTime);
            fileNameTextView = attachmentAudioLayout.findViewById(R.id.fileNameTextView);
            fileTimeTextView = attachmentAudioLayout.findViewById(R.id.fileTime);
            imAttachIcon = attachmentAudioLayout.findViewById(R.id.im_attach_icon);

            locationTimeTextView = chatLocationLayout.findViewById(R.id.locationTime);

            rootView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClick(MessageHolder.this);
                }
            });

            rootView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    onItemLongClick(MessageHolder.this);
                    return false;
                }
            });
        }
    }

    class MessageHolder2 extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        TextView customMessageTextView;
        View rootView, vSelect;
        ImageView checkImageView;

        public MessageHolder2(View itemView) {
            super(itemView);
            customMessageTextView = itemView.findViewById(R.id.customMessage);
            rootView = itemView.findViewById(R.id.rootView);
            vSelect = itemView.findViewById(R.id.v_select);
            checkImageView = itemView.findViewById(R.id.checkImageView);

            rootView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClick(MessageHolder2.this);
                }
            });

            rootView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    onItemLongClick(MessageHolder2.this);
                    return false;
                }
            });
        }
    }

    class MessageHolder3 extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        TextView timeTextView;

        public MessageHolder3(View itemView) {
            super(itemView);
            timeTextView = itemView.findViewById(R.id.timeTextView);
        }
    }

    public void displayImage(Message message, ImageView contactImage, TextView alphabeticTextView) {
        if (alphabeticTextView != null) {
            alphabeticTextView.setVisibility(View.VISIBLE);
            String sender = message.getSenderId();
            char firstLetter = 0;
            firstLetter = sender.charAt(0);
            alphabeticTextView.setText(String.valueOf(firstLetter));

            GradientDrawable bgShape = (GradientDrawable) alphabeticTextView.getBackground();
            bgShape.setColor(mContext.getResources().getColor(AlphaNumberColorUtil.alphabetBackgroundColorMap.get("0")));
        }
    }

    private void setupContactShareView(final Message message, MessageHolder messageHolder) {
        messageHolder.mainContactShareLayout.setLayoutParams(new RelativeLayout.LayoutParams((int) (0.7 * screenWidth), RelativeLayout.LayoutParams.WRAP_CONTENT));
        STContactParser parser = new STContactParser();
        try {
            STContactData data = parser.parseCVFContactData(message.getContact());
            String name = data.getName();
            messageHolder.shareContactName.setText(name);

            if (data.getAvatar() != null) {
                messageHolder.shareContactImage.setVisibility(View.VISIBLE);
                messageHolder.shareContactImage.setImageBitmap(data.getAvatar());
            } else {
                messageHolder.shareContactImage.setVisibility(View.INVISIBLE);
                GradientDrawable bgShape = (GradientDrawable) messageHolder.contactAlphabeticTextView.getBackground();
                bgShape.setColor(mContext.getResources().getColor(AlphaNumberColorUtil.alphabetBackgroundColorMap.get(String.valueOf(message.getSequence() % 10))));
                if (name != null) {
                    String[] chars = data.getName().split(" ");
                    String avaText = "";
                    if (chars.length > 1) {
                        String s1 = chars[0];
                        if (s1.length() > 0) {
                            avaText += s1.charAt(0);
                        }
                        String s2 = chars[1];
                        if (s2.length() > 0) {
                            avaText += s2.charAt(0);
                        }
                    } else if (chars.length > 0) {
                        avaText = String.valueOf(chars[0].charAt(0));
                    }
                    messageHolder.contactAlphabeticTextView.setText(avaText.toUpperCase());
                }
            }
            if (!TextUtils.isEmpty(data.getPhone())) {
                messageHolder.shareContactNo.setVisibility(View.VISIBLE);
                messageHolder.shareContactNo.setText(data.getPhone());
            } else {
                messageHolder.shareContactNo.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        messageHolder.addContactButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showContact(message);
            }
        });

        messageHolder.mainContactShareLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showContact(message);
            }
        });
    }

    private void showContact(Message message) {
        if (Utils.hasMarshmallow() && PermissionsUtils.checkSelfForStoragePermission((ConversationActivity) mContext)) {
            new StringeePermissions((ConversationActivity) mContext).requestStoragePermissions();
        } else {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String imageFileName = "contact_" + timeStamp + ".vcf";
            File outputFile = FileUtils.getFilePath(imageFileName, mContext.getApplicationContext(), FileType.CONTACT);
            byte[] buf = message.getContact().trim().getBytes();
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(outputFile.getAbsoluteFile());
                fileOutputStream.write(buf);
                fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            Uri outputUri = null;
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if (Build.VERSION.SDK_INT >= 24) {
                outputUri = FileProvider.getUriForFile(mContext, mContext.getPackageName() + ".provider", outputFile);
            } else {
                outputUri = Uri.fromFile(outputFile);
            }
            if (intent.resolveActivity(mContext.getPackageManager()) != null) {
                intent.setDataAndType(outputUri, "text/x-vcard");
                mContext.startActivity(intent);
            }
        }
    }


    public void playAudio(Message message, MessageHolder messageHolder) {
        Uri uri;
        if (Build.VERSION.SDK_INT >= 24) {
            uri = FileProvider.getUriForFile(mContext, mContext.getPackageName() + ".provider", new File(message.getFilePath()));
        } else {
            uri = Uri.parse(message.getFilePath());
        }
        MediaPlayerManager.getInstance(mContext).play(uri, message, messageHolder.playImageView, messageHolder.audioSeekBar, messageHolder.durationTextView);
        String key = message.getLocalId();
        if (message.getMsgType() == Message.MsgType.RECEIVE) {
            key = message.getId();
        }
        int state = MediaPlayerManager.getInstance(mContext).getAudioState(key);
        messageHolder.playImageView.setVisibility(View.VISIBLE);
        if (state == 1) {
            messageHolder.playImageView.setImageResource(R.drawable.ic_pause_circle_outline);
        } else {
            messageHolder.playImageView.setImageResource(R.drawable.ic_play_circle_outline);
        }

        updateApplozicSeekBar(message, messageHolder);
    }

    private void updateApplozicSeekBar(final Message message, final MessageHolder messageHolder) {
        String key = message.getLocalId();
        if (message.getMsgType() == Message.MsgType.RECEIVE) {
            key = message.getId();
        }
        MediaPlayer mediaplayer = MediaPlayerManager.getInstance(mContext).getMediaPlayer(key);
        if (mediaplayer == null) {
            messageHolder.audioSeekBar.setProgress(0);
        } else if (mediaplayer.isPlaying()) {
            messageHolder.audioSeekBar.setMax(mediaplayer.getDuration());
            messageHolder.audioSeekBar.setProgress(mediaplayer.getCurrentPosition());
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    updateApplozicSeekBar(message, messageHolder);
                }
            };
            mHandler.postDelayed(runnable, 0);
        } else {
            messageHolder.audioSeekBar.setMax(mediaplayer.getDuration());
            messageHolder.audioSeekBar.setProgress(mediaplayer.getCurrentPosition());
        }
    }

    private void openFile(Context context, Message message) {
        String localPath = message.getFilePath();
        if (localPath != null && localPath.length() > 0) {
            File file = new File(localPath);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri;
            if (Build.VERSION.SDK_INT >= 24) {
                uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
            } else {
                uri = Uri.fromFile(file);
            }
            intent.setDataAndType(uri, "*/*");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Intent chooser = Intent.createChooser(intent, context.getString(R.string.open_with));
            context.startActivity(chooser);
        }
    }

    private boolean isStickerDownloaded(String category) {
        if (category == null) {
            return false;
        }
        for (int i = 0; i < Common.stickerDirectories.size(); i++) {
            if (category.equals(Common.stickerDirectories.get(i))) {
                return true;
            }
        }
        return false;
    }

    private void onItemLongClick(androidx.recyclerview.widget.RecyclerView.ViewHolder holder) {
        if (selectedMap.size() == 0) {
            ((ConversationActivity) mContext).showSelectedMenu(2);
            int position = holder.getLayoutPosition();
            Message message = messageList.get(position);
            String id = message.getId();
            if (id == null) {
                id = message.getLocalId();
            }
            selectedMap.put(id, message);
            notifyDataSetChanged();
        }
    }

    private void onItemClick(androidx.recyclerview.widget.RecyclerView.ViewHolder holder) {
        int position = holder.getLayoutPosition();
        Message message = messageList.get(position);
        String id = message.getId();
        if (id == null) {
            id = message.getLocalId();
        }
        if (selectedMap.size() > 0) {
            Message message1 = selectedMap.get(id);
            if (message1 != null) {
                selectedMap.remove(id);
                if (selectedMap.size() == 0) {
                    // Hide menu
                    ((ConversationActivity) mContext).hideSelectedMenu();
                }
            } else {
                selectedMap.put(id, message);
            }
            ((ConversationActivity) mContext).showSelectedNo(selectedMap.size());
            notifyDataSetChanged();
        }
    }

    public void clearSelected() {
        selectedMap.clear();
        notifyDataSetChanged();
    }


    public Map<String, Message> getSelectedMessages() {
        return selectedMap;
    }

    public void removeMessages(List<Message> messages) {
        for (int i = 0; i < messages.size(); i++) {
            Message message = messages.get(i);
            for (int j = messageList.size() - 1; j >= 0; j--) {
                Message message1 = messageList.get(j);
                if ((message.getId() != null && message1.getId() != null && message.getId().equals(message1.getId())) || (message.getLocalId() != null && message1.getLocalId() != null && message.getLocalId().equals(message1.getLocalId()))) {
                    messageList.remove(j);
                    break;
                }
            }
        }
        notifyDataSetChanged();
    }
}
