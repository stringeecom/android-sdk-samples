package com.stringee.chat.ui.kit.fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.stringee.chat.ui.kit.activity.ConversationActivity;
import com.stringee.chat.ui.kit.activity.ConversationInfoActivity;
import com.stringee.chat.ui.kit.activity.StringeeLocationActivity;
import com.stringee.chat.ui.kit.adapter.MediaAdapter;
import com.stringee.chat.ui.kit.adapter.MessageAdapter;
import com.stringee.chat.ui.kit.adapter.StickerAdapter;
import com.stringee.chat.ui.kit.adapter.StickerCategoryAdapter;
import com.stringee.chat.ui.kit.adapter.StickerIconAdapter;
import com.stringee.chat.ui.kit.adapter.StringeeMultimediaPopupAdapter;
import com.stringee.chat.ui.kit.commons.Constant;
import com.stringee.chat.ui.kit.commons.utils.FileUtils;
import com.stringee.chat.ui.kit.commons.utils.PermissionsUtils;
import com.stringee.chat.ui.kit.listener.ChatUIListener;
import com.stringee.chat.ui.kit.listener.ICusKeyboard;
import com.stringee.chat.ui.kit.model.Contact;
import com.stringee.chat.ui.kit.model.DataItem;
import com.stringee.chat.ui.kit.model.Image;
import com.stringee.chat.ui.kit.model.Sticker;
import com.stringee.chat.ui.kit.model.StickerCategory;
import com.stringee.chat.ui.kit.model.Video;
import com.stringee.chat.ui.kit.notification.NotificationService;
import com.stringee.chat.ui.kit.view.CusKeyboardController;
import com.stringee.chat.ui.kit.view.CusKeyboardWidget;
import com.stringee.chat.ui.kit.view.CusRelativeLayout;
import com.stringee.exception.StringeeError;
import com.stringee.listener.StatusListener;
import com.stringee.messaging.Conversation;
import com.stringee.messaging.Conversation.ChannelType;
import com.stringee.messaging.Message;
import com.stringee.messaging.Message.MsgType;
import com.stringee.messaging.User;
import com.stringee.messaging.listeners.CallbackListener;
import com.stringee.stringeechatuikit.BaseActivity;
import com.stringee.stringeechatuikit.OutgoingCallActivity;
import com.stringee.stringeechatuikit.R;
import com.stringee.stringeechatuikit.R.id;
import com.stringee.stringeechatuikit.common.CallBack;
import com.stringee.stringeechatuikit.common.Common;
import com.stringee.stringeechatuikit.common.DataHandler;
import com.stringee.stringeechatuikit.common.PrefUtils;
import com.stringee.stringeechatuikit.common.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ChatFragment extends Fragment implements ChatUIListener, ICusKeyboard.ChangeListMessageSizeListener, CallBack {
    private Dialog dialog;
    private Conversation conversation;
    private EditText messageEditText;
    private ImageButton attachButton;
    private TextView tvNoMessage;
    private RecyclerView stickersRecyclerView;
    private GridView stickersGridView;
    private CusRelativeLayout rootView;
    private CusKeyboardWidget keyboardWidget;
    public CusKeyboardController keyboardController;
    private ListView stickersListView;

    private List<Message> messages = new ArrayList<>();
    private RecyclerView messagesRecyclerView;
    private MessageAdapter adapter;
    private LinearLayoutManager linearLayoutManager;
    private ProgressBar prLoading;
    private boolean isLoading;
    private boolean isTop = false;
    private boolean isTouched;
    private List<StickerCategory> stickerIcons = new ArrayList<>();
    private StickerIconAdapter stickerIconAdapter;
    private Map<String, List<Sticker>> stickerMap = new HashMap<String, List<Sticker>>();
    private StickerAdapter stickerAdapter;
    private boolean isScrolledDown = false;
    private List<StickerCategory> stickerCategories = new ArrayList<>();
    private StickerCategoryAdapter categoryAdapter;
    private int position;

    private final String ACTION_LOAD_STICKERS = "load_stickers";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (conversation != null) {
            outState.putSerializable("conversation", conversation);
        }

    }

    @Override
    public View onCreateView(@androidx.annotation.NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        prLoading = view.findViewById(id.prLoading);

        tvNoMessage = view.findViewById(id.tv_no_message);

        attachButton = view.findViewById(id.attachButton);
        messageEditText = view.findViewById(id.messageEditText);

        messagesRecyclerView = view.findViewById(id.messagesRecyclerView);
        linearLayoutManager = new LinearLayoutManager(getActivity());
        messagesRecyclerView.setLayoutManager(linearLayoutManager);
        messagesRecyclerView.setHasFixedSize(true);

        messagesRecyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                isTouched = true;
                return false;
            }
        });

        messagesRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (!isScrolledDown) {
                    int currentPosition = linearLayoutManager.findFirstVisibleItemPosition();
                    if (currentPosition < position) {
                        isScrolledDown = true;
                    }
                    position = currentPosition;
                }

                if (!recyclerView.canScrollVertically(-1) && !isLoading && !isTop && isTouched) {
                    final int lastItem = linearLayoutManager.findLastVisibleItemPosition();
                    isLoading = true;
                    prLoading.setVisibility(View.VISIBLE);
                    // Scroll to the top
                    long seq = 0;
                    for (int i = 0; i < messages.size(); i++) {
                        Message message = messages.get(i);
                        if (message.getType() == com.stringee.messaging.Message.Type.TEMP_DATE) {
                            continue;
                        } else {
                            seq = message.getSequence();
                            break;
                        }
                    }
                    conversation.getMessagesBefore(Common.client, seq, Constant.MESSAGES_COUNT, new CallbackListener<List<Message>>() {
                        @Override
                        public void onSuccess(final List<Message> lstMessages) {
                            if (getActivity() == null) {
                                return;
                            }
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (lstMessages.size() < Constant.MESSAGES_COUNT) {
                                        isTop = true;
                                    } else {
                                        isTop = false;
                                    }
                                    prLoading.setVisibility(View.GONE);
                                    if (lstMessages.size() > 0) {
                                        addTempDate(lstMessages, false);
                                        for (int i = 0; i < messages.size(); i++) {
                                            if (messages.get(0).getType() == Message.Type.TEMP_DATE) {
                                                messages.remove(0);
                                            } else {
                                                break;
                                            }
                                        }
                                        long dayDiff = Utils.daysBetween(new Date(lstMessages.get(lstMessages.size() - 1).getCreatedAt()), new Date(messages.get(0).getCreatedAt()));
                                        if (dayDiff >= 1) {
                                            Message tempMessage = new Message(Message.Type.TEMP_DATE);
                                            tempMessage.setCreatedAt(messages.get(0).getCreatedAt());
                                            lstMessages.add(tempMessage);
                                        }
                                        messages.addAll(0, lstMessages);
                                        adapter.notifyDataSetChanged();
                                        messagesRecyclerView.scrollToPosition(lstMessages.size() + lastItem - 1);
                                    }
                                    isLoading = false;
                                }
                            });
                        }
                    });
                }
            }
        });

        stickersRecyclerView = view.findViewById(id.stickersRecyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
        stickersRecyclerView.setLayoutManager(layoutManager);
        stickersGridView = view.findViewById(id.stickersGridView);
        stickersGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Sticker sticker = (Sticker) stickerAdapter.getItem(i);
                sendSticker(sticker.getCatId(), sticker.getName());
            }
        });

        stickersListView = view.findViewById(id.stickersListView);

        rootView = view.findViewById(id.rootView);
        keyboardWidget = view.findViewById(id.drawer);
        keyboardController = new CusKeyboardController((BaseActivity) getActivity(), rootView,
                keyboardWidget, this, this);


        loadStickers();

        Bundle args = getArguments();
        if (args != null) {
            conversation = (Conversation) args.getSerializable("conversation");
        } else if (savedInstanceState != null) {
            conversation = (Conversation) savedInstanceState.getSerializable("conversation");
        }
        if (conversation == null) {
            String convId = args.getString("convId");
            Common.client.getConversation(convId, new CallbackListener<Conversation>() {
                @Override
                public void onSuccess(Conversation conv) {
                    if (getActivity() == null) {
                        return;
                    }
                    conversation = conv;
                    Common.currentConvId = conversation.getId();
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setActionbarTitle();
                                getActivity().invalidateOptionsMenu();

                                adapter = new MessageAdapter(getActivity(), messages, conversation);
                                messagesRecyclerView.setAdapter(adapter);
                                getMessages();
                            }
                        });
                    }
                }

                @Override
                public void onError(StringeeError error) {
                    Log.e("Stringee", error.getMessage());
                }
            });
        } else {
            Common.currentConvId = conversation.getId();
            adapter = new MessageAdapter(getActivity(), messages, conversation);
            messagesRecyclerView.setAdapter(adapter);
            getMessages();
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Common.isChatting = true;

        if (conversation != null) {
            NotificationService.cancelNotification(getActivity());

            setActionbarTitle();

            if (messages.size() > 0) {
                Message lstMessage = messages.get(messages.size() - 1);
                if (lstMessage != null && lstMessage.getMsgType() == MsgType.RECEIVE && lstMessage.getState().getValue() < Message.State.READ.getValue()) {
                    lstMessage.markAsRead(Common.client, new StatusListener() {
                        @Override
                        public void onSuccess() {

                        }
                    });
                }
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (conversation.isGroup()) {
            menu.findItem(id.menu_voice_call).setVisible(false);
            menu.findItem(id.menu_video_call).setVisible(false);
        } else {
            menu.findItem(id.menu_voice_call).setVisible(true);
            menu.findItem(id.menu_video_call).setVisible(true);
        }
        menu.findItem(id.menu_info).setVisible(conversation.getChannelType() == ChannelType.NORMAL);
        menu.findItem(id.menu_end_chat).setVisible(conversation.getChannelType() != ChannelType.NORMAL);
        menu.findItem(id.menu_rate).setVisible(conversation.getChannelType() != ChannelType.NORMAL);
        menu.findItem(id.menu_email_chat_transcript).setVisible(conversation.getChannelType() != ChannelType.NORMAL);
        menu.findItem(id.menu_edit_info).setVisible(conversation.getChannelType() != ChannelType.NORMAL);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == id.menu_voice_call) {
            if (!conversation.isGroup()) {
                String callee = getCallee();
                if (callee.length() > 0) {
                    Intent intent = new Intent(getActivity(), OutgoingCallActivity.class);
                    intent.putExtra("from", PrefUtils.getString(com.stringee.stringeechatuikit.common.Constant.PREF_USER_ID, ""));
                    intent.putExtra("to", callee);
                    intent.putExtra("is_video_call", false);
                    startActivity(intent);
                }
            }
        } else if (itemId == id.menu_video_call) {
            if (!conversation.isGroup()) {
                String callee = getCallee();
                if (callee.length() > 0) {
                    Intent intent = new Intent(getActivity(), OutgoingCallActivity.class);
                    intent.putExtra("from", PrefUtils.getString(com.stringee.stringeechatuikit.common.Constant.PREF_USER_ID, ""));
                    intent.putExtra("to", callee);
                    intent.putExtra("is_video_call", true);
                    startActivity(intent);
                }
            }
        } else if (itemId == id.menu_info) {
            Intent intent = new Intent(getActivity(), ConversationInfoActivity.class);
            intent.putExtra("conversation", conversation);
            startActivity(intent);
        } else if (itemId == id.menu_end_chat) {
            endChat();
        } else if (itemId == id.menu_rate) {
            RateChatFragment fragment = new RateChatFragment(conversation);
            FragmentTransaction fragmentTransaction = getParentFragmentManager().beginTransaction();
            Fragment prev = getParentFragmentManager().findFragmentByTag("RateChatFragment");
            if (prev != null) {
                fragmentTransaction.remove(prev);
            }
            fragmentTransaction.addToBackStack(null);
            fragment.show(fragmentTransaction, "RateChatFragment");
        } else if (itemId == id.menu_email_chat_transcript) {
            EmailChatTranscriptFragment fragment = new EmailChatTranscriptFragment(conversation);
            FragmentTransaction fragmentTransaction = getParentFragmentManager().beginTransaction();
            Fragment prev = getParentFragmentManager().findFragmentByTag("EmailChatTranscriptFragment");
            if (prev != null) {
                fragmentTransaction.remove(prev);
            }
            fragmentTransaction.addToBackStack(null);
            fragment.show(fragmentTransaction, "EmailChatTranscriptFragment");
        } else if (itemId == id.menu_edit_info) {
            EditInfoFragment fragment = new EditInfoFragment();
            FragmentTransaction fragmentTransaction = getParentFragmentManager().beginTransaction();
            Fragment prev = getParentFragmentManager().findFragmentByTag("EditInfoFragment");
            if (prev != null) {
                fragmentTransaction.remove(prev);
            }
            fragmentTransaction.addToBackStack(null);
            fragment.show(fragmentTransaction, "EditInfoFragment");
        }
        return false;
    }

    public void endChat() {
        if (conversation.getChannelType() != ChannelType.NORMAL) {
            conversation.endChat(Common.client, new StatusListener() {
                @Override
                public void onSuccess() {

                }
            });
        }
    }

    private String getCallee() {
        String myUserId = PrefUtils.getString(com.stringee.stringeechatuikit.common.Constant.PREF_USER_ID, "");
        List<User> pars = conversation.getParticipants();
        for (int i = 0; i < pars.size(); i++) {
            String userId = pars.get(i).getUserId();
            if (!userId.equals(myUserId)) {
                return userId;
            }
        }
        return "";
    }

    private void loadStickers() {
        Object[] params = new Object[1];
        params[0] = ACTION_LOAD_STICKERS;
        DataHandler handler = new DataHandler(getActivity(), this);
        handler.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
    }

    private void doLoadStickers() {
        File file = FileUtils.getAppDir(getActivity(), "sticker");
        File[] directories = file.listFiles();
        if (directories != null && directories.length > 0) {
            Common.stickerDirectories.clear();
            for (File dir : directories) {
                if (dir.isDirectory() && dir.getName() != null) {
                    Common.stickerDirectories.add(dir.getName());
                    StickerCategory category = new StickerCategory();
                    category.setId(dir.getName());
                    category.setIconUrl("file://" + dir.getAbsolutePath() + "/" + "icon.png");
                    stickerIcons.add(category);
                    String[] files = dir.list();
                    if (files != null && files.length > 0) {
                        List<Sticker> lstStickers = new ArrayList<Sticker>();
                        for (int i = 0; i < files.length; i++) {
                            String filename = files[i];
                            if (!filename.equals("icon.png") && (filename.endsWith(".png") || filename.endsWith(".jpg") || filename.endsWith(".jpeg"))) {
                                Sticker sticker = new Sticker();
                                sticker.setCatId(dir.getName());
                                sticker.setName(filename);
                                sticker.setPath("file://" + dir.getAbsolutePath() + "/" + filename);
                                lstStickers.add(sticker);
                                Collections.sort(lstStickers);
                            }
                        }
                        stickerMap.put(category.getId(), lstStickers);
                    }
                }
            }
        }
    }

    private void doneLoadStickers() {
        StickerCategory category = new StickerCategory();
        category.setIconUrl("drawable://" + R.drawable.ic_sticker_download);
        stickerIcons.add(category);

        stickerIcons.get(0).setSelected(true);

        stickerIconAdapter = new StickerIconAdapter(getActivity(), stickerIcons, new ChooseStickerListener() {
            @Override
            public void onChooseSticker(StickerCategory category) {
                List<Sticker> stickers = stickerMap.get(category.getId());
                if (stickers != null) {
                    stickersGridView.setVisibility(View.VISIBLE);
                    stickersListView.setVisibility(View.GONE);
                    stickerAdapter = new StickerAdapter(getActivity(), stickers);
                    stickersGridView.setAdapter(stickerAdapter);
                } else {
                    stickersGridView.setVisibility(View.GONE);
                    stickersListView.setVisibility(View.VISIBLE);
                    if (stickerCategories.size() == 0) {
                        loadStickerCategories();
                    }
                }
            }
        });
        stickersRecyclerView.setAdapter(stickerIconAdapter);

        List<Sticker> stickers = stickerMap.get(stickerIcons.get(0).getId());
        if (stickers != null) {
            stickersGridView.setVisibility(View.VISIBLE);
            stickersListView.setVisibility(View.GONE);
            stickerAdapter = new StickerAdapter(getActivity(), stickers);
            stickersGridView.setAdapter(stickerAdapter);
        } else {
            stickersGridView.setVisibility(View.GONE);
            stickersListView.setVisibility(View.VISIBLE);
        }

        loadStickerCategories();
    }

    private void loadStickerCategories() {
        RequestQueue queue = Volley.newRequestQueue(getActivity());
        JsonArrayRequest request = new JsonArrayRequest(Constant.STICKER_URL, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                if (getActivity() == null) {
                    return;
                }
                if (response != null && response.length() > 0) {
                    stickerCategories.clear();
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject jsonObject = response.getJSONObject(i);
                            StickerCategory category = new StickerCategory();
                            category.setId(jsonObject.getString("id"));
                            category.setName(jsonObject.getString("name"));
                            category.setStickerNumber(jsonObject.getInt("number_of_stickers"));
                            category.setIconUrl(jsonObject.getString("icon_url"));
                            category.setCoverUrl(jsonObject.getString("cover_url"));
                            category.setZipUrl(jsonObject.getString("zip_url"));
                            for (int j = 0; j < stickerIcons.size(); j++) {
                                StickerCategory category1 = stickerIcons.get(j);
                                if (category.getId().equals(category1.getId())) {
                                    category.setDownloaded(true);
                                    break;
                                }
                            }

                            stickerCategories.add(category);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    categoryAdapter = new StickerCategoryAdapter(getActivity(), stickerCategories, new StickerCategoryAdapter.StickerCategoryListener() {
                        @Override
                        public void onDownloadOrRemoveCategory(final StickerCategory category) {
                            if (getActivity() == null) {
                                return;
                            }
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (category.isDownloaded()) {
                                        File dir = new File(FileUtils.getAppDir(getActivity(), "sticker").getAbsolutePath() + "/" + category.getId());
                                        category.setIconUrl("file://" + dir.getAbsolutePath() + "/icon.png");
                                        stickerIcons.add(0, category);
                                        stickerIconAdapter.notifyDataSetChanged();
                                        String[] files = dir.list();
                                        if (files != null && files.length > 0) {
                                            List<Sticker> lstStickers = new ArrayList<Sticker>();
                                            for (int i = 0; i < files.length; i++) {
                                                String filename = files[i];
                                                if (!filename.equals("icon.png") && (filename.endsWith(".png") || filename.endsWith(".jpg") || filename.endsWith(".jpeg"))) {
                                                    Sticker sticker = new Sticker();
                                                    sticker.setCatId(dir.getName());
                                                    sticker.setName(filename);
                                                    sticker.setPath("file://" + dir.getAbsolutePath() + "/" + filename);
                                                    lstStickers.add(sticker);
                                                    Collections.sort(lstStickers);
                                                }
                                            }
                                            stickerMap.put(category.getId(), lstStickers);
                                        }
                                    } else {
                                        for (int i = 0; i < stickerIcons.size(); i++) {
                                            StickerCategory category1 = stickerIcons.get(i);
                                            if (category1.getId().equals(category.getId())) {
                                                stickerIcons.remove(i);
                                                stickerIconAdapter.notifyDataSetChanged();
                                                break;
                                            }
                                        }
                                    }
                                }
                            });
                        }
                    });
                    stickersListView.setAdapter(categoryAdapter);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("Stringee", "onErrorResponse: " + error.getMessage());
            }
        });
        queue.add(request);
    }

    private void setActionbarTitle() {
        String title = conversation.getName();
        if (title == null || title.length() == 0) {
            title = "";
            List<User> pars = conversation.getParticipants();
            for (int i = 0; i < pars.size(); i++) {
                String name = pars.get(i).getName();
                if (name == null || name.trim().length() == 0) {
                    name = pars.get(i).getUserId();
                }
                if (conversation.isGroup()) {
                    title = title + name + ",";
                } else {
                    String userId = pars.get(i).getUserId();
                    if (!userId.equals(PrefUtils.getString(com.stringee.stringeechatuikit.common.Constant.PREF_USER_ID, ""))) {
                        title = title + name + ",";
                    }
                }
            }
            if (title.length() > 0) {
                title = title.substring(0, title.length() - 1);
            }
        }
        androidx.appcompat.app.ActionBar actionBar = ((ConversationActivity) getActivity()).getSupportActionBar();
        actionBar.setTitle(title);
    }

    @Override
    public void onPause() {
        super.onPause();
        Common.isChatting = false;
    }

    private void getMessages() {
        // Get local messages
        conversation.getLocalMessages(Common.client, Constant.MESSAGES_COUNT, new CallbackListener<List<Message>>() {
            @Override
            public void onSuccess(final List<Message> messageList) {
                if (getActivity() == null) {
                    return;
                }
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (messageList.size() > 0) {
                            prLoading.setVisibility(View.GONE);
                            tvNoMessage.setVisibility(View.GONE);
                        }
                        messages.addAll(messageList);
                        addTempDate(messages, true);
                        adapter.notifyDataSetChanged();
                        messagesRecyclerView.scrollToPosition(messages.size() - 1);

                        getLastMessages();
                    }
                });
            }

            @Override
            public void onError(StringeeError stringeeError) {
                super.onError(stringeeError);
                if (getActivity() == null) {
                    return;
                }
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                       getLastMessages();
                    }
                });
            }
        });
    }

    private void getLastMessages(){
        conversation.getLastMessages(Common.client, Constant.MESSAGES_COUNT, new CallbackListener<List<Message>>() {
            @Override
            public void onSuccess(final List<Message> messages1) {
                if (getActivity() == null) {
                    return;
                }
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        prLoading.setVisibility(View.GONE);

                        if (messages1.size() < Constant.MESSAGES_COUNT) {
                            isTop = true;
                        } else {
                            isTop = false;
                        }

                        merge(messages1);
                        addTempDate(messages, false);
                        adapter.notifyDataSetChanged();
                        messagesRecyclerView.scrollToPosition(messages.size() - 1);
                        readMessages(messages);

                        if (messages.size() == 0) {
                            tvNoMessage.setVisibility(View.VISIBLE);
                        } else {
                            tvNoMessage.setVisibility(View.GONE);
                        }
                    }
                });
            }
        });
    }

    private void merge(List<Message> lstMessages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i).getType() == Message.Type.TEMP_DATE) {
                messages.remove(i);
            }
        }
        messages.addAll(lstMessages);
        Collections.sort(messages, new Comparator<Message>() {
            @Override
            public int compare(Message message, Message m1) {
                String msgId1 = message.getId();
                String msgId2 = m1.getId();
                if (msgId1 == null) {
                    return 1;
                }
                if (msgId2 == null) {
                    return -1;
                }

                if (msgId1.equals(msgId2)) {
                    long lastUpdate = message.getCreatedAt();
                    long lastUpdate2 = m1.getCreatedAt();
                    if (lastUpdate > lastUpdate2) {
                        return 1;
                    } else {
                        return -1;
                    }
                } else {
                    return msgId1.compareTo(msgId2);
                }
            }
        });

        for (int i = messages.size() - 1; i >= 0; i--) {
            if (i > 0) {
                String msgId = messages.get(i).getId();
                String msgId2 = messages.get(i - 1).getId();
                if (msgId != null && msgId2 != null && msgId.equals(msgId2)) {
                    messages.remove(i);
                }
            }
        }

        Collections.sort(messages, new Comparator<Message>() {
            @Override
            public int compare(Message message, Message m1) {
                if (message.getCreatedAt() > m1.getCreatedAt()) {
                    return 1;
                } else {
                    return -1;
                }
            }
        });
    }

    private void addTempDate(List<Message> messageList, boolean isLocal) {
        if (messageList.size() == 0) {
            return;
        }
        for (int i = messageList.size() - 1; i > 0; i--) {
            Message message1 = messageList.get(i);
            Message message2 = messageList.get(i - 1);
            if (message1.getType() == Message.Type.TEMP_DATE || message2.getType() == Message.Type.TEMP_DATE) {
                continue;
            }

            long dayDifference = Utils.daysBetween(new Date(message2.getCreatedAt()), new Date(message1.getCreatedAt()));
            if (dayDifference >= 1) {
                Message tempMessage = new Message(Message.Type.TEMP_DATE);
                tempMessage.setCreatedAt(message1.getCreatedAt());
                messageList.add(i, tempMessage);
            }
        }

        if (isTop || isLocal) {
            if (messageList.get(0).getType() != Message.Type.TEMP_DATE) {
                Message firstTempMessage = new Message(Message.Type.TEMP_DATE);
                firstTempMessage.setCreatedAt(messageList.get(0).getCreatedAt());
                messageList.add(0, firstTempMessage);
            }
        }
    }

    public void onAddMessage(final Message message) {
        if (conversation == null) {
            return;
        }
        if (!message.getConversationId().equals(conversation.getId())) {
            return;
        }
        tvNoMessage.setVisibility(View.GONE);
        messages.add(message);
        adapter.notifyDataSetChanged();
        messagesRecyclerView.scrollToPosition(messages.size() - 1);
        if (Common.isChatting && Common.currentConvId != null && Common.currentConvId.equals(message.getConversationId()) && message.getMsgType() == Message.MsgType.RECEIVE) {
            message.markAsRead(Common.client, new StatusListener() {
                @Override
                public void onSuccess() {

                }
            });
        }
    }

    public void onUpdateMessage(final Message message) {
        if (conversation == null) {
            return;
        }
        if (!message.getConversationId().equals(conversation.getId())) {
            return;
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (int i = messages.size() - 1; i >= 0; i--) {
                    Message message1 = messages.get(i);
                    if (message1.getLocalId() != null && message.getLocalId() != null && message.getMsgType() == Message.MsgType.SEND && message1.getLocalId().equals(message.getLocalId())) {
                        messages.set(i, message);
                    }

                    // Update messages state
                    if (message1.getMsgType() == Message.MsgType.SEND && message.getSequence() >= message1.getSequence()) {
                        // Not sent messages, skip
                        if (message1.getSequence() == 0) {
                            String localId1 = message1.getLocalId();
                            String localId = message.getLocalId();
                            if (!(localId != null && localId1 != null && localId.equals(localId1))) {
                                continue;
                            }
                        }

                        // Update messages state
                        if (message.getState().getValue() > message1.getState().getValue()) {
                            message1.setState(message.getState());
                        } else {
                            break;
                        }
                    }
                }
                adapter.notifyDataSetChanged();
            }
        });
    }

    public void onUpdateConversation(Conversation conv) {
        if (conversation != null && conv != null && conv.getId() != null && conversation.getId() != null && conversation.getId().equals(conv.getId())) {
            conversation = conv;
        }
    }

    public void sendLocation(double latitude, double longitude) {
        Message message = new Message(Message.Type.LOCATION);
        message.setLatitude(latitude);
        message.setLongitude(longitude);
        conversation.sendMessage(Common.client, message, new StatusListener() {
            @Override
            public void onSuccess() {

            }
        });
    }

    public void sendContact(Contact contact) {
        String vCardStr = FileUtils.vCard(contact, getActivity());
        Message message = new Message(Message.Type.CONTACT);
        message.setContact(vCardStr);
        conversation.sendMessage(Common.client, message, new StatusListener() {
            @Override
            public void onSuccess() {

            }
        });
    }

    public void sendPhoto(String filePath) {
        Message message = new Message(Message.Type.PHOTO);
        message.setFilePath(filePath);
        conversation.sendMessage(Common.client, message, new StatusListener() {
            @Override
            public void onSuccess() {
                android.util.Log.d("Stringee", "onSuccess: ");
            }

            @Override
            public void onError(com.stringee.exception.StringeeError stringeeError) {
                super.onError(stringeeError);
            }
        });
    }

    public void sendAudio(String filePath) {
        Uri uri = Uri.parse(filePath);
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(getActivity(), uri);
        String durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        int millSecond = Integer.parseInt(durationStr);
        Message message = new Message(Message.Type.AUDIO);
        message.setFilePath(filePath);
        message.setDuration(millSecond);
        conversation.sendMessage(Common.client, message, new StatusListener() {
            @Override
            public void onSuccess() {

            }
        });
    }

    public void sendVideo(String filePath) {
        Message message = new Message(Message.Type.VIDEO);
        message.setFilePath(filePath);
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(getActivity(), Uri.parse(filePath));
        String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        message.setDuration(Integer.parseInt(time));
        retriever.release();
        conversation.sendMessage(Common.client, message, new StatusListener() {
            @Override
            public void onSuccess() {

            }
        });
    }

    public void sendFile(String filePath) {
        Message message = new Message(Message.Type.FILE);
        message.setFilePath(filePath);
        conversation.sendMessage(Common.client, message, new StatusListener() {
            @Override
            public void onSuccess() {

            }
        });
    }

    public void sendSticker(String category, String name) {
        Message message = new Message(Message.Type.STICKER);
        message.setStickerCategory(category);
        message.setStickerName(name);
        conversation.sendMessage(Common.client, message, new StatusListener() {
            @Override
            public void onSuccess() {

            }
        });
    }

    private void readMessages(List<Message> lstMessages) {
        if (lstMessages.size() == 0) {
            return;
        }
        Message lastMsg = null;
        for (int i = lstMessages.size() - 1; i >= 0; i--) {
            Message message = messages.get(i);
            if (message.getMsgType() == Message.MsgType.RECEIVE) {
                lastMsg = message;
                break;
            }
        }
        if (lastMsg != null && lastMsg.getState().getValue() < Message.State.READ.getValue()) {
            lastMsg.markAsRead(Common.client, new StatusListener() {
                @Override
                public void onSuccess() {

                }
            });
        }
    }

    private void prepareAttachmentData(GridView g, RecyclerView r) {
        String[] alltitles = getResources().getStringArray(R.array.multimediaOptions);
        TypedArray imgs = getResources().obtainTypedArray(R.array.multimediaOptionIcons);
        StringeeMultimediaPopupAdapter adapter2 = new StringeeMultimediaPopupAdapter(getContext(), imgs, Arrays.asList(alltitles));
        g.setAdapter(adapter2);

        MediaAdapter adapter = new MediaAdapter(getActivity(), this, getAllItem());
        r.setAdapter(adapter);
    }

    private ArrayList<Image> getAllShownImages(Activity activity) {
        ArrayList<Image> listOfAllImages = new ArrayList<>();
        Cursor cursor = getContext().getContentResolver().query(
                Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{Images.Media._ID, Images.Media.DATE_MODIFIED},
                null, null, null);

        if (cursor != null) {
            int column_index_id = cursor.getColumnIndexOrThrow(Images.Media._ID);
            int column_index_addDate = cursor.getColumnIndexOrThrow(Images.Media.DATE_MODIFIED);
            while (cursor.moveToNext()) {
                int id = cursor.getInt(column_index_id);
                String dateModified = cursor.getString(column_index_addDate);
                long lDateModified = dateModified != null ? Long.parseLong(dateModified) : 0;
                Image img = new Image(ContentUris.withAppendedId(Images.Media.EXTERNAL_CONTENT_URI, id).toString(), lDateModified);
                listOfAllImages.add(img);
            }
            cursor.close();
        }
        return listOfAllImages;
    }

    @SuppressLint("Recycle")
    private ArrayList<Video> getAllShownVideos(Activity activity) {
        ArrayList<Video> listOfAllVideos = new ArrayList<>();
        Cursor cursor = getContext().getContentResolver().query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Video.Media._ID, MediaStore.Video.Media.DURATION, MediaStore.Video.Media.DATE_MODIFIED},
                null, null, null);

        if (cursor != null) {
            int column_index_id = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
            int column_index_addDate = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED);
            int column_index_duration = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);
            while (cursor.moveToNext()) {
                int id = cursor.getInt(column_index_id);
                String dateModified = cursor.getString(column_index_addDate);
                long lDateModified = dateModified != null ? Long.parseLong(dateModified) : 0;
                String dur = cursor.getString(column_index_duration);
                long ldur = dateModified != null ? dur != null ? Long.parseLong(dur) : 0 : 0;
                String videoDuration = String.format(Locale.getDefault(), "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(ldur),
                        TimeUnit.MILLISECONDS.toMinutes(ldur) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(ldur)),
                        TimeUnit.MILLISECONDS.toSeconds(ldur) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(ldur)));

                Video video = new Video(ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id).toString(), lDateModified, videoDuration);
                listOfAllVideos.add(video);
            }
            cursor.close();
        }
        return listOfAllVideos;
    }

    private ArrayList<DataItem> getAllItem() {
        ArrayList<Image> listOfAllImages = getAllShownImages(getActivity());
        ArrayList<DataItem> listOfAllItems = new ArrayList<DataItem>(listOfAllImages);
        ArrayList<Video> listOfAllVideos = getAllShownVideos(getActivity());
        listOfAllItems.addAll(listOfAllVideos);

        Collections.sort(listOfAllItems, new Comparator<DataItem>() {
            @Override
            public int compare(DataItem sv1, DataItem sv2) {
                return (int) (sv2.getDateAdd() - sv1.getDateAdd());
            }
        });
        return listOfAllItems;
    }

    private void revealShow(View dialogView, boolean b, final Dialog dialog) {

        final View view = dialogView.findViewById(id.dialog);

        int w = view.getWidth();
        int h = view.getHeight();

        int endRadius = (int) Math.hypot(w, h);
        int cx = (int) (attachButton.getX() + (attachButton.getWidth() >> 1) + 580);
        int cy = (int) (attachButton.getY() + (attachButton.getHeight() >> 1) + 530);
        if (b) {
            if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
                Animator revealAnimator = ViewAnimationUtils.createCircularReveal(view, cx, cy, 0, endRadius);
                view.setVisibility(View.VISIBLE);
                revealAnimator.setDuration(300);
                revealAnimator.start();
            } else {
                Animator animator =
                        io.codetail.animation.ViewAnimationUtils.createCircularReveal(view, cx, cy, 0, endRadius);
                animator.setInterpolator(new AccelerateDecelerateInterpolator());
                animator.setDuration(300);
                animator.start();
            }
        } else {
            if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
                Animator anim = ViewAnimationUtils.createCircularReveal(view, cx, cy, endRadius, 0);
                anim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        dialog.dismiss();
                        view.setVisibility(View.INVISIBLE);
                    }
                });
                anim.setDuration(300);
                anim.start();
            } else {
                Animator anim = io.codetail.animation.ViewAnimationUtils.createCircularReveal(view, cx, cy, endRadius, 0);
                anim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        dialog.dismiss();
                        view.setVisibility(View.INVISIBLE);
                    }
                });
                anim.setDuration(300);
                anim.start();
            }
        }
    }

    public void closeDialog() {
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    @Override
    public void onButtonSendClick() {
        String text = messageEditText.getText().toString().trim();
        if (text.length() > 0) {
            Message message = new Message(text);
            conversation.sendMessage(Common.client, message, new StatusListener() {
                @Override
                public void onSuccess() {

                }

                @Override
                public void onError(StringeeError error) {

                }
            });
            messageEditText.setText("");
        }
    }

    @Override
    public void onButtonVoiceClick() {
        ((ConversationActivity) getActivity()).processAudioAction((ConversationActivity) getActivity());
    }

    @Override
    public void onButtonAttachClick() {
        ((ConversationActivity) getActivity()).hideKeyboard(attachButton);
        if (Utils.hasMarshmallow() && PermissionsUtils.checkSelfForStoragePermission(getActivity())) {
            PermissionsUtils.requestPermissions(getActivity(), PermissionsUtils.PERMISSIONS_STORAGE, PermissionsUtils.REQUEST_STORAGE);
        } else {
            final View dialogView = View.inflate(getContext(), R.layout.popup_window, null);

            dialog = new Dialog(getContext());
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(dialogView);

            GridView gridView = dialog.findViewById(id.attachGrid);

            RecyclerView listimage = dialog.findViewById(id.imageRecyclerView);
            LinearLayoutManager layoutManager = new LinearLayoutManager(this.getContext(), LinearLayoutManager.HORIZONTAL, false);
            listimage.setLayoutManager(layoutManager);

            prepareAttachmentData(gridView, listimage);
            gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int i, long id) {
                    ((ConversationActivity) getActivity()).setAttachType(i);
                    switch (i) {
                        case 0:
                            ((ConversationActivity) getActivity()).processCameraAction(getActivity());
                            dialog.dismiss();
                            break;
                        case 1:
                            ((ConversationActivity) getActivity()).processGalleryAction(getActivity());
                            dialog.dismiss();
                            break;
                        case 2:
                            ((ConversationActivity) getActivity()).processVideoAction(getActivity());
                            dialog.dismiss();
                            break;
                        case 3:
                            ((ConversationActivity) getActivity()).processAudioAction((ConversationActivity) getActivity());
                            dialog.dismiss();
                            break;
                        case 4:
                            ((ConversationActivity) getActivity()).processFileAction(getActivity());
                            dialog.dismiss();
                            break;
                        case 5:
                            ((ConversationActivity) getActivity()).processContactAction(getActivity());
                            dialog.dismiss();
                            break;
                        case 6:
                            Intent intent = new Intent(getActivity(), StringeeLocationActivity.class);
                            getActivity().startActivityForResult(intent, ConversationActivity.REQUEST_CODE_LOCATION);
                            dialog.dismiss();
                            break;
                        case 7:
                            revealShow(dialogView, false, dialog);
                            break;
                    }
                }
            });

            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialog) {
                    revealShow(dialogView, true, null);
                }
            });
            dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface d, int i, KeyEvent event) {
                    if (i == KeyEvent.KEYCODE_BACK) {

                        revealShow(dialogView, false, dialog);
                        return true;
                    }

                    return false;
                }
            });
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setGravity(Gravity.BOTTOM);
            dialog.show();
        }
    }

    @Override
    public void onButtonStickerClick(boolean isShow) {
        if (isShow && !isScrolledDown) {
            scrollToBottom();
        }
    }

    public void onKeyboardShown() {
        if (!isScrolledDown) {
            scrollToBottom();
        }
    }

    @Override
    public void onChangeListMessageSize(int sizeListMessage) {
        ViewGroup.LayoutParams params = (ViewGroup.LayoutParams) messagesRecyclerView.getLayoutParams();
        params.height = sizeListMessage;
        messagesRecyclerView.setLayoutParams(params);
    }

    @Override
    public void start() {

    }

    @Override
    public void doWork(Object... params) {
        String strAction = (String) params[0];
        if (strAction.equals(ACTION_LOAD_STICKERS)) {
            doLoadStickers();
        }
    }

    @Override
    public void end(Object[] params) {
        String strAction = (String) params[0];
        if (strAction.equals(ACTION_LOAD_STICKERS)) {
            doneLoadStickers();
        }
    }

    public interface ChooseStickerListener {
        public void onChooseSticker(StickerCategory category);
    }

    private void scrollToBottom() {
        messagesRecyclerView.smoothScrollToPosition(adapter.getItemCount());
    }

    public void clearSelected() {
        if (adapter != null) {
            adapter.clearSelected();
        }
    }

    public void deleteMessages() {
        Map<String, Message> msgs = adapter.getSelectedMessages();
        String title = getString(R.string.delete_message);
        if (msgs.size() > 1) {
            title = getString(R.string.delete_messages, String.valueOf(msgs.size()));
        }
        String msg = getString(R.string.confirm_delete_message);
        if (msgs.size() > 1) {
            msg = getString(R.string.confirm_delete_messages);
        }
        Builder builder = new Builder(getActivity());
        builder.setTitle(title);
        builder.setMessage(msg);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ((ConversationActivity) getActivity()).showProgress(getString(R.string.deleting));
                deleteSelectedMessages();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void deleteSelectedMessages() {
        Map<String, Message> msgs = adapter.getSelectedMessages();
        final List<Message> messages = new ArrayList<>();
        for (Map.Entry<String, Message> entry : msgs.entrySet()) {
            messages.add(entry.getValue());
        }
        conversation.deleteMessages(Common.client, messages, new StatusListener() {
            @Override
            public void onSuccess() {
                if (getActivity() == null) {
                    return;
                }
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((BaseActivity) getActivity()).dismissProgress();
                        adapter.clearSelected();
                        ((ConversationActivity) getActivity()).hideSelectedMenu();
                        adapter.removeMessages(messages);
                    }
                });
            }

            @Override
            public void onError(final StringeeError error) {
                if (getActivity() == null) {
                    return;
                }
                ((BaseActivity) getActivity()).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((BaseActivity) getActivity()).dismissProgress();
                        adapter.clearSelected();
                        ((ConversationActivity) getActivity()).hideSelectedMenu();
                        Utils.reportMessage(getActivity(), error.getMessage());
                    }
                });
            }
        });
    }
}
