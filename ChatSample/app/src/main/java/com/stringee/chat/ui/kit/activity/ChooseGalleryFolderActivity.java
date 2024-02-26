package com.stringee.chat.ui.kit.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.stringee.chat.ui.kit.adapter.GalleryFolderAdapter;
import com.stringee.chat.ui.kit.model.DataItem;
import com.stringee.chat.ui.kit.model.Image;
import com.stringee.chat.ui.kit.model.MediaFolder;
import com.stringee.chat.ui.kit.model.Video;
import com.stringee.stringeechatuikit.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ChooseGalleryFolderActivity extends AppCompatActivity {
    private ActionBar mActionBar;
    private List<MediaFolder> allMediaFolders = new ArrayList<>();
    private boolean isFolder;
    private GalleryFolderAdapter adapter;
    private GridView foldersGridView;
    private static final int REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_gallery_folder);

        mActionBar = getSupportActionBar();
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setTitle(R.string.gallery);

        foldersGridView = findViewById(R.id.gv_gallery_folder);
        foldersGridView.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(getApplicationContext(), ChooseGalleryFileActivity.class);
            intent.putExtra("folder", adapter.getItem(position));
            startActivityForResult(intent, REQUEST_CODE);
        });
        getAllMediaFolders();
        adapter = new GalleryFolderAdapter(this, allMediaFolders);
        foldersGridView.setAdapter(adapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return false;
    }

    public void getAllMediaFolders() {
        allMediaFolders.clear();
        int imgPosition = 0, vidPosition = 0;

//        get all images
        Cursor imgCursor = getContentResolver().query(
                Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{Images.Media._ID, Images.Media.BUCKET_DISPLAY_NAME, Images.Media.DATE_MODIFIED},
                null, null,
                MediaStore.Images.Media.DATE_MODIFIED + " DESC");

        if (imgCursor != null) {
            int imgColumn_index_id = imgCursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID);
            int imgColumn_index_folder_name = imgCursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
            int imgColumn_index_dateAdd = imgCursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED);
            while (imgCursor.moveToNext()) {
                int imageId = imgCursor.getInt(imgColumn_index_id);
                String timeAdd = imgCursor.getString(imgColumn_index_dateAdd);
                long ltime = timeAdd != null ? Long.parseLong(timeAdd) : 0;
                Image newImg = new Image(ContentUris.withAppendedId(Images.Media.EXTERNAL_CONTENT_URI, imageId).toString(), ltime);
                for (int i = 0; i < allMediaFolders.size(); i++) {
                    if (allMediaFolders.get(i).getName().equals(imgCursor.getString(imgColumn_index_folder_name))) {
                        isFolder = true;
                        imgPosition = i;
                        break;
                    } else {
                        isFolder = false;
                    }
                }

                if (isFolder) {
                    ArrayList<DataItem> allImage = new ArrayList<>(allMediaFolders.get(imgPosition).getListData());
                    allImage.add(newImg);
                    allMediaFolders.get(imgPosition).setListData(allImage);
                } else {
                    ArrayList<DataItem> allImage = new ArrayList<>();
                    allImage.add(newImg);
                    MediaFolder obj_model = new MediaFolder();
                    obj_model.setName(imgCursor.getString(imgColumn_index_folder_name));
                    obj_model.setListData(allImage);
                    allMediaFolders.add(obj_model);
                }
            }
        }

//        get all videos
        Cursor vidCursor = getContentResolver().query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Video.Media._ID, MediaStore.Video.Media.DURATION, MediaStore.Video.Media.DATE_MODIFIED, MediaStore.Video.Media.BUCKET_DISPLAY_NAME},
                null, null,
                MediaStore.Video.Media.DATE_MODIFIED + " DESC");

        if (vidCursor != null) {
            int vidColumn_index_id = vidCursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
            int vidColumn_index_folder_name = vidCursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME);
            int vidColumn_index_dateAdd = vidCursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED);
            int vidColumn_index_duration = vidCursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);
            while (vidCursor.moveToNext()) {
                int videoId = vidCursor.getInt(vidColumn_index_id);
                String timeAdd = vidCursor.getString(vidColumn_index_dateAdd);
                long ltime = timeAdd != null ? Long.parseLong(timeAdd) : 0;
                String dur = vidCursor.getString(vidColumn_index_duration);
                long ldur = timeAdd != null ? dur != null ? Long.parseLong(dur) : 0 : 0;
                @SuppressLint("DefaultLocale") String videoDuration = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(ldur),
                        TimeUnit.MILLISECONDS.toMinutes(ldur) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(ldur)),
                        TimeUnit.MILLISECONDS.toSeconds(ldur) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(ldur)));
                Video newVid = new Video(ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, videoId).toString(), ltime, videoDuration);
                for (int i = 0; i < allMediaFolders.size(); i++) {
                    if (allMediaFolders.get(i).getName().equals(vidCursor.getString(vidColumn_index_folder_name))) {
                        isFolder = true;
                        vidPosition = i;
                        break;
                    } else {
                        isFolder = false;
                    }
                }
                if (isFolder) {
                    ArrayList<DataItem> allVideo = new ArrayList<>(allMediaFolders.get(vidPosition).getListData());
                    allVideo.add(newVid);
                    allMediaFolders.get(vidPosition).setListData(allVideo);
                } else {
                    ArrayList<DataItem> allVideo = new ArrayList<>();
                    allVideo.add(newVid);
                    MediaFolder obj_model = new MediaFolder();
                    obj_model.setName(vidCursor.getString(vidColumn_index_folder_name));
                    obj_model.setListData(allVideo);
                    allMediaFolders.add(obj_model);
                }
            }
        }

//        sort all data in each folder
        if (allMediaFolders.size() > 0) {
            for (int i = 0; i < allMediaFolders.size(); i++) {
                ArrayList<DataItem> allFolderData = allMediaFolders.get(i).getListData();
                Collections.sort(allFolderData, (sv1, sv2) -> Long.compare(sv2.getDateAdd(), sv1.getDateAdd()));
                allMediaFolders.get(i).setListData(allFolderData);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                DataItem dataItem = (DataItem) data.getSerializableExtra("media");
                Intent intent = new Intent();
                intent.putExtra("media", dataItem);
                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        }
    }

    @Override
    public void onBackPressed() {
        setResult(Activity.RESULT_CANCELED);
        super.onBackPressed();
    }
}
