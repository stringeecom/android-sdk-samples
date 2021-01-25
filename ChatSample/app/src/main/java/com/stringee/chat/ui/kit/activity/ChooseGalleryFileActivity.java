package com.stringee.chat.ui.kit.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.stringee.chat.ui.kit.adapter.GalleyFileAdapter;
import com.stringee.chat.ui.kit.model.DataItem;
import com.stringee.chat.ui.kit.model.MediaFolder;
import com.stringee.stringeechatuikit.R;

public class ChooseGalleryFileActivity extends AppCompatActivity {
    private ActionBar mActionBar;
    private MediaFolder folder;
    private GalleyFileAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_gallery_file);

        folder = (MediaFolder) getIntent().getSerializableExtra("folder");
        mActionBar = getSupportActionBar();
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setTitle(folder.getName());

        GridView gridView = findViewById(R.id.galleryGridFile);
        adapter = new GalleyFileAdapter(this, folder);
        gridView.setAdapter(adapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent();
                DataItem dataItem = (DataItem) adapter.getItem(position);
                intent.putExtra("media", dataItem);
                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        setResult(Activity.RESULT_CANCELED);
        super.onBackPressed();
    }
}