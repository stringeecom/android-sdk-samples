package com.stringee.chat.ui.kit.adapter;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.stringee.chat.ui.kit.commons.Constant;
import com.stringee.chat.ui.kit.commons.utils.AlphaNumberColorUtil;
import com.stringee.chat.ui.kit.model.Contact;
import com.stringee.stringeechatuikit.R;

import java.util.List;

public class ContactAdapter extends BaseAdapter {
    private List<Contact> data;
    private Context context;
    private LayoutInflater inflater;

    public ContactAdapter(Context context, List<Contact> data) {
        this.context = context;
        this.data = data;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int pos) {
        return data.get(pos);
    }

    @Override
    public long getItemId(int pos) {
        return pos;
    }

    @Override
    public View getView(int pos, View view, ViewGroup arg2) {
        ContactHolder holder;
        Contact contact = data.get(pos);
        if (view == null) {
            view = inflater.inflate(R.layout.contact_row, null);
            holder = new ContactHolder();
            holder.vContact = view.findViewById(R.id.v_contact);
            holder.vHeader = view.findViewById(R.id.v_header);
            holder.imAvatar = (ImageView) view.findViewById(R.id.im_avatar);
            holder.tvName = (TextView) view.findViewById(R.id.tv_name);
            holder.tvPhone = (TextView) view.findViewById(R.id.tv_phone);
            holder.tvText = (TextView) view.findViewById(R.id.tv_text);
            holder.alphabeticTextView = view.findViewById(R.id.alphabeticImage);
            view.setTag(holder);
        } else {
            holder = (ContactHolder) view.getTag();
        }

        if (contact.getType() == Constant.TYPE_CONTACT_HEADER) {
            holder.vContact.setVisibility(View.GONE);
            holder.vHeader.setVisibility(View.VISIBLE);
            holder.tvText.setText(contact.getName());
        } else {
            holder.vContact.setVisibility(View.VISIBLE);
            holder.vHeader.setVisibility(View.GONE);
            holder.tvName.setText(contact.getName());
            holder.tvPhone.setText(contact.getPhone());

            int index = pos % 10;
            String name = contact.getName();
            if (name == null || name.length() == 0) {
                name = contact.getPhone();
            }
            GradientDrawable bgShape = (GradientDrawable) holder.alphabeticTextView.getBackground();
            bgShape.setColor(context.getResources().getColor(AlphaNumberColorUtil.alphabetBackgroundColorMap.get(String.valueOf(index))));
            String[] chars = name.split(" ");
            String avaText = "";
            if (chars.length > 1) {
                avaText = String.valueOf(chars[0].charAt(0)) + String.valueOf(chars[1].charAt(0));
            } else if (chars.length > 0) {
                avaText = String.valueOf(chars[0].charAt(0));
            }
            holder.alphabeticTextView.setText(avaText.toUpperCase());
        }
        return view;
    }

    private class ContactHolder {
        ImageView imAvatar;
        TextView tvName;
        TextView tvPhone;
        TextView tvText;
        View vContact;
        View vHeader;
        TextView alphabeticTextView;
    }
}
