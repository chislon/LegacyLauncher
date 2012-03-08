/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wordpress.chislonchow.legacylauncher.catalogue;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.TextView;
import com.wordpress.chislonchow.legacylauncher.R;

/** which is used to show list item */
class ApplicationListAdapter extends BaseAdapter {

    private List<AppListInfo> mAppInfoList;
    private LayoutInflater mInflater;

    public ApplicationListAdapter(Context context,
        List<AppListInfo> AppList) {
        mInflater = LayoutInflater.from(context);
        mAppInfoList = AppList;
    }

    public int getCount() {
        return mAppInfoList.size();
    }

    public Object getItem(int position) {
        return mAppInfoList.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;
        AppListInfo tempApplicationInfo = mAppInfoList.get(position);
    
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.list_item_icon_text, null);
    
            holder = new ViewHolder();
            holder.text = (TextView) convertView.findViewById(R.id.text);
            holder.icon = (ImageView) convertView.findViewById(R.id.icon);
            holder.checkbox = (CheckedTextView) convertView
                .findViewById(R.id.multi_picker_list_item_name);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
         } 
        holder.checkbox.setVisibility(View.VISIBLE);
            /* set check box attribute */
        holder.checkbox.setClickable(false);
        holder.checkbox.setChecked(tempApplicationInfo.checked);
        
        holder.text.setText(tempApplicationInfo.title);
        holder.icon.setImageDrawable(tempApplicationInfo.icon);
        return convertView;
    }

    public void updateList(List<AppListInfo> appList) {
        mAppInfoList = appList;
        notifyDataSetChanged();
    }

    public void updateList() {
        notifyDataSetChanged();
    }

    public class ViewHolder {
        TextView text;
        ImageView icon;
        CheckedTextView checkbox;
    }

}
