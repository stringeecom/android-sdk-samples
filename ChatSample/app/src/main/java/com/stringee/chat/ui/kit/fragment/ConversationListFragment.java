package com.stringee.chat.ui.kit.fragment;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.OnScrollListener;

import com.stringee.chat.ui.kit.activity.ConversationActivity;
import com.stringee.chat.ui.kit.adapter.ConversationAdapter;
import com.stringee.chat.ui.kit.commons.Constant;
import com.stringee.exception.StringeeError;
import com.stringee.listener.StatusListener;
import com.stringee.messaging.Conversation;
import com.stringee.messaging.User;
import com.stringee.messaging.listeners.CallbackListener;
import com.stringee.stringeechatuikit.R;
import com.stringee.stringeechatuikit.common.Common;
import com.stringee.stringeechatuikit.common.PrefUtils;
import com.stringee.stringeechatuikit.common.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class ConversationListFragment extends Fragment {

    public static List<Conversation> conversationList = new ArrayList<>();
    private ConversationAdapter adapter;

    private RecyclerView conversationListView;
    private LinearLayoutManager linearLayoutManager;
    private boolean isLoading;
    private boolean isLast;
    private boolean isTouched;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        ((androidx.appcompat.app.AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.conversations);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_conversations, container, false);

        conversationListView = view.findViewById(R.id.rv_conversation);
        linearLayoutManager = new LinearLayoutManager(getActivity());
        conversationListView.setLayoutManager(linearLayoutManager);
        conversationListView.setHasFixedSize(true);
        adapter = new ConversationAdapter(getActivity(), conversationList);
        conversationListView.setAdapter(adapter);

        conversationListView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                isTouched = true;
                return false;
            }
        });

        conversationListView.addOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (!recyclerView.canScrollVertically(1) && !isLoading && !isLast && isTouched) {
                    long lastUpdate = conversationList.get(0).getUpdateAt();
                    Common.client.getConversationsBefore(lastUpdate, Constant.CONVERSATIONS_COUNT, new CallbackListener<List<Conversation>>() {
                        @Override
                        public void onSuccess(final List<Conversation> conversations) {
                            if (getActivity() == null) {
                                return;
                            }
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    isLoading = false;
                                    isLast = Utils.isListEmpty(conversations);
                                    if (!Utils.isListEmpty(conversations)) {
                                        merge(conversations);
                                        adapter.notifyDataSetChanged();
                                    }
                                }
                            });
                        }
                    });
                }
            }
        });

        getLatestConversations();

        Common.client.getLocalConversations(PrefUtils.getString(com.stringee.stringeechatuikit.common.Constant.PREF_USER_ID, ""), new CallbackListener<List<Conversation>>() {
            @Override
            public void onSuccess(final List<Conversation> conversations) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            conversationList.clear();
                            if (!Utils.isListEmpty(conversations)) {
                                conversationList.addAll(conversations);
                                adapter.notifyDataSetChanged();
                            }

                            // Get latest conversations from server
                            getLatestConversations();
                        }
                    });
                }
            }

            @Override
            public void onError(StringeeError stringeeError) {
                super.onError(stringeeError);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Get latest conversations from server
                            getLatestConversations();
                        }
                    });
                }

            }
        });

        return view;
    }

    private void getLatestConversations() {
        Common.client.getLastConversations(Constant.CONVERSATIONS_COUNT, new CallbackListener<List<Conversation>>() {
            @Override
            public void onSuccess(final List<Conversation> conversations) {
                if (getActivity() == null) {
                    return;
                }
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        isLast = Utils.isListEmpty(conversations);
                        conversationList.clear();
                        if (!Utils.isListEmpty(conversations)) {
                            merge(conversations);
                            adapter.notifyDataSetChanged();
                        }
                    }
                });
            }
        });
    }

    private void merge(List<Conversation> conversations) {
        conversationList.addAll(conversations);
        Collections.sort(conversationList, new Comparator<Conversation>() {
            @Override
            public int compare(Conversation conversation, Conversation t1) {
                String convId = conversation.getId();
                String convId2 = t1.getId();
                if (convId.equals(convId2)) {
                    long lastUpdate = conversation.getUpdateAt();
                    long lastUpdate2 = t1.getUpdateAt();
                    if (lastUpdate > lastUpdate2) {
                        return -1;
                    } else {
                        return 1;
                    }
                } else {
                    return convId.compareTo(convId2);
                }
            }
        });

        for (int i = conversationList.size() - 1; i >= 0; i--) {
            if (i > 0) {
                String convId = conversationList.get(i).getId();
                String convId2 = conversationList.get(i - 1).getId();
                if (convId != null && convId2 != null && convId.equals(convId2)) {
                    conversationList.remove(i);
                }
            }
        }

        Collections.sort(conversationList, new Comparator<Conversation>() {
            @Override
            public int compare(Conversation conversation, Conversation t1) {
                if (conversation.getUpdateAt() > t1.getUpdateAt()) {
                    return -1;
                } else {
                    return 1;
                }
            }
        });
    }

    public void onAddConversation(Conversation conversation) {
        // Update conversation list
        conversationList.add(conversation);
        Collections.sort(conversationList, new Comparator<Conversation>() {
            @Override
            public int compare(Conversation conversation, Conversation t1) {
                if (conversation.getUpdateAt() > t1.getUpdateAt()) {
                    return -1;
                } else {
                    return 1;
                }
            }
        });
        adapter.notifyDataSetChanged();
    }

    public void onUpdateConversation(Conversation conversation) {
        for (int i = conversationList.size() - 1; i >= 0; i--) {
            Conversation conversation1 = conversationList.get(i);
            if (conversation.getId().equals(conversation1.getId())) {
                conversationList.set(i, conversation);
                break;
            }
        }
        Collections.sort(conversationList, new Comparator<Conversation>() {
            @Override
            public int compare(Conversation conversation, Conversation t1) {
                if (conversation.getUpdateAt() > t1.getUpdateAt()) {
                    return -1;
                } else {
                    return 1;
                }
            }
        });
        adapter.notifyDataSetChanged();
    }

    public void clearSelected() {
        if (adapter != null) {
            adapter.clearSelected();
        }
    }

    public void deleteChats() {
        Map<String, Conversation> convs = adapter.getSelectedConversations();
        String title = getString(R.string.delete_chat);
        if (convs.size() > 1) {
            title = getString(R.string.delete_chats, String.valueOf(convs.size()));
        }
        String msg = getString(R.string.confirm_delete_conversations);
        if (convs.size() == 1) {
            Map.Entry<String, Conversation> entry = convs.entrySet().iterator().next();
            Conversation conversation = entry.getValue();
            msg = getDeleteMsg(conversation);
        }
        Builder builder = new Builder(getActivity());
        builder.setTitle(title);
        builder.setMessage(msg);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ((ConversationActivity) getActivity()).showProgress(getString(R.string.deleting));
                deleteConversations();
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

    private String getDeleteMsg(Conversation conversation) {
        String convName = conversation.getName();
        if (convName == null || convName.length() == 0) {
            convName = "";
            List<User> pars = conversation.getParticipants();
            for (int i = 0; i < pars.size(); i++) {
                String name = pars.get(i).getName();
                if (name == null || name.trim().length() == 0) {
                    name = pars.get(i).getUserId();
                }
                if (conversation.isGroup()) {
                    convName = convName + name + ",";
                } else {
                    String userId = pars.get(i).getUserId();
                    if (!userId.equals(PrefUtils.getString(com.stringee.stringeechatuikit.common.Constant.PREF_USER_ID, ""))) {
                        convName = convName + name + ",";
                    }
                }
            }
            if (convName.length() > 0) {
                convName = convName.substring(0, convName.length() - 1);
            }
        }
        if (conversation.isGroup()) {
            return getString(R.string.delete_group_chat, convName);
        } else {
            return getString(R.string.confirm_delete_chat_with, convName);
        }
    }

    private void deleteConversations() {
        Map<String, Conversation> convs = adapter.getSelectedConversations();
        for (Map.Entry<String, Conversation> entry : convs.entrySet()) {
            final Conversation conversation = entry.getValue();
            if (conversation.isGroup()) {
                List<User> participants = new ArrayList<>();
                participants.add(new User(PrefUtils.getString(com.stringee.stringeechatuikit.common.Constant.PREF_USER_ID, "")));
                conversation.removeParticipants(Common.client, participants, new CallbackListener<List<User>>() {
                    @Override
                    public void onSuccess(List<User> users) {
                        conversation.delete(Common.client, new StatusListener() {
                            @Override
                            public void onSuccess() {
                                onConversationDeleted(conversation);
                            }

                            @Override
                            public void onError(StringeeError error) {
                                Log.d("Stringee", error.getMessage());
                            }
                        });
                    }

                    @Override
                    public void onError(StringeeError error) {
                        Log.d("Stringee", error.getMessage());
                    }
                });
            } else {
                conversation.delete(Common.client, new StatusListener() {
                    @Override
                    public void onSuccess() {
                        onConversationDeleted(conversation);
                    }

                    @Override
                    public void onError(StringeeError error) {
                        Log.d("Stringee", error.getMessage());
                    }
                });
            }
        }
    }

    public void onConversationDeleted(final Conversation conversation) {
        if (getActivity() == null) {
            return;
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.onRemoveConversation(conversation);
                if (adapter.getSelectedConversations().size() == 0) {
                    if (getActivity() != null) {
                        ((ConversationActivity) getActivity()).dismissProgress();
                        ((ConversationActivity) getActivity()).hideSelectedMenu();
                    }
                }
            }
        });
    }
}
