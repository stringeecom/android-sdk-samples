package com.stringee.chat.ui.kit.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.stringee.chat.ui.kit.commons.utils.FileUtils;
import com.stringee.chat.ui.kit.commons.utils.PermissionsUtils;
import com.stringee.chat.ui.kit.model.StickerCategory;
import com.stringee.stringeechatuikit.R;
import com.stringee.stringeechatuikit.common.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class StickerCategoryAdapter extends BaseAdapter {

    private Context mContext;
    private List<StickerCategory> categories;
    private LayoutInflater mInflater;
    private DisplayImageOptions options = new DisplayImageOptions.Builder()
            .cacheInMemory(true)
            .cacheOnDisk(true)
            .build();
    private StickerCategoryListener listener;

    public StickerCategoryAdapter(Context context, List<StickerCategory> categories, StickerCategoryListener listener) {
        mContext = context;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.categories = categories;
        this.listener = listener;
    }

    @Override
    public int getCount() {
        return categories.size();
    }

    @Override
    public Object getItem(int i) {
        return categories.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        final ViewHolder holder;
        if (view == null) {
            holder = new ViewHolder();
            view = mInflater.inflate(R.layout.sticker_category_row, null);
            holder.tvName = view.findViewById(R.id.nameTextView);
            holder.tvNumber = view.findViewById(R.id.numberTextView);
            holder.imCover = view.findViewById(R.id.coverImageView);
            holder.btnAdd = view.findViewById(R.id.addStickerButton);
            holder.prLoading = view.findViewById(R.id.prLoading);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        final StickerCategory category = categories.get(i);
        holder.tvName.setText(category.getName());
        holder.tvNumber.setText(category.getStickerNumber() + " " + mContext.getString(R.string.stickers));
        ImageLoader.getInstance().displayImage(category.getCoverUrl(), holder.imCover, options);
        if (category.isDownloaded()) {
            holder.btnAdd.setText(R.string.remove);
            holder.btnAdd.setBackgroundResource(R.drawable.sticker_remove_selector);
        } else {
            holder.btnAdd.setText(R.string.add);
            holder.btnAdd.setBackgroundResource(R.drawable.add_sticker_selector);
        }

        holder.btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Utils.hasMarshmallow() && PermissionsUtils.checkSelfForStoragePermission((Activity) mContext)) {
                    PermissionsUtils.requestPermissions((Activity) mContext, PermissionsUtils.PERMISSIONS_STORAGE, PermissionsUtils.REQUEST_STORAGE);
                } else {
                    holder.prLoading.setVisibility(View.VISIBLE);
                    downloadOrRemoveStickers(category, holder.prLoading);
                }
            }
        });

        return view;
    }

    private class ViewHolder {
        TextView tvName, tvNumber, btnAdd;
        ImageView imCover;
        ProgressBar prLoading;

    }

    private void downloadOrRemoveStickers(final StickerCategory category, final View loadingView) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (category.isDownloaded()) {
                    FileUtils.deleteDir(new File(FileUtils.getAppDir(mContext, "sticker").getAbsolutePath() + "/" + category.getId()));
                    ((Activity) mContext).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            category.setDownloaded(false);
                            loadingView.setVisibility(View.GONE);
                            notifyDataSetChanged();
                            listener.onDownloadOrRemoveCategory(category);
                        }
                    });
                } else {
                    try {
                        URL url = new URL(category.getZipUrl());
                        final HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                        urlConnection.setRequestMethod("GET");
                        urlConnection.setDoOutput(true);
                        urlConnection.connect();
                        File file = new File(FileUtils.getAppDir(mContext, "sticker"), category.getId() + ".zip");
                        FileOutputStream fileOutput = new FileOutputStream(file);
                        InputStream inputStream = urlConnection.getInputStream();
                        byte[] buffer = new byte[1024];
                        int bufferLength;
                        while ((bufferLength = inputStream.read(buffer)) > 0) {
                            fileOutput.write(buffer, 0, bufferLength);
                        }
                        fileOutput.close();
                        File dest = FileUtils.getAppDir(mContext, "sticker");
                        if (!dest.exists()) {
                            dest.mkdir();
                        }

                        if (unzip(file.getPath(), dest.getPath())) {
                            FileUtils.deleteFile(file.getPath());
                        }

                        ((Activity) mContext).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                category.setDownloaded(true);
                                loadingView.setVisibility(View.GONE);
                                notifyDataSetChanged();
                                listener.onDownloadOrRemoveCategory(category);
                            }
                        });
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (ProtocolException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
    }

    public boolean unzip(String _zipFile, String _location) {
        try {
            FileInputStream fin = new FileInputStream(_zipFile);
            ZipInputStream zin = new ZipInputStream(fin);
            ZipEntry ze = null;
            byte[] buffer = new byte[1024];
            int count;
            while ((ze = zin.getNextEntry()) != null) {
                if (ze.isDirectory()) {
                    String fileName = ze.getName();
                    File fmd = new File(_location, fileName);
                    fmd.mkdirs();
                    continue;
                }

                if (!ze.isDirectory()) {
                    String fileName = ze.getName();
                    File fileLocation = new File(_location, fileName);
                    FileOutputStream fout = new FileOutputStream(fileLocation);
                    while ((count = zin.read(buffer)) != -1) {
                        fout.write(buffer, 0, count);
                    }

                    zin.closeEntry();
                    fout.close();
                }
            }
            zin.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public interface StickerCategoryListener {
        public void onDownloadOrRemoveCategory(StickerCategory category);
    }
}
