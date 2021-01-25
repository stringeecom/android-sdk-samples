package com.stringee.chat.ui.kit.commons;

import android.os.AsyncTask;

import com.stringee.listener.StatusListener;
import com.stringee.messaging.Message;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;

public class DownloadFileService extends AsyncTask<Message, Integer, String> {

    private InputStream input = null;
    private OutputStream output = null;
    private HttpURLConnection connection = null;
    private String source;
    private StatusListener listener;
    private String dest;

    public DownloadFileService(String url, String path, StatusListener listener) {
        source = url;
        dest = path;
        this.listener = listener;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
    }

    @Override
    protected String doInBackground(Message... messages) {
        return downloadFile();
    }

    private String downloadFile() {
        String result = "";
        if (source == null || source.length() == 0) {
            return result;
        }
        try {
            URL url = new URL(source);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }

            int fileLength = connection.getContentLength();

            input = connection.getInputStream();

            File mediaFile = new File(dest);
            output = new FileOutputStream(mediaFile);

            byte data[] = new byte[4096];
            long total = 0;
            int count;
            while (((count = input.read(data)) != -1)) {

                if (isCancelled()) {
                    input.close();
                    return null;
                }
                total += count;
                // publishing the progress....
                if (fileLength > 0) { // only if total length is known
                    int pr = (int) (total * 100 / fileLength);
                    if (listener != null) {
                        listener.onProgress(pr);
                    }
                }
                output.write(data, 0, count);
            }
            result = "Success";
        } catch (SocketTimeoutException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (output != null) {
                    output.close();
                }
                if (input != null) {
                    input.close();
                }
            } catch (IOException ignored) {
            }

            if (connection != null) {
                connection.disconnect();
            }
        }
        return result;
    }

    @Override
    protected void onPostExecute(String result) {
        if (result != null && result.equals("Success") && listener != null) {
            listener.onSuccess();
        }
    }
}