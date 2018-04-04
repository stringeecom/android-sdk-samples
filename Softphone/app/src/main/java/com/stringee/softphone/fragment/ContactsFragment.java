package com.stringee.softphone.fragment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.stringee.softphone.R;
import com.stringee.softphone.activity.ContactDetailActivity;
import com.stringee.softphone.adapter.ContactAdapter;
import com.stringee.softphone.common.CallBack;
import com.stringee.softphone.common.Constant;
import com.stringee.softphone.common.DataHandler;
import com.stringee.softphone.common.Utils;
import com.stringee.softphone.model.Contact;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by luannguyen on 7/10/2017.
 */

public class ContactsFragment extends Fragment implements CallBack {

    private ListView lvContacts;

    private ContactAdapter adapter;
    public static List<Contact> contacts = new ArrayList<>();

    private final String GET_DEVICE_CONTACTS = "get_device_contacts";
    public static final int REQUEST_PERMISSION_CONTACT = 1;

    public ContactsFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_contacts, container, false);

        lvContacts = (ListView) layout.findViewById(R.id.lv_device_contacts);
        lvContacts.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Contact contact = contacts.get(i);
                if (contact.getType() != -1) {
                    Intent intent = new Intent(getActivity(), ContactDetailActivity.class);
                    intent.putExtra(Constant.PARAM_CONTACT, contact);
                    startActivity(intent);
                }
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            List<String> lstPermissions = new ArrayList<>();
            if (ContextCompat.checkSelfPermission(getActivity(),
                    Manifest.permission.READ_CONTACTS)
                    != PackageManager.PERMISSION_GRANTED) {
                lstPermissions.add(Manifest.permission.READ_CONTACTS);
            }
            if (lstPermissions.size() == 0) {
                getDeviceContacts();
            } else {
                String[] permissions = new String[lstPermissions.size()];
                for (int i = 0; i < lstPermissions.size(); i++) {
                    permissions[i] = lstPermissions.get(i);
                }
                requestPermissions(permissions,
                        REQUEST_PERMISSION_CONTACT);
            }
        } else {
            getDeviceContacts();
        }
        return layout;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        boolean isGranted = false;
        if (grantResults.length > 0) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    isGranted = false;
                    break;
                } else {
                    isGranted = true;
                }
            }
        }
        switch (requestCode) {
            case REQUEST_PERMISSION_CONTACT:
                if (isGranted) {
                    getDeviceContacts();
                }
                break;
        }
    }


    /**
     * Get device contacts
     */
    private void getDeviceContacts() {
        if (getActivity() != null) {
            Object[] params = new Object[1];
            params[0] = GET_DEVICE_CONTACTS;
            DataHandler handler = new DataHandler(getActivity(), this);
            handler.execute(params);
        }
    }

    private void doGetDeviceContacts() {
        contacts = Utils.getContactsFromDevice(getActivity());
        contacts = Utils.genDeviceContactHeaders(contacts);
    }

    private void doneGetDeviceContacts() {
        adapter = new ContactAdapter(getActivity(), contacts);
        lvContacts.setAdapter(adapter);
    }

    @Override
    public void start() {

    }

    @Override
    public void doWork(Object... params) {
        String action = (String) params[0];
        if (action.equals(GET_DEVICE_CONTACTS)) {
            doGetDeviceContacts();
        }
    }

    @Override
    public void end(Object[] params) {
        String action = (String) params[0];
        if (action.equals(GET_DEVICE_CONTACTS)) {
            doneGetDeviceContacts();
        }
    }
}
