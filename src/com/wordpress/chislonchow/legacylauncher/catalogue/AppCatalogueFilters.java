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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import com.wordpress.chislonchow.legacylauncher.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.TextView;

public final class AppCatalogueFilters {

	private static AppCatalogueFilters sInstance;

	public synchronized static AppCatalogueFilters getInstance() {
		if (sInstance == null) {
			sInstance = new AppCatalogueFilters();
		}
		return sInstance;
	}


	public class Catalog {
		private final int mIndex;
		private final SharedPreferences mPreferences;
		private final String mTitle;

		public Catalog(String title, int index) {
			mIndex = index;
			mTitle = title;
			mPreferences = mContext.getSharedPreferences(APP_GROUP_PREFS_PREFIX + index, 0);
		}

		public String getTitle() {
			return mTitle;
		}

		public SharedPreferences getPreferences() {
			return mPreferences;
		}

		public int getIndex() {
			return mIndex;
		}

		public void setTitleView(TextView v) {
			if (mTitle != null) {
				v.setText(mContext.getResources().getString(R.string.app_group_prepend) + mTitle);
			} else {
				v.setText(R.string.app_group_add);
			}
		}
	}

	private final static String PREF_GRP_NAME = "GrpName";
	private final static String APP_GROUP_PREFS_PREFIX = "APP_CATALOG_";
	Context mContext = null;
	private final AppCatalogueFilter mDrawerFilter = new AppCatalogueFilter();

	private SharedPreferences mAllGroupsIndex = null;
	private final Hashtable<Integer, Catalog> mAllAppGroups = new Hashtable<Integer, Catalog>();

	private AppCatalogueFilters() {}

	public AppCatalogueFilter getDrawerFilter() {
		return mDrawerFilter;
	}

	public String getGroupTitle(int index) {
		if (!mAllAppGroups.containsKey(index))
			return null;

		return mAllAppGroups.get(index).getTitle();
	}

	public synchronized void dropGroup(int index)
	{
		if (!mAllAppGroups.containsKey(index))
			return; // invalid index, do nothing.
		Catalog cat = mAllAppGroups.get(index);
		SharedPreferences.Editor ed = cat.getPreferences().edit();
		ed.clear();
		ed.commit();

		SharedPreferences.Editor editor = mAllGroupsIndex.edit();
		editor.remove(PREF_GRP_NAME + index);
		editor.commit();

		mAllAppGroups.remove(index);
	}

	public synchronized final int createNewGroup(String grpName) {
		int grp = getFirstFreeGroupIndex();

		SharedPreferences.Editor editor = mAllGroupsIndex.edit();
		editor.putString("GrpName"+grp, grpName);
		editor.commit();
		Catalog cat = new Catalog(grpName, grp);
		mAllAppGroups.put(grp, cat);

		return grp;
	}

	private int getFirstFreeGroupIndex() {
		int result = 0;
		while (true) {
			if (!mAllAppGroups.containsKey(++result))
				return result;
		}
	}

	public SharedPreferences getGroupPreferences(int index) {
		if (!mAllAppGroups.containsKey(index))
			return null;
		return mAllAppGroups.get(index).getPreferences();
	}

	public synchronized void init(Context context) {
		if (context == null && mContext == null)
			return; // We cant initialize without a context!
		if (context != null)
			mContext = context;
		mAllGroupsIndex = context.getSharedPreferences(APP_GROUP_PREFS_PREFIX + "Index" , 0);

		Map<String, ?> allGrps = mAllGroupsIndex.getAll();
		final int prefNameLen = PREF_GRP_NAME.length();

		for(String key : allGrps.keySet()) {
			if (key.startsWith(PREF_GRP_NAME)) {
				String title = mAllGroupsIndex.getString(key, "");
				int index = Integer.parseInt(key.substring(prefNameLen));
				Catalog cat = new Catalog(title, index);
				mAllAppGroups.put(index, cat);
			}
		}
	}

	public List<Catalog> getAllGroups() {
		List<Catalog> result = new ArrayList<Catalog>(mAllAppGroups.size());
		for (Catalog itm : mAllAppGroups.values()) {
			result.add(itm);
		}
		return result;
	}

	public List<Integer> getGroupsAndSpecialGroupIndexes() {
		List<Integer> result = new ArrayList<Integer>();
		result.add(AppGroupAdapter.APP_GROUP_ALL);
		for (Integer idx : mAllAppGroups.keySet())
			result.add(idx);
		Collections.sort(result);
		return result;
	}

	public Catalog getCatalogue(int index) {
		if (mAllAppGroups.containsKey(index))
			return mAllAppGroups.get(index);
		else
			return null;
	}

	public int getUserCatalogueCount() {
		return mAllAppGroups.size();
	}
}
