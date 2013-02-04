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

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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

	private AlertDialog mAlertDialog;

	// Custom Adapter used for managing items in the list
	private ApplicationListAdapter mAppInfoAdapter;
	// list of task info
	private ListView mAppInfoList;

	private Button mOkButton, mCancelButton, mMenuButton;
	private Catalog mCatalogue;
	private ToggleButton mSortDirection;
	private TextView mTextTitle;

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

	// saves name of catalog
	private String mTitle;

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

		// default state of prepare delete depends on if catalog is new or existing, defaults to true if new.
		mCatalogPrepareDelete = mCatalogueNew;	

		// get catalog title
		mTitle = mCatalogue.getTitle();

		// make changes to title bar
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.app_group_conf_list);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title1);
		mTextTitle = (TextView) findViewById(R.id.left_title_text);
		mTextTitle.setText(getString(R.string.app_group_prepend) + mTitle);

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

		/* sort mSortDirection */
		mSortDirection = (ToggleButton) findViewById(R.id.sortDirection);
		mSortDirection.setChecked(mSortAscending);
		mSortDirection.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mSortAscending = mSortDirection.isChecked();
				// set sort mSortDirection preference
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

		/* button info */
		mOkButton = ((Button) findViewById(R.id.button_ok_app_list));
		mOkButton.setOnClickListener(this);
		mCancelButton = ((Button) findViewById(R.id.button_cancel_app_list));
		mCancelButton.setOnClickListener(this);

		updateAppList();

		// options
		mMenuButton = (Button) findViewById(R.id.button_appinfo_menu);
		registerForContextMenu(mMenuButton);
		mMenuButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				openContextMenu(v);
			}
		});
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
		super.onPause();
		// handle catalog deletion if the flag was set
		if (mCatalogPrepareDelete) {
			MyLauncherSettingsHelper.setCurrentAppCatalog(this, -1);
			LauncherModel sModel = Launcher.getLauncherModel();
			sModel.getApplicationsAdapter().getCatalogueFilter().setCurrentGroupIndex(-1);
			AppCatalogueFilters.getInstance().dropGroup(mGroupSelectedIndex);
			setResult(Launcher.RESULT_CANCELED);
		}
		finish();	// if this activity is about to hidden, we are never coming back
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
			editor.clear();
			ApplicationListAdapter adapter = (ApplicationListAdapter) mAppInfoList.getAdapter();
			for (int i = 0; i < adapter.getCount(); i++) {
				AppListInfo tempAppListInfo = (AppListInfo) adapter.getItem(i);
				boolean checked = tempAppListInfo.checked;

				if (checked) {
					editor.putBoolean(tempAppListInfo.className, true);
					checkedCount ++;
				} 
				/*
				 * 
				//ADW TODO: Change to only store hidden apps
				else {
					editor.remove(tempAppListInfo.className);
				}
				 */
				/*
				if (DBG && checked)
					Log.v("-----", tempAppListInfo.className);
				 */
			}

			if (checkedCount == 0) {
				AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
				if (mCatalogueNew) {
					alertBuilder.setMessage(R.string.app_group_no_items_add);
				} else {
					alertBuilder.setMessage(R.string.app_group_no_items_modify);
				}
				mAlertDialog = alertBuilder.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						/* User clicked OK so do some stuff */
						if (mCatalogueNew) {
							Toast.makeText(AppInfoListActivity.this, R.string.app_group_add_abort, Toast.LENGTH_SHORT).show();
							// mCatalogPrepareDelete is already true for new catalogs by default
						} else {
							Toast.makeText(AppInfoListActivity.this, R.string.app_group_remove_success, Toast.LENGTH_SHORT).show();
							mCatalogPrepareDelete = true;
						}
						finish();
					}
				})
				.setNegativeButton(android.R.string.cancel, null).create();
				mAlertDialog.show();
			} else {
				// only commit when writing items
				editor.commit();
				setResult(RESULT_OK);
				mCatalogPrepareDelete = false;

				// write new title
				if (!mCatalogue.getTitle().equals(mTitle)) {
					mCatalogue.setTitle(mTitle);
					AppCatalogueFilters.getInstance().renameGroupAtIndex(mCatalogue.getIndex(), mTitle);
				}
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
	@SuppressLint("NewApi")
	private void updateAppList() {

		ArrayList<ApplicationInfo> appInfos = ApplicationsAdapter.allItems;
		/* app info */
		final List<AppListInfo> savedAppInfos = new ArrayList<AppListInfo>();

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
	private void updateAppList(boolean value) {
		ApplicationListAdapter adapter = (ApplicationListAdapter) mAppInfoList.getAdapter();
		for (int i = 0; i < adapter.getCount(); i++) {
			AppListInfo tempAppListInfo = (AppListInfo) adapter.getItem(i);
			tempAppListInfo.checked=value;
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

	@Override
	public void onDestroy() {
		if (mAlertDialog != null) {
			if (mAlertDialog.isShowing()) {
				mAlertDialog.dismiss();
				mAlertDialog = null;
			}
		}
		super.onDestroy();
	}

	@Override  
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {  
		super.onCreateContextMenu(menu, v, menuInfo);
		getMenuInflater().inflate(R.menu.appinfo_menu, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menuitem_appinfo_rename:
			showDialog(0);
			break;
		case R.id.menuitem_appinfo_selectall:
			updateAppList(true);
			break;
		case R.id.menuitem_appinfo_selectnone:
			updateAppList(false);
			break;
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			openContextMenu(mMenuButton);
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		final View layout = View.inflate(this, R.layout.rename_grp, null);
		final EditText mInput = (EditText) layout.findViewById(R.id.group_name);
		mInput.setText(mCatalogue.getTitle());
		
		return new AlertDialog.Builder(this)
		.setIcon(0)
		.setCancelable(true)
		.setTitle(R.string.rename_group_title)
		.setNegativeButton(android.R.string.cancel, null)
		.setPositiveButton(getString(android.R.string.ok),
				new Dialog.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				final String name = mInput.getText().toString()
						.trim();
				if (!TextUtils.isEmpty(name)) {
					// Make sure we have the right folder info
					mTitle = name;
					mTextTitle.setText(getString(R.string.app_group_prepend) + name);
				} else {
					Toast.makeText(AppInfoListActivity.this,
							R.string.rename_group_fail,
							Toast.LENGTH_SHORT).show();
				}
			}
		})
		.setView(layout)
		.create();
	}
}