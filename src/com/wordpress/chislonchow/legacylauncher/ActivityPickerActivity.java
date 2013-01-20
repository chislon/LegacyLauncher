/**
 * @author AnderWeb <anderweb@gmail.com>
 *
 */
package com.wordpress.chislonchow.legacylauncher;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.wordpress.chislonchow.legacylauncher.R;

import android.app.ExpandableListActivity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;

public class ActivityPickerActivity extends ExpandableListActivity {
	private PackageManager mPackageManager;
	private final class LoadingTask extends AsyncTask<Void, Void, Void> {
		List<PackageInfo> groups;
		@Override
		public void onPreExecute() {
			setProgressBarIndeterminateVisibility(true);
		}
		@Override
		public Void doInBackground(Void... params) {
			groups = mPackageManager.getInstalledPackages(0);
			Collections.sort(groups, new PackageInfoComparable());
			return null;
		}
		@Override
		public void onPostExecute(Void result) {
			setProgressBarIndeterminateVisibility(false);
			setListAdapter(new MyExpandableListAdapter(groups));
		}
	}

	@Override
	protected void onCreate(Bundle savedState) {
		super.onCreate(savedState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_list);
		getExpandableListView().setTextFilterEnabled(true);
		mPackageManager = getPackageManager();
		// Start async loading the data
		new LoadingTask().execute();
	}
	@Override
	public boolean onChildClick(ExpandableListView parent, View v,
			int groupPosition, int childPosition, long id) {
		ActivityInfo info = (ActivityInfo) getExpandableListAdapter().getChild(groupPosition, childPosition);
		Intent intent = new Intent();
		intent.setComponent(new ComponentName(info.applicationInfo.packageName,
				info.name));
		Intent mReturnData = new Intent();
		intent.setAction(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		mReturnData.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent);

		// Set the name of the activity
		mReturnData.putExtra(Intent.EXTRA_SHORTCUT_NAME, info.loadLabel(mPackageManager));

		ShortcutIconResource iconResource = new ShortcutIconResource();
		iconResource.packageName = info.packageName;
		try {
			Resources resources = mPackageManager.getResourcesForApplication(iconResource.packageName);
			iconResource.resourceName = resources.getResourceName(info.getIconResource());
			mReturnData.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);
		} catch (NameNotFoundException e) {
		} catch (Resources.NotFoundException e) {
		}
		setResult(RESULT_OK, mReturnData);
		finish();
		return true;
	}
	/**
	 * ExpandableListAdapter to handle packages and activities
	 * @author adw
	 *
	 */
	public class MyExpandableListAdapter extends BaseExpandableListAdapter {
		private final List<PackageInfo> mGroups;
		private final AbsListView.LayoutParams mLpGroup;
		private final AbsListView.LayoutParams mLpChild;
		private final int mIconSize = getResources().getDimensionPixelSize(android.R.dimen.app_icon_size);

		private TextView mTextView;

		public MyExpandableListAdapter(List<PackageInfo> groups) {
			super();
			
			mGroups = groups;
			mLpGroup = new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			mLpChild = new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, mIconSize);
		}
		public ActivityInfo getChild(int groupPosition, int childPosition) {
			//return mGroups.get(groupPosition).activities[childPosition];
			PackageInfo tmp;
			try {
				tmp = mPackageManager.getPackageInfo(mGroups.get(groupPosition).packageName, PackageManager.GET_ACTIVITIES);
				if(tmp.activities!=null)
					return tmp.activities[childPosition];
				else
					return null;
			} catch (NameNotFoundException e) {
				return null;
			}

		}
		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}
		public int getChildrenCount(int groupPosition) {
			//return mGroups.get(groupPosition).activities.length;
			PackageInfo tmp;
			try {
				tmp = mPackageManager.getPackageInfo(mGroups.get(groupPosition).packageName, PackageManager.GET_ACTIVITIES);
				if(tmp.activities!=null)
					return tmp.activities.length;
				else
					return 0;
			} catch (NameNotFoundException e) {
				return 0;
			}
		}
		
		public PackageInfo getGroup(int groupPosition) {
			return mGroups.get(groupPosition);
		}
		public int getGroupCount() {
			return mGroups.size();
		}
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}
		
		public void getGenericView() {
			mTextView = new TextView(ActivityPickerActivity.this);
			mTextView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
			mTextView.setPadding(mIconSize, 0, 0, 0);
		}
		
		public View getChildView(int groupPosition, int childPosition, 
				boolean isLastChild, View convertView, ViewGroup parent) {
			if (convertView == null) {
				getGenericView();
			} else {
				mTextView = (TextView) convertView;
				mTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
			}
			
			ActivityInfo activity = getChild(groupPosition, childPosition);
			if (activity != null) {
				mTextView.setLayoutParams(mLpChild);
				mTextView.setEnabled(true);
				final String name=activity.name.replace(activity.packageName, "");
				mTextView.setText(activity.loadLabel(mPackageManager)+"("+name+")");
			} else {
				mTextView.setText("");
			}
			return mTextView;
		}
		public View getGroupView(int groupPosition, boolean isExpanded, 
				View convertView, ViewGroup parent) {
			if (convertView == null) {
				getGenericView();
			} else {
				mTextView = (TextView) convertView; 
			}

			mTextView.setLayoutParams(mLpGroup);
			PackageInfo info = getGroup(groupPosition);
			if (getChildrenCount(groupPosition) == 0) {
				mTextView.setEnabled(false);
			} else {
				mTextView.setEnabled(true);
			}
			mTextView.setText(info.applicationInfo.loadLabel(mPackageManager));
			mTextView.setCompoundDrawablesWithIntrinsicBounds(Utilities.createIconThumbnail(info.applicationInfo.loadIcon(mPackageManager),ActivityPickerActivity.this), null, null, null);
			return mTextView;
		}

		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}
		public boolean hasStableIds() {
			return true;
		}
	}
	public class PackageInfoComparable implements Comparator<PackageInfo>{
		@Override
		public int compare(PackageInfo o1, PackageInfo o2) {
			return o1.applicationInfo.loadLabel(mPackageManager).toString().compareToIgnoreCase(o2.applicationInfo.loadLabel(mPackageManager).toString());
		}
	}
}
