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
import java.util.List;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.wordpress.chislonchow.legacylauncher.ApplicationInfo;
import com.wordpress.chislonchow.legacylauncher.ApplicationsAdapter;
import com.wordpress.chislonchow.legacylauncher.Launcher;
import com.wordpress.chislonchow.legacylauncher.LauncherModel;
import com.wordpress.chislonchow.legacylauncher.MyLauncherSettingsHelper;
import com.wordpress.chislonchow.legacylauncher.R;
import com.wordpress.chislonchow.legacylauncher.catalogue.AppCatalogueFilters.Catalog;
import com.wordpress.chislonchow.legacylauncher.catalogue.ApplicationListAdapter.SortType;

public class AppInfoListActivity extends ListActivity implements
View.OnCreateContextMenuListener, View.OnClickListener {
	private static final String TAG = "AppInfoListActivity";
	private static final boolean DBG = false;
	public static final String EXTRA_CATALOGUE_INDEX = "EXTRA_CATALOGUE_INDEX";
	public static final String EXTRA_CATALOGUE_NEW = "EXTRA_CATALOGUE_NEW";

	// Custom Adapter used for managing items in the list
	private ApplicationListAdapter mAppInfoAdapter;
	// list of task info
	private ListView mAppInfoList;

	private Button mOkButton, mCancelButton;
	private Catalog mCatalogue;

	private ApplicationListAdapter.SortType mSortType = ApplicationListAdapter.SortType.NAME;
	private boolean mSortAscending = DEFAULT_SORT_ASCENDING;

	private static final SortType DEFAULT_SORTING = ApplicationListAdapter.SortType.NAME_SELECTED;
	private static final boolean DEFAULT_SORT_ASCENDING = true;

	private static final boolean POST_API_9 = (android.os.Build.VERSION.SDK_INT >= 9);

	private boolean mCatalogueNew;

	// take care of catalog deletion 
	public static final int RESULT_NO_REFRESH_LAUNCHER_TRAY = 20;	// sent back to launcher so it doesn't update the interface
	private boolean mCatalogPrepareDelete;							// if set to true, this deletes the selected catalog onPause

	private int mGroupSelectedIndex;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();

		mGroupSelectedIndex = intent.getIntExtra(EXTRA_CATALOGUE_INDEX,
				AppCatalogueFilters.getInstance().getDrawerFilter().getCurrentFilterIndex());
		mCatalogue = AppCatalogueFilters.getInstance().getCatalogue(mGroupSelectedIndex);

		if (mCatalogue == null) {
			setResult(RESULT_NO_REFRESH_LAUNCHER_TRAY);
			mCatalogPrepareDelete = false;
			finish();
			return;
		}

		mCatalogueNew = intent.getBooleanExtra(EXTRA_CATALOGUE_NEW, false);
		mCatalogPrepareDelete = mCatalogueNew;	//default state of prepare delete depends on if catalog is new or existing

		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.app_group_conf_list);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
				R.layout.custom_title1);

		final int sortType = DEFAULT_SORTING.ordinal();
		mSortType = ApplicationListAdapter.SortType.values()[sortType];

		/* sort type spinner */
		final Spinner spinner = (Spinner) findViewById(R.id.sortType);
		ArrayAdapter<CharSequence> adapter;
		if (POST_API_9) {
			adapter = ArrayAdapter.createFromResource(
					this, R.array.catalog_sorttype_entries, android.R.layout.simple_spinner_item);
		} else {
			adapter = ArrayAdapter.createFromResource(
					this, R.array.catalog_sorttype_prefroyo_entries, android.R.layout.simple_spinner_item);
		}
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		spinner.setSelection(sortType);
		spinner.setOnItemSelectedListener(new OnSortTypeItemSelectedListener());

		/* sort direction */
		final ToggleButton direction = (ToggleButton) findViewById(R.id.sortDirection);
		direction.setChecked(mSortAscending);
		direction.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mSortAscending = direction.isChecked();
				// set sort direction preference
				mAppInfoAdapter.sortBy(mSortType, mSortAscending);
			}
		});

		List<AppListInfo> appInfos = new ArrayList<AppListInfo>();

		/* list info */
		mAppInfoAdapter = new ApplicationListAdapter(this, appInfos);
		mAppInfoList = getListView();
		setListAdapter(mAppInfoAdapter);
		mAppInfoList.setOnCreateContextMenuListener(this);
		mAppInfoList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

		/* select all text field */
		final TextView tv = (TextView) findViewById(R.id.text_select_all);

		/* button info */
		mOkButton = ((Button) findViewById(R.id.button_ok_app_list));
		mOkButton.setOnClickListener(this);
		mCancelButton = ((Button) findViewById(R.id.button_cancel_app_list));
		mCancelButton.setOnClickListener(this);

		CheckBox cb = (CheckBox) findViewById(R.id.checkAll);
		cb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				updateAppList(isChecked);
				tv.setText(isChecked ? R.string.selectNone : R.string.selectAll);
			}
		});

		updateAppList();
	}

	/*
	 * This way of exiting ensures that changes have not been made to catalogs
	 */
	private void noDeleteExit(){
		if (mCatalogueNew) {
			Toast.makeText(this, R.string.app_group_add_abort, Toast.LENGTH_SHORT).show();
		} else {
			setResult(RESULT_NO_REFRESH_LAUNCHER_TRAY);
			mCatalogPrepareDelete = false;
		}
		finish();
	}

	@Override
	public void onPause() {
		// handle catalog deletion if the flag was set
		if (mCatalogPrepareDelete) {
			LauncherModel sModel = Launcher.getLauncherModel();
			AppCatalogueFilters.getInstance().dropGroup(mGroupSelectedIndex);
			sModel.getApplicationsAdapter().getCatalogueFilter().setCurrentGroupIndex(-1);
			MyLauncherSettingsHelper.setCurrentAppCatalog(this, -1);
		}
		super.onPause();
	}

	// back key handling
	@Override
	public void onBackPressed(){
		noDeleteExit();
	}

	/*
	 * Method implementing functionality of buttons clicked
	 *
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	public void onClick(View v) {
		if (v == mOkButton) {

			SharedPreferences curAppGrp = mCatalogue.getPreferences();
			if (curAppGrp == null)
				return;// should not go here.

			int checkedCount = 0;

			SharedPreferences.Editor editor = curAppGrp.edit();
			//editor.clear();
			ApplicationListAdapter adapter = (ApplicationListAdapter) mAppInfoList.getAdapter();
			for (int i = 0; i < adapter.getCount(); i++) {
				AppListInfo tempAppListInfo = (AppListInfo) adapter.getItem(i);
				boolean checked = tempAppListInfo.checked;
				//ADW TODO: Change to only store hidden apps
				if (checked) {
					editor.putBoolean(tempAppListInfo.className, true);
					checkedCount ++;
				} else {
					editor.remove(tempAppListInfo.className);
				}
				/*
				if (DBG && checked)
					Log.v("-----", tempAppListInfo.className);
				 */
			}
			editor.commit();
			if (checkedCount == 0) {
				AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
				if (mCatalogueNew) {
					alertBuilder.setTitle(R.string.app_group_no_items_add);
				} else {
					alertBuilder.setTitle(R.string.app_group_no_items_modify);
				}
				alertBuilder.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,
							int whichButton) {
						if (mCatalogueNew) {
							Toast.makeText(AppInfoListActivity.this, R.string.app_group_add_abort, Toast.LENGTH_SHORT).show();
						} else {
							Toast.makeText(AppInfoListActivity.this, R.string.app_group_remove_success, Toast.LENGTH_SHORT).show();
							mCatalogPrepareDelete = true;
						}
						finish();
						/* User clicked OK so do some stuff */
					}
				})
				.setNegativeButton(android.R.string.cancel, null).create().show();
			} else {
				setResult(RESULT_OK);
				mCatalogPrepareDelete = false;
				finish();
			}
		} else if (v == mCancelButton) {
			noDeleteExit();
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		ApplicationListAdapter adapter = (ApplicationListAdapter) mAppInfoList.getAdapter();
		AppListInfo tempAppInfo = (AppListInfo) adapter.getItem(position);

		/* change check of list item */
		tempAppInfo.checked = !tempAppInfo.checked;
		mAppInfoAdapter.updateList();
		return;
	}

	/* update app into */
	private void updateAppList() {

		ArrayList<ApplicationInfo> appInfos = ApplicationsAdapter.allItems;
		/* app info */
		final List<AppListInfo> savedAppInfos = new ArrayList<AppListInfo>();

		TextView t = (TextView) findViewById(R.id.left_title_text);

		mCatalogue.setTitleView(t);

		SharedPreferences curAppGrp = mCatalogue.getPreferences();
		final PackageManager pm = getPackageManager();

		for (int i = 0; i < appInfos.size(); i++) {
			AppListInfo tempAppListInfo = new AppListInfo();
			/* get App info */
			ApplicationInfo tempAppInfo = appInfos.get(i);

			tempAppListInfo.className = tempAppInfo.intent.getComponent()
					.flattenToString();
			tempAppListInfo.icon = tempAppInfo.icon;

			if (POST_API_9) {
				try {
					PackageInfo pkgInfo = pm.getPackageInfo(tempAppInfo.intent.getComponent().getPackageName(), 0);
					tempAppListInfo.firstInstallTime = pkgInfo.firstInstallTime;
				} catch (NameNotFoundException e) {
					tempAppListInfo.firstInstallTime = 0;
				}
			}

			tempAppListInfo.title = tempAppInfo.title.toString();
			if (curAppGrp != null)
				tempAppListInfo.checked = curAppGrp.getBoolean(
						tempAppListInfo.className, false);
			else
				tempAppListInfo.checked = false;

			savedAppInfos.add(tempAppListInfo);
			if (DBG) {
				if (POST_API_9) {
					Log.d(TAG, tempAppListInfo.className + " "
							+ tempAppListInfo.checked + " installTime: " + tempAppListInfo.firstInstallTime);
				} else {
					Log.d(TAG, tempAppListInfo.className + " "
							+ tempAppListInfo.checked + " installTime: ");
				}
			}
		}

		mAppInfoAdapter.updateList(savedAppInfos);
	}

	/* update app into */
	private void updateAppList(boolean bool) {
		ApplicationListAdapter adapter = (ApplicationListAdapter) mAppInfoList.getAdapter();
		for (int i = 0; i < adapter.getCount(); i++) {
			AppListInfo tempAppListInfo = (AppListInfo) adapter.getItem(i);
			tempAppListInfo.checked=bool;
		}
		mAppInfoAdapter.updateList();
	}

	public class OnSortTypeItemSelectedListener implements OnItemSelectedListener {

		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
			mSortType = ApplicationListAdapter.SortType.values()[pos];
			// set sort type preference
			mAppInfoAdapter.sortBy(mSortType, mSortAscending);
		}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {
			// Do nothing.
		}
	}
}