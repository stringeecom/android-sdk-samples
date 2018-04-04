package com.stringee.softphone.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.stringee.softphone.R;
import com.stringee.softphone.common.Constant;
import com.stringee.softphone.common.PrefUtils;
import com.stringee.softphone.common.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by luannguyen on 3/20/2018.
 */

public class EnterCodeActivity extends MActivity {

    private TextView tvInfo;
    private EditText etCode;
    private String phone;
    private String text;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_enter_code);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            phone = extras.getString("phone");
            text = extras.getString("text");
        }


        etCode = (EditText) findViewById(R.id.et_code);
        tvInfo = (TextView) findViewById(R.id.tv_info);
        if (text != null) {
            tvInfo.setText(getString(R.string.code_sent_to, text));
        }

        etCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String code = editable.toString();
                if (code.length() == 5) {
                    showProgressDialog(R.string.executing);
                    confirm();
                }
            }
        });

        ImageButton btnBack = (ImageButton) findViewById(R.id.btn_back);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }

    private void confirm() {
        String url = Constant.URL_BASE + Constant.URL_CONFIRM;
        final String code = etCode.getText().toString();
        Map<String, String> params = new HashMap();
        params.put("phone", phone);
        params.put("code", code);
        JSONObject jsonObject = new JSONObject(params);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url
                , jsonObject, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                dismissProgressDialog();
                try {
                    int status = response.getInt("status");
                    if (status == 200) {
                        PrefUtils.getInstance(EnterCodeActivity.this).putBoolean(Constant.PREF_LOGINED, true);
                        PrefUtils.getInstance(EnterCodeActivity.this).putString(Constant.PREF_USER_ID, text);
                        JSONObject dataObject = response.getJSONObject("data");
                        String token = dataObject.getString("token");
                        String accessToken = dataObject.getString("access_token");
                        long expiredTime = dataObject.getLong("expire_time");
                        JSONArray numbers = dataObject.getJSONArray("callOutNumber");
                        if (numbers != null && numbers.length() > 0) {
                            PrefUtils.getInstance(EnterCodeActivity.this).putString(Constant.PREF_SELECTED_NUMBER, numbers.getString(0));
                        }
                        PrefUtils.getInstance(EnterCodeActivity.this).putString(Constant.PREF_TOKEN, token);
                        PrefUtils.getInstance(EnterCodeActivity.this).putString(Constant.PREF_ACCESS_TOKEN, accessToken);
                        PrefUtils.getInstance(EnterCodeActivity.this).putLong(Constant.PREF_EXPIRED_TIME, expiredTime);
                        PrefUtils.getInstance(EnterCodeActivity.this).putString(Constant.PREF_SIP_NUMBERS, numbers.toString());
                        Intent intent = new Intent(EnterCodeActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Utils.reportMessage(EnterCodeActivity.this, R.string.error_occured);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Utils.reportMessage(EnterCodeActivity.this, R.string.error_occured);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                dismissProgressDialog();
                Utils.reportMessage(EnterCodeActivity.this, R.string.error_occured);
            }
        });
        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }
}
