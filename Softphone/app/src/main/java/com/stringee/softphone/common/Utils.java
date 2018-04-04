package com.stringee.softphone.common;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.ContactsContract;
import android.view.Gravity;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.stringee.softphone.R;
import com.stringee.softphone.model.Contact;
import com.stringee.softphone.model.Recent;
import com.stringee.softphone.view.TextDrawable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by luannguyen on 7/12/2017.
 */

public class Utils {

    public static void hideKeyboard(Activity activity) {
        try {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void showKeyboard(Context context) {
        ((InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(
                InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    public static void reportMessage(Context context, String message) {
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        TextView v = (TextView) toast.getView().findViewById(android.R.id.message);
        if (v != null)
            v.setGravity(Gravity.CENTER);
        toast.show();
    }

    public static void reportMessage(Context context, int resId) {
        Toast toast = Toast.makeText(context, context.getString(resId), Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        TextView v = (TextView) toast.getView().findViewById(android.R.id.message);
        if (v != null)
            v.setGravity(Gravity.CENTER);
        toast.show();
    }

    public static boolean isPhoneNumber(String text) {
        if (text == null || text.length() == 0) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (!((c >= '0' && c <= '9') || c == '+')) {
                return false;
            }
        }
        return true;
    }

    public static byte[] fileToBytes(File file) {
        try {
            FileInputStream ios = new FileInputStream(file);
            int read = 0;
            byte[] bytes = new byte[(int) file.length()];
            byte[] buffer = new byte[4096];
            int total = 0;
            while ((read = ios.read(buffer)) != -1) {
                System.arraycopy(buffer, 0, bytes, total, read);
                total += read;
            }
            try {
                ios.close();
            } catch (Exception ex) {
            }
            return bytes;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    // Get all contacts
    public static List<Contact> getContactsFromDevice(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        List<Contact> deviceContacts = new ArrayList<>();
        Cursor cursor = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, null, null,
                ContactsContract.CommonDataKinds.Phone
                        .DISPLAY_NAME + " ASC");
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    String phoneNumber = cursor.getString(cursor
                            .getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    String displayName = cursor.getString(cursor
                            .getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone
                                    .DISPLAY_NAME));

                    Contact contact = new Contact();
                    contact.setName(displayName);
                    contact.setPhone(phoneNumber);
                    contact.setPhoneNo(phoneNumber);
                    deviceContacts.add(contact);
                } while (cursor.moveToNext());
            }
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        return deviceContacts;
    }

    public static List<Contact> genDeviceContactHeaders(List<Contact> contacts) {
        List<Contact> result = new ArrayList<>();
        if (contacts == null || contacts.size() == 0) {
            return new ArrayList<Contact>();
        }
        for (int i = contacts.size() - 1; i > 0; i--) {
            Contact contact1 = contacts.get(i);
            if (contact1.getType() == Constant.TYPE_CONTACT_HEADER) {
                continue;
            }
            Contact contact2 = contacts.get(i - 1);

            String name1 = contact1.getName();
            char c1 = name1.charAt(0);
            String name2 = contact2.getName();
            char c2 = name2.charAt(0);
            if (c1 != c2) {
                result.add(contact1);
                Contact contact = new Contact();
                contact.setType(Constant.TYPE_CONTACT_HEADER);
                contact.setName(String.valueOf(c1).toUpperCase());
                result.add(contact);
            } else {
                result.add(contact1);
            }
        }

        Contact contact0 = contacts.get(0);
        result.add(contact0);
        Contact contact = new Contact();
        contact.setType(Constant.TYPE_CONTACT_HEADER);
        contact.setName(contact0.getName().substring(0, 1).toUpperCase());
        result.add(contact);

        Collections.reverse(result);
        return result;
    }

    public static List<Recent> genHistoryHeader(List<Recent> recents, Context context) {
        if (recents == null || recents.size() == 0) {
            return recents;
        }
        List<Recent> result = new ArrayList<>();
        for (int i = recents.size() - 1; i > 0; i--) {
            Recent recent1 = recents.get(i);
            if (recent1.getType() == Constant.TYPE_TIME_HEADER) {
                continue;
            }
            Recent recent2 = recents.get(i - 1);

            String datetime1 = recent1.getDatetime();
            String datetime2 = recent2.getDatetime();
            if (!datetime1.substring(0, 8).equals(datetime2.substring(0, 8))) {
                result.add(recent1);
                Recent recent = new Recent();
                recent.setType(Constant.TYPE_TIME_HEADER);
                recent.setText(DateTimeUtils.getHeaderTime(datetime1, context));
                result.add(recent);
            } else {
                result.add(recent1);
            }
        }

        Recent recent0 = recents.get(0);
        result.add(recent0);
        Recent recent = new Recent();
        recent.setType(Constant.TYPE_TIME_HEADER);
        recent.setText(DateTimeUtils.getHeaderTime(recent0.getDatetime(), context));
        result.add(recent);

        Collections.reverse(result);
        return result;
    }

    public static String formatPhone(String phone) {
        if (phone != null) {
            phone = phone.replaceAll("\\s+", "");
            phone = phone.replace("+840", "84");
            phone = phone.replace("+84", "84");
            if (phone.startsWith("0")) {
                phone = "84" + phone.substring(1, phone.length());
            }
        }
        return phone;
    }

    public static void displayAvatar(Context context, String avatar, String text, ImageView imAvatar, int index) {
        if (text == null)
            return;
        String character = getCharacterOfContact(text);
        Bitmap bitmap = Utils.createBitmapByCharacter(character, index);
        if (avatar == null || avatar.equals("") || avatar.equalsIgnoreCase("null")) {
            imAvatar.setImageBitmap(bitmap);
        } else {
            BitmapDrawable bitmapDrawable = new BitmapDrawable(context.getResources(), bitmap);
            DisplayImageOptions options = new DisplayImageOptions.Builder().cacheInMemory(true).cacheOnDisk(true)
                    .resetViewBeforeLoading(true).showImageOnLoading(bitmapDrawable).showImageForEmptyUri(bitmapDrawable)
                    .showImageOnFail(bitmapDrawable).resetViewBeforeLoading(true).build();
            ImageLoader.getInstance().displayImage(avatar, imAvatar, options);
        }
    }

    public static String getCharacterOfContact(String text) {
        String character = "";
        if (text != null) {
            String[] nameArray = text.split("\\s+");
            if (nameArray.length > 1) {
                character = nameArray[0].substring(0, 1) + nameArray[nameArray.length - 1].substring(0, 1);
            } else {
                if (text.length() >= 2)
                    character = text.substring(0, 2);
                else if (text.length() >= 1)
                    character = text.substring(0, 1);
            }
        }
        return character.toUpperCase();
    }

    public static Bitmap createBitmapByCharacter(String character, int index) {
        ColorGenerator generator = ColorGenerator.DEFAULT;
        TextDrawable textDrawable = TextDrawable.builder().beginConfig().fontSize(40)
                .textColor(Common.context.getResources().getColor(R.color.white)).endConfig()
                .buildRound(character, generator.getColorByIndex(index));
        return drawableToBitmap(textDrawable);
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {

        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
        boolean isConnected = activeNetInfo != null && activeNetInfo.isConnected();
        return isConnected;
    }
}
