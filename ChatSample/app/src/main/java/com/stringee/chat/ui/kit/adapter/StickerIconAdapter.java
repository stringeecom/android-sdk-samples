package com.stringee.chat.ui.kit.adapter;

import android.content.Context;
import android.graphics.Color;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.stringee.chat.ui.kit.commons.LocalImageLoader;
import com.stringee.chat.ui.kit.fragment.ChatFragment;
import com.stringee.chat.ui.kit.model.StickerCategory;
import com.stringee.stringeechatuikit.R;

import java.util.List;

public class StickerIconAdapter extends Adapter {

    private List<StickerCategory> categories;
    private LayoutInflater mInflater;
    private Context mContext;
    private ChatFragment.ChooseStickerListener listener;

    public StickerIconAdapter(Context context, List<StickerCategory> data, ChatFragment.ChooseStickerListener listener) {
        mContext = context;
        this.listener = listener;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        categories = data;
    }

    @androidx.annotation.NonNull
    @Override
    public androidx.recyclerview.widget.RecyclerView.ViewHolder onCreateViewHolder(@androidx.annotation.NonNull ViewGroup parent, int viewType) {
        View v = mInflater.inflate(R.layout.stringee_sticker_icon_row, null);
        return new StickerIconHolder(v);
    }

    @Override
    public void onBindViewHolder(@androidx.annotation.NonNull androidx.recyclerview.widget.RecyclerView.ViewHolder holder, int position) {
        StickerIconHolder stickerIconHolder = (StickerIconHolder) holder;
        StickerCategory category = categories.get(position);
        boolean isSelected = category.isSelected();
        if (isSelected) {
            if (position == categories.size() - 1) {
                stickerIconHolder.iconImageView.setColorFilter(ContextCompat.getColor(mContext, R.color.stringee_colorPrimary));
            } else {
                stickerIconHolder.iconImageView.setColorFilter(Color.TRANSPARENT);
            }
            stickerIconHolder.vIndicatior.setVisibility(View.VISIBLE);
        } else {
            if (position == categories.size() - 1) {
                stickerIconHolder.iconImageView.setColorFilter(Color.parseColor("#929395"));
            } else {
                stickerIconHolder.iconImageView.setColorFilter(Color.TRANSPARENT);
            }
            stickerIconHolder.vIndicatior.setVisibility(View.GONE);
        }

        if (position < categories.size() - 1) {
            stickerIconHolder.iconImageView.setColorFilter(Color.TRANSPARENT);
            LocalImageLoader.getInstance().displayImage(category.getIconUrl(), stickerIconHolder.iconImageView);
        } else {
            stickerIconHolder.iconImageView.setImageResource(R.drawable.ic_sticker_download);
        }
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    class StickerIconHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        ImageView iconImageView;
        View vIndicatior;

        public StickerIconHolder(View itemView) {
            super(itemView);
            iconImageView = itemView.findViewById(R.id.im_sticker_icon);
            vIndicatior = itemView.findViewById(R.id.v_indicator);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getLayoutPosition();
                    for (int i = 0; i < categories.size(); i++) {
                        StickerCategory category = categories.get(i);
                        category.setSelected(i == position);
                    }
                    notifyDataSetChanged();

                    if (listener != null) {
                        listener.onChooseSticker(categories.get(position));
                    }
                }
            });
        }
    }
}
