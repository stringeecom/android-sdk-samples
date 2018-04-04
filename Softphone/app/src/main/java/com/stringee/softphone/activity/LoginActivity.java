package com.stringee.softphone.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.stringee.softphone.R;
import com.stringee.softphone.common.Constant;
import com.stringee.softphone.common.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by luannguyen on 7/10/2017.
 */

public class LoginActivity extends MActivity {

    private EditText etPhoneNo;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Button btnLogin = (Button) findViewById(R.id.btn_login);
        btnLogin.setOnClickListener(this);

        etPhoneNo = (EditText) findViewById(R.id.et_phone);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_login:
                if (isInputValid()) {
                    String phone = etPhoneNo.getText().toString().trim();
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(getString(R.string.phone_confirm, phone));
                    builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    });
                    builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            showProgressDialog(R.string.executing);
                            register();
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
                break;
        }
    }

    private boolean isInputValid() {
        String phone = etPhoneNo.getText().toString().trim();
        if (phone.length() == 0) {
            Utils.reportMessage(this, getString(R.string.input_invalid));
            return false;
        }

        if (phone.contains(" ")) {
            Utils.reportMessage(this, getString(R.string.username_invalid));
            return false;
        }

        return true;
    }

    private void register() {
        String url = Constant.URL_BASE + Constant.URL_LOGIN;
        RequestQueue queue = Volley.newRequestQueue(this);
        final String text = etPhoneNo.getText().toString().trim();
        final String phone = Utils.formatPhone(text);

        Map<String, String> params = new HashMap();
        params.put("phone", phone);
        JSONObject jsonObject = new JSONObject(params);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url
                , jsonObject, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                dismissProgressDialog();
                try {
                    int status = response.getInt("status");
                    if (status == 200) {
                        Intent intent = new Intent(LoginActivity.this, EnterCodeActivity.class);
                        intent.putExtra("text", text);
                        intent.putExtra("phone", phone);
                        startActivity(intent);
                    } else {
                        Utils.reportMessage(LoginActivity.this, R.string.error_occured);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Utils.reportMessage(LoginActivity.this, R.string.error_occured);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                dismissProgressDialog();
                Utils.reportMessage(LoginActivity.this, R.string.error_occured);
            }
        });
        queue.add(request);
    }
}
