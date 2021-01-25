package com.stringee.chat.ui.kit.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import androidx.appcompat.app.ActionBar;

import com.stringee.chat.ui.kit.adapter.FileAdapter;
import com.stringee.chat.ui.kit.commons.utils.FileUtils;
import com.stringee.chat.ui.kit.model.StringeeFile;
import com.stringee.stringeechatuikit.BaseActivity;
import com.stringee.stringeechatuikit.R;
import com.stringee.stringeechatuikit.common.CallBack;
import com.stringee.stringeechatuikit.common.DataHandler;
import com.stringee.stringeechatuikit.common.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SelectFileActivity extends BaseActivity implements CallBack {
    private List<StringeeFile> files = new ArrayList<StringeeFile>();
    private ListView lvFile;
    private FileAdapter fileAdapter;
    int count = 0;
    String pathCurrentFile;
    private int level = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_file);
        initActionbar();
        setComponentView();
        setListenerView();
        initData();
    }

    @Override
    public void onBackPressed() {
        level--;
        if (count > 1) {
            File file = new File(pathCurrentFile);
            getFiles(file.getParent());
            count--;
        } else if (count == 1) {
            initData();
        } else {
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void initActionbar() {
        ActionBar mActionBar = getSupportActionBar();
        mActionBar.setTitle(R.string.select_file);
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setHomeButtonEnabled(true);
    }

    private void setComponentView() {
        lvFile = (ListView) findViewById(R.id.lv_file);
    }

    private void setListenerView() {
        lvFile.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                StringeeFile file = (StringeeFile) fileAdapter.getItem(position);
                // check directory
                if (file.getType() == StringeeFile.TYPE_BACK) {
                    onBackPressed();
                } else if (file.getType() == StringeeFile.TYPE_DIRECTORY) {
                    level++;
                    getFiles(file.getPath());
                    count++;
                } else {
                    Intent intent = getIntent();
                    intent.putExtra("path", file.getPath());
                    setResult(RESULT_OK, intent);
                    finish();
                }
            }
        });
    }

    private void initData() {
        count = 0;
        getSupportActionBar().setTitle(R.string.select_file);

        files = new ArrayList<StringeeFile>();
        String storagePath = FileUtils.getStorage(getBaseContext());
        StringeeFile storageFile = new StringeeFile();
        storageFile.setLevel(0);
        storageFile.setPath(storagePath);
        storageFile.setName(getString(R.string.stringee_internal_storage));
        storageFile.setType(StringeeFile.TYPE_DIRECTORY);
        files.add(storageFile);

        String stPath = Utils.getAppDirectory(getBaseContext()).getAbsolutePath();
        StringeeFile stFile = new StringeeFile();
        storageFile.setLevel(0);
        stFile.setPath(stPath);
        stFile.setName(getString(R.string.app_name));
        stFile.setType(StringeeFile.TYPE_DIRECTORY);
        files.add(stFile);

        if (files == null)
            return;
        if (fileAdapter == null) {
            fileAdapter = new FileAdapter(getBaseContext(), files);
            lvFile.setAdapter(fileAdapter);
        } else {
            fileAdapter.setFiles(files);
        }
    }

    private void getFiles(String folder) {
        pathCurrentFile = folder;
//        showProgress(getString(R.string.loading));
        Object[] params = new Object[2];
        params[1] = folder;
        DataHandler dataHandler = new DataHandler(this, this);
        dataHandler.execute(params);
    }

    @Override
    public void start() {
    }

    @Override
    public void doWork(Object... params) {
        String folder = (String) params[1];
        files = new ArrayList<StringeeFile>();
        StringeeFile stringeFile = new StringeeFile();
        stringeFile.setPath(folder);
        stringeFile.setLevel(level);
        stringeFile.setType(StringeeFile.TYPE_BACK);
        stringeFile.setName("...");
        files.add(stringeFile);
        List<StringeeFile> fs = FileUtils.getFiles(folder, null, 1);
        if (fs != null && fs.size() > 0)
            for (int i = 0; i < fs.size(); i++) {
                StringeeFile file = fs.get(i);
                file.setLevel(level);
                files.add(file);
            }
    }

    @Override
    public void end(Object[] params) {
//        dismissProgress();
        if (files == null)
            return;
        if (files.size() == 0)
            return;
        if (fileAdapter == null) {
            fileAdapter = new FileAdapter(getBaseContext(), files);
            lvFile.setAdapter(fileAdapter);
        } else
            fileAdapter.setFiles(files);
        getSupportActionBar().setTitle(pathCurrentFile);
    }
}
