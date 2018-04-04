package com.stringee.softphone.activity;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.stringee.softphone.R;
import com.stringee.softphone.common.Constant;
import com.stringee.softphone.common.Utils;

/**
 * Created by luannguyen on 8/5/2017.
 */

public class CallUserInfoActivity extends MActivity {

    private WebView webView;
    private String url;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);

        url = getIntent().getStringExtra(Constant.PARAM_URL);

        initActionBar();
        initViews();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return true;
    }

    private void initActionBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(0xffffffff);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(R.string.user_info);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_back_white);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    private void initViews() {
        webView = (WebView) findViewById(R.id.web_content);

        startWebView(url);
    }

    private void startWebView(String url) {

        webView.setWebViewClient(new WebViewClient() {
            //If you will not use this method url links are opeen in new brower not in webview
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            //Show loader on url load
            public void onLoadResource(WebView view, String url) {
            }

            public void onPageFinished(WebView view, String url) {
                try {
                    dismissProgressDialog();
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }

        });
        // Javascript inabled on webview
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);

        webView.loadUrl(url);
    }
}
