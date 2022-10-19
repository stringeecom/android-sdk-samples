package com.stringee.chat.ui.kit.commons.utils;

import android.content.Context;
import android.content.ContextWrapper;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.OpenableColumns;
import android.util.Log;

import com.stringee.chat.ui.kit.model.Contact;
import com.stringee.chat.ui.kit.model.StringeeFile;
import com.stringee.stringeechatuikit.common.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.PatternSyntaxException;

public class FileUtils {

    public static final String MAIN_FOLDER_META_DATA = "main_folder_name";
    public static final String STRINGEE_IMAGES_FOLDER = "/Images";
    public static final String STRINGEE_VIDEOS_FOLDER = "/Video";
    private static final String STRINGEE_AUDIO_FOLDER = "/audio";
    private static final String STRINGEE_CONTACT_FOLDER = "/contact";
    public static final String STRINGEE_OTHER_FILES_FOLDER = "/Documents";
    public static final String STRINGEE_THUMBNAIL_SUFIX = "/.Thumbnail";
    public static final String STRINGEE_STICKER_FOLDER = "/Sticker";
    public static final String STRINGEE_AUDIOS_FOLDER = "/Audio";
    public static final String IMAGE_DIR = "image";
    private static final String TAG = "FileUtils";

    public enum FileType {
        IMAGE,
        VIDEO,
        AUDIO,
        CONTACT,
        STICKER,
        OTHER,
    }

    public static File getFilePath(String fileName, Context context, FileType fileType) {
        return new File(getSaveFileDir(context, fileType), fileName);
    }

