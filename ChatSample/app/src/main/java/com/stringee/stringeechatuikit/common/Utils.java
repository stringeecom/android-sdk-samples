package com.stringee.stringeechatuikit.common;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.ContactsContract;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.stringee.chat.ui.kit.commons.Constant;
import com.stringee.chat.ui.kit.commons.DownloadFileService;
import com.stringee.chat.ui.kit.fragment.ConversationListFragment;
import com.stringee.chat.ui.kit.model.Contact;
import com.stringee.listener.StatusListener;
import com.stringee.messaging.Conversation;
import com.stringee.messaging.Message;
import com.stringee.messaging.User;
import com.stringee.stringeechatuikit.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by luannguyen on 3/23/2016.
 */
public class Utils {

    private static final String JUST_NOW = "Just now";
    private static final String MINUTES = " mins";
    private static final String HOURS = " hrs";
    private static final String H = "h";
    private static final String AGO = " ago";
    private static final String YESTERDAY = "Yesterday";

    public static void hideKeyboard(Activity activity) {
        try {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void hideKeyboard(Activity activity, View view) {
        try {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void showKeyboard(Context context) {
        ((InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(
                InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    public static String md5(String str) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(str.getBytes());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < array.length; ++i) {
                sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1, 3));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            return "";
        }
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

    /**
     * Get free call time
     *
     * @param currentTime
     * @param startTime
     * @return
     */
    public static String getCallTime(long currentTime, long startTime) {
        long time = currentTime - startTime;
        SimpleDateFormat format = new SimpleDateFormat("mm:ss");
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        return format.format(new Date(time));
    }

    /**
     * Get free call time from duration
     *
     * @param duration
     * @return
     */
    public static String getAudioTime(long duration) {
        SimpleDateFormat format = new SimpleDateFormat("mm:ss");
        String res = format.format(new Date(duration));
        return res == null ? "00:00" : res;
    }

    public static String getConversationTime(long duration) {
        SimpleDateFormat format = new SimpleDateFormat("dd/MM hh:mm:ss");
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        String res = format.format(new Date(duration));
        return res == null ? "00:00" : res;
    }

    public static String getFormattedDate(Long timestamp) {
        Date date = new Date(timestamp);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
        return simpleDateFormat.format(date);
    }

    public static boolean isSameDay(Long timestamp) {
        Calendar calendarForCurrent = Calendar.getInstance();
        Calendar calendarForScheduled = Calendar.getInstance();
        Date currentDate = new Date();
        Date date = new Date(timestamp);
        calendarForCurrent.setTime(currentDate);
        calendarForScheduled.setTime(date);
        return calendarForCurrent.get(Calendar.YEAR) == calendarForScheduled.get(Calendar.YEAR) &&
                calendarForCurrent.get(Calendar.DAY_OF_YEAR) == calendarForScheduled.get(Calendar.DAY_OF_YEAR);
    }

    public static boolean isSameYear(Long timestamp) {
        Calendar calendarForCurrent = Calendar.getInstance();
        Calendar calendarForScheduled = Calendar.getInstance();
        Date currentDate = new Date();
        Date date = new Date(timestamp);
        calendarForCurrent.setTime(currentDate);
        calendarForScheduled.setTime(date);
        return calendarForCurrent.get(Calendar.YEAR) == calendarForScheduled.get(Calendar.YEAR);
    }

    public static long daysBetween(Date startDate, Date endDate) {
        Calendar sDate = getDatePart(startDate);
        Calendar eDate = getDatePart(endDate);

        long daysBetween = 0;
        while (sDate.before(eDate)) {
            sDate.add(Calendar.DAY_OF_MONTH, 1);
            daysBetween++;
        }
        return daysBetween;
    }

    public static Calendar getDatePart(Date date) {
        Calendar cal = Calendar.getInstance();       // get calendar instance
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);            // set hour to midnight
        cal.set(Calendar.MINUTE, 0);                 // set minute in hour
        cal.set(Calendar.SECOND, 0);                 // set second in minute
        cal.set(Calendar.MILLISECOND, 0);            // set millisecond in second

        return cal;                                  // return the date part
    }

    public static String getFormattedDateAndTime(Long timestamp) {
        boolean sameDay = isSameDay(timestamp);
        Date date = new Date(timestamp);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
        SimpleDateFormat fullDateFormat = new SimpleDateFormat("dd MMM");

        try {
            if (sameDay) {
                return simpleDateFormat.format(date);
            }
            return fullDateFormat.format(date);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean hasMarshmallow() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    public static String getMetaDataValue(Context context, String metaDataName) {
        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            if (ai.metaData != null) {
                return ai.metaData.getString(metaDataName);

            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    public static boolean hasHoneycombMR1() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1;
    }

    public static int getLauncherIcon(Context context) {
        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            return ai.icon;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static File getAppDirectory(Context context) {
        File picDir = null;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            String appName = context.getApplicationInfo().loadLabel(context.getPackageManager()).toString();
            if (appName == null || appName.isEmpty()) {
                appName = context.getString(R.string.app_name);
            }
            picDir = new File(Environment.getExternalStorageDirectory(), appName);
            if (!picDir.exists()) {
                if (!picDir.mkdirs()) {
                    return context.getCacheDir();
                }
            }
        }
        if (picDir == null) {
            picDir = context.getCacheDir();
        }
        return picDir;
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

    public static void downloadAttachment(String url, String dest, StatusListener listener) {
        DownloadFileService downloadFileService = new DownloadFileService(url, dest, listener);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            downloadFileService.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            downloadFileService.execute();
        }
    }

    public static void showSoftKeyboard(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
    }

    public static void hideSoftKeyboardWithEditText(Context context, EditText editText) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }

    public static String getCreator(Conversation conversation) {
        String creator = conversation.getCreator();
        if (creator == null) {
            return "";
        }
        List<User> participants = conversation.getParticipants();
        if (participants != null && participants.size() > 0) {
            for (int i = 0; i < participants.size(); i++) {
                User user = participants.get(i);
                if (user.getUserId().equals(creator)) {
                    if (user.getName() != null && user.getName().trim().length() > 0) {
                        return user.getName();
                    } else {
                        return user.getUserId();
                    }
                }
            }
        }
        return "";
    }


    public static String getConversationName(Context context, Conversation conversation) {
        String convName = conversation.getName();
        String myUserId = PrefUtils.getString(com.stringee.stringeechatuikit.common.Constant.PREF_USER_ID, "");
        if (convName == null || convName.length() == 0) {
            convName = "";
            List<User> participants = conversation.getParticipants();
            for (int j = 0; j < participants.size(); j++) {
                String userId = participants.get(j).getUserId();
                if (!conversation.isGroup() && myUserId != null && myUserId.equals(userId)) {
                    continue;
                } else {
                    String name = participants.get(j).getName();
                    if (userId.equals(myUserId)) {
                        name = context.getString(R.string.you);
                    }
                    if (name != null && name.trim().length() > 0) {
                        convName = convName + name + ",";
                    } else {
                        convName = convName + userId + ",";
                    }
                }
            }
            if (convName.length() > 0) {
                convName = convName.substring(0, convName.length() - 1);
            }
        }
        return convName;
    }

    public static String getNotificationText(Context context, Conversation conversation, String text) {
        if (text == null) {
            return "";
        }
        try {
            JSONObject jsonObject = new JSONObject(text);
            int notiType = jsonObject.getInt("type");
            if (notiType == 1 || notiType == 2) {
                JSONArray parsArray = jsonObject.getJSONArray("participants");
                String strPars = "";
                for (int j = 0; j < parsArray.length(); j++) {
                    JSONObject parObject = parsArray.getJSONObject(j);
                    String userId = parObject.getString("user");
                    String name = parObject.optString("displayName");
                    if (name.trim().length() == 0) {
                        name = getParticipantName(conversation, userId);
                    }
                    if (name.trim().length() == 0) {
                        name = userId;
                    }
                    strPars = strPars + name + ",";
                }
                strPars = strPars.substring(0, strPars.length() - 1);

                String actor;
                if (notiType == 1) {
                    actor = jsonObject.getString("addedby");
                    String actorName = getParticipantName(conversation, actor);
                    if (actorName.trim().length() == 0) {
                        actorName = actor;
                    }
                    text = context.getString(R.string.add_participants_to_group, actorName, strPars);
                } else {
                    actor = jsonObject.getString("removedBy");
                    String actorName = getParticipantName(conversation, actor);
                    if (actorName.trim().length() == 0) {
                        actorName = actor;
                    }
                    text = context.getString(R.string.remove_participants, actorName, strPars);
                }
            } else if (notiType == 3) {
                String groupName = jsonObject.getString("groupName");
                text = context.getString(R.string.rename_group_to, groupName);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return text;
    }

    private static String getParticipantName(Conversation conversation, String userId) {
        List<User> participants = conversation.getParticipants();
        if (participants != null && participants.size() > 0) {
            for (int i = 0; i < participants.size(); i++) {
                User user = participants.get(i);
                if (user.getUserId().equals(userId)) {
                    if (user.getName() != null && user.getName().trim().length() > 0) {
                        return user.getName();
                    } else {
                        return user.getUserId();
                    }
                }
            }
        }
        return "";
    }

    public static String getMsgNotification(Context context, Message message) {
        String convId = message.getConversationId();
        for (int i = 0; i < ConversationListFragment.conversationList.size(); i++) {
            Conversation conversation = ConversationListFragment.conversationList.get(i);
            if (conversation.getId().equals(convId)) {
                return getNotificationText(context, conversation, message.getText());
            }
        }
        return "";
    }

    public static String getCreator(String userId, String convId) {
        for (int i = 0; i < ConversationListFragment.conversationList.size(); i++) {
            Conversation conversation = ConversationListFragment.conversationList.get(i);
            if (conversation.getId().equals(convId)) {
                List<User> participants = conversation.getParticipants();
                if (participants != null && participants.size() > 0) {
                    for (int j = 0; j < participants.size(); j++) {
                        User user = participants.get(j);
                        if (user.getUserId().equals(userId)) {
                            if (user.getName() != null && user.getName().trim().length() > 0) {
                                return user.getName();
                            } else {
                                return user.getUserId();
                            }
                        }
                    }
                }
            }
        }
        return "";
    }
}
