package com.stringee.apptoappcallsample.utils;

import android.content.Context;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by luannguyen on 10/27/2017.
 */

public class Utils {

    public static void reportMessage(Context context, String message) {
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        TextView v = (TextView) toast.getView().findViewById(android.R.id.message);
        if (v != null)
            v.setGravity(Gravity.CENTER);
        toast.show();
    }
}
