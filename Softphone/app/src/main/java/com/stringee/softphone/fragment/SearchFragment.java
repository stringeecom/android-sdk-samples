package com.stringee.softphone.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

import com.stringee.softphone.R;
import com.stringee.softphone.activity.ContactDetailActivity;
import com.stringee.softphone.adapter.ContactAdapter;
import com.stringee.softphone.common.Constant;
import com.stringee.softphone.common.Utils;
import com.stringee.softphone.model.Contact;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by luannguyen on 7/12/2017.
 */

public class SearchFragment extends Fragment implements View.OnClickListener {

    private EditText etSearch;
    private ListView lvSearch;

    private List<Contact> totalContacts = new ArrayList<>();
    private List<Contact> searchContacts = new ArrayList<>();
    private ContactAdapter adapter;

    private long currentSearchTime = 0;
    private long lastSearchTime = 0;
    private Handler handler = new Handler();

    public SearchFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_search, container, false);

        ImageButton btnBack = (ImageButton) layout.findViewById(R.id.btn_back);
        btnBack.setOnClickListener(this);

        final ImageButton btnClear = (ImageButton) layout.findViewById(R.id.btn_clear);
        btnClear.setOnClickListener(this);

        etSearch = (EditText) layout.findViewById(R.id.et_search);
        etSearch.requestFocus();
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                currentSearchTime = System.currentTimeMillis();
                final String text = editable.toString();
                if (text.length() > 0) {
                    btnClear.setVisibility(View.VISIBLE);
                    if (text.trim().length() > 0) {
                        long delta = currentSearchTime - lastSearchTime;
                        if (delta > 500) {
                            lastSearchTime = currentSearchTime;
                            searchContacts = searchContact(text.trim());
                            adapter = new ContactAdapter(getActivity(), searchContacts);
                            lvSearch.setAdapter(adapter);
                        } else {
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    long delta = System.currentTimeMillis() - lastSearchTime;
                                    if (delta > 500) {
                                        lastSearchTime = currentSearchTime;
                                        searchContacts = searchContact(text.trim());
                                        adapter = new ContactAdapter(getActivity(), searchContacts);
                                        lvSearch.setAdapter(adapter);
                                    }
                                }
                            }, 500);
                        }
                    }
                } else {
                    btnClear.setVisibility(View.INVISIBLE);
                    adapter = new ContactAdapter(getActivity(), totalContacts);
                    lvSearch.setAdapter(adapter);
                }
            }
        });

        lvSearch = (ListView) layout.findViewById(R.id.lv_search);
        lvSearch.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Contact contact = (Contact) adapter.getItem(i);
                Intent intent = new Intent(getActivity(), ContactDetailActivity.class);
                intent.putExtra(Constant.PARAM_CONTACT, contact);
                startActivity(intent);
            }
        });

        layout.setOnClickListener(null);
        Utils.showKeyboard(getActivity());

        totalContacts.addAll(ContactsFragment.contacts);
        for (int i = totalContacts.size() - 1; i >= 0; i--) {
            if (totalContacts.get(i).getType() == Constant.TYPE_CONTACT_HEADER) {
                totalContacts.remove(i);
            }
        }
        Collections.sort(totalContacts, new Comparator<Contact>() {
            @Override
            public int compare(Contact contact, Contact t1) {
                return contact.getName().toLowerCase().compareTo(t1.getName().toLowerCase());
            }
        });
        adapter = new ContactAdapter(getActivity(), totalContacts);
        lvSearch.setAdapter(adapter);
        return layout;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_back:
                Utils.hideKeyboard(getActivity());
                Fragment fragment = getActivity().getSupportFragmentManager().findFragmentByTag("SEARCH");
                if (fragment != null) {
                    getActivity().getSupportFragmentManager().beginTransaction().remove(fragment).commit();
                }
                break;
            case R.id.btn_clear:
                etSearch.setText("");
                break;
        }
    }

    private List<Contact> searchContact(String name) {
        List<Contact> result = new ArrayList<>();
        for (int i = 0; i < totalContacts.size(); i++) {
            String contactName = totalContacts.get(i).getName();
            if (contactName.toLowerCase().contains(name.toLowerCase())) {
                result.add(totalContacts.get(i));
            }
        }

        return result;
    }
}