    public static File getSaveFileDir(Context context, FileType fileType) {
        File dir;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            String folder = "/";
            if (VERSION.SDK_INT >= VERSION_CODES.R) {
                folder = folder + Utils.getMetaDataValue(context, MAIN_FOLDER_META_DATA);
                switch (fileType) {
                    case IMAGE:
                        dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath() + folder);
                        break;
                    case VIDEO:
                        dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath() + folder);
                        break;
                    case AUDIO:
                        dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_AUDIOBOOKS).getAbsolutePath() + folder);
                        break;
                    case CONTACT:
                        dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + folder);
                        break;
                    case OTHER:
                    default:
                        dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + folder);
                        break;
                }
            } else {
                switch (fileType) {
                    case IMAGE:
                        folder = folder + Utils.getMetaDataValue(context, MAIN_FOLDER_META_DATA) + STRINGEE_IMAGES_FOLDER;
                        break;
                    case VIDEO:
                        folder = folder + Utils.getMetaDataValue(context, MAIN_FOLDER_META_DATA) + STRINGEE_VIDEOS_FOLDER;
                        break;
                    case AUDIO:
                        folder = folder + Utils.getMetaDataValue(context, MAIN_FOLDER_META_DATA) + STRINGEE_AUDIO_FOLDER;
                        break;
                    case CONTACT:
                        folder = folder + Utils.getMetaDataValue(context, MAIN_FOLDER_META_DATA) + STRINGEE_CONTACT_FOLDER;
                        break;
                    case OTHER:
                    default:
                        folder = folder + Utils.getMetaDataValue(context, MAIN_FOLDER_META_DATA) + STRINGEE_OTHER_FILES_FOLDER;
                        break;
                }
                dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + folder);
            }

            if (!dir.exists()) {
                dir.mkdirs();
            }
        } else {
            ContextWrapper cw = new ContextWrapper(context);
            // path to /data/data/yourapp/app_data/imageDir
            dir = cw.getDir(IMAGE_DIR, Context.MODE_PRIVATE);
        }
        if (fileType== FileType.IMAGE || fileType== FileType.VIDEO || fileType== FileType.STICKER) {
            File noMediaFile = new File(dir, ".nomedia");
            if (!noMediaFile.exists()) {
                try {
                    noMediaFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return dir;
    }

    public Bitmap loadMessageImage(Context context, String url) {
        try {
            Bitmap attachedImage = null;

            if (attachedImage == null) {
                InputStream in = new java.net.URL(url).openStream();
                if (in != null) {
                    attachedImage = BitmapFactory.decodeStream(in);
                }
            }
            return attachedImage;
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            Log.e(TAG, "File not found on server: " + ex.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.e(TAG, "Exception fetching file from server: " + ex.getMessage());
        }

        return null;
    }

    /**
     * @param contactData
     *
     * @return
     */
    public static String vCard(Uri contactData, Context context) throws Exception {
        Cursor cursor = context.getContentResolver().query(contactData, null, null, null, null);
        cursor.moveToFirst();
        String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
        String lookupKey = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY));
        Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, lookupKey);

        BufferedReader br = null;
        InputStream inputStream = context.getContentResolver().openInputStream(uri);

        br = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
        StringBuilder sb = new StringBuilder();
        try {
            String line;
            if (br != null) {
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("FN")) {
                        line = "FN:" + name;
                    }
                    sb.append(line).append('\n');
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return sb.toString().trim();
    }

    public static String vCard(Contact contact, Context context) {
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCARD\nVERSION:2.1\n");
        if (contact.getName() != null) {
            sb.append("FN:" + contact.getName() + "\n");
        }
        if (contact.getPhone() != null) {
            sb.append("TEL;CELL:" + contact.getPhone() + "\n");
        }
        sb.append("END:VCARD");
        return sb.toString().trim();
    }

//    private static boolean validateData(String data) {
//        return (data != null && data.replaceAll("[\n\r]", "").trim().startsWith(BEGIN_VCARD) && data.replaceAll("[\n\r]", "").trim().endsWith(END_VCARD));
//    }

    public static String getStorage(Context context) {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return Environment.getExternalStorageDirectory().getAbsolutePath();
        } else
            return null;
    }

    public static ArrayList<StringeeFile> getFiles(String folder, String fileNamePattern, int sort)
            throws PatternSyntaxException {
        ArrayList<StringeeFile> dics = new ArrayList<StringeeFile>();
        ArrayList<StringeeFile> files = new ArrayList<StringeeFile>();
        File file = new File(folder);
        if (!file.exists() || !file.isDirectory())
            return null;
        String[] subFiles = file.list();
        if (subFiles == null || subFiles.length == 0)
            return null;
        for (String subFile : subFiles) {
            if (fileNamePattern == null || subFile.matches(fileNamePattern)) {
                StringeeFile StringeeFile = new StringeeFile();
                String filePath = folder + "/" + subFile;
                StringeeFile.setName(subFile);
                StringeeFile.setPath(filePath);
                // check type file
                StringeeFile.setType(checkTypeFile(filePath));
                if (StringeeFile.getType() == StringeeFile.TYPE_OTHER_FILE || StringeeFile.getType() == StringeeFile.TYPE_DOCUMENT
                        || StringeeFile.getType() == StringeeFile.TYPE_IMAGE || StringeeFile.getType() == StringeeFile.TYPE_MEDIA
                        || StringeeFile.getType() == StringeeFile.TYPE_VIDEO || StringeeFile.getType() == StringeeFile.TYPE_ZIP) {
                    File f = new File(filePath);
                    int length = (int) f.length() / 1024;
                    StringeeFile.setSize(length);
                    if (StringeeFile.getName().charAt(0) != '.')
                        files.add(StringeeFile);
                } else {
                    if (StringeeFile.getName().charAt(0) != '.')
                        dics.add(StringeeFile);
                }
            }
        }
        if (files.size() == 0 && dics.size() == 0)
            return null;
        if (sort == 1) {
            Collections.sort(dics, new Comparator<StringeeFile>() {

                @Override
                public int compare(StringeeFile lhs, StringeeFile rhs) {
                    String name1 = lhs.getName();
                    if (name1 != null && name1.trim().length() > 0) {
                        name1 = name1.trim();
                    } else {
                        name1 = "";
                    }
                    name1 = name1.toLowerCase();

                    String name2 = rhs.getName();
                    if (name2 != null && name2.trim().length() > 0) {
                        name2 = name2.trim();
                    } else {
                        name2 = "";
                    }
                    name2 = name2.toLowerCase();
                    return name1.compareTo(name2);
                }
            });
            Collections.sort(files, new Comparator<StringeeFile>() {

                @Override
                public int compare(StringeeFile lhs, StringeeFile rhs) {
                    String name1 = lhs.getName();
                    if (name1 != null && name1.trim().length() > 0) {
                        name1 = name1.trim();
                    } else {
                        name1 = "";
                    }
                    name1 = name1.toLowerCase();

                    String name2 = rhs.getName();
                    if (name2 != null && name2.trim().length() > 0) {
                        name2 = name2.trim();
                    } else {
                        name2 = "";
                    }
                    name2 = name2.toLowerCase();
                    return name1.compareTo(name2);
                }
            });
        }
        dics.addAll(files);
        return dics;
    }

    public static int checkTypeFile(String path) {
        if (isDirectory(path)) {
            return StringeeFile.TYPE_DIRECTORY;
        } else if (isImage(path)) {
            return StringeeFile.TYPE_IMAGE;
        }
        return StringeeFile.TYPE_OTHER_FILE;
    }

    public static boolean isDirectory(String path) {
        if (new File(path).isDirectory()) {
            return true;
        } else if (new File(path).isFile()) {
            return false;
        }
        return false;
    }

    public static boolean isImage(String path) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        return options.outWidth != -1 && options.outHeight != -1;
    }

    public static File getAppDir(Context context, String contentType) {
        File dir;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            String folder = "/" + Utils.getMetaDataValue(context, MAIN_FOLDER_META_DATA) + STRINGEE_OTHER_FILES_FOLDER;

            if (contentType.startsWith("image")) {
                folder = "/" + Utils.getMetaDataValue(context, MAIN_FOLDER_META_DATA) + STRINGEE_IMAGES_FOLDER;
            } else if (contentType.startsWith("video")) {
                folder = "/" + Utils.getMetaDataValue(context, MAIN_FOLDER_META_DATA) + STRINGEE_VIDEOS_FOLDER;
            } else if (contentType.equalsIgnoreCase("audio")) {
                folder = "/" + Utils.getMetaDataValue(context, MAIN_FOLDER_META_DATA) + STRINGEE_AUDIOS_FOLDER;
            } else if (contentType.startsWith("sticker")) {
                folder = "/" + Utils.getMetaDataValue(context, MAIN_FOLDER_META_DATA) + STRINGEE_STICKER_FOLDER;
            }

            dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + folder);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    ContextWrapper cw = new ContextWrapper(context);
                    // path to /data/data/yourapp/app_data/imageDir
                    dir = cw.getDir(IMAGE_DIR, Context.MODE_PRIVATE);
                }
            }
        } else {
            ContextWrapper cw = new ContextWrapper(context);
            // path to /data/data/yourapp/app_data/imageDir
            dir = cw.getDir(IMAGE_DIR, Context.MODE_PRIVATE);
        }

        if (contentType.startsWith("image") || contentType.startsWith("video") || contentType.startsWith("sticker")) {
            File noMediaFile = new File(dir, ".nomedia");
            if (!noMediaFile.exists()) {
                try {
                    noMediaFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return dir;
    }

    public static boolean deleteFile(String filepath) {
        try {
            if (filepath != null) {
                File file = new File(filepath);
                file.delete();
                return true;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
        return false;
    }

    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        // The directory is now empty so delete it
        return dir.delete();
    }

    public static String copyFileToCache(Context context, Uri uri, FileType fileType) {
        Cursor cursor = context.getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
        String name = "";
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int nameIndex = cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME);
                name = (cursor.getString(nameIndex));
            }
            if (!cursor.isClosed()) {
                cursor.close();
            }
        }
        // Create cache file
        File output = new File(getCacheDir(context, fileType) + "/" + name);
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            java.io.FileOutputStream outputStream = new java.io.FileOutputStream(output);

            int read;
            final byte[] buffers = new byte[4096];
            while ((read = inputStream.read(buffers)) != -1) {
                outputStream.write(buffers, 0, read);
            }
            inputStream.close();
            outputStream.close();
        } catch (Exception e) {
            Log.e("Exception", e.getMessage());
        }
        return output.getPath();
    }

    public static File getCacheDir(Context context, FileType fileType) {
        File dir;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            String folder = "/";
            switch (fileType) {
                case IMAGE:
                    folder = folder + Utils.getMetaDataValue(context, MAIN_FOLDER_META_DATA) + STRINGEE_IMAGES_FOLDER;
                    break;
                case VIDEO:
                    folder = folder + Utils.getMetaDataValue(context, MAIN_FOLDER_META_DATA) + STRINGEE_VIDEOS_FOLDER;
                    break;
                case AUDIO:
                    folder = folder + Utils.getMetaDataValue(context, MAIN_FOLDER_META_DATA) + STRINGEE_AUDIO_FOLDER;
                    break;
                case CONTACT:
                    folder = folder + Utils.getMetaDataValue(context, MAIN_FOLDER_META_DATA) + STRINGEE_CONTACT_FOLDER;
                    break;
                case OTHER:
                default:
                    folder = folder + Utils.getMetaDataValue(context, MAIN_FOLDER_META_DATA) + STRINGEE_OTHER_FILES_FOLDER;
                    break;
            }
            // Create cache folder
            if (VERSION.SDK_INT >= 19) {
                File[] dirs = context.getExternalCacheDirs();
                dir = new File(dirs[0] + folder);
            } else {
                dir = new File(context.getExternalCacheDir() + folder);
            }

            if (!dir.exists()) {
                dir.mkdirs();
            }
        } else {
            ContextWrapper cw = new ContextWrapper(context);
            // path to /data/data/yourapp/app_data/imageDir
            dir = cw.getDir(IMAGE_DIR, Context.MODE_PRIVATE);
        }
        return dir;
    }

    public static File getFileCachePath(Context context, String fileName, FileType fileType) {
        return new File(getCacheDir(context, fileType), fileName);
    }
}
