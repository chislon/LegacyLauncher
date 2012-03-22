/*
 * Copyright (C) 2011 The CyanogenMod Project
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

package com.wordpress.chislonchow.legacylauncher;

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.wordpress.chislonchow.legacylauncher.widgets.ReorderTouchInterpolator;

public class FolderIconReorderActivity extends ListActivity {

	private ListView mList;

	private boolean mDirty = false;

	UserFolderInfo mFolderInfo;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle icicle)
	{
		super.onCreate(icicle);
		setContentView(R.layout.order_folder_icon_activity);

		Bundle extras = getIntent().getExtras(); 

		if (extras != null) {
			final long id = extras.getLong(UserFolder.EXTRA_FOLDER_INFO_ID);
			final LauncherModel launcherModel = Launcher.getLauncherModel();
			if (launcherModel==null || id==0) {
				finish();
				return;
			}

			mFolderInfo = (UserFolderInfo) launcherModel.findFolderById(id);
			this.setTitle(getString(R.string.activity_label_reorder_activity) + mFolderInfo.title);
		} else {
			finish();
			return;
		}

		mList = getListView();

		((ReorderTouchInterpolator) mList).setDropListener(mDropListener);

		setListAdapter(new FolderIconAdapter(this, mFolderInfo.contents));


		Button button;
		button = (Button) findViewById(R.id.button_folder_icon_done);
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}

	@Override
	public void onDestroy() {
		((ReorderTouchInterpolator) mList).setDropListener(null);
		setListAdapter(null);
		if (mDirty) {
			Toast.makeText(this, getString(R.string.toast_folder_icon_reorder_finished) + mFolderInfo.title, Toast.LENGTH_SHORT).show();
		}
		super.onDestroy();
	}

	private ReorderTouchInterpolator.DropListener mDropListener = new ReorderTouchInterpolator.DropListener() {
		public void drop(int from, int to) {

			// move the icon
			if(from < mFolderInfo.contents.size()) {
				ApplicationInfo icon = mFolderInfo.contents.remove(from);
				if(to <= mFolderInfo.contents.size()) {
					mFolderInfo.contents.add(to, icon);
					mList.invalidateViews();
					mDirty = true;
				}
			}
		}
	};

	private class FolderIconAdapter extends BaseAdapter {
		private Context mContext;

		private LayoutInflater mInflater;

		ArrayList<ApplicationInfo> mFolderContents;

		public FolderIconAdapter(Context c, ArrayList<ApplicationInfo> folderContents) {
			mContext = c;
			mInflater = LayoutInflater.from(mContext);

			mFolderContents = folderContents;
		}

		public int getCount() {
			return mFolderContents.size();
		}

		public Object getItem(int position) {
			return mFolderContents.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			final View v;
			if(convertView == null) {
				v = mInflater.inflate(R.layout.order_folder_icon_list_item, null);
			} else {
				v = convertView;
			}

			ApplicationInfo appInfo = mFolderContents.get(position);

			final TextView name = (TextView)v.findViewById(R.id.name);
			final ImageView icon = (ImageView)v.findViewById(R.id.icon);

			name.setText(appInfo.toString());

			icon.setImageDrawable(appInfo.icon);

			return v;
		}

		// these two functions disable list highlighting
		@Override
		public boolean areAllItemsEnabled() { 
			return false; 
		} 
		@Override
		public boolean isEnabled(int position) { 
			return false; 
		} 
	}
}