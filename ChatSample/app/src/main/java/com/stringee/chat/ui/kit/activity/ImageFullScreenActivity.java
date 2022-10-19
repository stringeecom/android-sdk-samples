package com.stringee.chat.ui.kit.activity;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;

import androidx.appcompat.widget.Toolbar;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.ortiz.touchview.TouchImageView;
import com.stringee.chat.ui.kit.commons.LocalImageLoader;
import com.stringee.chat.ui.kit.commons.utils.FileUtils;
import com.stringee.chat.ui.kit.commons.utils.FileUtils.FileType;
import com.stringee.listener.StatusListener;
import com.stringee.messaging.Message;
import com.stringee.stringeechatuikit.R;
import com.stringee.stringeechatuikit.common.Common;
import com.stringee.stringeechatuikit.common.Utils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ImageFullScreenActivity extends androidx.appcompat.app.AppCompatActivity {

    private TouchImageView photoImageView;
    private ProgressBar progressBar;

    private Message message;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_full_image);

        message = (Message) getIntent().getSerializableExtra("message");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().show();
        showUi();

        photoImageView = findViewById(R.id.photoImageView);

        photoImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getSupportActionBar().isShowing()) {
                    getSupportActionBar().hide();
                } else {
                    getSupportActionBar().show();
                }
            }
        });
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {

                @Override
                public void onSystemUiVisibilityChange(int visibility) {
                    if (visibility == 0) {
                        getSupportActionBar().show();
                    }
                }
            });
        }

        String fileUrl = message.getFileUrl();
        String filePath = message.getFilePath();
        ImageLoader imageLoader = ImageLoader.getInstance();
        if (filePath != null && filePath.trim().length() > 0) {
            fileUrl = "file://" + filePath;
            imageLoader = LocalImageLoader.getInstance();
        }


        DisplayImageOptions options = new DisplayImageOptions.Builder().cacheInMemory(true).cacheOnDisk(true)
                .resetViewBeforeLoading(true).resetViewBeforeLoading(true).build();
        imageLoader.displayImage(fileUrl, photoImageView, options, new ImageLoadingListener() {

            @Override
            public void onLoadingStarted(String imageUri, View view) {
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onLoadingCancelled(String imageUri, View view) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.menu_save_photo:
                savePhoto();
                break;
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_view_photo, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void showUi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
            }
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    private void savePhoto() {
        String filePath = message.getFilePath();
        if (filePath == null || filePath.length() == 0) {
            String url = message.getFileUrl();
            if (url != null && url.length() > 0) {
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String imageFileName = "photo_" + timeStamp + ".jpeg";
                File file = FileUtils.getFilePath(imageFileName, this, FileType.IMAGE);
                final String dest = file.getAbsolutePath();
                Utils.downloadAttachment(url, dest, new StatusListener() {
                    @Override
                    public void onSuccess() {
                        message.setFilePath(dest);
                        if (Common.client != null) {
                            Common.client.updateAttachment(message);
                        }
                        galleryAddPic(dest);
                    }
                });
            }
        } else {
            galleryAddPic(filePath);
        }
    }

    private void galleryAddPic(String path) {
        ContentValues values = new ContentValues();

        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.MediaColumns.DATA, path);

        getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(path);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        sendBroadcast(mediaScanIntent);
    }
}
