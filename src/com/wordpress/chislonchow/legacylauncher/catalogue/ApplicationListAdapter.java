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

import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
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

	public enum SortType {
		NAME_SELECTED, NAME, AGE, 
	}

	private List<AppListInfo> mAppInfoList;
	private LayoutInflater mInflater;
	private static final Collator sCollator = Collator.getInstance();

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

	public void sortBy(SortType type, boolean ascending) {
		if (SortType.NAME == type) {
			if (ascending)
				Collections.sort(mAppInfoList, new AppListInfoNameComparator());
			else {
				Comparator<AppListInfo> revert = Collections.reverseOrder(new AppListInfoNameComparator());
				Collections.sort(mAppInfoList, revert);
			}
		} else if (SortType.AGE == type) {
			if (ascending)
				Collections.sort(mAppInfoList, new AppListInfoAgeComparator());
			else {
				Comparator<AppListInfo> revert = Collections.reverseOrder(new AppListInfoAgeComparator());
				Collections.sort(mAppInfoList, revert);
			}
		} else if (SortType.NAME_SELECTED == type) {
			if (ascending)
				Collections.sort(mAppInfoList, new AppListInfoNameSelectedAscComparator());
			else {
				Comparator<AppListInfo> revert = Collections.reverseOrder(new AppListInfoNameSelectedDscComparator());
				Collections.sort(mAppInfoList, revert);
			}
		}
		notifyDataSetChanged();
	}

	static class AppListInfoNameComparator implements Comparator<AppListInfo> {
		public final int compare(AppListInfo a, AppListInfo b) {
			int result = sCollator.compare(a.title, b.title);
			if (0 == result)
				result = sCollator.compare(a.className, b.className);
			return result;
		}
	}

	static class AppListInfoNameSelectedAscComparator implements Comparator<AppListInfo> {
		public final int compare(AppListInfo a, AppListInfo b) {
			int result = (a.checked == b.checked ? 0 : (a.checked ? -1 : 1));
			if (0 == result)
				result = sCollator.compare(a.title, b.title);
			if (0 == result)
				result = sCollator.compare(a.className, b.className);
			return result;
		}
	}
	
	static class AppListInfoNameSelectedDscComparator implements Comparator<AppListInfo> {
		public final int compare(AppListInfo a, AppListInfo b) {
			int result = (a.checked == b.checked ? 0 : (a.checked ? 1 : -1));
			if (0 == result)
				result = sCollator.compare(a.title, b.title);
			if (0 == result)
				result = sCollator.compare(a.className, b.className);
			return result;
		}
	}

	static class AppListInfoAgeComparator implements Comparator<AppListInfo> {
		public final int compare(AppListInfo a, AppListInfo b) {
			int result = a.firstInstallTime < b.firstInstallTime ? -1 :
				(a.firstInstallTime > b.firstInstallTime) ? 1 : 0;
			if (0 == result)
				result = sCollator.compare(a.className, b.className);
			return result;
		}
	}

	public class ViewHolder {
		TextView text;
		ImageView icon;
		CheckedTextView checkbox;
	}

}