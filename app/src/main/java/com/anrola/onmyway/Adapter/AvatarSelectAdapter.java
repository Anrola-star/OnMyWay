package com.anrola.onmyway.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.anrola.onmyway.Entity.Avatar;
import com.anrola.onmyway.R;

import java.util.List;

public class AvatarSelectAdapter extends BaseAdapter {
    private Context context;
    private List<Avatar> avatarList;
    private LayoutInflater inflater;

    // 选中项索引
    private int selectedPosition = 0;

    public AvatarSelectAdapter(Context context, List<Avatar> avatarList) {
        this.context = context;
        this.avatarList = avatarList;
        this.inflater = LayoutInflater.from(context);
    }

    // 设置选中项
    public void setSelectedPosition(int position) {
        selectedPosition = position;
        notifyDataSetChanged();
    }

    // 获取选中项
    public int getSelectedPosition() {
        return selectedPosition;
    }

    @Override
    public int getCount() {
        return avatarList.size();
    }

    @Override
    public Object getItem(int position) {
        return avatarList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_avatar_select, parent, false);
            holder = new ViewHolder();
            holder.ivAvatar = convertView.findViewById(R.id.iv_avatar);
            holder.ivSelected = convertView.findViewById(R.id.iv_selected);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // 设置头像
        Avatar avatar = avatarList.get(position);
        holder.ivAvatar.setImageBitmap(avatar.getAvatarBitmap());

        // 设置选中状态
        if (position == selectedPosition) {
            holder.ivSelected.setVisibility(View.VISIBLE);
        } else {
            holder.ivSelected.setVisibility(View.GONE);
        }

        return convertView;
    }

    static class ViewHolder {
        ImageView ivAvatar;
        ImageView ivSelected;
    }
}
