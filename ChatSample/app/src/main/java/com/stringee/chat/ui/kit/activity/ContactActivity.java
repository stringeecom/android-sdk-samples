package com.stringee.chat.ui.kit.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;

import com.stringee.chat.ui.kit.adapter.ContactAdapter;
import com.stringee.chat.ui.kit.model.Contact;
import com.stringee.stringeechatuikit.BaseActivity;
import com.stringee.stringeechatuikit.R;
import com.stringee.stringeechatuikit.common.Utils;

import java.util.ArrayList;
import java.util.List;

public class ContactActivity extends BaseActivity {
    private ListView lvContacts;
    private ContactAdapter adapter;
    private List<Contact> contacts = new ArrayList<>();
    private List<Contact> searchContacts = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        androidx.appcompat.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.select_contact);
        getSupportActionBar().show();

        lvContacts = (ListView) findViewById(R.id.lv_contacts);
        lvContacts.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Contact contact = (Contact) adapter.getItem(i);
                if (contact.getType() != -1) {
                    Intent intent = new Intent();
                    intent.putExtra("contact", contact);
                    setResult(RESULT_OK, intent);
                    finish();
                }
            }
        });

        contacts = Utils.getContactsFromDevice(this);
        contacts = Utils.genDeviceContactHeaders(contacts);
        adapter = new ContactAdapter(this, contacts);
        lvContacts.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_contacts, menu);

        SearchView search = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.trim().length() > 0) {
                    searchContacts = searchContact(newText.trim());
                    adapter = new ContactAdapter(ContactActivity.this, searchContacts);
                    lvContacts.setAdapter(adapter);
                } else {
                    adapter = new ContactAdapter(ContactActivity.this, contacts);
                    lvContacts.setAdapter(adapter);
                }
                return true;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return false;
    }

    private List<Contact> searchContact(String name) {
        List<Contact> result = new ArrayList<>();
        for (int i = 0; i < contacts.size(); i++) {
            String contactName = contacts.get(i).getName();
            if (contactName.toLowerCase().contains(name.toLowerCase())) {
                result.add(contacts.get(i));
            }
        }

        return result;
    }
}
