package com.stringee.softphone.fragment;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import com.stringee.listener.StatusListener;
import com.stringee.softphone.R;
import com.stringee.softphone.activity.IncomingCallActivity;
import com.stringee.softphone.activity.OutgoingCallActivity;
import com.stringee.softphone.common.CallBack;
import com.stringee.softphone.common.Common;
import com.stringee.softphone.common.Constant;
import com.stringee.softphone.common.DataHandler;
import com.stringee.softphone.common.Notify;
import com.stringee.softphone.common.Utils;
import com.stringee.softphone.model.Message;

/**
 * Created by luannguyen on 7/12/2017.
 */

public class DialFragment extends Fragment implements View.OnClickListener, CallBack {

    private EditText etPhone;
    private ImageButton btnClear;

    private String text = "";
    private boolean isInCall = false;
    private boolean outgoing;

    private final String GET_LAST_OUTGOING_CALL = "get_last_outgoing_call";

    public DialFragment() {

    }

    @Override
    public void onCreate(Bundle savedIntanceState) {
        super.onCreate(savedIntanceState);
        Bundle args = getArguments();
        if (args != null) {
            isInCall = args.getBoolean("isInCall");
            outgoing = args.getBoolean("outgoing");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_dial, container, false);

        ImageButton btnAnswer = (ImageButton) layout.findViewById(R.id.btn_call);
        btnAnswer.setOnClickListener(this);
        if (isInCall) {
            btnAnswer.setBackgroundResource(R.drawable.btn_end_call_selector);
        }

        View btnDial = layout.findViewById(R.id.btn_hide);
        btnDial.setOnClickListener(this);

        btnClear = (ImageButton) layout.findViewById(R.id.btn_clear);
        btnClear.setOnClickListener(this);

        Button btn0 = (Button) layout.findViewById(R.id.btn_0);
        btn0.setOnClickListener(this);

        Button btn1 = (Button) layout.findViewById(R.id.btn_1);
        btn1.setOnClickListener(this);

        Button btn2 = (Button) layout.findViewById(R.id.btn_2);
        btn2.setOnClickListener(this);

        Button btn3 = (Button) layout.findViewById(R.id.btn_3);
        btn3.setOnClickListener(this);

        Button btn4 = (Button) layout.findViewById(R.id.btn_4);
        btn4.setOnClickListener(this);

        Button btn5 = (Button) layout.findViewById(R.id.btn_5);
        btn5.setOnClickListener(this);

        Button btn6 = (Button) layout.findViewById(R.id.btn_6);
        btn6.setOnClickListener(this);

        Button btn7 = (Button) layout.findViewById(R.id.btn_7);
        btn7.setOnClickListener(this);

        Button btn8 = (Button) layout.findViewById(R.id.btn_8);
        btn8.setOnClickListener(this);

        Button btn9 = (Button) layout.findViewById(R.id.btn_9);
        btn9.setOnClickListener(this);

        Button btnSao = (Button) layout.findViewById(R.id.btn_sao);
        btnSao.setOnClickListener(this);

        Button btnThang = (Button) layout.findViewById(R.id.btn_thang);
        btnThang.setOnClickListener(this);

        Typeface typeface = Typeface.createFromAsset(getActivity().getAssets(), "font/Roboto-Light.ttf");
        btn0.setTypeface(typeface);
        btn1.setTypeface(typeface);
        btn2.setTypeface(typeface);
        btn3.setTypeface(typeface);
        btn4.setTypeface(typeface);
        btn5.setTypeface(typeface);
        btn6.setTypeface(typeface);
        btn7.setTypeface(typeface);
        btn8.setTypeface(typeface);
        btn9.setTypeface(typeface);
        btnSao.setTypeface(typeface);
        btnThang.setTypeface(typeface);

        etPhone = (EditText) layout.findViewById(R.id.et_phone);
        etPhone.setTypeface(typeface);
        if (android.os.Build.VERSION.SDK_INT >= 11) {
            etPhone.setRawInputType(InputType.TYPE_CLASS_TEXT);
            etPhone.setTextIsSelectable(true);
        } else {
            etPhone.setRawInputType(InputType.TYPE_NULL);
            etPhone.setFocusable(true);
        }
        etPhone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String s = editable.toString();
                if (s.trim().length() > 0) {
                    btnClear.setVisibility(View.VISIBLE);
                } else {
                    btnClear.setVisibility(View.GONE);
                }
                text = s;
            }
        });

        layout.setOnClickListener(null);
        return layout;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_call:
                if (isInCall) {
                    Fragment dialFragment = getActivity().getSupportFragmentManager().findFragmentByTag("DIAL_IN_CALL");
                    if (dialFragment != null) {
                        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
                        ft.setCustomAnimations(R.anim.slide_in_up, R.anim.slide_out_up);
                        ft.remove(dialFragment).commit();
                    }

                    Intent intent = new Intent(Notify.END_CALL_FROM_DIAL.getValue());
                    LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                } else {
                    String phone = etPhone.getText().toString();
                    if (phone.length() > 0) {
                        if (Utils.isNetworkAvailable(getActivity())) {
                            Intent intent = new Intent(getActivity(), OutgoingCallActivity.class);
                            intent.putExtra(Constant.PARAM_PHONE, phone);
                            intent.putExtra(Constant.PARAM_PHONE_NO, phone);
                            intent.putExtra(Constant.PARAM_CALLOUT, true);
                            startActivity(intent);
                        } else {
                            Utils.reportMessage(getActivity(), R.string.network_required);
                        }
                    } else {
                        getLastOutgoingCall();
                    }
                }
                break;
            case R.id.btn_hide:
                Fragment dialFragment;
                if (isInCall) {
                    dialFragment = getActivity().getSupportFragmentManager().findFragmentByTag("DIAL_IN_CALL");
                } else {
                    dialFragment = getActivity().getSupportFragmentManager().findFragmentByTag("DIAL");
                }
                if (dialFragment != null) {
                    FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
                    ft.setCustomAnimations(R.anim.slide_in_up, R.anim.slide_out_up);
                    ft.remove(dialFragment).commit();
                }
                break;
            case R.id.btn_0:
                setPhone("0");
                btnClear.setVisibility(View.VISIBLE);
                break;
            case R.id.btn_1:
                setPhone("1");
                btnClear.setVisibility(View.VISIBLE);
                break;
            case R.id.btn_2:
                setPhone("2");
                btnClear.setVisibility(View.VISIBLE);
                break;
            case R.id.btn_3:
                setPhone("3");
                btnClear.setVisibility(View.VISIBLE);
                break;
            case R.id.btn_4:
                setPhone("4");
                btnClear.setVisibility(View.VISIBLE);
                break;
            case R.id.btn_5:
                setPhone("5");
                btnClear.setVisibility(View.VISIBLE);
                break;
            case R.id.btn_6:
                setPhone("6");
                btnClear.setVisibility(View.VISIBLE);
                break;
            case R.id.btn_7:
                setPhone("7");
                btnClear.setVisibility(View.VISIBLE);
                break;
            case R.id.btn_8:
                setPhone("8");
                btnClear.setVisibility(View.VISIBLE);
                break;
            case R.id.btn_9:
                setPhone("9");
                btnClear.setVisibility(View.VISIBLE);
                break;
            case R.id.btn_sao:
                setPhone("*");
                btnClear.setVisibility(View.VISIBLE);
                break;
            case R.id.btn_thang:
                setPhone("#");
                btnClear.setVisibility(View.VISIBLE);
                break;
            case R.id.btn_clear:
                int index = etPhone.getSelectionStart();
                if (index > 0) {
                    StringBuilder builder = new StringBuilder(text);
                    builder.deleteCharAt(index - 1);
                    text = builder.toString();
                    etPhone.setText(text);
                    etPhone.setSelection(index - 1);
                    if (text.length() == 0) {
                        btnClear.setVisibility(View.INVISIBLE);
                    }
                }
                break;
        }
    }

    /**
     * Get device contacts
     */
    private void getLastOutgoingCall() {
        if (getActivity() != null) {
            Object[] params = new Object[2];
            params[0] = GET_LAST_OUTGOING_CALL;
            DataHandler handler = new DataHandler(getActivity(), this);
            handler.execute(params);
        }
    }

    private Message doGetLastOutgoingCall() {
        return Common.messageDb.getLastOutgoingCall();
    }

    private void doneGetLastOutgoingCall(Message message) {
        if (message != null) {
            String phone = message.getPhoneNumber();
            if (phone != null) {
                text = phone;
                etPhone.setText(text);
                etPhone.setSelection(phone.length());
                btnClear.setVisibility(View.VISIBLE);
            }
        }
    }

    private void setPhone(String c) {
        if (isInCall) {
            sendDTMF(c);
        }
        int index = etPhone.getSelectionStart();
        if (index < text.length()) {
            String pre = text.substring(0, index);
            String after = text.substring(index, text.length() - index);
            text = pre + c + after;
        } else {
            text = text + c;
        }
        etPhone.setText(text);
        etPhone.setSelection(index + 1);
    }

    @Override
    public void start() {

    }

    @Override
    public void doWork(Object... params) {
        String action = (String) params[0];
        if (action.equals(GET_LAST_OUTGOING_CALL)) {
            params[1] = doGetLastOutgoingCall();
        }
    }

    @Override
    public void end(Object[] params) {
        String action = (String) params[0];
        if (action.equals(GET_LAST_OUTGOING_CALL)) {
            doneGetLastOutgoingCall((Message) params[1]);
        }
    }

    private void sendDTMF(String s) {
        if (outgoing) {
            if (OutgoingCallActivity.outgoingCall != null) {
                OutgoingCallActivity.outgoingCall.sendDTMF(s, new StatusListener() {
                    @Override
                    public void onSuccess() {
                        Log.d("Stringee", "sendDTMF onSuccess");
                    }
                });
            }
        } else {
            if (IncomingCallActivity.incomingCall != null) {
                IncomingCallActivity.incomingCall.sendDTMF(s, new StatusListener() {
                    @Override
                    public void onSuccess() {
                        Log.d("Stringee", "sendDTMF onSuccess");
                    }
                });
            }
        }
    }
}
