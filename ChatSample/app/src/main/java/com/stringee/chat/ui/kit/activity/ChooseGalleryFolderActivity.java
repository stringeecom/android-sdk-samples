package com.stringee.chat.ui.kit.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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
import java.util.Comparator;
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

        foldersGridView = findViewById(R.id.galleryGridFolder);
        foldersGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(), ChooseGalleryFileActivity.class);
                intent.putExtra("folder", adapter.getItem(position));
                startActivityForResult(intent, REQUEST_CODE);
            }
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
        Uri imgUri, vidUri;
        Cursor imgCursor, vidCursor;
        int imgColumn_index_data, imgColumn_index_folder_name, imgColumn_index_dateAdd,
                vidColumn_index_data, vidColumn_index_folder_name, vidColumn_index_dateAdd, vidColumn_index_duration;
//
//        get all images
        String absolutePathOfImage;
        imgUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.MediaColumns.DATA, MediaStore.Images.Media.BUCKET_DISPLAY_NAME, MediaStore.MediaColumns.DATE_MODIFIED};

        final String orderBy = MediaStore.Images.Media.DATE_MODIFIED;
        imgCursor = getApplicationContext().getContentResolver().query(imgUri, projection, null, null, orderBy + " DESC");

        assert imgCursor != null;
        imgColumn_index_data = imgCursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
        imgColumn_index_folder_name = imgCursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
        imgColumn_index_dateAdd = imgCursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED);
        while (imgCursor.moveToNext()) {
            absolutePathOfImage = imgCursor.getString(imgColumn_index_data);
            String timeAdd = imgCursor.getString(imgColumn_index_dateAdd);
            long ltime = timeAdd != null ? Long.parseLong(timeAdd) : 0;
            Image newImg = new Image(absolutePathOfImage, ltime);
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

//        get all videos
        String absolutePathOfVideo;
        vidUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String[] vidProjection = {MediaStore.Video.Media.DATA, MediaStore.Video.Media.DURATION, MediaStore.Video.Media.DATE_MODIFIED, MediaStore.Video.Media.BUCKET_DISPLAY_NAME};

        final String vidOrderBy = MediaStore.Video.Media.DATE_MODIFIED;
        vidCursor = getApplicationContext().getContentResolver().query(vidUri, vidProjection, null, null, vidOrderBy + " DESC");

        assert vidCursor != null;
        vidColumn_index_data = vidCursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
        vidColumn_index_folder_name = vidCursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME);
        vidColumn_index_dateAdd = vidCursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED);
        vidColumn_index_duration = vidCursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);
        while (vidCursor.moveToNext()) {
            absolutePathOfVideo = vidCursor.getString(vidColumn_index_data);
            String timeAdd = vidCursor.getString(vidColumn_index_dateAdd);
            long ltime = timeAdd != null ? Long.parseLong(timeAdd) : 0;
            String dur = vidCursor.getString(vidColumn_index_duration);
            long ldur = timeAdd != null ? Long.parseLong(dur) : 0;
            @SuppressLint("DefaultLocale") String videoDuration = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(ldur),
                    TimeUnit.MILLISECONDS.toMinutes(ldur) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(ldur)),
                    TimeUnit.MILLISECONDS.toSeconds(ldur) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(ldur)));
            Video newVid = new Video(absolutePathOfVideo, ltime, videoDuration);
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
//        sort all data in each folder
        for (int i = 0; i < allMediaFolders.size(); i++) {
            ArrayList<DataItem> allFolderData = allMediaFolders.get(i).getListData();
            Collections.sort(allFolderData, new Comparator<DataItem>() {
                @Override
                public int compare(DataItem sv1, DataItem sv2) {
                    return (int) (sv2.getDateAdd() - sv1.getDateAdd());
                }
            });
            allMediaFolders.get(i).setListData(allFolderData);
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
