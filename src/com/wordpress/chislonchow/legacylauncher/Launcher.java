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

package com.wordpress.chislonchow.legacylauncher;

import static android.util.Log.d;
import static android.util.Log.e;
import static android.util.Log.w;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import mobi.intuitit.android.content.LauncherIntent;
import mobi.intuitit.android.content.LauncherMetadata;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.app.SearchManager;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ProviderInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.os.Parcelable;
import android.os.Vibrator;
import android.provider.LiveFolders;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.TextKeyListener;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.EditText;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.devoteam.quickaction.QuickActionWindow;
import com.wordpress.chislonchow.legacylauncher.catalogue.AppCatalogueFilter;
import com.wordpress.chislonchow.legacylauncher.catalogue.AppCatalogueFilters;
import com.wordpress.chislonchow.legacylauncher.catalogue.AppCatalogueFilters.Catalog;
import com.wordpress.chislonchow.legacylauncher.catalogue.AppGroupAdapter;
import com.wordpress.chislonchow.legacylauncher.catalogue.AppInfoListActivity;
import com.wordpress.chislonchow.legacylauncher.ui.NumberPicker;

/**
 * Default launcher application.
 */
public final class Launcher extends Activity implements View.OnClickListener,
OnLongClickListener, OnSharedPreferenceChangeListener {
	static final String LOG_TAG = "Launcher";
	static final boolean LOGD = false;

	private static final boolean PROFILE_STARTUP = false;
	private static final boolean PROFILE_ROTATE = false;
	private static final boolean DEBUG_USER_INTERFACE = false;

	private static final int MENU_GROUP_ADD = 1;
	private static final int MENU_GROUP_LAUNCHER_SETUP = 2;
	private static final int MENU_GROUP_CATALOGUE = 3;
	private static final int MENU_GROUP_NORMAL = 4;
	private static final int MENU_GROUP_WALLPAPER = 5;

	private static final int MENU_ADD = Menu.FIRST + 1;
	private static final int MENU_WALLPAPER_SETTINGS = MENU_ADD + 1;
	private static final int MENU_SEARCH = MENU_WALLPAPER_SETTINGS + 1;
	private static final int MENU_EDIT = MENU_SEARCH + 1;
	private static final int MENU_SETTINGS = MENU_EDIT + 1;
	private static final int MENU_ALMOSTNEXUS = MENU_SETTINGS + 1;
	private static final int MENU_APP_GRP_CONFIG = MENU_SETTINGS + 2;
	private static final int MENU_APP_GRP_RENAME = MENU_SETTINGS + 3;
	private static final int MENU_APP_SWITCH_GRP = MENU_SETTINGS + 4;
	private static final int MENU_APP_DELETE_GRP = MENU_SETTINGS + 5;
	private static final int MENU_LOCK_DESKTOP = MENU_SETTINGS + 6;
	private static final int MENU_MANAGE_APPS = MENU_SETTINGS + 7;

	private static final int REQUEST_CREATE_SHORTCUT = 1;
	private static final int REQUEST_CREATE_LIVE_FOLDER = 4;
	private static final int REQUEST_CREATE_APPWIDGET = 5;
	private static final int REQUEST_PICK_APPLICATION = 6;
	private static final int REQUEST_PICK_SHORTCUT = 7;
	private static final int REQUEST_PICK_LIVE_FOLDER = 8;
	private static final int REQUEST_PICK_APPWIDGET = 9;
	private static final int REQUEST_PICK_ANYCUT = 10;
	private static final int REQUEST_SHOW_APP_LIST = 11;
	private static final int REQUEST_EDIT_SHORTCUT = 12;

	static final String EXTRA_SHORTCUT_DUPLICATE = "duplicate";

	static final String EXTRA_CUSTOM_WIDGET = "custom_widget";
	static final String SEARCH_WIDGET = "search_widget";

	static final int WALLPAPER_SCREENS_SPAN = 2;
	static final int SCREEN_COUNT = 5;
	static final int DEFAULT_SCREEN = 2;
	static final int NUMBER_CELLS_X = 4;
	static final int NUMBER_CELLS_Y = 4;

	private static final int DIALOG_CREATE_SHORTCUT = 1;
	static final int DIALOG_RENAME_FOLDER = 2;
	static final int DIALOG_CHOOSE_GROUP = 3;
	static final int DIALOG_NEW_GROUP = 4;
	static final int DIALOG_DELETE_GROUP_CONFIRM = 5;
	static final int DIALOG_PICK_GROUPS = 6;
	static final int DIALOG_ADD_WIDGET_FAILURE = 7;
	static final int DIALOG_GET_LAUNCHER_UNLOCK_PASSWORD = 8;

	private static final String PREFERENCES = "launcher.preferences";

	// Type: int
	private static final String RUNTIME_STATE_CURRENT_SCREEN = "launcher.current_screen";
	// Type: boolean
	private static final String RUNTIME_STATE_ALL_APPS_FOLDER = "launcher.all_apps_folder";
	// Type: long
	private static final String RUNTIME_STATE_USER_FOLDERS = "launcher.user_folder";
	// Type: int
	private static final String RUNTIME_STATE_PENDING_ADD_SCREEN = "launcher.add_screen";
	// Type: int
	private static final String RUNTIME_STATE_PENDING_ADD_CELL_X = "launcher.add_cellX";
	// Type: int
	private static final String RUNTIME_STATE_PENDING_ADD_CELL_Y = "launcher.add_cellY";
	// Type: int
	private static final String RUNTIME_STATE_PENDING_ADD_SPAN_X = "launcher.add_spanX";
	// Type: int
	private static final String RUNTIME_STATE_PENDING_ADD_SPAN_Y = "launcher.add_spanY";
	// Type: int
	private static final String RUNTIME_STATE_PENDING_ADD_COUNT_X = "launcher.add_countX";
	// Type: int
	private static final String RUNTIME_STATE_PENDING_ADD_COUNT_Y = "launcher.add_countY";
	// Type: int[]
	private static final String RUNTIME_STATE_PENDING_ADD_OCCUPIED_CELLS = "launcher.add_occupied_cells";
	// Type: boolean
	private static final String RUNTIME_STATE_PENDING_FOLDER_RENAME = "launcher.rename_folder";
	// Type: long
	private static final String RUNTIME_STATE_PENDING_FOLDER_RENAME_ID = "launcher.rename_folder_id";
	// Type: boolean
	private static final String RUNTIME_STATE_PENDING_GROUP_PICK = "launcher.rename_folder";
	// Type: long
	private static final String RUNTIME_STATE_PENDING_GROUP_PICK_ID = "launcher.rename_folder_id";

	private static final LauncherModel sLauncherModel = new LauncherModel();

	private static final Object sLock = new Object();
	private static int sScreen = DEFAULT_SCREEN;

	private final BroadcastReceiver mApplicationsReceiver = new ApplicationsIntentReceiver();
	private final BroadcastReceiver mCloseSystemDialogsReceiver = new CloseSystemDialogsIntentReceiver();
	private final ContentObserver mObserver = new FavoritesChangeObserver();
	private final ContentObserver mWidgetObserver = new AppWidgetResetObserver();

	private LayoutInflater mInflater;

	private DragLayer mDragLayer;
	private Workspace mWorkspace;

	private AppWidgetManager mAppWidgetManager;
	private LauncherAppWidgetHost mAppWidgetHost;

	static final int APPWIDGET_HOST_ID = 1024;

	private CellLayout.CellInfo mAddItemCellInfo;
	private CellLayout.CellInfo mMenuAddInfo;
	private final int[] mCellCoordinates = new int[2];
	private FolderInfo mFolderInfo;
	private ApplicationInfo mPickGroupInfo;

	/**
	 * ADW: now i use an ActionButton instead of a fixed app-drawer button
	 */
	private ActionButton mMAB;
	/**
	 * mAllAppsGrid will be "AllAppsGridView" or "AllAppsSlidingView" depending
	 * on user settings, so I cast it later.
	 */
	private Drawer mAllAppsGrid;

	private boolean mDesktopLocked = true;
	private Bundle mSavedState;

	private SpannableStringBuilder mDefaultKeySsb = null;

	private boolean mDestroyed;

	private boolean mIsNewIntent;

	private boolean mRestoring;
	private boolean mWaitingForResult;
	private boolean mLocaleChanged;

	private Bundle mSavedInstanceState;

	private DesktopBinder mBinder;
	/**
	 * ADW: New views/elements for dots, dockbar, lab/rab, etc
	 */
	private ImageView mPreviousView;
	private ImageView mNextView;
	private ActionButton mLAB;
	private ActionButton mRAB;
	private ActionButton mLAB2;
	private ActionButton mRAB2;
	private View mDrawerToolbar;
	private DeleteZone mDeleteZone;
	/**
	 * ADW: variables to store actual status of elements
	 */
	private boolean allAppsOpen = false;
	private final boolean allAppsAnimating = false;
	private boolean showingPreviews = false;
	private boolean mShouldHideStatusbaronFocus = false;
	/**
	 * ADW: A lot of properties to store the custom settings
	 */
	private boolean mDrawerAnimate = true;
	private boolean mPreviewsNew = true;
	private boolean mHideStatusBar = false;
	private boolean mShowDots = false;
	protected boolean mAutoCloseFolder;
	private boolean mHideABBg = true;
	private float mUiScaleAB = 0.5f;
	private boolean mUiABTint = false;
	private int mUiABTintColor = 0xffffffff;
	private int mUiABSelectorColor = 0xff82b600;

	private boolean mUiHideLabels = false;

	private boolean mWallpaperHack = true;
	private DesktopIndicator mDesktopIndicator;
	private boolean mUseDrawerCatalogNavigation = false;
	public boolean mUseDrawerUngroupCatalog = false;
	protected boolean mUseDrawerTitleCatalog = false;
	protected int mTransitionStyle = 1;

	private static int mAppDrawerPadding = 0;
	private int mAppDrawerPaddingLast = -1;

	private boolean mFolderAnimate = true;

	private boolean mLauncherLocked = false;

	private AlertDialog mAlertDialog;

	// lockdown entire launcher from layout changes
	public boolean isLauncherLocked() {
		return mLauncherLocked;
	}

	/**
	 * ADW: Home binding constants
	 */
	protected static final int BIND_NONE = 0;
	protected static final int BIND_DEFAULT = 1;
	protected static final int BIND_HOME_PREVIEWS = 2;
	protected static final int BIND_PREVIEWS = 3;
	protected static final int BIND_APPS = 4;
	protected static final int BIND_STATUSBAR = 5;
	protected static final int BIND_NOTIFICATIONS = 6;
	protected static final int BIND_HOME_NOTIFICATIONS = 7;
	protected static final int BIND_APP_LAUNCHER = 8;
	protected static final int BIND_ACTIVITY = 9;

	private static final int BIND_TYPE_HOME = 1;
	private static final int BIND_TYPE_SWIPEUP = 2;
	private static final int BIND_TYPE_SWIPEDOWN = 3;
	private static final int BIND_TYPE_PINCHIN = 4;

	private int mHomeBinding = BIND_DEFAULT;

	/**
	 * wjax: Swipe Down binding enum
	 */
	private int mSwipedownAction = BIND_NONE;
	/**
	 * wjax: Swipe UP binding enum
	 */
	private int mSwipeupAction = BIND_NONE;
	/**
	 * cchow: Pinch in binding enum
	 */
	private int mPinchInAction = BIND_PREVIEWS;

	/**
	 * ADW:Wallpaper intent receiver
	 */
	private static WallpaperIntentReceiver sWallpaperReceiver;
	private boolean mShouldRestart = false;
	// ADW Theme constants
	public static final int THEME_ITEM_BACKGROUND = 0;
	public static final int THEME_ITEM_FOREGROUND = 1;
	public static final String THEME_DEFAULT = "ADW.Default theme";
	private Typeface themeFont = null;
	private boolean mIsEditMode = false;
	private View mScreensEditor = null;
	private boolean mIsWidgetEditMode = false;
	// /TODO:ADW. Current code fully ready for upto 9
	// but need to add more drawables for the desktop dots...
	// or completely redo the desktop dots implementation
	private final static int MAX_SCREENS = 7;
	// ADW: NAVIGATION VALUES FOR THE NEXT/PREV CATALOG ACTIONS
	protected final static int ACTION_CATALOG_SAME = 0;
	protected final static int ACTION_CATALOG_PREV = 1;
	protected final static int ACTION_CATALOG_NEXT = 2;
	// ADW: Custom counter receiver
	private CounterReceiver mCounterReceiver;
	/**
	 * ADW: Different main dock styles/configurations, default is 4
	 */
	protected static final int DOCK_STYLE_NONE = 0;
	protected static final int DOCK_STYLE_3 = 1;
	protected static final int DOCK_STYLE_5 = 2;
	protected static final int DOCK_STYLE_1 = 3;
	protected static final int DOCK_STYLE_4 = 4;

	private int mDockStyle = DOCK_STYLE_5;

	/*
	 * Different main dock hide configurations
	 */
	private boolean mDockHide = false;

	// Display
	private Display mDisplay;

	// DRAWER STYLES
	private final int[] mDrawerStyles = { R.layout.old_drawer,
			R.layout.new_drawer };
	private int mDrawerStyle = 1;

	// Quick Action Options
	static final String ANDROID_MARKET_PACKAGE = "com.android.vending";
	private Drawable mMarketIcon;
	private CharSequence mMarketLabel;
	public static final String ANDROID_SETTINGS_PACKAGE = "com.android.settings";
	private String mAppInfoLabel;

	// Manage applications on launcher menu
	static final ComponentName ANDROID_MANAGE_COMPONENT = new ComponentName(
			ANDROID_SETTINGS_PACKAGE, ANDROID_SETTINGS_PACKAGE
			+ ".ManageApplications");

	// password prefs
	ObscuredSharedPreferences mObscurePrefs;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		changeOrientation(MyLauncherSettingsHelper.getDesktopOrientation(this));
		super.onCreate(savedInstanceState);
		mInflater = getLayoutInflater();

		mDisplay = getWindowManager().getDefaultDisplay();

		AppCatalogueFilters.getInstance().init(this);
		LauncherActions.getInstance().init(this);

		mAppWidgetManager = AppWidgetManager.getInstance(this);

		mAppWidgetHost = new LauncherAppWidgetHost(this, APPWIDGET_HOST_ID);
		mAppWidgetHost.startListening();

		if (PROFILE_STARTUP) {
			android.os.Debug.startMethodTracing(Environment
					.getExternalStorageDirectory().getPath() + "/launcher");
		}
		updateAlmostNexusVars();
		checkForLocaleChange();
		setWallpaperDimension();
		setContentView(R.layout.launcher);
		setupViews();

		registerIntentReceivers();
		registerContentObservers();

		mSavedState = savedInstanceState;
		restoreState(mSavedState);

		if (PROFILE_STARTUP) {
			android.os.Debug.stopMethodTracing();
		}

		if (!mRestoring) {
			startLoaders();
		}

		// For handling default keys
		mDefaultKeySsb = new SpannableStringBuilder();
		Selection.setSelection(mDefaultKeySsb, 0);

		// ADW: register a sharedpref listener
		getSharedPreferences("launcher.preferences.almostnexus",
				Context.MODE_PRIVATE).registerOnSharedPreferenceChangeListener(
						this);
		mObscurePrefs = new ObscuredSharedPreferences(this,
				this.getSharedPreferences("secure", Context.MODE_PRIVATE));
	}

	private void checkForLocaleChange() {
		final LocaleConfiguration localeConfiguration = new LocaleConfiguration();
		readConfiguration(this, localeConfiguration);

		final Configuration configuration = getResources().getConfiguration();

		final String previousLocale = localeConfiguration.locale;
		final String locale = configuration.locale.toString();

		final int previousMcc = localeConfiguration.mcc;
		final int mcc = configuration.mcc;

		final int previousMnc = localeConfiguration.mnc;
		final int mnc = configuration.mnc;

		mLocaleChanged = !locale.equals(previousLocale) || mcc != previousMcc
				|| mnc != previousMnc;

		if (mLocaleChanged) {
			localeConfiguration.locale = locale;
			localeConfiguration.mcc = mcc;
			localeConfiguration.mnc = mnc;

			new Thread("WriteLocaleConfiguration") {
				public void run() {
					writeConfiguration(Launcher.this, localeConfiguration);
				}
			}.start();
		}
	}

	private static class LocaleConfiguration {
		public String locale;
		public int mcc = -1;
		public int mnc = -1;
	}

	private static void readConfiguration(Context context,
			LocaleConfiguration configuration) {
		DataInputStream in = null;
		try {
			in = new DataInputStream(context.openFileInput(PREFERENCES));
			configuration.locale = in.readUTF();
			configuration.mcc = in.readInt();
			configuration.mnc = in.readInt();
		} catch (FileNotFoundException e) {
			// Ignore
		} catch (IOException e) {
			// Ignore
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					// Ignore
				}
			}
		}
	}

	private static void writeConfiguration(Context context,
			LocaleConfiguration configuration) {
		DataOutputStream out = null;
		try {
			out = new DataOutputStream(context.openFileOutput(PREFERENCES,
					MODE_PRIVATE));
			out.writeUTF(configuration.locale);
			out.writeInt(configuration.mcc);
			out.writeInt(configuration.mnc);
			out.flush();
		} catch (NullPointerException e) {
			// Something is wrong, but we'll ignore this. 
		} catch (FileNotFoundException e) {
			// Ignore
		} catch (IOException e) {
			// noinspection ResultOfMethodCallIgnored
			context.getFileStreamPath(PREFERENCES).delete();
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					// Ignore
				}
			}
		}
	}

	static int getScreen() {
		synchronized (sLock) {
			return sScreen;
		}
	}

	static void setScreen(int screen) {
		synchronized (sLock) {
			sScreen = screen;
		}
	}

	private void startLoaders() {
		boolean loadApplications = sLauncherModel.loadApplications(true, this,
				mLocaleChanged);
		sLauncherModel.loadUserItems(!mLocaleChanged, this, mLocaleChanged,
				loadApplications);

		mRestoring = false;
	}

	private void setWallpaperDimension() {
		final int maxDim = Math.max(mDisplay.getWidth(), mDisplay.getHeight());
		final int minDim = Math.min(mDisplay.getWidth(), mDisplay.getHeight());

		final int width = Math.max((int) (minDim * WALLPAPER_SCREENS_SPAN),
				maxDim);
		final int height = maxDim;

		new Thread("setWallpaperDimension") {
			public void run() {
				final WallpaperManager wpm = (WallpaperManager) getSystemService(WALLPAPER_SERVICE);
				wpm.suggestDesiredDimensions(width, height);
			}
		}.start();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		mWaitingForResult = false;
		// The pattern used here is that a user PICKs a specific application,
		// which, depending on the target, might need to CREATE the actual
		// target.

		// For example, the user would PICK_SHORTCUT for "Music playlist", and
		// we
		// launch over to the Music app to actually CREATE_SHORTCUT.

		if (resultCode == RESULT_OK && mAddItemCellInfo != null) {
			switch (requestCode) {
			case REQUEST_PICK_APPLICATION:
				completeAddApplication(this, data, mAddItemCellInfo,
						!mDesktopLocked);
				break;
			case REQUEST_PICK_SHORTCUT:
				processShortcut(data, REQUEST_PICK_APPLICATION,
						REQUEST_CREATE_SHORTCUT);
				break;
			case REQUEST_CREATE_SHORTCUT:
				completeAddShortcut(data, mAddItemCellInfo, !mDesktopLocked);
				break;
			case REQUEST_PICK_LIVE_FOLDER:
				addLiveFolder(data);
				break;
			case REQUEST_CREATE_LIVE_FOLDER:
				completeAddLiveFolder(data, mAddItemCellInfo, !mDesktopLocked);
				break;
			case REQUEST_PICK_APPWIDGET:
				addAppWidget(data);
				break;
			case REQUEST_CREATE_APPWIDGET:
				completeAddAppWidget(data, mAddItemCellInfo, !mDesktopLocked);
				break;
			case REQUEST_PICK_ANYCUT:
				completeAddShortcut(data, mAddItemCellInfo, !mDesktopLocked);
				break;
			case REQUEST_EDIT_SHORTCUT:
				completeEditShortcut(data);
				break;
			}
		} else if (resultCode == RESULT_OK) {
			switch (requestCode) {
			case REQUEST_SHOW_APP_LIST:
				mAllAppsGrid.updateAppGrp();
				showAllApps(true, null);
				break;
			case REQUEST_EDIT_SHORTCUT:
				completeEditShortcut(data);
				break;
			}
		} else if ((resultCode == RESULT_CANCELED)
				&& (requestCode == REQUEST_SHOW_APP_LIST)) {
			checkActionButtonsSpecialMode();
			mAllAppsGrid.updateAppGrp();
			showAllApps(true, null);
		} else if ((requestCode == REQUEST_PICK_APPWIDGET || requestCode == REQUEST_CREATE_APPWIDGET)
				&& resultCode == RESULT_CANCELED && data != null) {
			// Clean up the appWidgetId if we canceled
			int appWidgetId = data.getIntExtra(
					AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
			if (appWidgetId != -1) {
				mAppWidgetHost.deleteAppWidgetId(appWidgetId);
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (shouldRestart())
			return;

		// ADW: Use custom settings to set the rotation
		/*
		 * this.setRequestedOrientation(
		 * AlmostNexusSettingsHelper.getDesktopRotation(this)?
		 * ActivityInfo.SCREEN_ORIENTATION_USER
		 * :ActivityInfo.SCREEN_ORIENTATION_NOSENSOR );
		 */
		// ADW: Use custom settings to change number of columns (and rows for
		// SlidingGrid) depending on phone rotation
		/*
		 * int orientation = getResources().getConfiguration().orientation; if
		 * (orientation == Configuration.ORIENTATION_PORTRAIT) {
		 */
		if (mDisplay.getWidth() > mDisplay.getHeight()) {
			mAllAppsGrid.setNumColumns(MyLauncherSettingsHelper
					.getColumnsLandscape(Launcher.this));
			mAllAppsGrid.setNumRows(MyLauncherSettingsHelper
					.getRowsLandscape(Launcher.this));
		} else {
			mAllAppsGrid.setNumColumns(MyLauncherSettingsHelper
					.getColumnsPortrait(Launcher.this));
			mAllAppsGrid.setNumRows(MyLauncherSettingsHelper
					.getRowsPortrait(Launcher.this));
		}

		mWorkspace.setWallpaper(false);
		if (mRestoring) {
			startLoaders();
		}

		// If this was a new intent (i.e., the mIsNewIntent flag got set to true
		// by
		// onNewIntent), then close the search dialog if needed, because it
		// probably
		// came from the user pressing 'home' (rather than, for example,
		// pressing 'back').
		if (mIsNewIntent) {
			// Post to a handler so that this happens after the search dialog
			// tries to open
			// itself again.
			mWorkspace.post(new Runnable() {
				public void run() {
					// ADW: changed from using ISearchManager to use
					// SearchManager (thanks to Launcher+ source code)
					SearchManager searchManagerService = (SearchManager) Launcher.this
							.getSystemService(Context.SEARCH_SERVICE);
					try {
						searchManagerService.stopSearch();
					} catch (Exception e) {
						e(LOG_TAG, "error stopping search", e);
					}
				}
			});
		}

		mIsNewIntent = false;
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();

		// update locked variables
		mLauncherLocked = MyLauncherSettingsHelper.getLauncherLocked(this);
	}

	@Override
	protected void onStop() {
		super.onStop();
		dismissQuickActionWindow();
	}

	@Override
	protected void onPause() {
		super.onPause();

		// ADW: removed cause it was closing app-drawer every time Home button
		// is triggered
		// ADW: it should be done only on certain circumstances
		// closeDrawer(false);
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		// Flag any binder to stop early before switching
		if (mBinder != null) {
			mBinder.mTerminate = true;
		}
		if (PROFILE_ROTATE) {
			android.os.Debug.startMethodTracing(Environment
					.getExternalStorageDirectory().getPath()
					+ "/launcher-rotate");
		}
		// CCHOW CHANGE START
		// 2013-02-03: dismiss previews, fixes memory leak on rotate
		dismissPreviews();
		// CCHOW CHANGE END
		return null;
	}

	private boolean acceptFilter() {
		final InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		return !inputManager.isFullscreenMode();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		boolean handled = super.onKeyDown(keyCode, event);
		if (!handled && acceptFilter() && keyCode != KeyEvent.KEYCODE_ENTER) {
			boolean gotKey = TextKeyListener.getInstance().onKeyDown(
					mWorkspace, mDefaultKeySsb, keyCode, event);
			if (gotKey && mDefaultKeySsb != null && mDefaultKeySsb.length() > 0) {
				// something usable has been typed - start a search
				// the typed text will be retrieved and cleared by
				// showSearchDialog()
				// If there are multiple keystrokes before the search dialog
				// takes focus,
				// onSearchRequested() will be called for every keystroke,
				// but it is idempotent, so it's fine.
				return onSearchRequested();
			}
		}

		return handled;
	}

	private String getTypedText() {
		return mDefaultKeySsb.toString();
	}

	private void clearTypedText() {
		mDefaultKeySsb.clear();
		mDefaultKeySsb.clearSpans();
		Selection.setSelection(mDefaultKeySsb, 0);
	}

	/**
	 * Restores the previous state, if it exists.
	 * 
	 * @param savedState
	 *            The previous state.
	 */
	private void restoreState(Bundle savedState) {
		if (savedState == null) {
			return;
		}

		final int currentScreen = savedState.getInt(
				RUNTIME_STATE_CURRENT_SCREEN, -1);
		if (currentScreen > -1) {
			mWorkspace.setCurrentScreen(currentScreen);
		}

		final int addScreen = savedState.getInt(
				RUNTIME_STATE_PENDING_ADD_SCREEN, -1);
		if (addScreen > -1) {
			mAddItemCellInfo = new CellLayout.CellInfo();
			final CellLayout.CellInfo addItemCellInfo = mAddItemCellInfo;
			addItemCellInfo.valid = true;
			addItemCellInfo.screen = addScreen;
			addItemCellInfo.cellX = savedState
					.getInt(RUNTIME_STATE_PENDING_ADD_CELL_X);
			addItemCellInfo.cellY = savedState
					.getInt(RUNTIME_STATE_PENDING_ADD_CELL_Y);
			addItemCellInfo.spanX = savedState
					.getInt(RUNTIME_STATE_PENDING_ADD_SPAN_X);
			addItemCellInfo.spanY = savedState
					.getInt(RUNTIME_STATE_PENDING_ADD_SPAN_Y);
			addItemCellInfo.findVacantCellsFromOccupied(savedState
					.getBooleanArray(RUNTIME_STATE_PENDING_ADD_OCCUPIED_CELLS),
					savedState.getInt(RUNTIME_STATE_PENDING_ADD_COUNT_X),
					savedState.getInt(RUNTIME_STATE_PENDING_ADD_COUNT_Y));
			mRestoring = true;
		}

		boolean renameFolder = savedState.getBoolean(
				RUNTIME_STATE_PENDING_FOLDER_RENAME, false);
		if (renameFolder) {
			long id = savedState
					.getLong(RUNTIME_STATE_PENDING_FOLDER_RENAME_ID);
			mFolderInfo = sLauncherModel.getFolderById(this, id);
			mRestoring = true;
		}
		boolean groupPick = savedState.getBoolean(
				RUNTIME_STATE_PENDING_GROUP_PICK, false);
		if (groupPick) {
			long id = savedState.getLong(RUNTIME_STATE_PENDING_GROUP_PICK_ID);
			mPickGroupInfo = LauncherModel.loadApplicationInfoById(this, id);
			mRestoring = true;
		}
	}

	/**
	 * Finds all the views we need and configure them properly.
	 */
	private void setupViews() {
		mDragLayer = (DragLayer) findViewById(R.id.drag_layer);
		final DragLayer dragLayer = mDragLayer;

		mWorkspace = (Workspace) dragLayer.findViewById(R.id.workspace);
		final Workspace workspace = mWorkspace;
		// ADW: The app drawer is now a ViewStub and we load the resource
		// depending on custom settings
		ViewStub tmp = (ViewStub) dragLayer.findViewById(R.id.stub_drawer);
		mDrawerStyle = MyLauncherSettingsHelper.getDrawerStyle(this);
		tmp.setLayoutResource(mDrawerStyles[mDrawerStyle]);
		mAllAppsGrid = (Drawer) tmp.inflate();
		mDeleteZone = (DeleteZone) dragLayer.findViewById(R.id.delete_zone);

		mMAB = (ActionButton) dragLayer.findViewById(R.id.btn_mab);
		mMAB.setFocusable(true);
		mMAB.setLauncher(this);
		mMAB.setOnClickListener(this);
		dragLayer.addDragListener(mMAB);
		/*
		 * mMAB.setOnTriggerListener(new OnTriggerListener() { public void
		 * onTrigger(View v, int whichHandle) { mDockBar.open(); } public void
		 * onGrabbedStateChange(View v, boolean grabbedState) { } public void
		 * onClick(View v) { if (allAppsOpen) { closeAllApps(true); } else {
		 * showAllApps(true, null); } } });
		 */
		mAllAppsGrid.setTextFilterEnabled(false);
		mAllAppsGrid.setDragger(dragLayer);
		mAllAppsGrid.setLauncher(this);

		workspace.setOnLongClickListener(this);
		workspace.setDragger(dragLayer);
		workspace.setLauncher(this);

		mDeleteZone.setLauncher(this);
		mDeleteZone.setDragController(dragLayer);

		dragLayer.setIgnoredDropTarget((View) mAllAppsGrid);
		dragLayer.setDragScoller(workspace);
		dragLayer.addDragListener(mDeleteZone);

		// ADW: Action Buttons (LAB/RAB)
		mLAB = (ActionButton) dragLayer.findViewById(R.id.btn_lab);
		mLAB.setLauncher(this);
		mLAB.setSpecialIcon(R.drawable.arrow_left);
		mLAB.setSpecialAction(ACTION_CATALOG_PREV);
		dragLayer.addDragListener(mLAB);
		mRAB = (ActionButton) dragLayer.findViewById(R.id.btn_rab);
		mRAB.setLauncher(this);
		mRAB.setSpecialIcon(R.drawable.arrow_right);
		mRAB.setSpecialAction(ACTION_CATALOG_NEXT);
		dragLayer.addDragListener(mRAB);
		mLAB.setOnClickListener(this);
		mRAB.setOnClickListener(this);
		// ADW: secondary aActionButtons
		mLAB2 = (ActionButton) dragLayer.findViewById(R.id.btn_lab2);
		mLAB2.setLauncher(this);
		dragLayer.addDragListener(mLAB2);
		mRAB2 = (ActionButton) dragLayer.findViewById(R.id.btn_rab2);
		mRAB2.setLauncher(this);
		dragLayer.addDragListener(mRAB2);
		mLAB2.setOnClickListener(this);
		mRAB2.setOnClickListener(this);
		// ADW: Dots ImageViews
		mPreviousView = (ImageView) findViewById(R.id.btn_scroll_left);
		mNextView = (ImageView) findViewById(R.id.btn_scroll_right);
		mPreviousView.setOnLongClickListener(this);
		mNextView.setOnLongClickListener(this);

		mMAB.setDragger(dragLayer);
		mLAB.setDragger(dragLayer);
		mRAB.setDragger(dragLayer);
		mRAB2.setDragger(dragLayer);
		mLAB2.setDragger(dragLayer);

		// ADW linearlayout with apptray, lab and rab
		mDrawerToolbar = findViewById(R.id.drawer_toolbar);

		// wait till dock is finished drawing to get the padding dimension
		ViewTreeObserver vto = mDrawerToolbar.getViewTreeObserver(); 
		vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() { 
			@Override 
			public void onGlobalLayout() { 
				mDrawerToolbar.getViewTreeObserver().removeGlobalOnLayoutListener(this); 
				int width  = mDrawerToolbar.getMeasuredWidth();
				int height = mDrawerToolbar.getMeasuredHeight();
				if (width > height) {
					mAppDrawerPadding = height;
				} else {
					mAppDrawerPadding = width;
				}
			} 
		});

		mMAB.setNextFocusUpId(R.id.drag_layer);
		mMAB.setNextFocusLeftId(R.id.drag_layer);
		mLAB.setNextFocusUpId(R.id.drag_layer);
		mLAB.setNextFocusLeftId(R.id.drag_layer);
		mRAB.setNextFocusUpId(R.id.drag_layer);
		mRAB.setNextFocusLeftId(R.id.drag_layer);
		mLAB2.setNextFocusUpId(R.id.drag_layer);
		mLAB2.setNextFocusLeftId(R.id.drag_layer);
		mRAB2.setNextFocusUpId(R.id.drag_layer);
		mRAB2.setNextFocusLeftId(R.id.drag_layer);

		if (MyLauncherSettingsHelper.getDesktopIndicator(this)) {
			mDesktopIndicator = (DesktopIndicator) (findViewById(R.id.desktop_indicator));
		}
		// ADW: Add focusability to screen items
		mLAB.setFocusable(true);
		mRAB.setFocusable(true);
		mLAB2.setFocusable(true);
		mRAB2.setFocusable(true);
		mPreviousView.setFocusable(true);
		mNextView.setFocusable(true);

		// ADW: Load the specified theme
		String themePackage = MyLauncherSettingsHelper.getThemePackageName(
				this, THEME_DEFAULT);
		PackageManager pm = getPackageManager();
		Resources themeResources = null;
		if (!themePackage.equals(THEME_DEFAULT)) {
			try {
				themeResources = pm.getResourcesForApplication(themePackage);
			} catch (NameNotFoundException e) {
				// ADW The saved theme was uninstalled so we save the default
				// one
				MyLauncherSettingsHelper.setThemePackageName(this,
						Launcher.THEME_DEFAULT);
			}
		}
		if (themeResources != null) {
			// Action Buttons

			loadThemeResource(themeResources, themePackage, "lab_bg", mLAB,
					THEME_ITEM_BACKGROUND);
			loadThemeResource(themeResources, themePackage, "rab_bg", mRAB,
					THEME_ITEM_BACKGROUND);
			loadThemeResource(themeResources, themePackage, "lab2_bg", mLAB2,
					THEME_ITEM_BACKGROUND);
			loadThemeResource(themeResources, themePackage, "rab2_bg", mRAB2,
					THEME_ITEM_BACKGROUND);
			loadThemeResource(themeResources, themePackage, "mab_bg", mMAB,
					THEME_ITEM_BACKGROUND);

			// App drawer button
			// loadThemeResource(themeResources,themePackage,"handle_icon",mMAB,THEME_ITEM_FOREGROUND);
			// View appsBg=findViewById(R.id.appsBg);
			// loadThemeResource(themeResources,themePackage,"handle",appsBg,THEME_ITEM_BACKGROUND);
			// Deletezone
			loadThemeResource(themeResources, themePackage, "ic_delete",
					mDeleteZone, THEME_ITEM_FOREGROUND);
			loadThemeResource(themeResources, themePackage,
					"delete_zone_selector", mDeleteZone, THEME_ITEM_BACKGROUND);
			// Desktop dots
			loadThemeResource(themeResources, themePackage, "home_arrows_left",
					mPreviousView, THEME_ITEM_FOREGROUND);
			loadThemeResource(themeResources, themePackage,
					"home_arrows_right", mNextView, THEME_ITEM_FOREGROUND);
			try {
				themeFont = Typeface.createFromAsset(
						themeResources.getAssets(), "themefont.ttf");
			} catch (RuntimeException e) {
				// TODO: handle exception
			}
		}
		Drawable previous = mPreviousView.getDrawable();
		Drawable next = mNextView.getDrawable();
		mWorkspace.setIndicators(previous, next);
		// ADW: EOF Themes
		updateAlmostNexusUI();
	}

	/**
	 * Creates a view representing a shortcut.
	 * 
	 * @param info
	 *            The data structure describing the shortcut.
	 * 
	 * @return A View inflated from R.layout.application.
	 */
	View createShortcut(ApplicationInfo info) {
		return createShortcut(
				R.layout.application,
				(ViewGroup) mWorkspace.getChildAt(mWorkspace.getCurrentScreen()),
				info);
	}

	/**
	 * Creates a view representing a shortcut inflated from the specified
	 * resource.
	 * 
	 * @param layoutResId
	 *            The id of the XML layout used to create the shortcut.
	 * @param parent
	 *            The group the shortcut belongs to.
	 * @param info
	 *            The data structure describing the shortcut.
	 * 
	 * @return A View inflated from layoutResId.
	 */
	View createShortcut(int layoutResId, ViewGroup parent, ApplicationInfo info) {
		CounterTextView favorite = (CounterTextView) mInflater.inflate(
				layoutResId, parent, false);

		if (!info.filtered) {
			info.icon = Utilities.createIconThumbnail(info.icon, this);
			info.filtered = true;
		}

		favorite.setCompoundDrawablesWithIntrinsicBounds(null, info.icon, null,
				null);
		if (!mUiHideLabels)
			favorite.setText(info.title);
		favorite.setTag(info);
		favorite.setOnClickListener(this);
		// ADW: Custom font
		if (themeFont != null)
			favorite.setTypeface(themeFont);
		// ADW: Counters stuff
		favorite.setCounter(info.counter, info.counterColor);
		return favorite;
	}

	/**
	 * Add an application shortcut to the workspace.
	 * 
	 * @param data
	 *            The intent describing the application.
	 * @param cellInfo
	 *            The position on screen where to create the shortcut.
	 */
	void completeAddApplication(Context context, Intent data,
			CellLayout.CellInfo cellInfo, boolean insertAtFirst) {
		cellInfo.screen = mWorkspace.getCurrentScreen();
		if (!findSingleSlot(cellInfo))
			return;

		final ApplicationInfo info = infoFromApplicationIntent(context, data);
		if (info != null) {
			mWorkspace.addApplicationShortcut(info, cellInfo, insertAtFirst);
		}
	}

	private static ApplicationInfo infoFromApplicationIntent(Context context,
			Intent data) {
		ComponentName component = data.getComponent();
		PackageManager packageManager = context.getPackageManager();
		ActivityInfo activityInfo = null;
		try {
			activityInfo = packageManager.getActivityInfo(component, 0 /*
			 * no
			 * flags
			 */);
		} catch (NameNotFoundException e) {
			e(LOG_TAG, "Couldn't find ActivityInfo for selected application", e);
		}

		if (activityInfo != null) {
			ApplicationInfo itemInfo = new ApplicationInfo();

			itemInfo.title = activityInfo.loadLabel(packageManager);
			if (itemInfo.title == null) {
				itemInfo.title = activityInfo.name;
			}

			itemInfo.setActivity(component, Intent.FLAG_ACTIVITY_NEW_TASK
					| Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
			// itemInfo.icon = activityInfo.loadIcon(packageManager);
			itemInfo.container = ItemInfo.NO_ID;

			itemInfo.icon = LauncherModel.getIcon(packageManager, context,
					activityInfo);

			return itemInfo;
		}

		return null;
	}

	/**
	 * Add a shortcut to the workspace.
	 * 
	 * @param data
	 *            The intent describing the shortcut.
	 * @param cellInfo
	 *            The position on screen where to create the shortcut.
	 * @param insertAtFirst
	 */
	private void completeAddShortcut(Intent data, CellLayout.CellInfo cellInfo,
			boolean insertAtFirst) {
		cellInfo.screen = mWorkspace.getCurrentScreen();
		if (!findSingleSlot(cellInfo))
			return;

		final ApplicationInfo info = addShortcut(this, data, cellInfo, false);

		if (!mRestoring) {
			sLauncherModel.addDesktopItem(info);

			final View view = createShortcut(info);
			mWorkspace.addInCurrentScreen(view, cellInfo.cellX, cellInfo.cellY,
					1, 1, insertAtFirst);
		} else if (sLauncherModel.isDesktopLoaded()) {
			sLauncherModel.addDesktopItem(info);
		}
	}

	/**
	 * Add a widget to the workspace.
	 * 
	 * @param data
	 *            The intent describing the appWidgetId.
	 * @param cellInfo
	 *            The position on screen where to create the widget.
	 */
	private void completeAddAppWidget(Intent data,
			CellLayout.CellInfo cellInfo, final boolean insertAtFirst) {

		Bundle extras = data.getExtras();
		final int appWidgetId = extras.getInt(
				AppWidgetManager.EXTRA_APPWIDGET_ID, -1);

		final AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager
				.getAppWidgetInfo(appWidgetId);

		// Calculate the grid spans needed to fit this widget
		CellLayout layout = (CellLayout) mWorkspace.getChildAt(cellInfo.screen);
		final int[] spans = layout.rectToCell(appWidgetInfo.minWidth,
				appWidgetInfo.minHeight);
		final CellLayout.CellInfo cInfo = cellInfo;
		AlertDialog.Builder builder;

		final View span_dlg_layout = View.inflate(Launcher.this,
				R.layout.widget_span_setup, null);
		final NumberPicker ncols = (NumberPicker) span_dlg_layout
				.findViewById(R.id.widget_columns_span);
		ncols.setRange(1, mWorkspace.currentDesktopColumns());
		ncols.setCurrent(spans[0]);

		final NumberPicker nrows = (NumberPicker) span_dlg_layout
				.findViewById(R.id.widget_rows_span);

		nrows.setRange(1, mWorkspace.currentDesktopRows());
		nrows.setCurrent(spans[1]);
		builder = new AlertDialog.Builder(Launcher.this);
		builder.setView(span_dlg_layout);

		mAlertDialog = builder.create();
		mAlertDialog.setMessage(null);
		mAlertDialog.setTitle(null);
		mAlertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getResources()
				.getString(android.R.string.ok),
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				spans[0] = ncols.getCurrent();
				spans[1] = nrows.getCurrent();
				realAddWidget(appWidgetInfo, cInfo, spans, appWidgetId,
						insertAtFirst);
			}
		});
		mAlertDialog.setCanceledOnTouchOutside(true);
		mAlertDialog.setCancelable(true);
		mAlertDialog.show();
	}

	public LauncherAppWidgetHost getAppWidgetHost() {
		return mAppWidgetHost;
	}

	static ApplicationInfo addShortcut(Context context, Intent data,
			CellLayout.CellInfo cellInfo, boolean notify) {

		final ApplicationInfo info = infoFromShortcutIntent(context, data);
		LauncherModel.addItemToDatabase(context, info,
				LauncherSettings.Favorites.CONTAINER_DESKTOP, cellInfo.screen,
				cellInfo.cellX, cellInfo.cellY, notify);

		return info;
	}

	private static ApplicationInfo infoFromShortcutIntent(Context context,
			Intent data) {
		Intent intent = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
		String name = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
		Bitmap bitmap = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON);

		Drawable icon = null;
		boolean filtered = false;
		boolean customIcon = false;
		ShortcutIconResource iconResource = null;

		if (bitmap != null) {
			icon = new FastBitmapDrawable(Utilities.createBitmapThumbnail(
					bitmap, context));
			filtered = true;
			customIcon = true;
		} else {
			Parcelable extra = data
					.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
			if (extra != null && extra instanceof ShortcutIconResource) {
				try {
					iconResource = (ShortcutIconResource) extra;
					final PackageManager packageManager = context
							.getPackageManager();
					Resources resources = packageManager
							.getResourcesForApplication(iconResource.packageName);
					final int id = resources.getIdentifier(
							iconResource.resourceName, null, null);
					icon = resources.getDrawable(id);
				} catch (Exception e) {
					w(LOG_TAG, "Could not load shortcut icon: " + extra);
				}
			}
		}

		if (icon == null) {
			icon = context.getPackageManager().getDefaultActivityIcon();
		}

		final ApplicationInfo info = new ApplicationInfo();
		info.icon = icon;
		info.filtered = filtered;
		info.title = name;
		info.intent = intent;
		info.customIcon = customIcon;
		info.iconResource = iconResource;

		return info;
	}

	/**
	 * 
	 */
	void closeSystemDialogs() {
		getWindow().closeAllPanels();

		try {
			dismissDialog(DIALOG_CREATE_SHORTCUT);
			// Unlock the workspace if the dialog was showing
			mWorkspace.unlock();
		} catch (Exception e) {
			// An exception is thrown if the dialog is not visible, which is
			// fine
		}

		try {
			dismissDialog(DIALOG_RENAME_FOLDER);
			// Unlock the workspace if the dialog was showing
			mWorkspace.unlock();
		} catch (Exception e) {
			// An exception is thrown if the dialog is not visible, which is
			// fine
		}
		try {
			dismissDialog(DIALOG_CHOOSE_GROUP);
			// Unlock the workspace if the dialog was showing
		} catch (Exception e) {
			// An exception is thrown if the dialog is not visible, which is
			// fine
		}
		try {
			dismissDialog(DIALOG_NEW_GROUP);
			// Unlock the workspace if the dialog was showing
		} catch (Exception e) {
			// An exception is thrown if the dialog is not visible, which is
			// fine
		}
		try {
			dismissDialog(DIALOG_DELETE_GROUP_CONFIRM);
			// Unlock the workspace if the dialog was showing
		} catch (Exception e) {
			// An exception is thrown if the dialog is not visible, which is
			// fine
		}
		try {
			removeDialog(DIALOG_PICK_GROUPS);
			// Unlock the workspace if the dialog was showing
		} catch (Exception e) {
			// An exception is thrown if the dialog is not visible, which is
			// fine
		}

		try {
			removeDialog(DIALOG_ADD_WIDGET_FAILURE);
		} catch (Exception e) {
			// An exception is thrown if the dialog is not visible, which is
			// fine
		}
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onNewIntent(android.content.Intent)
	 */
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		// Close the menu
		if (Intent.ACTION_MAIN.equals(intent.getAction())) {
			closeSystemDialogs();

			// Set this flag so that onResume knows to close the search dialog
			// if it's open,
			// because this was a new intent (thus a press of 'home' or some
			// such) rather than
			// for example onResume being called when the user pressed the
			// 'back' button.
			mIsNewIntent = true;

			if ((intent.getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) {
				if (!isAllAppsVisible() || mHomeBinding == BIND_APPS)
					fireActionBinding(mHomeBinding, BIND_TYPE_HOME);
				if (mHomeBinding != BIND_APPS) {
					closeDrawer(true);
				}
				final View v = getWindow().peekDecorView();
				if (v != null && v.getWindowToken() != null) {
					InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
				}
			} else {
				closeDrawer(false);
			}
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		// NOTE: Do NOT do this. Ever. This is a terrible and horrifying hack.
		//
		// Home loads the content of the workspace on a background thread. This
		// means that
		// a previously focused view will be, after orientation change, added to
		// the view
		// hierarchy at an undeterminate time in the future. If we were to
		// invoke
		// super.onRestoreInstanceState() here, the focus restoration would fail
		// because the
		// view to focus does not exist yet.
		//
		// However, not invoking super.onRestoreInstanceState() is equally bad.
		// In such a case,
		// panels would not be restored properly. For instance, if the menu is
		// open then the
		// user changes the orientation, the menu would not be opened in the new
		// orientation.
		//
		// To solve both issues Home messes up with the internal state of the
		// bundle to remove
		// the properties it does not want to see restored at this moment. After
		// invoking
		// super.onRestoreInstanceState(), it removes the panels state.
		//
		// Later, when the workspace is done loading, Home calls
		// super.onRestoreInstanceState()
		// again to restore focus and other view properties. It will not,
		// however, restore
		// the panels since at this point the panels' state has been removed
		// from the bundle.
		//
		// This is a bad example, do not do this.
		//
		// If you are curious on how this code was put together, take a look at
		// the following
		// in Android's source code:
		// - Activity.onRestoreInstanceState()
		// - PhoneWindow.restoreHierarchyState()
		// - PhoneWindow.DecorView.onAttachedToWindow()
		//
		// The source code of these various methods shows what states should be
		// kept to
		// achieve what we want here.

		Bundle windowState = savedInstanceState
				.getBundle("android:viewHierarchyState");
		SparseArray<Parcelable> savedStates = null;
		int focusedViewId = View.NO_ID;

		if (windowState != null) {
			savedStates = windowState.getSparseParcelableArray("android:views");
			windowState.remove("android:views");
			focusedViewId = windowState.getInt("android:focusedViewId",
					View.NO_ID);
			windowState.remove("android:focusedViewId");
		}

		super.onRestoreInstanceState(savedInstanceState);

		if (windowState != null) {
			windowState.putSparseParcelableArray("android:views", savedStates);
			windowState.putInt("android:focusedViewId", focusedViewId);
			windowState.remove("android:Panels");
		}

		mSavedInstanceState = savedInstanceState;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// ADW: If we leave the menu open, on restoration it will try to auto
		// find
		// the ocupied cells. But this could happen before the workspace is
		// fully loaded,
		// so it can cause a NPE cause of the way we load the desktop
		// columns/rows count.
		// I prefer to just close it than diggin the code to make it load
		// later...
		// Accepting patches :-)
		closeOptionsMenu();
		super.onSaveInstanceState(outState);

		outState.putInt(RUNTIME_STATE_CURRENT_SCREEN,
				mWorkspace.getCurrentScreen());
		if (mWorkspace != null) {
			final ArrayList<Folder> folders = mWorkspace.getOpenFolders();
			if (folders.size() > 0) {
				final int count = folders.size();
				long[] ids = new long[count];
				for (int i = 0; i < count; i++) {
					final FolderInfo info = folders.get(i).getInfo();
					ids[i] = info.id;
				}
				outState.putLongArray(RUNTIME_STATE_USER_FOLDERS, ids);
			}
		}
		final boolean isConfigurationChange = getChangingConfigurations() != 0;

		// When the drawer is opened and we are saving the state because of a
		// configuration change
		if (allAppsOpen && isConfigurationChange) {
			outState.putBoolean(RUNTIME_STATE_ALL_APPS_FOLDER, true);
		}
		if (mAddItemCellInfo != null && mAddItemCellInfo.valid
				&& mWaitingForResult) {
			final CellLayout.CellInfo addItemCellInfo = mAddItemCellInfo;
			final CellLayout layout = (CellLayout) mWorkspace
					.getChildAt(addItemCellInfo.screen);

			outState.putInt(RUNTIME_STATE_PENDING_ADD_SCREEN,
					addItemCellInfo.screen);
			outState.putInt(RUNTIME_STATE_PENDING_ADD_CELL_X,
					addItemCellInfo.cellX);
			outState.putInt(RUNTIME_STATE_PENDING_ADD_CELL_Y,
					addItemCellInfo.cellY);
			outState.putInt(RUNTIME_STATE_PENDING_ADD_SPAN_X,
					addItemCellInfo.spanX);
			outState.putInt(RUNTIME_STATE_PENDING_ADD_SPAN_Y,
					addItemCellInfo.spanY);

			// check that the workspace screen is valid to avoid a NPE
			if (layout != null) {
				outState.putInt(RUNTIME_STATE_PENDING_ADD_COUNT_X,
						layout.getCountX());
				outState.putInt(RUNTIME_STATE_PENDING_ADD_COUNT_Y,
						layout.getCountY());
				outState.putBooleanArray(RUNTIME_STATE_PENDING_ADD_OCCUPIED_CELLS,
						layout.getOccupiedCells());
			}
		}

		if (mFolderInfo != null && mWaitingForResult) {
			outState.putBoolean(RUNTIME_STATE_PENDING_FOLDER_RENAME, true);
			outState.putLong(RUNTIME_STATE_PENDING_FOLDER_RENAME_ID,
					mFolderInfo.id);
		}
		if (mPickGroupInfo != null && mWaitingForResult) {
			outState.putBoolean(RUNTIME_STATE_PENDING_GROUP_PICK, true);
			outState.putLong(RUNTIME_STATE_PENDING_GROUP_PICK_ID,
					mPickGroupInfo.id);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (mAlertDialog != null) {
			if (mAlertDialog.isShowing()) {
				mAlertDialog.dismiss();
			}
		}

		mDestroyed = true;
		// ADW: unregister the sharedpref listener
		getSharedPreferences("launcher.preferences.almostnexus",
				Context.MODE_PRIVATE)
				.unregisterOnSharedPreferenceChangeListener(this);

		try {
			mAppWidgetHost.stopListening();
		} catch (NullPointerException ex) {
			w(LOG_TAG,
					"problem while stopping AppWidgetHost during Launcher destruction",
					ex);
		}

		TextKeyListener.getInstance().release();

		mAllAppsGrid.clearTextFilter();
		mAllAppsGrid.setAdapter(null);

		// unbind items
		sLauncherModel.unbind();
		sLauncherModel.abortLoaders();

		mWorkspace.unbindWidgetScrollableViews(); // removes views

		getContentResolver().unregisterContentObserver(mObserver);
		getContentResolver().unregisterContentObserver(mWidgetObserver);
		unregisterReceiver(mApplicationsReceiver);
		unregisterReceiver(mCloseSystemDialogsReceiver);
		if (mCounterReceiver != null)
			unregisterReceiver(mCounterReceiver);
		mWorkspace.unregisterProvider();

		// remove screen editor view
		if (mDragLayer != null) {
			if (mScreensEditor != null) {
				mDragLayer.removeView(mScreensEditor);
				mScreensEditor = null;
			}
		}

		// set nulls
		mAppWidgetHost = null;
	}

	@Override
	public void startActivityForResult(Intent intent, int requestCode) {
		if (intent == null)
			return;
		// ADW: closing drawer, removed from onpause
		if (requestCode != REQUEST_SHOW_APP_LIST && // do not close drawer if it
				// is for switching
				// catalogue.
				!CustomShortcutActivity.ACTION_LAUNCHERACTION.equals(intent
						.getAction()))
			closeDrawer(false);
		if (requestCode >= 0)
			mWaitingForResult = true;
		try {
			super.startActivityForResult(intent, requestCode);
		} catch (Exception e) {
			Toast.makeText(this, R.string.activity_not_found,
					Toast.LENGTH_SHORT).show();
			;
		}
	}

	// CCHOW: global search is disabled for non-system app Launchers <Android 2.2
	/*
	 * @Override public void startSearch(String initialQuery, boolean
	 * selectInitialQuery, Bundle appSearchData, boolean globalSearch) {
	 * 
	 * closeDrawer(false);
	 * 
	 * // Slide the search widget to the top, if it's on the current screen, //
	 * otherwise show the search dialog immediately. Search searchWidget =
	 * mWorkspace.findSearchWidgetOnCurrentScreen(); if (searchWidget == null) {
	 * showSearchDialog(initialQuery, selectInitialQuery, appSearchData,
	 * globalSearch); } else { searchWidget.startSearch(initialQuery,
	 * selectInitialQuery, appSearchData, globalSearch); // show the currently
	 * typed text in the search widget while sliding
	 * searchWidget.setQuery(getTypedText()); } }
	 */

	/**
	 * Show the search dialog immediately, without changing the search widget.
	 * 
	 * @see Activity#startSearch(String, boolean, android.os.Bundle, boolean)
	 */
	/*
	 * void showSearchDialog(String initialQuery, boolean selectInitialQuery,
	 * Bundle appSearchData, boolean globalSearch) {
	 * 
	 * if (initialQuery == null) { // Use any text typed in the launcher as the
	 * initial query initialQuery = getTypedText(); clearTypedText(); } if
	 * (appSearchData == null) { appSearchData = new Bundle();
	 * appSearchData.putString(SearchManager.SOURCE, "launcher-search"); }
	 * 
	 * final SearchManager searchManager = (SearchManager)
	 * getSystemService(Context.SEARCH_SERVICE);
	 * 
	 * final Search searchWidget = mWorkspace.findSearchWidgetOnCurrentScreen();
	 * if (searchWidget != null) { // This gets called when the user leaves the
	 * search dialog to go back to // the Launcher.
	 * searchManager.setOnCancelListener(new SearchManager.OnCancelListener() {
	 * public void onCancel() { searchManager.setOnCancelListener(null);
	 * stopSearch(); } }); }
	 * 
	 * searchManager.startSearch(initialQuery, selectInitialQuery,
	 * getComponentName(), appSearchData, globalSearch); }
	 */

	/**
	 * Cancel search dialog if it is open.
	 */
	/*
	 * void stopSearch() { // Close search dialog SearchManager searchManager =
	 * (SearchManager) getSystemService(Context.SEARCH_SERVICE);
	 * searchManager.stopSearch(); // Restore search widget to its normal
	 * position Search searchWidget =
	 * mWorkspace.findSearchWidgetOnCurrentScreen(); if (searchWidget != null) {
	 * searchWidget.stopSearch(false); } }
	 */

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (mDesktopLocked && mSavedInstanceState == null)
			return false;

		super.onCreateOptionsMenu(menu);

		menu.add(MENU_GROUP_ADD, MENU_ADD, 0, R.string.menu_add)
		.setIcon(android.R.drawable.ic_menu_add)
		.setAlphabeticShortcut('A');

		menu.add(MENU_GROUP_WALLPAPER, MENU_WALLPAPER_SETTINGS, 0,
				R.string.menu_wallpaper)
				.setIcon(android.R.drawable.ic_menu_gallery)
				.setAlphabeticShortcut('W');

		menu.add(MENU_GROUP_LAUNCHER_SETUP, MENU_EDIT, 0,
				R.string.menu_edit_desktop)
				.setIcon(android.R.drawable.ic_menu_edit)
				.setAlphabeticShortcut('E');

		// CCHOW CHANGE START
		// disable manage apps menu item
		/*
		 * // Manage apps for workspace createManageApplications( menu,
		 * MENU_GROUP_NORMAL );
		 */
		// CCHOW CHANGE END

		// ADW: add custom settings
		menu.add(MENU_GROUP_LAUNCHER_SETUP, MENU_ALMOSTNEXUS, 0,
				R.string.menu_launcher_settings)
				.setIcon(R.drawable.ic_menu_launcher_settings)
				.setAlphabeticShortcut('X');

		menu.add(MENU_GROUP_NORMAL, MENU_LOCK_DESKTOP, 0, R.string.menu_lock)
		.setIcon(R.drawable.ic_menu_block)
		.setAlphabeticShortcut('L');

		menu.add(MENU_GROUP_NORMAL, MENU_SEARCH, 0, R.string.menu_search)
		.setIcon(android.R.drawable.ic_search_category_default)
		.setAlphabeticShortcut(SearchManager.MENU_KEY);

		final Intent settings = new Intent(
				android.provider.Settings.ACTION_SETTINGS);
		settings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		menu.add(MENU_GROUP_NORMAL, MENU_SETTINGS, 0, R.string.menu_settings)
		.setIcon(android.R.drawable.ic_menu_preferences)
		.setAlphabeticShortcut('P').setIntent(settings);

		// drawer options
		menu.add(MENU_GROUP_CATALOGUE, MENU_APP_SWITCH_GRP, 0,
				R.string.app_group_choose).setIcon(
						android.R.drawable.ic_menu_agenda);

		menu.add(MENU_GROUP_CATALOGUE, MENU_APP_GRP_CONFIG, 0,
				R.string.app_group_config).setIcon(
						android.R.drawable.ic_menu_manage);

		menu.add(MENU_GROUP_CATALOGUE, MENU_APP_DELETE_GRP, 0,
				R.string.app_group_delete).setIcon(
						android.R.drawable.ic_menu_delete);

		// CCHOW CHANGE START
		/*
		 * // Manage apps for apps drawer createManageApplications(menu,
		 * MENU_GROUP_CATALOGUE);
		 */
		// CCHOW CHANGE END

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		if (mIsEditMode || mIsWidgetEditMode || showingPreviews)
			return false;

		// We can't trust the view state here since views we may not be done
		// binding.
		// Get the vacancy state from the model instead.
		mMenuAddInfo = mWorkspace.findAllVacantCellsFromModel();

		boolean forceHidden = getResources().getBoolean(
				R.bool.force_hidden_settings);
		boolean showmenu = true;
		if (!forceHidden && LOGD) {
			// //ADW: Check if this is the default launcher
			checkDefaultLauncher();
		}

		if (allAppsOpen)
			showmenu = false;

		menu.setGroupVisible(MENU_GROUP_ADD, !allAppsOpen && !mLauncherLocked
				&& mMenuAddInfo != null && mMenuAddInfo.valid);
		menu.setGroupVisible(MENU_GROUP_WALLPAPER, !allAppsOpen
				&& !mLauncherLocked
				&& !(mMenuAddInfo != null && mMenuAddInfo.valid));

		menu.setGroupVisible(MENU_GROUP_LAUNCHER_SETUP, showmenu
				&& !mLauncherLocked);
		menu.setGroupVisible(MENU_GROUP_NORMAL, !allAppsOpen);
		menu.setGroupVisible(MENU_GROUP_CATALOGUE, allAppsOpen
				&& !mLauncherLocked);
		if (mLauncherLocked) {
			menu.findItem(MENU_LOCK_DESKTOP).setTitle(R.string.menu_unlock)
			.setIcon(R.drawable.ic_menu_unblock);
		} else {
			menu.findItem(MENU_LOCK_DESKTOP).setTitle(R.string.menu_lock)
			.setIcon(R.drawable.ic_menu_block);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_ADD:
			addItems();
			return true;
		case MENU_WALLPAPER_SETTINGS:
			startWallpaper();
			return true;
		case MENU_SEARCH:
			onSearchRequested();
			return true;
		case MENU_EDIT:
			// showNotifications();
			// ADW: temp usage for desktop eiting
			if (allAppsOpen)
				closeAllApps(false);
			startDesktopEdit();
			return true;
		case MENU_ALMOSTNEXUS:
			dismissPreviews();
			showCustomConfig();
			return true;
		case MENU_APP_GRP_CONFIG:
			showAppPickerList(false);
			return true;
		case MENU_APP_GRP_RENAME:
			showNewGrpDialog();
			return true;
		case MENU_APP_SWITCH_GRP:
			showSwitchGrp();
			return true;
		case MENU_APP_DELETE_GRP:
			showDeleteGrpDialog();
			return true;
		case MENU_LOCK_DESKTOP:
			// toggle icons locked
			if (mLauncherLocked) {
				final String passSecure = mObscurePrefs.getString("pw", null);
				if (passSecure != null && passSecure.length() > 0) {
					showDialog(DIALOG_GET_LAUNCHER_UNLOCK_PASSWORD);
				} else {
					// no password was set, so unlock right away
					Toast.makeText(this, R.string.toast_launcher_unlock,
							Toast.LENGTH_SHORT).show();
					mLauncherLocked = false;
					// commit setting
					MyLauncherSettingsHelper.setLauncherLocked(this,
							mLauncherLocked);
				}
			} else {
				// lock if unlocked
				Toast.makeText(this, R.string.toast_launcher_lock,
						Toast.LENGTH_SHORT).show();
				mLauncherLocked = true;
				// commit setting
				MyLauncherSettingsHelper.setLauncherLocked(this,
						mLauncherLocked);
			}
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private void showAppPickerList(boolean isNew) {

		final AppCatalogueFilter flt = sLauncherModel.getApplicationsAdapter()
				.getCatalogueFilter();
		if (!flt.isUserGroup()) {
			Toast.makeText(this, getString(R.string.app_group_config_error),
					Toast.LENGTH_SHORT).show();
			return;
		}
		Intent i = new Intent(this, AppInfoListActivity.class);
		i.putExtra(AppInfoListActivity.EXTRA_CATALOGUE_INDEX,
				flt.getCurrentFilterIndex());
		i.putExtra(AppInfoListActivity.EXTRA_CATALOGUE_NEW, isNew);
		startActivityForResult(i, REQUEST_SHOW_APP_LIST);
	}

	void showDeleteGrpDialog() {
		if (!sLauncherModel.getApplicationsAdapter().getCatalogueFilter()
				.isUserGroup()) {
			Toast.makeText(this, getString(R.string.app_group_remove_error),
					Toast.LENGTH_SHORT).show();
			return;
		}
		showDialog(DIALOG_DELETE_GROUP_CONFIRM);
	}

	void showNewGrpDialog() {
		mWaitingForResult = true;
		showDialog(DIALOG_NEW_GROUP);
	}

	/**
	 * Indicates that we want global search for this activity by setting the
	 * globalSearch argument for {@link #startSearch} to true.
	 */

	@Override
	public boolean onSearchRequested() {
		startSearch(getTypedText(), false, null, true);
		clearTypedText();
		return true;
	}

	private void addItems() {
		showAddDialog(mMenuAddInfo);
	}

	private void removeShortcutsForPackage(String packageName) {
		if (packageName != null && packageName.length() > 0) {
			mWorkspace.removeShortcutsForPackage(packageName);
		}
	}

	private void updateShortcutsForPackage(String packageName) {
		if (packageName != null && packageName.length() > 0) {
			mWorkspace.updateShortcutsForPackage(packageName);
			// ADW: Update ActionButtons icons
			mLAB.reloadIcon(packageName);
			mLAB2.reloadIcon(packageName);
			mRAB.reloadIcon(packageName);
			mRAB2.reloadIcon(packageName);
			mMAB.reloadIcon(packageName);
		}
	}

	void addAppWidget(final Intent data) {
		// TODO: catch bad widget exception when sent
		int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);

		if (Build.VERSION.SDK_INT < 8 && SEARCH_WIDGET.equals(data.getStringExtra(EXTRA_CUSTOM_WIDGET))) {
			// We don't need this any more, since this isn't a real app widget.
			mAppWidgetHost.deleteAppWidgetId(appWidgetId); 
			// add the search widget 
			// addSearch();
		} else {

			AppWidgetProviderInfo appWidget = mAppWidgetManager
					.getAppWidgetInfo(appWidgetId);

			try {
				Bundle metadata = getPackageManager().getReceiverInfo(
						appWidget.provider, PackageManager.GET_META_DATA).metaData;
				if (metadata != null) {
					if (metadata
							.containsKey(LauncherMetadata.Requirements.APIVersion)) {
						int requiredApiVersion = metadata
								.getInt(LauncherMetadata.Requirements.APIVersion);
						if (requiredApiVersion > LauncherMetadata.CurrentAPIVersion) {
							onActivityResult(REQUEST_CREATE_APPWIDGET,
									Activity.RESULT_CANCELED, data);
							// Show some information here to tell the user why the
							// widget is rejected.
							showDialog(DIALOG_ADD_WIDGET_FAILURE);
							return;
						}
					}
				}
			} catch (PackageManager.NameNotFoundException expt) {
				// No Metadata available... then it is all OK...
			}
			configureOrAddAppWidget(data);
		}
	}

	private void configureOrAddAppWidget(Intent data) {
		int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
		AppWidgetProviderInfo appWidget = mAppWidgetManager
				.getAppWidgetInfo(appWidgetId);
		if (appWidget.configure != null) {
			// Launch over to configure widget, if needed
			Intent intent = new Intent(
					AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
			intent.setComponent(appWidget.configure);
			intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

			startActivityForResult(intent, REQUEST_CREATE_APPWIDGET);
		} else {
			// Otherwise just add it
			onActivityResult(REQUEST_CREATE_APPWIDGET, Activity.RESULT_OK, data);
		}
	}


	void processShortcut(Intent intent, int requestCodeApplication,
			int requestCodeShortcut) {
		// Handle case where user selected "Applications"
		String applicationName = getResources().getString(
				R.string.group_applications);
		String shortcutName = intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);

		if (applicationName != null && applicationName.equals(shortcutName)) {
			Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
			mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

			Intent pickIntent = new Intent(Intent.ACTION_PICK_ACTIVITY);
			pickIntent.putExtra(Intent.EXTRA_INTENT, mainIntent);
			startActivityForResult(pickIntent, requestCodeApplication);
		} else {
			startActivityForResult(intent, requestCodeShortcut);
		}
	}

	void addLiveFolder(Intent intent) {
		// Handle case where user selected "Folder"
		String folderName = getResources().getString(R.string.group_folder);
		String shortcutName = intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);

		if (folderName != null && folderName.equals(shortcutName)) {
			addFolder(!mDesktopLocked);
		} else {
			startActivityForResult(intent, REQUEST_CREATE_LIVE_FOLDER);
		}
	}

	void addFolder(boolean insertAtFirst) {
		UserFolderInfo folderInfo = new UserFolderInfo();
		folderInfo.title = getText(R.string.folder_name);

		CellLayout.CellInfo cellInfo = mAddItemCellInfo;
		cellInfo.screen = mWorkspace.getCurrentScreen();
		if (!findSingleSlot(cellInfo))
			return;

		// Update the model
		LauncherModel.addItemToDatabase(this, folderInfo,
				LauncherSettings.Favorites.CONTAINER_DESKTOP,
				mWorkspace.getCurrentScreen(), cellInfo.cellX, cellInfo.cellY,
				false);
		sLauncherModel.addDesktopItem(folderInfo);
		sLauncherModel.addFolder(folderInfo);

		// Create the view
		FolderIcon newFolder = FolderIcon
				.fromXml(R.layout.folder_icon, this, (ViewGroup) mWorkspace
						.getChildAt(mWorkspace.getCurrentScreen()), folderInfo);
		if (themeFont != null)
			((TextView) newFolder).setTypeface(themeFont);
		mWorkspace.addInCurrentScreen(newFolder, cellInfo.cellX,
				cellInfo.cellY, 1, 1, insertAtFirst);
	}

	private void completeAddLiveFolder(Intent data,
			CellLayout.CellInfo cellInfo, boolean insertAtFirst) {
		cellInfo.screen = mWorkspace.getCurrentScreen();
		if (!findSingleSlot(cellInfo))
			return;

		final LiveFolderInfo info = addLiveFolder(this, data, cellInfo, false);

		if (!mRestoring) {
			sLauncherModel.addDesktopItem(info);

			final View view = LiveFolderIcon.fromXml(R.layout.live_folder_icon,
					this, (ViewGroup) mWorkspace.getChildAt(mWorkspace
							.getCurrentScreen()), info);
			if (themeFont != null)
				((TextView) view).setTypeface(themeFont);
			mWorkspace.addInCurrentScreen(view, cellInfo.cellX, cellInfo.cellY,
					1, 1, insertAtFirst);
		} else if (sLauncherModel.isDesktopLoaded()) {
			sLauncherModel.addDesktopItem(info);
		}
	}

	static LiveFolderInfo addLiveFolder(Context context, Intent data,
			CellLayout.CellInfo cellInfo, boolean notify) {

		Intent baseIntent = data
				.getParcelableExtra(LiveFolders.EXTRA_LIVE_FOLDER_BASE_INTENT);
		String name = data.getStringExtra(LiveFolders.EXTRA_LIVE_FOLDER_NAME);

		Drawable icon = null;
		boolean filtered = false;
		Intent.ShortcutIconResource iconResource = null;

		Parcelable extra = data
				.getParcelableExtra(LiveFolders.EXTRA_LIVE_FOLDER_ICON);
		if (extra != null && extra instanceof Intent.ShortcutIconResource) {
			try {
				iconResource = (Intent.ShortcutIconResource) extra;
				final PackageManager packageManager = context
						.getPackageManager();
				Resources resources = packageManager
						.getResourcesForApplication(iconResource.packageName);
				final int id = resources.getIdentifier(
						iconResource.resourceName, null, null);
				icon = resources.getDrawable(id);
			} catch (Exception e) {
				w(LOG_TAG, "Could not load live folder icon: " + extra);
			}
		}

		if (icon == null) {
			icon = context.getResources().getDrawable(
					R.drawable.ic_launcher_folder);
		}

		final LiveFolderInfo info = new LiveFolderInfo();
		info.icon = icon;
		info.filtered = filtered;
		info.title = name;
		info.iconResource = iconResource;
		info.uri = data.getData();
		info.baseIntent = baseIntent;
		info.displayMode = data.getIntExtra(
				LiveFolders.EXTRA_LIVE_FOLDER_DISPLAY_MODE,
				LiveFolders.DISPLAY_MODE_GRID);

		LauncherModel.addItemToDatabase(context, info,
				LauncherSettings.Favorites.CONTAINER_DESKTOP, cellInfo.screen,
				cellInfo.cellX, cellInfo.cellY, notify);
		sLauncherModel.addFolder(info);

		return info;
	}

	private boolean findSingleSlot(CellLayout.CellInfo cellInfo) {
		final int[] xy = new int[2];
		if (findSlot(cellInfo, xy, 1, 1)) {
			cellInfo.cellX = xy[0];
			cellInfo.cellY = xy[1];
			return true;
		}
		return false;
	}

	private boolean findSlot(CellLayout.CellInfo cellInfo, int[] xy, int spanX,
			int spanY) {
		if (!cellInfo.findCellForSpan(xy, spanX, spanY)) {
			boolean[] occupied = mSavedState != null ? mSavedState
					.getBooleanArray(RUNTIME_STATE_PENDING_ADD_OCCUPIED_CELLS)
					: null;
					cellInfo = mWorkspace.findAllVacantCells(occupied);
					if (!cellInfo.findCellForSpan(xy, spanX, spanY)) {
						Toast.makeText(this, getString(R.string.out_of_space),
								Toast.LENGTH_SHORT).show();
						return false;
					}
		}
		return true;
	}

	private void showNotifications() {
		if (mHideStatusBar) {
			fullScreen(false);
			mShouldHideStatusbaronFocus = true;
		}
		try {
			Object service = getSystemService("statusbar");
			if (service != null) {
				Method expand = service.getClass().getMethod("expand");
				expand.invoke(service);
			}
		} catch (Exception e) {
		}
	}

	private void startWallpaper() {
		final Intent pickWallpaper = new Intent(Intent.ACTION_SET_WALLPAPER);
		Intent chooser = Intent.createChooser(pickWallpaper,
				getText(R.string.chooser_wallpaper));
		WallpaperManager wm = (WallpaperManager) getSystemService(Context.WALLPAPER_SERVICE);
		WallpaperInfo wi = wm.getWallpaperInfo();
		if (wi != null && wi.getSettingsActivity() != null) {
			LabeledIntent li = new LabeledIntent(getPackageName(),
					R.string.configure_wallpaper, 0);
			li.setClassName(wi.getPackageName(), wi.getSettingsActivity());
			chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] { li });
		}
		startActivity(chooser);
	}

	/**
	 * Registers various intent receivers. The current implementation registers
	 * only a wallpaper intent receiver to let other applications change the
	 * wallpaper.
	 */
	private void registerIntentReceivers() {
		boolean useNotifReceiver = MyLauncherSettingsHelper
				.getNotifReceiver(this);
		if (useNotifReceiver && mCounterReceiver == null) {
			mCounterReceiver = new CounterReceiver(this);
			mCounterReceiver
			.setCounterListener(new CounterReceiver.OnCounterChangedListener() {
				public void onTrigger(String pname, int counter,
						int color) {
					updateCountersForPackage(pname, counter, color);
				}
			});
			registerReceiver(mCounterReceiver, mCounterReceiver.getFilter());
		}
		if (sWallpaperReceiver == null) {
			final Application application = getApplication();

			sWallpaperReceiver = new WallpaperIntentReceiver(application, this);

			IntentFilter filter = new IntentFilter(
					Intent.ACTION_WALLPAPER_CHANGED);
			application.registerReceiver(sWallpaperReceiver, filter);
		} else {
			sWallpaperReceiver.setLauncher(this);
		}
		IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
		filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
		filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
		filter.addDataScheme("package");
		registerReceiver(mApplicationsReceiver, filter);
		filter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
		registerReceiver(mCloseSystemDialogsReceiver, filter);
		// ADW: damn, this should be only for froyo
		filter = new IntentFilter();
		filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
		filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
		registerReceiver(mApplicationsReceiver, filter);

	}

	/**
	 * Registers various content observers. The current implementation registers
	 * only a favorites observer to keep track of the favorites applications.
	 */
	private void registerContentObservers() {
		ContentResolver resolver = getContentResolver();
		resolver.registerContentObserver(
				LauncherSettings.Favorites.CONTENT_URI, true, mObserver);
		resolver.registerContentObserver(
				LauncherProvider.CONTENT_APPWIDGET_RESET_URI, true,
				mWidgetObserver);
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_DOWN) {
			switch (event.getKeyCode()) {
			case KeyEvent.KEYCODE_BACK:
				return true;
			case KeyEvent.KEYCODE_HOME:
				return true;
			}
		} else if (event.getAction() == KeyEvent.ACTION_UP) {
			switch (event.getKeyCode()) {
			case KeyEvent.KEYCODE_BACK:
				if (!event.isCanceled()) {
					mWorkspace.dispatchKeyEvent(event);
					if (allAppsOpen) {
						closeDrawer();
					} else {
						closeFolder();
					}
					if (isPreviewing()) {
						dismissPreviews();
					}
					if (mIsEditMode) {
						stopDesktopEdit();
					}
					if (mIsWidgetEditMode) {
						stopWidgetEdit();
					}
				}
				return true;
			case KeyEvent.KEYCODE_HOME:
				return true;
			case KeyEvent.KEYCODE_DPAD_CENTER:
				if (isPreviewing()) {
					dismissPreviews();
					return true;
				}
				if (mIsWidgetEditMode) {
					stopWidgetEdit();
					return true;
				}
			}
		}

		return super.dispatchKeyEvent(event);
	}

	private void closeDrawer() {
		closeDrawer(true);
	}

	private void closeDrawer(boolean animated) {
		if (allAppsOpen) {
			if (animated) {
				closeAllApps(true);
			} else {
				closeAllApps(false);
			}
			if (mAllAppsGrid.hasFocus()) {
				mWorkspace.getChildAt(mWorkspace.getCurrentScreen())
				.requestFocus();
			}
		}
	}

	private void closeFolder() {
		Folder folder = mWorkspace.getOpenFolder();
		if (folder != null) {
			closeFolder(folder);
		}
	}

	void closeFolder(Folder folder) {
		if (mFolderAnimate)
			folder.startAnimation(AnimationUtils.loadAnimation(this,
					R.anim.fade_out_fast));
		folder.getInfo().opened = false;
		ViewGroup parent = (ViewGroup) folder.getParent();
		if (parent != null) {
			parent.removeView(folder);
		}
		folder.onClose();
	}

	/**
	 * When the notification that favorites have changed is received, requests a
	 * favorites list refresh.
	 */
	private void onFavoritesChanged() {
		mDesktopLocked = true;
		sLauncherModel.loadUserItems(false, this, false, false);
	}

	/**
	 * Re-listen when widgets are reset.
	 */
	private void onAppWidgetReset() {
		mAppWidgetHost.startListening();
	}

	void onDesktopItemsLoaded(ArrayList<ItemInfo> shortcuts,
			ArrayList<LauncherAppWidgetInfo> appWidgets) {
		if (mDestroyed) {
			if (LauncherModel.DEBUG_LOADERS) {
				d(LauncherModel.LOG_TAG,
						"  ------> destroyed, ignoring desktop items");
			}
			return;
		}
		bindDesktopItems(shortcuts, appWidgets);
	}

	/**
	 * Refreshes the shortcuts shown on the workspace.
	 */
	private void bindDesktopItems(ArrayList<ItemInfo> shortcuts,
			ArrayList<LauncherAppWidgetInfo> appWidgets) {

		final ApplicationsAdapter drawerAdapter = sLauncherModel
				.getApplicationsAdapter();
		if (shortcuts == null || appWidgets == null || drawerAdapter == null) {
			if (LauncherModel.DEBUG_LOADERS)
				d(LauncherModel.LOG_TAG, "  ------> a source is null");
			return;
		}

		final Workspace workspace = mWorkspace;
		int count = workspace.getChildCount();
		for (int i = 0; i < count; i++) {
			((ViewGroup) workspace.getChildAt(i)).removeAllViewsInLayout();
		}
		if (DEBUG_USER_INTERFACE) {
			android.widget.Button finishButton = new android.widget.Button(this);
			finishButton.setText("Finish");
			workspace.addInScreen(finishButton, 1, 0, 0, 1, 1);

			finishButton
			.setOnClickListener(new android.widget.Button.OnClickListener() {
				public void onClick(View v) {
					finish();
				}
			});
		}

		// Flag any old binder to terminate early
		if (mBinder != null) {
			mBinder.mTerminate = true;
		}

		mBinder = new DesktopBinder(this, shortcuts, appWidgets, drawerAdapter);
		mBinder.startBindingItems();
	}

	private void bindItems(Launcher.DesktopBinder binder,
			ArrayList<ItemInfo> shortcuts, int start, int count) {

		final Workspace workspace = mWorkspace;
		final boolean desktopLocked = mDesktopLocked;

		final int end = Math.min(start + DesktopBinder.ITEMS_COUNT, count);
		int i = start;

		for (; i < end; i++) {
			final ItemInfo item = shortcuts.get(i);
			switch ((int) item.container) {
			case LauncherSettings.Favorites.CONTAINER_LAB:
				mLAB.UpdateLaunchInfo(item);
				break;
			case LauncherSettings.Favorites.CONTAINER_RAB:
				mRAB.UpdateLaunchInfo(item);
				break;
			case LauncherSettings.Favorites.CONTAINER_LAB2:
				mLAB2.UpdateLaunchInfo(item);
				break;
			case LauncherSettings.Favorites.CONTAINER_RAB2:
				mRAB2.UpdateLaunchInfo(item);
				break;
			case LauncherSettings.Favorites.CONTAINER_MAB:
				mMAB.UpdateLaunchInfo(item);
				break;
			default:
				switch (item.itemType) {
				case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
				case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
					final View shortcut = createShortcut((ApplicationInfo) item);
					workspace.addInScreen(shortcut, item.screen, item.cellX,
							item.cellY, 1, 1, !desktopLocked);
					break;
				case LauncherSettings.Favorites.ITEM_TYPE_USER_FOLDER:
					final FolderIcon newFolder = FolderIcon.fromXml(
							R.layout.folder_icon, this, (ViewGroup) workspace
							.getChildAt(workspace.getCurrentScreen()),
							(UserFolderInfo) item);
					if (themeFont != null)
						((TextView) newFolder).setTypeface(themeFont);
					workspace.addInScreen(newFolder, item.screen, item.cellX,
							item.cellY, 1, 1, !desktopLocked);
					break;
				case LauncherSettings.Favorites.ITEM_TYPE_LIVE_FOLDER:
					final FolderIcon newLiveFolder = LiveFolderIcon
					.fromXml(R.layout.live_folder_icon, this,
							(ViewGroup) workspace.getChildAt(workspace
									.getCurrentScreen()),
									(LiveFolderInfo) item);
					if (themeFont != null)
						((TextView) newLiveFolder).setTypeface(themeFont);
					workspace.addInScreen(newLiveFolder, item.screen,
							item.cellX, item.cellY, 1, 1, !desktopLocked);
					break;
					/*
					 * case LauncherSettings.Favorites.ITEM_TYPE_WIDGET_SEARCH:
					 * final int screen = workspace.getCurrentScreen(); final View
					 * view = mInflater.inflate(R.layout.widget_search, (ViewGroup)
					 * workspace.getChildAt(screen), false);
					 * 
					 * Search search = (Search)
					 * view.findViewById(R.id.widget_search);
					 * search.setLauncher(this);
					 * 
					 * final Widget widget = (Widget) item; view.setTag(widget);
					 * 
					 * workspace.addWidget(view, widget, !desktopLocked); break;
					 */
				}
			}
		}

		workspace.requestLayout();

		if (end >= count) {
			finishBindDesktopItems();
			binder.startBindingDrawer();
		} else {
			binder.obtainMessage(DesktopBinder.MESSAGE_BIND_ITEMS, i, count)
			.sendToTarget();
		}
	}

	private void finishBindDesktopItems() {
		if (mSavedState != null) {
			if (!mWorkspace.hasFocus()) {
				mWorkspace.getChildAt(mWorkspace.getCurrentScreen())
				.requestFocus();
			}

			final long[] userFolders = mSavedState
					.getLongArray(RUNTIME_STATE_USER_FOLDERS);
			if (userFolders != null) {
				for (long folderId : userFolders) {
					final FolderInfo info = sLauncherModel
							.findFolderById(folderId);
					if (info != null) {
						openFolder(info);
					}
				}
				final Folder openFolder = mWorkspace.getOpenFolder();
				if (openFolder != null) {
					openFolder.requestFocus();
				}
			}

			final boolean allApps = mSavedState.getBoolean(
					RUNTIME_STATE_ALL_APPS_FOLDER, false);
			if (allApps) {
				showAllApps(false, null);
			}
			mSavedState = null;
		}

		if (mSavedInstanceState != null) {
			// ADW: sometimes on rotating the phone, some widgets fail to
			// restore its states.... so... damn.
			try {
				super.onRestoreInstanceState(mSavedInstanceState);
			} catch (Exception e) {
			}
			mSavedInstanceState = null;
		}

		if (allAppsOpen && !mAllAppsGrid.hasFocus()) {
			mAllAppsGrid.requestFocus();
		}

		mDesktopLocked = false;
		final boolean showChangeLog = MyLauncherSettingsHelper
				.shouldShowChangelog(this);
		// ADW: Show the changelog screen if needed
		if (showChangeLog) {
			try {
				mAlertDialog = MyLauncherSettingsHelper.ChangelogDialogBuilder
						.create(this, showChangeLog);
				mAlertDialog.show();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	private void bindDrawer(Launcher.DesktopBinder binder,
			ApplicationsAdapter drawerAdapter) {
		int currCatalog = MyLauncherSettingsHelper.getCurrentAppCatalog(this);
		AppCatalogueFilters.getInstance().getDrawerFilter()
		.setCurrentGroupIndex(currCatalog);
		drawerAdapter.buildViewCache((ViewGroup) mAllAppsGrid);
		mAllAppsGrid.setAdapter(drawerAdapter);
		mAllAppsGrid.updateAppGrp();
		binder.startBindingAppWidgetsWhenIdle();
	}

	private void bindAppWidgets(Launcher.DesktopBinder binder,
			LinkedList<LauncherAppWidgetInfo> appWidgets) {

		final Workspace workspace = mWorkspace;
		final boolean desktopLocked = mDesktopLocked;

		if (!appWidgets.isEmpty()) {
			final LauncherAppWidgetInfo item = appWidgets.removeFirst();

			final int appWidgetId = item.appWidgetId;
			final AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager
					.getAppWidgetInfo(appWidgetId);
			item.hostView = mAppWidgetHost.createView(this, appWidgetId,
					appWidgetInfo);

			if (LOGD) {
				d(LOG_TAG, String.format(
						"about to setAppWidget for id=%d, info=%s",
						appWidgetId, appWidgetInfo));
			}

			item.hostView.setAppWidget(appWidgetId, appWidgetInfo);
			item.hostView.setTag(item);

			workspace.addInScreen(item.hostView, item.screen, item.cellX,
					item.cellY, item.spanX, item.spanY, !desktopLocked);

			workspace.requestLayout();
			// finish load a widget, send it an intent
			if (appWidgetInfo != null)
				appwidgetReadyBroadcast(appWidgetId, appWidgetInfo.provider,
						new int[] { item.spanX, item.spanY });
		}

		if (appWidgets.isEmpty()) {
			if (PROFILE_ROTATE) {
				android.os.Debug.stopMethodTracing();
			}
		} else {
			binder.obtainMessage(DesktopBinder.MESSAGE_BIND_APPWIDGETS)
			.sendToTarget();
		}
	}

	/**
	 * Launches the intent referred by the clicked shortcut.
	 * 
	 * @param v
	 *            The view representing the clicked shortcut.
	 */
	public void onClick(View v) {
		Object tag = v.getTag();
		// ADW: Check if the tag is a special action (the app drawer category
		// navigation)
		if (tag instanceof Integer) {
			navigateCatalogs(Integer.parseInt(tag.toString()));
			return;
		}
		// TODO:ADW Check whether to show a toast if clicked mLAB or mRAB
		// without binding
		if (tag == null && v instanceof ActionButton) {
			return;
		}
		if (tag instanceof ApplicationInfo) {
			// Open shortcut
			final ApplicationInfo info = (ApplicationInfo) tag;
			final Intent intent = info.intent;
			int[] pos = new int[2];
			v.getLocationOnScreen(pos);
			try {
				intent.setSourceBounds(new Rect(pos[0], pos[1], pos[0]
						+ v.getWidth(), pos[1] + v.getHeight()));
			} catch (NoSuchMethodError e) {
			}
			;
			startActivityAsNewTaskSafely(intent);
		} else if (tag instanceof FolderInfo) {
			handleFolderClick((FolderInfo) tag);
		}
	}

	void startActivityAsSafely(Intent intent) {
		try {
			startActivity(intent);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(this, R.string.activity_not_found,
					Toast.LENGTH_SHORT).show();
		} catch (SecurityException e) {
			Toast.makeText(this, R.string.activity_security_exception,
					Toast.LENGTH_SHORT).show();
			e(LOG_TAG,
					"Launcher does not have the permission to launch "
							+ intent
							+ ". Make sure to create a MAIN intent-filter for the corresponding activity "
							+ "or use the exported attribute for this activity.",
							e);
		}
	}

	void startActivityAsNewTaskSafely(Intent intent) {
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		try {
			startActivity(intent);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(this, R.string.activity_not_found,
					Toast.LENGTH_SHORT).show();
		} catch (SecurityException e) {
			Toast.makeText(this, R.string.activity_security_exception,
					Toast.LENGTH_SHORT).show();
			e(LOG_TAG,
					"Launcher does not have the permission to launch "
							+ intent
							+ ". Make sure to create a MAIN intent-filter for the corresponding activity "
							+ "or use the exported attribute for this activity.",
							e);
		}
	}

	private void handleFolderClick(FolderInfo folderInfo) {
		if (!folderInfo.opened) {
			// Close any open folder
			closeFolder();
			// Open the requested folder
			openFolder(folderInfo);
		} else {
			// Find the open folder...
			Folder openFolder = mWorkspace.getFolderForTag(folderInfo);
			int folderScreen;
			if (openFolder != null) {
				folderScreen = mWorkspace.getScreenForView(openFolder);
				// .. and close it
				closeFolder(openFolder);
				if (folderScreen != mWorkspace.getCurrentScreen()) {
					// Close any folder open on the current screen
					closeFolder();
					// Pull the folder onto this screen
					openFolder(folderInfo);
				}
			}
		}
	}

	/**
	 * Opens the user folder described by the specified tag. The opening of the
	 * folder is animated relative to the specified View. If the View is null,
	 * no animation is played.
	 * 
	 * @param folderInfo
	 *            The FolderInfo describing the folder to open.
	 */
	private void openFolder(FolderInfo folderInfo) {
		Folder openFolder;

		if (folderInfo instanceof UserFolderInfo) {
			openFolder = UserFolder.fromXml(this);
		} else if (folderInfo instanceof LiveFolderInfo) {
			openFolder = com.wordpress.chislonchow.legacylauncher.LiveFolder
					.fromXml(this, folderInfo);
		} else {
			return;
		}

		if (mFolderAnimate)
			openFolder.startAnimation(AnimationUtils.loadAnimation(this,
					R.anim.fade_in_fast));

		openFolder.setDragger(mDragLayer);
		openFolder.setLauncher(this);

		openFolder.bind(folderInfo);
		folderInfo.opened = true;

		if (folderInfo.container == LauncherSettings.Favorites.CONTAINER_LAB
				|| folderInfo.container == LauncherSettings.Favorites.CONTAINER_RAB
				|| folderInfo.container == LauncherSettings.Favorites.CONTAINER_LAB2
				|| folderInfo.container == LauncherSettings.Favorites.CONTAINER_RAB2) {
			mWorkspace.addInScreen(openFolder, mWorkspace.getCurrentScreen(),
					0, 0, mWorkspace.currentDesktopColumns(),
					mWorkspace.currentDesktopRows());
		} else {
			mWorkspace.addInScreen(openFolder, folderInfo.screen, 0, 0,
					mWorkspace.currentDesktopColumns(),
					mWorkspace.currentDesktopRows());
		}
		openFolder.onOpen();
		// ADW: closing drawer, removed from onpause
		closeDrawer(false);
	}

	/**
	 * Returns true if the workspace is being loaded. When the workspace is
	 * loading, no user interaction should be allowed to avoid any conflict.
	 * 
	 * @return True if the workspace is locked, false otherwise.
	 */
	boolean isWorkspaceLocked() {
		return mDesktopLocked;
	}

	public boolean onLongClick(View v) {
		if (mDesktopLocked) {
			return false;
		}
		// ADW: Show previews on longpressing the dots
		switch (v.getId()) {
		case R.id.btn_scroll_left:
			mWorkspace.performHapticFeedback(
					HapticFeedbackConstants.LONG_PRESS,
					HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
			showPreviewPrevious(v);
			return true;
		case R.id.btn_scroll_right:
			mWorkspace.performHapticFeedback(
					HapticFeedbackConstants.LONG_PRESS,
					HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
			showPreviewNext(v);
			return true;
		}

		if (!(v instanceof CellLayout)) {
			v = (View) v.getParent();
		}

		CellLayout.CellInfo cellInfo = (CellLayout.CellInfo) v.getTag();

		// This happens when long clicking an item with the dpad/trackball
		if (cellInfo == null) {
			return true;
		}

		if (mWorkspace.allowLongPress() && !mLauncherLocked) {
			if (cellInfo.cell == null) {
				if (cellInfo.valid) {
					// User long pressed on empty space
					mWorkspace.setAllowLongPress(false);
					showAddDialog(cellInfo);
				}
			} else {
				if (!(cellInfo.cell instanceof Folder)) {
					// User long pressed on an item
					mWorkspace.startDrag(cellInfo);
				}
			}
		}
		return true;
	}

	public static LauncherModel getLauncherModel() {
		return sLauncherModel;
	}

	void closeAllApplications() {
		closeAllApps(false);
	}

	View getDrawerHandle() {
		return mMAB;
	}

	/*
	 * boolean isDrawerDown() { return !mDrawer.isMoving() &&
	 * !mDrawer.isOpened(); }
	 * 
	 * boolean isDrawerUp() { return mDrawer.isOpened() && !mDrawer.isMoving();
	 * }
	 * 
	 * boolean isDrawerMoving() { return mDrawer.isMoving(); }
	 */

	Workspace getWorkspace() {
		return mWorkspace;
	}

	// ADW: we return a View, so classes using this should cast
	// to AllAppsGridView or AllAppsSlidingView if they need to access proper
	// members
	View getApplicationsGrid() {
		return (View) mAllAppsGrid;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_CREATE_SHORTCUT:
			return new CreateShortcut().createDialog();
		case DIALOG_RENAME_FOLDER:
			return new RenameFolder().createDialog();
		case DIALOG_CHOOSE_GROUP:
			return new ChooseGrpDialog().createDialog();
		case DIALOG_NEW_GROUP:
			return new NewGrpTitle().createDialog();
		case DIALOG_DELETE_GROUP_CONFIRM:
			return new AlertDialog.Builder(this)
			.setTitle(R.string.app_group_delete_long)
			.setPositiveButton(android.R.string.ok,
					new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog,
						int whichButton) {
					deleteCurrentGroup();
					Toast.makeText(Launcher.this,
							R.string.app_group_remove_success,
							Toast.LENGTH_SHORT).show();
					/* User clicked OK so do some stuff */
				}
			}).setNegativeButton(android.R.string.cancel, null)
			.create();
		case DIALOG_PICK_GROUPS:
			return new PickGrpDialog().createDialog();
		case DIALOG_ADD_WIDGET_FAILURE:
			return new AlertDialog.Builder(this).setTitle(null)
					.setCancelable(true).setIcon(R.drawable.ic_launcher_home)
					.setPositiveButton(getString(android.R.string.ok), null)
					.setMessage(getString(R.string.scrollable_api_required))
					.create();
		case DIALOG_GET_LAUNCHER_UNLOCK_PASSWORD:
			// ask for the password securely
			final EditText input = new EditText(this);
			input.setMaxLines(1);
			input.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
			input.setHint(getString(R.string.hint_password_old));
			final int maxLength = 32;
			InputFilter[] FilterArray = new InputFilter[2];
			FilterArray[0] = new InputFilter.LengthFilter(maxLength);
			FilterArray[1] = new InputFilter() {
				@Override
				public CharSequence filter(CharSequence source, int start,
						int end, Spanned dest, int dstart, int dend) {
					for (int i = start; i < end; i++) {
						if (Character.isWhitespace((source.charAt(i)))) {
							return "";
						}
					}
					return null;
				}
			};
			input.setFilters(FilterArray);
			mAlertDialog = new AlertDialog.Builder(this)
			.setMessage(R.string.dialog_lock_password_get)
			.setView(input)
			.setPositiveButton(android.R.string.ok,
					new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog,
						int whichButton) {
					Editable value = input.getText();
					final String passSecure = mObscurePrefs
							.getString("pw", null);
					if (value.toString().equals(passSecure)) {
						// password valid unlock
						input.setText("");	// clear password field
						Toast.makeText(Launcher.this,
								R.string.toast_launcher_unlock,
								Toast.LENGTH_SHORT).show();
						mLauncherLocked = false;
						// commit setting
						MyLauncherSettingsHelper
						.setLauncherLocked(
								Launcher.this,
								mLauncherLocked);
					} else {
						Vibrator vibrateService = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
						if (vibrateService != null)
							vibrateService.vibrate(1000);// vibrate
						// for
						// ~1000
						// seconds

						// password invalid
						Toast.makeText(
								Launcher.this,
								getString(R.string.lock_password_invalid),
								Toast.LENGTH_SHORT).show();
					}
				}
			}).setNegativeButton(android.R.string.cancel, null)
			.create();
			return mAlertDialog;
		default:
			return null;
		}
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
		case DIALOG_CREATE_SHORTCUT:
			break;
		case DIALOG_RENAME_FOLDER:
			if (mFolderInfo != null) {
				EditText input = (EditText) dialog
						.findViewById(R.id.folder_name);
				final CharSequence text = mFolderInfo.title;
				input.setText(text);
				input.setSelection(0, text.length());
			}
			break;
		}
	}

	private void deleteCurrentGroup() {
		int index = sLauncherModel.getApplicationsAdapter()
				.getCatalogueFilter().getCurrentFilterIndex();
		AppCatalogueFilters.getInstance().dropGroup(index);
		checkActionButtonsSpecialMode();

		// CCHOW go to all apps catalog instead on remove group success
		/*
		 * showSwitchGrp();
		 */
		sLauncherModel.getApplicationsAdapter().getCatalogueFilter()
		.setCurrentGroupIndex(-1);
		MyLauncherSettingsHelper.setCurrentAppCatalog(Launcher.this, -1);
		mAllAppsGrid.updateAppGrp();
	}

	private void showSwitchGrp() {
		removeDialog(DIALOG_CHOOSE_GROUP);
		mWorkspace.lock();
		showDialog(DIALOG_CHOOSE_GROUP);
	}

	private void showAddDialog(CellLayout.CellInfo cellInfo) {
		mAddItemCellInfo = cellInfo;
		mWaitingForResult = true;
		mWorkspace.lock();
		showDialog(DIALOG_CREATE_SHORTCUT);
	}

	private void pickShortcut(int requestCode, int title) {
		Bundle bundle = new Bundle();

		ArrayList<String> shortcutNames = new ArrayList<String>();
		shortcutNames.add(getString(R.string.group_applications));
		bundle.putStringArrayList(Intent.EXTRA_SHORTCUT_NAME, shortcutNames);

		ArrayList<ShortcutIconResource> shortcutIcons = new ArrayList<ShortcutIconResource>();
		shortcutIcons.add(ShortcutIconResource.fromContext(Launcher.this,
				R.drawable.ic_launcher_application));
		bundle.putParcelableArrayList(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
				shortcutIcons);

		Intent pickIntent = new Intent(Intent.ACTION_PICK_ACTIVITY);
		pickIntent.putExtra(Intent.EXTRA_INTENT, new Intent(
				Intent.ACTION_CREATE_SHORTCUT));
		pickIntent.putExtra(Intent.EXTRA_TITLE, getText(title));
		pickIntent.putExtras(bundle);

		startActivityForResult(pickIntent, requestCode);
	}

	private class RenameFolder {
		private EditText mInput;

		Dialog createDialog() {
			mWaitingForResult = true;
			final View layout = View.inflate(Launcher.this,
					R.layout.rename_folder, null);
			mInput = (EditText) layout.findViewById(R.id.folder_name);

			AlertDialog.Builder builder = new AlertDialog.Builder(Launcher.this);
			builder.setIcon(0);
			builder.setTitle(getString(R.string.rename_folder_title));
			builder.setCancelable(true);
			builder.setOnCancelListener(new Dialog.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					cleanup();
				}
			});
			builder.setNegativeButton(getString(android.R.string.cancel),
					new Dialog.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					cleanup();
				}
			});
			builder.setPositiveButton(getString(android.R.string.ok),
					new Dialog.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					changeFolderName();
				}
			});
			builder.setView(layout);

			final AlertDialog dialog = builder.create();

			return dialog;
		}

		private void changeFolderName() {
			final String name = mInput.getText().toString();
			if (!TextUtils.isEmpty(name)) {
				// Make sure we have the right folder info
				mFolderInfo = sLauncherModel.findFolderById(mFolderInfo.id);
				mFolderInfo.title = name;
				LauncherModel.updateItemInDatabase(Launcher.this, mFolderInfo);

				if (mDesktopLocked) {
					sLauncherModel.loadUserItems(false, Launcher.this, false,
							false);
				} else {
					final FolderIcon folderIcon = (FolderIcon) mWorkspace
							.getViewForTag(mFolderInfo);
					if (folderIcon != null) {
						folderIcon.setText(name);
						getWorkspace().requestLayout();
					} else {
						mDesktopLocked = true;
						sLauncherModel.loadUserItems(false, Launcher.this,
								false, false);
					}
				}
			}
			cleanup();
		}

		private void cleanup() {
			try {
				mWorkspace.unlock();
				dismissDialog(DIALOG_RENAME_FOLDER);
			} catch (Exception e) {
				// Restarted while dialog or whatever causes
				// IllegalStateException???
			}
			mWaitingForResult = false;
			mFolderInfo = null;
		}
	}

	protected class ChooseGrpDialog implements DialogInterface.OnClickListener,
	DialogInterface.OnCancelListener,
	DialogInterface.OnDismissListener, DialogInterface.OnShowListener {

		private AppGroupAdapter mAdapter;

		Dialog createDialog() {
			mWaitingForResult = true;

			mAdapter = new AppGroupAdapter(Launcher.this);

			final AlertDialog.Builder builder = new AlertDialog.Builder(
					Launcher.this);
			builder.setTitle(getString(R.string.app_group_choose));
			builder.setAdapter(mAdapter, this);

			builder.setInverseBackgroundForced(true);

			AlertDialog dialog = builder.create();
			dialog.setOnCancelListener(this);
			dialog.setOnDismissListener(this);
			return dialog;
		}

		public void onCancel(DialogInterface dialog) {
			mWaitingForResult = false;
			cleanup();
		}

		public void onDismiss(DialogInterface dialog) {
			mWorkspace.unlock();
		}

		private void cleanup() {
			try {
				mWorkspace.unlock();
				dismissDialog(DIALOG_CHOOSE_GROUP);
			} catch (Exception e) {
				// Restarted while dialog or whatever causes
				// IllegalStateException???
			}
		}

		public void onClick(DialogInterface dialog, int which) {
			cleanup();
			AppGroupAdapter.ListItem itm = (AppGroupAdapter.ListItem) mAdapter
					.getItem(which);
			int action = itm.actionTag;

			// 1st is add,
			// 2nd is All, mapping to -1, check AppGrpUtils For detail
			// int dbGrp = AppGrpUtils.getGrpNumber(which-2);
			if (action == AppGroupAdapter.APP_GROUP_ADD) {
				showNewGrpDialog();
			} else {
				sLauncherModel.getApplicationsAdapter().getCatalogueFilter()
				.setCurrentGroupIndex(action);
				MyLauncherSettingsHelper.setCurrentAppCatalog(Launcher.this,
						action);
				mAllAppsGrid.updateAppGrp();
				checkActionButtonsSpecialMode();
			}
			// mDrawer.open();
		}

		public void onShow(DialogInterface dialog) {
			mWorkspace.lock();
		}
	}

	private class NewGrpTitle {
		private EditText mInput;

		Dialog createDialog() {
			mWaitingForResult = true;
			final View layout = View.inflate(Launcher.this,
					R.layout.rename_grp, null);
			mInput = (EditText) layout.findViewById(R.id.group_name);

			AlertDialog.Builder builder = new AlertDialog.Builder(Launcher.this);
			builder.setIcon(0);
			builder.setTitle(getString(R.string.new_group_title));
			builder.setCancelable(true);
			builder.setOnCancelListener(new Dialog.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					cleanup();
				}
			});
			builder.setNegativeButton(getString(android.R.string.cancel),
					new Dialog.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					cleanup();
				}
			});
			builder.setPositiveButton(getString(android.R.string.ok),
					new Dialog.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					final String name = mInput.getText().toString()
							.trim();

					if (!TextUtils.isEmpty(name)) {
						// Make sure we have the right folder info
						int newGroupIndex = AppCatalogueFilters
								.getInstance().createNewGroup(name);
						MyLauncherSettingsHelper.setCurrentAppCatalog(
								Launcher.this, newGroupIndex);
						sLauncherModel.getApplicationsAdapter()
						.getCatalogueFilter()
						.setCurrentGroupIndex(newGroupIndex);
						checkActionButtonsSpecialMode();
						LauncherModel.mApplicationsAdapter
						.updateDataSet();

						// go to the modify apps screen immediately
						Toast.makeText(
								Launcher.this,
								getString(R.string.create_group_success)
								+ name, Toast.LENGTH_SHORT)
								.show();
						showAppPickerList(true);
					} else {
						Toast.makeText(Launcher.this,
								R.string.create_group_fail,
								Toast.LENGTH_SHORT).show();
					}
					cleanup();
				}
			});
			builder.setView(layout);

			final AlertDialog dialog = builder.create();

			return dialog;
		}

		private void cleanup() {
			try {
				mWorkspace.unlock();
				dismissDialog(DIALOG_NEW_GROUP);
			} catch (Exception e) {
				// Restarted while dialog or whatever causes
				// IllegalStateException???
			}
			mWaitingForResult = false;
			mFolderInfo = null;

			if (mPickGroupInfo != null) {
				// reopen the pick dialog
				mWaitingForResult = true;
				mWorkspace.lock();
				showDialog(DIALOG_PICK_GROUPS);
			}
		}
	}

	/**
	 * Displays the shortcut creation dialog and launches, if necessary, the
	 * appropriate activity.
	 */
	private class CreateShortcut implements DialogInterface.OnClickListener,
	DialogInterface.OnCancelListener, DialogInterface.OnDismissListener {

		private AddAdapter mAdapter;

		Dialog createDialog() {
			mWaitingForResult = true;

			mAdapter = new AddAdapter(Launcher.this);

			final AlertDialog.Builder builder = new AlertDialog.Builder(
					Launcher.this);
			builder.setTitle(getString(R.string.menu_item_add_item));
			builder.setAdapter(mAdapter, this);

			builder.setInverseBackgroundForced(true);

			AlertDialog dialog = builder.create();
			dialog.setOnCancelListener(this);
			dialog.setOnDismissListener(this);

			return dialog;
		}

		public void onCancel(DialogInterface dialog) {
			mWaitingForResult = false;
			cleanup();
		}

		public void onDismiss(DialogInterface dialog) {
			mWorkspace.unlock();
		}

		private void cleanup() {
			try {
				mWorkspace.unlock();
				dismissDialog(DIALOG_CREATE_SHORTCUT);
			} catch (Exception e) {
				// Restarted while dialog or whatever causes
				// IllegalStateException???
			}
		}

		/**
		 * Handle the action clicked in the "Add to home" dialog.
		 */
		public void onClick(DialogInterface dialog, int which) {
			Resources res = getResources();
			cleanup();

			switch (which) {
			case AddAdapter.ITEM_SHORTCUT: {
				// Insert extra item to handle picking application
				pickShortcut(REQUEST_PICK_SHORTCUT,
						R.string.title_select_shortcut);
				break;
			}

			case AddAdapter.ITEM_APPWIDGET: {
				int appWidgetId = Launcher.this.mAppWidgetHost
						.allocateAppWidgetId();

				Intent pickIntent = new Intent(
						AppWidgetManager.ACTION_APPWIDGET_PICK);
				pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
						appWidgetId);

				// CCHOW CHANGE START
				// Workaround for crash in Android 2.1. ADW removed the search widget 
				// from the launcher code, which broke things.
				ArrayList<AppWidgetProviderInfo> customInfo = new ArrayList<AppWidgetProviderInfo>();
				final boolean SDK_LESS_THAN_FROYO = Build.VERSION.SDK_INT < 8;

				if (!SDK_LESS_THAN_FROYO) {
					// add the search widget
					AppWidgetProviderInfo info = new AppWidgetProviderInfo();
					info.provider = new ComponentName(getPackageName(), "XXX.YYY");
					info.label = getString(R.string.group_search);
					info.icon = R.drawable.ic_search_widget;
					customInfo.add(info);
				}
				pickIntent.putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_INFO, customInfo);

				if (SDK_LESS_THAN_FROYO) {
					// Android 2.1 crashes if this isn't there, but this is useless if SDK > 7
					ArrayList<Bundle> customExtras = new ArrayList<Bundle>();
					Bundle b = new Bundle(); b.putString(EXTRA_CUSTOM_WIDGET, SEARCH_WIDGET); customExtras.add(b);
					pickIntent.putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_EXTRAS, customExtras);
				}
				// CCHOW CHANGE END

				// start the pick activity
				startActivityForResult(pickIntent, REQUEST_PICK_APPWIDGET);
				break;
			}

			case AddAdapter.ITEM_LIVE_FOLDER: {
				// Insert extra item to handle inserting folder
				Bundle bundle = new Bundle();

				ArrayList<String> shortcutNames = new ArrayList<String>();
				shortcutNames.add(res.getString(R.string.group_folder));
				bundle.putStringArrayList(Intent.EXTRA_SHORTCUT_NAME,
						shortcutNames);

				ArrayList<ShortcutIconResource> shortcutIcons = new ArrayList<ShortcutIconResource>();
				shortcutIcons.add(ShortcutIconResource.fromContext(
						Launcher.this, R.drawable.ic_launcher_folder));
				bundle.putParcelableArrayList(
						Intent.EXTRA_SHORTCUT_ICON_RESOURCE, shortcutIcons);

				Intent pickIntent = new Intent(Intent.ACTION_PICK_ACTIVITY);
				pickIntent.putExtra(Intent.EXTRA_INTENT, new Intent(
						LiveFolders.ACTION_CREATE_LIVE_FOLDER));
				pickIntent.putExtra(Intent.EXTRA_TITLE,
						getText(R.string.title_select_live_folder));
				pickIntent.putExtras(bundle);

				startActivityForResult(pickIntent, REQUEST_PICK_LIVE_FOLDER);
				break;
			}

			case AddAdapter.ITEM_WALLPAPER: {
				startWallpaper();
				break;
			}

			case AddAdapter.ITEM_ANYCUT: {
				Intent anycutIntent = new Intent();
				anycutIntent.setClass(Launcher.this,
						CustomShortcutActivity.class);
				startActivityForResult(anycutIntent, REQUEST_PICK_ANYCUT);
				break;
			}
			case AddAdapter.ITEM_LAUNCHER_ACTION: {
				AlertDialog.Builder builder = new AlertDialog.Builder(
						Launcher.this);
				builder.setTitle(getString(R.string.launcher_actions));
				final ListAdapter adapter = LauncherActions.getInstance()
						.getSelectActionAdapter();
				builder.setAdapter(adapter, new Dialog.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						LauncherActions.Action action = (LauncherActions.Action) adapter
								.getItem(which);
						Intent result = new Intent();
						result.putExtra(Intent.EXTRA_SHORTCUT_NAME,
								action.getName());
						result.putExtra(Intent.EXTRA_SHORTCUT_INTENT,
								LauncherActions.getInstance()
								.getIntentForAction(action));
						ShortcutIconResource iconResource = new ShortcutIconResource();
						iconResource.packageName = Launcher.this
								.getPackageName();
						iconResource.resourceName = getResources()
								.getResourceName(action.getIconResourceId());
						result.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
								iconResource);
						onActivityResult(REQUEST_CREATE_SHORTCUT, RESULT_OK,
								result);
					}
				});
				builder.create().show();
				break;
			}
			}
		}

		public void onShow(DialogInterface dialog) {
			mWorkspace.lock();
		}
	}

	/**
	 * Receives notifications when applications are added/removed.
	 */
	private class ApplicationsIntentReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (Intent.ACTION_PACKAGE_CHANGED.equals(action)
					|| Intent.ACTION_PACKAGE_REMOVED.equals(action)
					|| Intent.ACTION_PACKAGE_ADDED.equals(action)) {

				final String packageName = intent.getData()
						.getSchemeSpecificPart();
				final boolean replacing = intent.getBooleanExtra(
						Intent.EXTRA_REPLACING, false);

				if (LauncherModel.DEBUG_LOADERS) {
					d(LauncherModel.LOG_TAG, "application intent received: "
							+ action + ", replacing=" + replacing);
					d(LauncherModel.LOG_TAG, "  --> " + intent.getData());
				}

				if (!Intent.ACTION_PACKAGE_CHANGED.equals(action)) {
					if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
						if (!replacing) {
							removeShortcutsForPackage(packageName);
							if (LauncherModel.DEBUG_LOADERS) {
								d(LauncherModel.LOG_TAG, "  --> remove package");
							}
							sLauncherModel.removePackage(Launcher.this,
									packageName);
						}
						// else, we are replacing the package, so a
						// PACKAGE_ADDED will be sent
						// later, we will update the package at this time
					} else {
						if (!replacing) {
							if (LauncherModel.DEBUG_LOADERS) {
								d(LauncherModel.LOG_TAG, "  --> add package");
							}
							sLauncherModel.addPackage(Launcher.this,
									packageName);
						} else {
							if (LauncherModel.DEBUG_LOADERS) {
								d(LauncherModel.LOG_TAG,
										"  --> update package " + packageName);
							}
							sLauncherModel.updatePackage(Launcher.this,
									packageName);
							updateShortcutsForPackage(packageName);
						}
					}
					removeDialog(DIALOG_CREATE_SHORTCUT);
				} else {
					if (LauncherModel.DEBUG_LOADERS) {
						d(LauncherModel.LOG_TAG, "  --> sync package "
								+ packageName);
					}
					sLauncherModel.syncPackage(Launcher.this, packageName);
				}
			} else {
				// ADW: Damn, this should be only for froyo!!!
				if (Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE
						.equals(action)) {
					String packages[] = intent
							.getStringArrayExtra(Intent.EXTRA_CHANGED_PACKAGE_LIST);
					if (packages == null || packages.length == 0) {
						return;
					} else {
						for (int i = 0; i < packages.length; i++) {
							sLauncherModel.addPackage(Launcher.this,
									packages[i]);
							updateShortcutsForPackage(packages[i]);
						}
					}
				} else if (Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE
						.equals(action)) {
					String packages[] = intent
							.getStringArrayExtra(Intent.EXTRA_CHANGED_PACKAGE_LIST);
					if (packages == null || packages.length == 0) {
						return;
					} else {
						for (int i = 0; i < packages.length; i++) {
							sLauncherModel.removePackage(Launcher.this,
									packages[i]);
							// ADW: We tell desktop to update packages
							// (probably will load the standard android icon)
							// to show the user the app is no more available.
							// We may add the froyo code to just load a
							// grayscale version of the icon, but...
							updateShortcutsForPackage(packages[i]);
						}
					}
				}
			}
		}
	}

	/**
	 * Receives notifications when system dialogs are to be closed.
	 */
	private class CloseSystemDialogsIntentReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			closeSystemDialogs();
		}
	}

	/**
	 * Receives notifications whenever the user favorites have changed.
	 */
	private class FavoritesChangeObserver extends ContentObserver {
		public FavoritesChangeObserver() {
			super(new Handler());
		}

		@Override
		public void onChange(boolean selfChange) {
			onFavoritesChanged();
		}
	}

	/**
	 * Receives notifications whenever the appwidgets are reset.
	 */
	private class AppWidgetResetObserver extends ContentObserver {
		public AppWidgetResetObserver() {
			super(new Handler());
		}

		@Override
		public void onChange(boolean selfChange) {
			onAppWidgetReset();
		}
	}

	private static class DesktopBinder extends Handler implements
	MessageQueue.IdleHandler {
		static final int MESSAGE_BIND_ITEMS = 0x1;
		static final int MESSAGE_BIND_APPWIDGETS = 0x2;
		static final int MESSAGE_BIND_DRAWER = 0x3;

		// Number of items to bind in every pass
		static final int ITEMS_COUNT = 6;

		private final ArrayList<ItemInfo> mShortcuts;
		private final LinkedList<LauncherAppWidgetInfo> mAppWidgets;
		private final ApplicationsAdapter mDrawerAdapter;
		private final WeakReference<Launcher> mLauncher;

		public boolean mTerminate = false;

		DesktopBinder(Launcher launcher, ArrayList<ItemInfo> shortcuts,
				ArrayList<LauncherAppWidgetInfo> appWidgets,
				ApplicationsAdapter drawerAdapter) {

			mLauncher = new WeakReference<Launcher>(launcher);
			mShortcuts = shortcuts;
			mDrawerAdapter = drawerAdapter;

			// Sort widgets so active workspace is bound first
			final int currentScreen = launcher.mWorkspace.getCurrentScreen();
			final int size = appWidgets.size();
			mAppWidgets = new LinkedList<LauncherAppWidgetInfo>();

			for (int i = 0; i < size; i++) {
				LauncherAppWidgetInfo appWidgetInfo = appWidgets.get(i);
				if (appWidgetInfo.screen == currentScreen) {
					mAppWidgets.addFirst(appWidgetInfo);
				} else {
					mAppWidgets.addLast(appWidgetInfo);
				}
			}

			if (LauncherModel.DEBUG_LOADERS) {
				d(Launcher.LOG_TAG, "------> binding " + shortcuts.size()
						+ " items");
				d(Launcher.LOG_TAG, "------> binding " + appWidgets.size()
						+ " widgets");
			}
		}

		public void startBindingItems() {
			if (LauncherModel.DEBUG_LOADERS)
				d(Launcher.LOG_TAG, "------> start binding items");
			obtainMessage(MESSAGE_BIND_ITEMS, 0, mShortcuts.size())
			.sendToTarget();
		}

		public void startBindingDrawer() {
			obtainMessage(MESSAGE_BIND_DRAWER).sendToTarget();
		}

		public void startBindingAppWidgetsWhenIdle() {
			// Ask for notification when message queue becomes idle
			final MessageQueue messageQueue = Looper.myQueue();
			messageQueue.addIdleHandler(this);
		}

		public boolean queueIdle() {
			// Queue is idle, so start binding items
			startBindingAppWidgets();
			return false;
		}

		public void startBindingAppWidgets() {
			obtainMessage(MESSAGE_BIND_APPWIDGETS).sendToTarget();
		}

		@Override
		public void handleMessage(Message msg) {
			Launcher launcher = mLauncher.get();
			if (launcher == null || mTerminate) {
				return;
			}

			switch (msg.what) {
			case MESSAGE_BIND_ITEMS: {
				launcher.bindItems(this, mShortcuts, msg.arg1, msg.arg2);
				break;
			}
			case MESSAGE_BIND_DRAWER: {
				launcher.bindDrawer(this, mDrawerAdapter);
				break;
			}
			case MESSAGE_BIND_APPWIDGETS: {
				if (mAppWidgets != null)
					launcher.bindAppWidgets(this, mAppWidgets);
				break;
			}
			}
		}
	}

	/****************************************************************
	 * ADW: Start custom functions/modifications
	 ***************************************************************/

	/**
	 * ADW: Show the custom settings activity
	 */
	private void showCustomConfig() {
		Intent launchPreferencesIntent = new Intent().setClass(this,
				MyLauncherSettings.class);
		startActivity(launchPreferencesIntent);
	}

	/**
	 * Update variables
	 */
	private void updateAlmostNexusVars() {
		mDrawerAnimate = MyLauncherSettingsHelper.getDrawerAnimated(Launcher.this);
		mPreviewsNew = MyLauncherSettingsHelper.getPreviewsNew(this);
		mHomeBinding = MyLauncherSettingsHelper.getHomeBinding(this);
		mSwipedownAction = MyLauncherSettingsHelper.getSwipeDownActions(this);
		mSwipeupAction = MyLauncherSettingsHelper.getSwipeUpActions(this);
		mPinchInAction = MyLauncherSettingsHelper.getPinchInActions(this);
		mHideStatusBar = MyLauncherSettingsHelper.getHideStatusbar(this);
		mShowDots = MyLauncherSettingsHelper.getUIDots(this);
		mFolderAnimate = MyLauncherSettingsHelper.getFolderAnimate(this);
		mDockStyle = MyLauncherSettingsHelper.getDockStyle(this);
		mDockHide = MyLauncherSettingsHelper.getDockHide(this);
		mAutoCloseFolder = MyLauncherSettingsHelper.getUICloseFolder(this);
		mHideABBg = MyLauncherSettingsHelper.getUIABBg(this);
		mUiHideLabels = MyLauncherSettingsHelper.getUIHideLabels(this);

		if (mWorkspace != null) {
			mWorkspace.setSpeed(MyLauncherSettingsHelper.getDesktopSpeed(this));
			mWorkspace.setBounceAmount(MyLauncherSettingsHelper
					.getDesktopBounce(this));
			mWorkspace.setSnapAmount(MyLauncherSettingsHelper
					.getDesktopSnap(this));
			mWorkspace.setDesktopLooping(MyLauncherSettingsHelper
					.getDesktopLooping(this));
			mWorkspace.setDefaultScreen(MyLauncherSettingsHelper
					.getDefaultScreen(this));
			mWorkspace.setWallpaperScroll(MyLauncherSettingsHelper
					.getWallpaperScrolling(this));
		}
		int animationSpeed = MyLauncherSettingsHelper
				.getDrawerAnimationSpeed(this);
		if (mAllAppsGrid != null) {
			mAllAppsGrid.setAnimationSpeed(animationSpeed);
			if (mDrawerStyle == 1) {
				mAllAppsGrid.setSpeed(MyLauncherSettingsHelper
						.getDrawerSpeed(this));
				mAllAppsGrid.setSnap(MyLauncherSettingsHelper
						.getDrawerSnap(this));
				mAllAppsGrid.setOvershoot(MyLauncherSettingsHelper
						.getDrawerOvershoot(this));
				mAllAppsGrid.setPageHorizontalMargin(MyLauncherSettingsHelper
						.getPageHorizontalMargin(Launcher.this));
			}
		}
		mWallpaperHack = MyLauncherSettingsHelper.getWallpaperHack(this);
		mUseDrawerCatalogNavigation = MyLauncherSettingsHelper
				.getDrawerCatalogsNavigation(this);
		mUseDrawerUngroupCatalog = MyLauncherSettingsHelper
				.getDrawerUngroupCatalog(this);
		mUseDrawerTitleCatalog = MyLauncherSettingsHelper
				.getDrawerTitleCatalogs(this);
		mTransitionStyle = MyLauncherSettingsHelper
				.getDesktopTransitionStyle(this);
	}

	/**
	 * ADW: Refresh UI status variables and elements after changing settings.
	 */
	private void updateAlmostNexusUI() {
		if (mIsEditMode || mIsWidgetEditMode)
			return;

		updateAlmostNexusVars();

		float uiScaleAB = MyLauncherSettingsHelper.getuiScaleAB(this);
		boolean tint = MyLauncherSettingsHelper.getUIABTint(this);
		int tintcolor = MyLauncherSettingsHelper.getUIABTintColor(this);
		int selectorColor = MyLauncherSettingsHelper.getUIABSelectorColor(this);

		if (uiScaleAB != mUiScaleAB || tint != mUiABTint || tintcolor != mUiABTintColor || mUiABSelectorColor != selectorColor) {

			mUiScaleAB = uiScaleAB;
			mUiABTint = tint;
			mUiABTintColor = tintcolor;
			mUiABSelectorColor = selectorColor;

			mRAB.setSelectorColor(selectorColor);
			mLAB.setSelectorColor(selectorColor);
			mRAB2.setSelectorColor(selectorColor);
			mLAB2.setSelectorColor(selectorColor);
			mMAB.setSelectorColor(selectorColor);

			mRAB.updateIcon();
			mLAB.updateIcon();
			mRAB2.updateIcon();
			mLAB2.updateIcon();
			mMAB.updateIcon();
		}

		fullScreen(mHideStatusBar);

		// dots
		if (!showingPreviews) {
			if (!isAllAppsVisible()) {
				mNextView.setVisibility(mShowDots ? View.VISIBLE : View.GONE);
				mPreviousView.setVisibility(mShowDots ? View.VISIBLE
						: View.GONE);
			}
		}

		// dock setup
		switch (mDockStyle) {
		case DOCK_STYLE_1:
			mRAB.setVisibility(View.GONE);
			mLAB.setVisibility(View.GONE);
			if (mUseDrawerCatalogNavigation && allAppsOpen) {
				mLAB.setVisibility(View.VISIBLE);
				mRAB.setVisibility(View.VISIBLE);
			} else {
				mRAB.setVisibility(View.GONE);
				mLAB.setVisibility(View.GONE);
			}
			mRAB2.setVisibility(View.GONE);
			mLAB2.setVisibility(View.GONE);
			mMAB.setVisibility(View.VISIBLE);
			break;
		case DOCK_STYLE_3:
			if (mUseDrawerCatalogNavigation && allAppsOpen) {
				mLAB.setVisibility(View.VISIBLE);
				mRAB.setVisibility(View.VISIBLE);
			} else {
				mLAB.setVisibility(View.INVISIBLE);
				mRAB.setVisibility(View.INVISIBLE);
			}
			mRAB2.setVisibility(View.VISIBLE);
			mLAB2.setVisibility(View.VISIBLE);
			mMAB.setVisibility(View.VISIBLE);
			break;
		case DOCK_STYLE_5:
			mRAB.setVisibility(View.VISIBLE);
			mLAB.setVisibility(View.VISIBLE);
			mRAB2.setVisibility(View.VISIBLE);
			mLAB2.setVisibility(View.VISIBLE);
			mMAB.setVisibility(View.VISIBLE);
			break;
		case DOCK_STYLE_4:
			mRAB.setVisibility(View.VISIBLE);
			mLAB.setVisibility(View.VISIBLE);
			mLAB2.setVisibility(View.VISIBLE);
			mRAB2.setVisibility(View.VISIBLE);
			mMAB.setVisibility(View.GONE);
			break;
		case DOCK_STYLE_NONE:
		default:
			break;
		}
		mMAB.hideBg(mHideABBg);
		mRAB.hideBg(mHideABBg);
		mLAB.hideBg(mHideABBg);
		mRAB2.hideBg(mHideABBg);
		mLAB2.hideBg(mHideABBg);

		if (mDockStyle == DOCK_STYLE_NONE || showingPreviews || (mDockHide && allAppsOpen)) {
			mDrawerToolbar.setVisibility(View.GONE);
		} else {
			mDrawerToolbar.setVisibility(View.VISIBLE);
		}

		// wallpaper setup
		if (mWorkspace != null) {
			mWorkspace.setWallpaperHack(mWallpaperHack);
		}

		// desktop indicator setup
		if (mDesktopIndicator != null) {
			mDesktopIndicator.setType(MyLauncherSettingsHelper
					.getDesktopIndicatorType(this));
			mDesktopIndicator.setAutoHide(MyLauncherSettingsHelper
					.getDesktopIndicatorAutohide(this));
			if (mWorkspace != null) {
				mDesktopIndicator.setItems(mWorkspace.getChildCount());
			}
			if (allAppsOpen) {
				if (mDesktopIndicator != null)
					mDesktopIndicator.hide();
			}
		}
	}

	/**
	 * ADW: Create a copy of an application icon/shortcut with a reflection
	 * 
	 * @param layoutResId
	 * @param parent
	 * @param info
	 * @return
	 */
	View createSmallShortcut(int layoutResId, ViewGroup parent,
			ApplicationInfo info) {
		CounterImageView favorite = (CounterImageView) mInflater.inflate(
				layoutResId, parent, false);

		if (!info.filtered) {
			info.icon = Utilities.createIconThumbnail(info.icon, this);
			info.filtered = true;
		}
		favorite.setImageDrawable(Utilities.drawReflection(info.icon, this));
		favorite.setTag(info);
		favorite.setOnClickListener(this);
		// ADW: Counters stuff
		favorite.setCounter(info.counter, info.counterColor);
		return favorite;
	}

	/**
	 * ADW: Create a copy of an folder icon with a reflection
	 * 
	 * @param layoutResId
	 * @param parent
	 * @param info
	 * @return
	 */
	View createSmallFolder(int layoutResId, ViewGroup parent,
			UserFolderInfo info) {
		ImageView favorite = (ImageView) mInflater.inflate(layoutResId, parent,
				false);

		final Resources resources = getResources();
		// Drawable d = resources.getDrawable(R.drawable.ic_launcher_folder);
		Drawable d = null;
		if (MyLauncherSettingsHelper.getThemeIcons(this)) {
			String packageName = MyLauncherSettingsHelper.getThemePackageName(
					this, THEME_DEFAULT);
			if (packageName.equals(THEME_DEFAULT)) {
				d = resources.getDrawable(R.drawable.ic_launcher_folder);
			} else {
				d = FolderIcon.loadFolderFromTheme(this, getPackageManager(),
						packageName, "ic_launcher_folder");
				if (d == null) {
					d = resources.getDrawable(R.drawable.ic_launcher_folder);
				}
			}
		} else {
			d = resources.getDrawable(R.drawable.ic_launcher_folder);
		}
		d = Utilities.drawReflection(d, this);
		favorite.setImageDrawable(d);
		favorite.setTag(info);
		favorite.setOnClickListener(this);
		return favorite;
	}

	/**
	 * ADW: Create a copy of an LiveFolder icon with a reflection
	 * 
	 * @param layoutResId
	 * @param parent
	 * @param info
	 * @return
	 */
	View createSmallLiveFolder(int layoutResId, ViewGroup parent,
			LiveFolderInfo info) {
		ImageView favorite = (ImageView) mInflater.inflate(layoutResId, parent,
				false);

		final Resources resources = getResources();
		Drawable d = info.icon;
		if (d == null) {
			if (MyLauncherSettingsHelper.getThemeIcons(this)) {
				// Drawable d =
				// resources.getDrawable(R.drawable.ic_launcher_folder);
				String packageName = MyLauncherSettingsHelper
						.getThemePackageName(this, THEME_DEFAULT);
				if (packageName.equals(THEME_DEFAULT)) {
					d = resources.getDrawable(R.drawable.ic_launcher_folder);
				} else {
					d = FolderIcon.loadFolderFromTheme(this,
							getPackageManager(), packageName,
							"ic_launcher_folder");
					if (d == null) {
						d = resources
								.getDrawable(R.drawable.ic_launcher_folder);
					}
				}
			} else {
				d = resources.getDrawable(R.drawable.ic_launcher_folder);
			}
			info.filtered = true;
		}
		d = Utilities.drawReflection(d, this);
		favorite.setImageDrawable(d);
		favorite.setTag(info);
		favorite.setOnClickListener(this);
		return favorite;
	}

	/**
	 * ADW:Create a smaller copy of an icon for use inside Action Buttons
	 * 
	 * @param info
	 * @return
	 */
	Drawable createSmallActionButtonIcon(ItemInfo info) {
		Drawable d = null;
		final Resources resources = getResources();
		if (info != null) {
			if (info instanceof ApplicationInfo) {
				if (!((ApplicationInfo) info).filtered) {
					((ApplicationInfo) info).icon = Utilities
							.createIconThumbnail(((ApplicationInfo) info).icon,
									this);
					((ApplicationInfo) info).filtered = true;
				}
				d = ((ApplicationInfo) info).icon;
			} else if (info instanceof LiveFolderInfo) {
				d = ((LiveFolderInfo) info).icon;
				if (d == null) {
					if (MyLauncherSettingsHelper.getThemeIcons(this)) {
						// d =
						// Utilities.createIconThumbnail(resources.getDrawable(R.drawable.ic_launcher_folder),
						// this);
						String packageName = MyLauncherSettingsHelper
								.getThemePackageName(this, THEME_DEFAULT);
						if (!packageName.equals(THEME_DEFAULT)) {
							d = FolderIcon.loadFolderFromTheme(this,
									getPackageManager(), packageName,
									"ic_launcher_folder");
						} else {
							d = Utilities
									.createIconThumbnail(
											resources
											.getDrawable(R.drawable.ic_launcher_folder),
											this);
						}
					} else {
						d = Utilities.createIconThumbnail(resources
								.getDrawable(R.drawable.ic_launcher_folder),
								this);
					}
					((LiveFolderInfo) info).filtered = true;
				}
			} else if (info instanceof UserFolderInfo) {
				if (MyLauncherSettingsHelper.getThemeIcons(this)) {
					// d = resources.getDrawable(R.drawable.ic_launcher_folder);
					String packageName = MyLauncherSettingsHelper
							.getThemePackageName(this, THEME_DEFAULT);
					if (!packageName.equals(THEME_DEFAULT)) {
						d = FolderIcon.loadFolderFromTheme(this,
								getPackageManager(), packageName,
								"ic_launcher_folder");
					} else {
						d = resources
								.getDrawable(R.drawable.ic_launcher_folder);
					}
				} else {
					d = resources.getDrawable(R.drawable.ic_launcher_folder);
				}
			}
		}
		if (d == null) {
			d = Utilities.createIconThumbnail(
					resources.getDrawable(R.drawable.ab_empty), this);
		}
		d = Utilities.scaledDrawable(d, this, mUiABTint, mUiScaleAB, mUiABTintColor);

		return d;
	}

	Drawable createSmallActionButtonDrawable(Drawable d) {
		d = Utilities.scaledDrawable(d, this, mUiABTint, mUiScaleAB, mUiABTintColor);
		return d;
	}

	// ADW: Previews Functions
	public void previousScreen(View v) {
		mWorkspace.scrollLeft();
	}

	public void nextScreen(View v) {
		mWorkspace.scrollRight();
	}

	protected boolean isPreviewing() {
		return showingPreviews;
	}

	private void fullScreen(boolean enable) {
		if (enable) {
			// go full screen
			WindowManager.LayoutParams attrs = getWindow().getAttributes();
			attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
			getWindow().setAttributes(attrs);
			getWindow().addFlags(
					WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
			mHideStatusBar = true;
		} else {
			// go non-full screen
			WindowManager.LayoutParams attrs = getWindow().getAttributes();
			attrs.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
			getWindow().setAttributes(attrs);
			getWindow().clearFlags(
					WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
			mHideStatusBar = false;
		}
	}

	protected void fullScreenTemporary(boolean enable) {
		if (enable) {
			// go full screen
			WindowManager.LayoutParams attrs = getWindow().getAttributes();
			attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
			getWindow().setAttributes(attrs);
			getWindow().addFlags(
					WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
		} else {
			// go non-full screen
			WindowManager.LayoutParams attrs = getWindow().getAttributes();
			attrs.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
			getWindow().setAttributes(attrs);
			getWindow().clearFlags(
					WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
		}
	}

	protected void fullScreenRestore() {
		fullScreenTemporary(mHideStatusBar);
	}

	private void hideDesktop(boolean enable) {
		if (enable) {
			if (mDesktopIndicator != null)
				mDesktopIndicator.hide();
			mNextView.setVisibility(View.INVISIBLE);
			mPreviousView.setVisibility(View.INVISIBLE);
			mDrawerToolbar.setVisibility(View.INVISIBLE); // WAS VIEW>GONE
		} else {
			if (mDesktopIndicator != null)
				mDesktopIndicator.show();

			if (mDockStyle != DOCK_STYLE_NONE)
				mDrawerToolbar.setVisibility(View.VISIBLE);
			if (mShowDots) {
				mNextView.setVisibility(View.VISIBLE);
				mPreviousView.setVisibility(View.VISIBLE);
			}
		}
	}

	public void dismissPreviews() {
		if (showingPreviews) {
			if (mPreviewsNew) {
				hideDesktop(false);
				showingPreviews = false;
				// mDesktopLocked=false;
				mWorkspace.openSense(false);
			} else {
				dismissPreview(mNextView);
				dismissPreview(mPreviousView);
				dismissPreview(mMAB);
				for (int i = 0; i < mWorkspace.getChildCount(); i++) {
					View cell = mWorkspace.getChildAt(i);
					cell.setDrawingCacheEnabled(false);
				}
			}
		}
	}

	private void dismissPreview(final View v) {
		final PopupWindow window = (PopupWindow) v.getTag(R.id.TAG_PREVIEW);
		if (window != null) {
			hideDesktop(false);
			window.setOnDismissListener(new PopupWindow.OnDismissListener() {
				public void onDismiss() {
					ViewGroup group = (ViewGroup) v.getTag(R.id.workspace);
					int count = group.getChildCount();
					for (int i = 0; i < count; i++) {
						((ImageView) group.getChildAt(i))
						.setImageDrawable(null);
					}
					@SuppressWarnings("unchecked")
					ArrayList<Bitmap> bitmaps = ((ArrayList<Bitmap>) v.getTag(R.id.icon));
					for (Bitmap bitmap : bitmaps)
						bitmap.recycle();

					v.setTag(R.id.workspace, null);
					v.setTag(R.id.icon, null);
					window.setOnDismissListener(null);
				}
			});
			window.dismiss();
			showingPreviews = false;
			mWorkspace.unlock();
			mWorkspace.invalidate();
			mDesktopLocked = false;
		}
		v.setTag(R.id.TAG_PREVIEW, null);
	}

	private void showPreviewPrevious(View anchor) {
		if (mWorkspace == null)
			return;
		int current = mWorkspace.getCurrentScreen();
		if (mPreviewsNew) {
			if (current <= 0)
				return;
			showPreviews(anchor, 0, mWorkspace.getCurrentScreen());
		} else {
			showPreviews(anchor, 0, mWorkspace.getChildCount());
		}
	}

	private void showPreviewNext(View anchor) {
		if (mWorkspace == null)
			return;
		int current = mWorkspace.getCurrentScreen();
		if (mPreviewsNew) {
			if (current >= mWorkspace.getChildCount() - 1)
				return;
			showPreviews(anchor, mWorkspace.getCurrentScreen() + 1,
					mWorkspace.getChildCount());
		} else {
			showPreviews(anchor, 0, mWorkspace.getChildCount());
		}
	}

	public void showPreviews(final View anchor, int start, int end) {
		if (mWorkspace != null && mWorkspace.getChildCount() > 0) {
			if (mPreviewsNew) {
				showingPreviews = true;
				hideDesktop(true);
				mWorkspace.lock();
				mWorkspace.openSense(true);
			} else {
				// check first if it's already open
				final PopupWindow window = (PopupWindow) anchor.getTag(R.id.TAG_PREVIEW);
				if (window != null)
					return;
				Resources resources = getResources();

				Workspace workspace = mWorkspace;
				CellLayout cell = ((CellLayout) workspace.getChildAt(start));
				float max;
				ViewGroup preview;
				max = workspace.getChildCount();
				preview = new LinearLayout(this);

				Rect r = new Rect();
				// ADW: seems sometimes this throws an out of memory error....
				// so...
				try {
					resources.getDrawable(R.drawable.preview_background)
					.getPadding(r);
				} catch (OutOfMemoryError e) {
				}
				int extraW = (int) ((r.left + r.right) * max);
				int extraH = r.top + r.bottom;

				int aW = cell.getWidth() - extraW;
				float w = aW / max;

				int width = cell.getWidth();
				int height = cell.getHeight();
				// width -= (x + cell.getRightPadding());
				// height -= (y + cell.getBottomPadding());
				if (width != 0 && height != 0) {
					showingPreviews = true;
					float scale = w / width;

					int count = end - start;

					final float sWidth = width * scale;
					float sHeight = height * scale;

					PreviewTouchHandler handler = new PreviewTouchHandler(
							anchor);
					ArrayList<Bitmap> bitmaps = new ArrayList<Bitmap>(count);

					for (int i = start; i < end; i++) {
						ImageView image = new ImageView(this);
						cell = (CellLayout) workspace.getChildAt(i);
						Bitmap bitmap = Bitmap.createBitmap((int) sWidth,
								(int) sHeight, Bitmap.Config.ARGB_8888);
						cell.setDrawingCacheEnabled(false);
						Canvas c = new Canvas(bitmap);
						c.scale(scale, scale);
						c.translate(-cell.getLeftPadding(),
								-cell.getTopPadding());
						cell.dispatchDraw(c);

						image.setBackgroundDrawable(resources
								.getDrawable(R.drawable.preview_background));
						image.setImageBitmap(bitmap);
						image.setTag(i);
						image.setOnClickListener(handler);
						image.setOnFocusChangeListener(handler);
						image.setFocusable(true);
						if (i == mWorkspace.getCurrentScreen())
							image.requestFocus();

						preview.addView(image,
								LinearLayout.LayoutParams.WRAP_CONTENT,
								LinearLayout.LayoutParams.WRAP_CONTENT);

						bitmaps.add(bitmap);
					}

					PopupWindow p = new PopupWindow(this);
					p.setContentView(preview);
					p.setWidth((int) (sWidth * count + extraW));
					p.setHeight((int) (sHeight + extraH));
					p.setAnimationStyle(R.style.AnimationPreview);
					p.setOutsideTouchable(true);
					p.setFocusable(true);
					p.setBackgroundDrawable(new ColorDrawable(0));
					p.showAsDropDown(anchor, 0, 0);
					p.setOnDismissListener(new PopupWindow.OnDismissListener() {
						public void onDismiss() {
							dismissPreview(anchor);
						}
					});

					anchor.setTag(R.id.TAG_PREVIEW, p);
					anchor.setTag(R.id.workspace, preview);
					anchor.setTag(R.id.icon, bitmaps);
				}
			}
		}
	}

	class PreviewTouchHandler implements View.OnClickListener, Runnable,
	View.OnFocusChangeListener {
		private final View mAnchor;

		public PreviewTouchHandler(View anchor) {
			mAnchor = anchor;
		}

		public void onClick(View v) {
			mWorkspace.snapToScreen((Integer) v.getTag());
			v.post(this);
		}

		public void run() {
			dismissPreview(mAnchor);
		}

		public void onFocusChange(View v, boolean hasFocus) {
			if (hasFocus) {
				mWorkspace.snapToScreen((Integer) v.getTag());
			}
		}
	}

	/**
	 * ADW: Override this to hide statusbar when necessary
	 */
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		// TODO Auto-generated method stub
		super.onWindowFocusChanged(hasFocus);
		if (mShouldHideStatusbaronFocus && hasFocus) {
			fullScreen(true);
			mShouldHideStatusbaronFocus = false;
		}
	}

	/************************************************
	 * ADW: Functions to handle Apps Grid
	 */
	public void showAllApps(boolean animated, AppCatalogueFilter filter) {
		final ApplicationsAdapter appAdapter = sLauncherModel
				.getApplicationsAdapter();
		if (appAdapter != null) {
			if (filter != null) {
				appAdapter.setCatalogueFilter(filter);
			} else {
				appAdapter.setCatalogueFilter(AppCatalogueFilters.getInstance()
						.getDrawerFilter());
			}
		}

		if (!allAppsOpen && mAllAppsGrid != null) {
			final boolean hideDock = (mDockStyle == DOCK_STYLE_NONE)
					|| mDockHide;
			if (isScreenLandscape()) {
				// landscape
				if (hideDock) {
					mDrawerToolbar.setVisibility(View.GONE);
				} else {
					mDrawerToolbar.setVisibility(View.VISIBLE);
					mMAB.setNextFocusUpId(R.id.drag_layer);
					mMAB.setNextFocusLeftId(R.id.drag_layer);

					mLAB.setNextFocusUpId(R.id.drag_layer);
					mLAB.setNextFocusLeftId(R.id.all_apps_view);

					mRAB.setNextFocusUpId(R.id.drag_layer);
					mRAB.setNextFocusLeftId(R.id.all_apps_view);

					mLAB2.setNextFocusUpId(R.id.drag_layer);
					mLAB2.setNextFocusLeftId(R.id.all_apps_view);

					mRAB2.setNextFocusUpId(R.id.drag_layer);
					mRAB2.setNextFocusLeftId(R.id.all_apps_view);

					if (mUseDrawerCatalogNavigation && (mDockStyle == DOCK_STYLE_1 || mDockStyle == DOCK_STYLE_3)) {
						mRAB.setVisibility(View.VISIBLE);
						mLAB.setVisibility(View.VISIBLE);
					}
				}

				final int dockSize = (hideDock ? 0 : mAppDrawerPadding);

				if (dockSize != mAppDrawerPaddingLast) {
					mAppDrawerPaddingLast = dockSize;
					mAllAppsGrid.setPadding(0, 0, mAppDrawerPaddingLast, 0);
				}
			} else {
				// portrait
				if (hideDock) {
					mDrawerToolbar.setVisibility(View.GONE);
				} else {
					mMAB.setNextFocusUpId(R.id.all_apps_view);
					mMAB.setNextFocusLeftId(R.id.drag_layer);

					mLAB.setNextFocusUpId(R.id.all_apps_view);
					mLAB.setNextFocusLeftId(R.id.drag_layer);

					mRAB.setNextFocusUpId(R.id.all_apps_view);
					mRAB.setNextFocusLeftId(R.id.drag_layer);

					mLAB2.setNextFocusUpId(R.id.all_apps_view);
					mLAB2.setNextFocusLeftId(R.id.drag_layer);

					mRAB2.setNextFocusUpId(R.id.all_apps_view);
					mRAB2.setNextFocusLeftId(R.id.drag_layer);

					if (mUseDrawerCatalogNavigation && (mDockStyle == DOCK_STYLE_1 || mDockStyle == DOCK_STYLE_3)) {
						mRAB.setVisibility(View.VISIBLE);
						mLAB.setVisibility(View.VISIBLE);
					}
				}
				final int dockSize = (hideDock ? 0 : mAppDrawerPadding);

				if (dockSize != mAppDrawerPaddingLast) {
					mAppDrawerPaddingLast = dockSize;
					mAllAppsGrid.setPadding(0, 0, 0, mAppDrawerPaddingLast);
				}
			}

			mWorkspace.hideWallpaper(true);
			allAppsOpen = true;
			mWorkspace.enableChildrenCache(mWorkspace.getCurrentScreen(),
					mWorkspace.getCurrentScreen());
			mWorkspace.lock();

			// mDesktopLocked=true;
			mWorkspace.invalidate();
			checkActionButtonsSpecialMode();
			mAllAppsGrid.setUngroupMode(mUseDrawerUngroupCatalog);
			mAllAppsGrid.open(animated && mDrawerAnimate);
			mPreviousView.setVisibility(View.GONE);
			mNextView.setVisibility(View.GONE);
			if (mDesktopIndicator != null)
				mDesktopIndicator.hide();
		}
	}

	private void checkActionButtonsSpecialMode() {

		boolean showSpecialMode = mUseDrawerCatalogNavigation
				&& allAppsOpen
				&& AppCatalogueFilters.getInstance().getUserCatalogueCount() > 0;
				mLAB.setSpecialMode(showSpecialMode);
				mRAB.setSpecialMode(showSpecialMode);
	}

	private void closeAllApps(boolean animated) {
		if (allAppsOpen && mAllAppsGrid != null) {
			if (mDockStyle == DOCK_STYLE_NONE) {
				mDrawerToolbar.setVisibility(View.GONE);
			} else {
				mDrawerToolbar.setVisibility(View.VISIBLE);
				if (mUseDrawerCatalogNavigation && (mDockStyle == DOCK_STYLE_1 || mDockStyle == DOCK_STYLE_3)) {
					mRAB.setVisibility(View.INVISIBLE);
					mLAB.setVisibility(View.INVISIBLE);
				}
			}

			mMAB.setNextFocusUpId(R.id.drag_layer);
			mMAB.setNextFocusLeftId(R.id.drag_layer);
			mLAB.setNextFocusUpId(R.id.drag_layer);
			mLAB.setNextFocusLeftId(R.id.drag_layer);
			mRAB.setNextFocusUpId(R.id.drag_layer);
			mRAB.setNextFocusLeftId(R.id.drag_layer);
			mLAB2.setNextFocusUpId(R.id.drag_layer);
			mLAB2.setNextFocusLeftId(R.id.drag_layer);
			mRAB2.setNextFocusUpId(R.id.drag_layer);
			mRAB2.setNextFocusLeftId(R.id.drag_layer);
			mWorkspace.hideWallpaper(false);
			allAppsOpen = false;
			mWorkspace.unlock();
			// mDesktopLocked=false;
			mWorkspace.setVisibility(View.VISIBLE);
			mWorkspace.invalidate();
			mLAB.setSpecialMode(false);
			mRAB.setSpecialMode(false);

			if (mShowDots) {
				mPreviousView.setVisibility(View.VISIBLE);
				mNextView.setVisibility(View.VISIBLE);
			} else {
				mPreviousView.setVisibility(View.GONE);
				mNextView.setVisibility(View.GONE);
			}
			if (mDesktopIndicator != null)
				mDesktopIndicator.show();

			mAllAppsGrid.close(animated && mDrawerAnimate);
			mAllAppsGrid.clearTextFilter();
		}
	}

	boolean isAllAppsVisible() {
		return allAppsOpen;
		/*
		 * if(mAllAppsGrid!=null) return
		 * mAllAppsGrid.getVisibility()==View.VISIBLE; else return false;
		 */
	}

	boolean isAllAppsOpaque() {
		return mAllAppsGrid.isOpaque() && !allAppsAnimating;
	}

	/**
	 * ADW: wallpaper intent receiver for proper trackicng of wallpaper changes
	 */
	private static class WallpaperIntentReceiver extends BroadcastReceiver {
		private WeakReference<Launcher> mLauncher;

		WallpaperIntentReceiver(Application application, Launcher launcher) {
			setLauncher(launcher);
		}

		void setLauncher(Launcher launcher) {
			mLauncher = new WeakReference<Launcher>(launcher);
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			if (mLauncher != null) {
				final Launcher launcher = mLauncher.get();
				if (launcher != null) {
					final Workspace workspace = launcher.getWorkspace();
					if (workspace != null) {
						workspace.setWallpaper(true);
					}
				}
			}
		}
	}

	public void setWindowBackground(boolean lwp) {
		mWallpaperHack = lwp;
		if (!lwp) {
			getWindow().setBackgroundDrawable(null);
			getWindow().setFormat(PixelFormat.OPAQUE);
			// getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
		} else {
			getWindow().setBackgroundDrawable(new ColorDrawable(0));
			getWindow().setFormat(PixelFormat.TRANSPARENT);
			// getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
		}
	}

	private boolean shouldRestart() {
		try {
			if (mShouldRestart) {

				android.os.Process.killProcess(android.os.Process.myPid());
				finish();
				startActivity(getIntent());
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
		// ADW: Try to add the restart flag here instead on preferences activity
		if (MyLauncherSettingsHelper.needsRestart(key) || mShouldRestart) {
			mShouldRestart = true;

		} else {
			// TODO: ADW Move here all the updates instead on
			// updateAlmostNexusUI()
			if (key.equals("homeOrientation")) {
				changeOrientation(MyLauncherSettingsHelper
						.getDesktopOrientation(this));
			} else if (key.equals("notifReceiver")) {
				boolean useNotifReceiver = MyLauncherSettingsHelper
						.getNotifReceiver(this);
				if (!useNotifReceiver) {
					if (mCounterReceiver != null)
						unregisterReceiver(mCounterReceiver);
					mCounterReceiver = null;
				} else {
					if (mCounterReceiver == null) {
						mCounterReceiver = new CounterReceiver(this);
						mCounterReceiver
						.setCounterListener(new CounterReceiver.OnCounterChangedListener() {
							public void onTrigger(String pname,
									int counter, int color) {
								updateCountersForPackage(pname,
										counter, color);
							}
						});
					}
					registerReceiver(mCounterReceiver,
							mCounterReceiver.getFilter());
				}
			} else if (key.equals("main_dock_style")) {
				int dockStyle = MyLauncherSettingsHelper.getDockStyle(this);
				if (dockStyle == DOCK_STYLE_NONE
						|| mDockStyle == DOCK_STYLE_NONE) {
					mShouldRestart = true;
					return; // we are going to be restarting onResume, so
					// abandon remaining execution
				}
			} else if (key.equals("deleteZoneLocation")) {
				int dz = MyLauncherSettingsHelper.getDeletezoneStyle(this);
				if (mDeleteZone != null)
					mDeleteZone.setPosition(dz);
			}
			updateAlmostNexusUI(); // call rest
		}
	}

	private void appwidgetReadyBroadcast(int appWidgetId, ComponentName cname,
			int[] widgetSpan) {
		Intent motosize = new Intent(
				"com.motorola.blur.home.ACTION_SET_WIDGET_SIZE");

		motosize.setComponent(cname);
		motosize.putExtra("appWidgetId", appWidgetId);
		motosize.putExtra("spanX", widgetSpan[0]);
		motosize.putExtra("spanY", widgetSpan[1]);
		motosize.putExtra("com.motorola.blur.home.EXTRA_NEW_WIDGET", true);
		sendBroadcast(motosize);

		Intent ready = new Intent(LauncherIntent.Action.ACTION_READY)
		.putExtra(LauncherIntent.Extra.EXTRA_APPWIDGET_ID, appWidgetId)
		.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
		.putExtra(LauncherIntent.Extra.EXTRA_API_VERSION,
				LauncherMetadata.CurrentAPIVersion).setComponent(cname);
		sendBroadcast(ready);
	}

	/**
	 * ADW: Action binding actions
	 */
	public void fireActionBinding(int bindingValue, int type) {
		// ADW: switch home button binding user selection
		if (mIsEditMode || mIsWidgetEditMode)
			return;
		switch (bindingValue) {
		case BIND_DEFAULT:
			dismissPreviews();
			if (!mWorkspace.isDefaultScreenShowing()) {
				mWorkspace.moveToDefaultScreen();
			}
			break;
		case BIND_HOME_PREVIEWS:
			if (!mWorkspace.isDefaultScreenShowing()) {
				dismissPreviews();
				mWorkspace.moveToDefaultScreen();
			} else {
				if (!showingPreviews) {
					showPreviews(mMAB, 0, mWorkspace.mHomeScreens);
				} else {
					dismissPreviews();
				}
			}
			break;
		case BIND_PREVIEWS:
			if (!showingPreviews) {
				showPreviews(mMAB, 0, mWorkspace.mHomeScreens);
			} else {
				dismissPreviews();
			}
			break;
		case BIND_APPS:
			dismissPreviews();
			if (isAllAppsVisible()) {
				closeDrawer();
			} else {
				showAllApps(true, null);
			}
			break;
		case BIND_STATUSBAR:
			WindowManager.LayoutParams attrs = getWindow().getAttributes();
			if ((attrs.flags & WindowManager.LayoutParams.FLAG_FULLSCREEN) == WindowManager.LayoutParams.FLAG_FULLSCREEN) {
				// go non-full screen
				fullScreen(false);
			} else {
				// go full screen
				fullScreen(true);
			}
			break;
		case BIND_NOTIFICATIONS:
			dismissPreviews();
			showNotifications();
			break;
		case BIND_HOME_NOTIFICATIONS:
			if (!mWorkspace.isDefaultScreenShowing()) {
				dismissPreviews();
				mWorkspace.moveToDefaultScreen();
			} else {
				dismissPreviews();
				showNotifications();
			}
			break;
		case BIND_APP_LAUNCHER:
		case BIND_ACTIVITY:
			// Launch or bring to front selected app
			// Get PackageName and ClassName of selected App
			String package_name = "";
			String name = "";
			switch (type) {
			case BIND_TYPE_HOME:
				package_name = MyLauncherSettingsHelper
				.getHomeBindingAppToLaunchPackageName(this);
				name = MyLauncherSettingsHelper
						.getHomeBindingAppToLaunchName(this);
				break;
			case BIND_TYPE_SWIPEUP:
				package_name = MyLauncherSettingsHelper
				.getSwipeUpAppToLaunchPackageName(this);
				name = MyLauncherSettingsHelper.getSwipeUpAppToLaunchName(this);
				break;
			case BIND_TYPE_SWIPEDOWN:
				package_name = MyLauncherSettingsHelper
				.getSwipeDownAppToLaunchPackageName(this);
				name = MyLauncherSettingsHelper
						.getSwipeDownAppToLaunchName(this);
				break;
			case BIND_TYPE_PINCHIN:
				package_name = MyLauncherSettingsHelper
				.getPinchInAppToLaunchPackageName(this);
				name = MyLauncherSettingsHelper
						.getPinchInAppToLaunchName(this);
				break;
			default:
				break;
			}
			// Create Intent to Launch App
			if (package_name != "" && name != "") {
				Intent i = new Intent();
				i.setAction(Intent.ACTION_MAIN);
				i.addCategory(Intent.CATEGORY_LAUNCHER);
				i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
						| Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
				i.setComponent(new ComponentName(package_name, name));
				try {
					startActivity(i);
				} catch (Exception e) {
				}
			}
			break;
		default:
			break;
		}
	}

	/**
	 * wjax: Swipe down binding action
	 */
	public void fireSwipeDownAction() {
		// wjax: switch SwipeDownAction button binding user selection
		fireActionBinding(mSwipedownAction, BIND_TYPE_SWIPEDOWN);
	}

	/**
	 * wjax: Swipe up binding action
	 */
	public void fireSwipeUpAction() {
		// wjax: switch SwipeUpAction button binding user selection
		fireActionBinding(mSwipeupAction, BIND_TYPE_SWIPEUP);
	}

	/**
	 * wjax: Pinch in binding action
	 */
	public void firePinchInAction() {
		// wjax: switch SwipeUpAction button binding user selection
		fireActionBinding(mPinchInAction, BIND_TYPE_PINCHIN);
	}

	private void realAddWidget(AppWidgetProviderInfo appWidgetInfo,
			CellLayout.CellInfo cellInfo, int[] spans, int appWidgetId,
			boolean insertAtFirst) {
		// Try finding open space on Launcher screen
		final int[] xy = mCellCoordinates;
		if (!findSlot(cellInfo, xy, spans[0], spans[1])) {
			if (appWidgetId != -1)
				mAppWidgetHost.deleteAppWidgetId(appWidgetId);
			return;
		}

		// Build Launcher-specific widget info and save to database
		LauncherAppWidgetInfo launcherInfo = new LauncherAppWidgetInfo(
				appWidgetId);
		launcherInfo.spanX = spans[0];
		launcherInfo.spanY = spans[1];

		LauncherModel.addItemToDatabase(this, launcherInfo,
				LauncherSettings.Favorites.CONTAINER_DESKTOP,
				mWorkspace.getCurrentScreen(), xy[0], xy[1], false);

		if (!mRestoring) {
			sLauncherModel.addDesktopAppWidget(launcherInfo);

			// Perform actual inflation because we're live
			launcherInfo.hostView = mAppWidgetHost.createView(this,
					appWidgetId, appWidgetInfo);

			launcherInfo.hostView.setAppWidget(appWidgetId, appWidgetInfo);
			launcherInfo.hostView.setTag(launcherInfo);

			mWorkspace.addInCurrentScreen(launcherInfo.hostView, xy[0], xy[1],
					launcherInfo.spanX, launcherInfo.spanY, insertAtFirst);
		} else if (sLauncherModel.isDesktopLoaded()) {
			sLauncherModel.addDesktopAppWidget(launcherInfo);
		}
		// finish load a widget, send it an intent
		if (appWidgetInfo != null)
			appwidgetReadyBroadcast(appWidgetId, appWidgetInfo.provider, spans);
	}

	public static int getScreenCount(Context context) {
		return MyLauncherSettingsHelper.getDesktopScreens(context);
	}

	public DesktopIndicator getDesktopIndicator() {
		return mDesktopIndicator;
	}

	private boolean checkDefaultLauncher() {
		if (LOGD)
			Log.d(LOG_TAG, "full activity name="
					+ (getPackageName() + "." + getLocalClassName()));
		Intent intent = new Intent();
		intent.setAction("android.intent.action.MAIN");
		intent.addCategory("android.intent.category.HOME");
		ActivityInfo a = getPackageManager().resolveActivity(intent,
				PackageManager.MATCH_DEFAULT_ONLY).activityInfo;
		if (a != null
				&& a.name.equals(getPackageName() + "." + getLocalClassName())
				&& (a.applicationInfo.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0) {
			if (LOGD)
				Log.d(LOG_TAG, "is default");
			return true;
		} else {
			if (LOGD)
				Log.d(LOG_TAG, "is NOT default");
			return false;
		}
	}

	/**
	 * ADW: Load the specified theme resource
	 * 
	 * @param themeResources
	 *            Resources from the theme package
	 * @param themePackage
	 *            the theme's package name
	 * @param item_name
	 *            the theme item name to load
	 * @param item
	 *            the View Item to apply the theme into
	 * @param themeType
	 *            Specify if the themed element will be a background or a
	 *            foreground item
	 */
	public static void loadThemeResource(Resources themeResources,
			String themePackage, String item_name, View item, int themeType) {
		Drawable d = null;
		if (themeResources != null) {
			int resource_id = themeResources.getIdentifier(item_name,
					"drawable", themePackage);
			if (resource_id != 0) {
				try {
					d = themeResources.getDrawable(resource_id);
				} catch (Resources.NotFoundException e) {
					return;
				}
				if (themeType == THEME_ITEM_FOREGROUND
						&& item instanceof ImageView) {
					// ADW remove the old drawable
					Drawable tmp = ((ImageView) item).getDrawable();
					if (tmp != null) {
						tmp.setCallback(null);
						tmp = null;
					}
					((ImageView) item).setImageDrawable(d);
				} else {
					// ADW remove the old drawable
					Drawable tmp = item.getBackground();
					if (tmp != null) {
						tmp.setCallback(null);
						tmp = null;
					}
					item.setBackgroundDrawable(d);
				}
			}
		}
	}

	public Typeface getThemeFont() {
		return themeFont;
	}

	private void changeOrientation(int type) {
		switch (type) {
		case MyLauncherSettingsHelper.ORIENTATION_SENSOR:
			this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
			break;
		case MyLauncherSettingsHelper.ORIENTATION_NOSENSOR:
			this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
			break;
		case MyLauncherSettingsHelper.ORIENTATION_LANDSCAPE:
			this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			break;
		case MyLauncherSettingsHelper.ORIENTATION_PORTRAIT:
			this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			break;
		default:
			break;
		}
	}

	void editShortcut(ItemInfo info) {
		Intent edit = new Intent(Intent.ACTION_EDIT);
		edit.setClass(this, CustomShortcutActivity.class);
		edit.putExtra(CustomShortcutActivity.EXTRA_APPLICATIONINFO, info.id);
		startActivityForResult(edit, REQUEST_EDIT_SHORTCUT);
	}

	private void completeEditShortcut(Intent data) {
		if (!data.hasExtra(CustomShortcutActivity.EXTRA_APPLICATIONINFO))
			return;
		long appInfoId = data.getLongExtra(
				CustomShortcutActivity.EXTRA_APPLICATIONINFO, 0);
		FolderInfo folderInfo = LauncherModel.getFolderById(this, appInfoId,
				null);
		if (folderInfo != null) {
			String name = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
			if (!TextUtils.isEmpty(name)) {
				// Make sure we have the right folder info
				folderInfo = getLauncherModel().findFolderById(folderInfo.id);
				folderInfo.title = name;

				Bitmap bitmap = data
						.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON);
				if (bitmap != null) {
					folderInfo.icon = new FastBitmapDrawable(
							Utilities.createBitmapThumbnail(bitmap, this));
					folderInfo.customIcon = true;
				} else {
					folderInfo.icon = null;
					folderInfo.customIcon = false;
				}
				LauncherModel.updateItemInDatabase(this, folderInfo);

				if (!isWorkspaceLocked()) {
					mDesktopLocked = true;
				}
				getLauncherModel().loadUserItems(false, this, false, false);
			}
		} else {
			ApplicationInfo info = LauncherModel.loadApplicationInfoById(this,
					appInfoId);
			if (info != null) {
				Bitmap bitmap = data
						.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON);

				Drawable icon = null;
				if (bitmap != null) {
					// custom
					icon = new FastBitmapDrawable(
							Utilities.createBitmapThumbnail(bitmap, this));
				} else if (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION) {
					// well we need one so get the app's icon
					icon = sLauncherModel.getApplicationInfoIcon(
							getPackageManager(), info, this);
					if (icon instanceof BitmapDrawable) {
						icon = new FastBitmapDrawable(
								((BitmapDrawable) icon).getBitmap());
					}
				}

				if (icon != null) {
					info.icon = icon;
					info.customIcon = true;
					info.iconResource = null;
					info.filtered = false;
				} else {
					info.customIcon = false;
				}

				info.itemType = LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT;
				info.title = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
				info.intent = data
						.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
				LauncherModel.updateItemInDatabase(this, info);

				if (info.container == LauncherSettings.Favorites.CONTAINER_MAB)
					mMAB.UpdateLaunchInfo(info);
				else if (info.container == LauncherSettings.Favorites.CONTAINER_LAB)
					mLAB.UpdateLaunchInfo(info);
				else if (info.container == LauncherSettings.Favorites.CONTAINER_LAB2)
					mLAB2.UpdateLaunchInfo(info);
				else if (info.container == LauncherSettings.Favorites.CONTAINER_RAB)
					mRAB.UpdateLaunchInfo(info);
				else if (info.container == LauncherSettings.Favorites.CONTAINER_RAB2)
					mRAB2.UpdateLaunchInfo(info);

				mWorkspace.updateShortcutFromApplicationInfo(info);
			}
		}
	}

	/**
	 * ADW: Put the launcher in desktop edit mode We could be able to add,
	 * remove and reorder screens
	 */
	private void startDesktopEdit() {
		if (!mIsEditMode) {
			mIsEditMode = true;
			final Workspace workspace = mWorkspace;
			if (workspace == null)
				return;
			workspace.enableChildrenCache(0, workspace.getChildCount());
			workspace.lock();
			hideDesktop(true);

			// Load a gallery view
			final ScreensAdapter screens = new ScreensAdapter(this, workspace
					.getChildAt(0).getWidth(), workspace.getChildAt(0)
					.getHeight());
			for (int i = 0; i < workspace.getChildCount(); i++) {
				screens.addScreen((CellLayout) workspace.getChildAt(i));
			}
			mScreensEditor = mInflater.inflate(R.layout.screens_editor, null);
			mScreensEditor.setAnimation(AnimationUtils.loadAnimation(this,
					R.anim.slide_in_right));

			final Gallery gallery = (Gallery) mScreensEditor
					.findViewById(R.id.gallery_screens);
			gallery.setCallbackDuringFling(false);
			gallery.setClickable(false);
			gallery.setAdapter(screens);

			// buttons
			final View deleteButton = mScreensEditor
					.findViewById(R.id.delete_screen);
			final View addLeftButton = mScreensEditor
					.findViewById(R.id.add_left);
			final View addRightButton = mScreensEditor
					.findViewById(R.id.add_right);
			final View swapLeftButton = mScreensEditor
					.findViewById(R.id.swap_left);
			final View swapRightButton = mScreensEditor
					.findViewById(R.id.swap_right);
			final View setDefaultButton = mScreensEditor
					.findViewById(R.id.set_default);
			final TextView screenEditorText = (TextView) mScreensEditor
					.findViewById(R.id.screen_counter);
			final Animation fadeAnimation = AnimationUtils.loadAnimation(this,
					R.anim.fade_out_slow);

			// Setup delete button event
			deleteButton
			.setOnLongClickListener(new android.view.View.OnLongClickListener() {
				public boolean onLongClick(View v) {
					Toast.makeText(Launcher.this,
							R.string.hint_delete_desktop,
							Toast.LENGTH_SHORT).show();
					return true;
				}
			});
			deleteButton.setVisibility((screens.getCount() > 1) ? View.VISIBLE
					: View.INVISIBLE);
			deleteButton
			.setOnClickListener(new android.view.View.OnClickListener() {
				public void onClick(View v) {
					if (mAlertDialog != null) {
						if (mAlertDialog.isShowing()) {
							return;
						}
					}
					final int screenToDelete = gallery
							.getSelectedItemPosition();
					if (workspace.getChildCount() > 1) {
						mAlertDialog = new AlertDialog.Builder(
								Launcher.this)
						.setTitle(null)
						.setMessage(
								R.string.message_delete_desktop)
								.setPositiveButton(
										android.R.string.ok,
										new DialogInterface.OnClickListener() {
											public void onClick(
													DialogInterface dialog,
													int which) {
												final int workspaceCount = workspace
														.getChildCount();
												if (workspaceCount > screenToDelete) {
													if (workspaceCount == 2) {
														// two screens,
														// but we are
														// getting rid
														// of one asap
														deleteButton
														.setVisibility(View.INVISIBLE);
														swapLeftButton
														.setVisibility(View.INVISIBLE);
														swapRightButton
														.setVisibility(View.INVISIBLE);
													}
													workspace
													.removeScreen(screenToDelete);
													screens.removeScreen(screenToDelete);

													addLeftButton
													.setVisibility(View.VISIBLE);
													addRightButton
													.setVisibility(View.VISIBLE);

													if (screenToDelete == 0) {
														screenEditorText
														.setText("" + 1);
													} else if (screenToDelete == (workspaceCount - 1)) {
														screenEditorText
														.setText(""
																+ (workspaceCount - 1));
													} else {
														if (screenToDelete == (workspaceCount - 2)) {
															swapRightButton
															.setVisibility(View.INVISIBLE);
														}
														screenEditorText
														.setText(""
																+ (screenToDelete + 1));
													}
													screenEditorText
													.startAnimation(fadeAnimation);
													screenEditorText
													.setVisibility(View.INVISIBLE);
												}
											}
										})
										.setNegativeButton(
												android.R.string.cancel, null)
												.create();
						mAlertDialog.show();
					} else {
						if (mAlertDialog != null) {
							if (mAlertDialog.isShowing()) {
								return;
							}
						}
						showSimpleAlertDialog(R.string.message_cannot_delete_desktop);
					}
				}
			});

			// Setup add buttons events
			addLeftButton
			.setVisibility((screens.getCount() < MAX_SCREENS) ? View.VISIBLE
					: View.INVISIBLE);
			addLeftButton
			.setOnLongClickListener(new android.view.View.OnLongClickListener() {
				public boolean onLongClick(View v) {
					Toast.makeText(Launcher.this,
							R.string.hint_add_left_desktop,
							Toast.LENGTH_SHORT).show();
					return true;
				}
			});
			addLeftButton
			.setOnClickListener(new android.view.View.OnClickListener() {
				public void onClick(View v) {
					final int screenCount = screens.getCount();
					if (screenCount < MAX_SCREENS) {
						final int screenToAddLeft = gallery
								.getSelectedItemPosition();
						CellLayout newScreen = workspace
								.addScreen(screenToAddLeft);
						screens.addScreen(newScreen, screenToAddLeft);

						screenEditorText.setText(""
								+ (screenToAddLeft + 1));
						screenEditorText.startAnimation(fadeAnimation);
						screenEditorText.setVisibility(View.INVISIBLE);
						if (screenCount < MAX_SCREENS - 1) {
							addRightButton.setVisibility(View.VISIBLE);
							addLeftButton.setVisibility(View.VISIBLE);
						} else {
							addRightButton
							.setVisibility(View.INVISIBLE);
							addLeftButton.setVisibility(View.INVISIBLE);
						}
						deleteButton.setVisibility(View.VISIBLE);
						swapLeftButton.setVisibility(View.VISIBLE);
					} else {
						addRightButton.setVisibility(View.INVISIBLE);
						addLeftButton.setVisibility(View.INVISIBLE);
						if (mAlertDialog != null) {
							if (mAlertDialog.isShowing()) {
								return;
							}
						}
						showSimpleAlertDialog(R.string.message_cannot_add_desktop);
					}
				}
			});

			addRightButton
			.setVisibility((screens.getCount() < MAX_SCREENS) ? View.VISIBLE
					: View.INVISIBLE);
			addRightButton
			.setOnLongClickListener(new android.view.View.OnLongClickListener() {
				public boolean onLongClick(View v) {
					Toast.makeText(Launcher.this,
							R.string.hint_add_right_desktop,
							Toast.LENGTH_SHORT).show();
					return true;
				}
			});
			addRightButton
			.setOnClickListener(new android.view.View.OnClickListener() {
				public void onClick(View v) {
					final int screenCount = screens.getCount();
					if (screenCount < MAX_SCREENS) {
						final int screenToAddRight = gallery
								.getSelectedItemPosition();
						CellLayout newScreen = workspace
								.addScreen(screenToAddRight + 1);
						screens.addScreen(newScreen,
								screenToAddRight + 1);
						screenEditorText.setText(""
								+ (screenToAddRight + 1));
						screenEditorText.startAnimation(fadeAnimation);
						screenEditorText.setVisibility(View.INVISIBLE);
						if (screenCount < MAX_SCREENS - 1) {
							addRightButton.setVisibility(View.VISIBLE);
							addLeftButton.setVisibility(View.VISIBLE);
						} else {
							addRightButton
							.setVisibility(View.INVISIBLE);
							addLeftButton.setVisibility(View.INVISIBLE);
						}
						deleteButton.setVisibility(View.VISIBLE);
						swapRightButton.setVisibility(View.VISIBLE);
					} else {
						addRightButton.setVisibility(View.INVISIBLE);
						addLeftButton.setVisibility(View.INVISIBLE);
						if (mAlertDialog != null) {
							if (mAlertDialog.isShowing()) {
								return;
							}
						}
						showSimpleAlertDialog(R.string.message_cannot_add_desktop);
					}
				}
			});

			swapLeftButton
			.setOnLongClickListener(new android.view.View.OnLongClickListener() {
				public boolean onLongClick(View v) {
					Toast.makeText(Launcher.this,
							R.string.hint_swap_left_desktop,
							Toast.LENGTH_SHORT).show();
					return true;
				}
			});
			swapLeftButton
			.setOnClickListener(new android.view.View.OnClickListener() {
				public void onClick(View v) {
					int currentScreen = gallery
							.getSelectedItemPosition();
					if (currentScreen > 0) {
						workspace.swapScreens(currentScreen - 1,
								currentScreen);
						screens.swapScreens(currentScreen - 1,
								currentScreen);
					} else {
						if (mAlertDialog != null) {
							if (mAlertDialog.isShowing()) {
								return;
							}
						}
						showSimpleAlertDialog(R.string.message_cannot_swap_desktop);
					}
				}
			});

			swapRightButton
			.setOnLongClickListener(new android.view.View.OnLongClickListener() {
				public boolean onLongClick(View v) {
					Toast.makeText(Launcher.this,
							R.string.hint_swap_right_desktop,
							Toast.LENGTH_SHORT).show();
					return true;
				}
			});
			swapRightButton
			.setOnClickListener(new android.view.View.OnClickListener() {
				public void onClick(View v) {
					int currentScreen = gallery
							.getSelectedItemPosition();
					if (currentScreen < gallery.getCount() - 1) {
						workspace.swapScreens(currentScreen,
								currentScreen + 1);
						screens.swapScreens(currentScreen,
								currentScreen + 1);
					} else {
						if (mAlertDialog != null) {
							if (mAlertDialog.isShowing()) {
								return;
							}
						}
						showSimpleAlertDialog(R.string.message_cannot_swap_desktop);
					}
				}
			});

			setDefaultButton
			.setOnLongClickListener(new android.view.View.OnLongClickListener() {
				public boolean onLongClick(View v) {
					Toast.makeText(Launcher.this,
							R.string.hint_default_desktop,
							Toast.LENGTH_SHORT).show();
					return true;
				}
			});
			setDefaultButton
			.setOnClickListener(new android.view.View.OnClickListener() {
				public void onClick(View v) {
					int currentScreen = gallery
							.getSelectedItemPosition();
					if (currentScreen < mWorkspace.getChildCount()) {
						mWorkspace.setDefaultScreen(currentScreen);
						MyLauncherSettingsHelper.setDefaultScreen(
								Launcher.this, currentScreen);
						if (mAlertDialog != null) {
							if (mAlertDialog.isShowing()) {
								return;
							}
						}
						showSimpleAlertDialog(R.string.pref_title_default_screen);
					}
				}
			});

			gallery.setOnItemLongClickListener(new OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> parent,
						View view, int position, long id) {
					mAlertDialog = new AlertDialog.Builder(Launcher.this)
					.setTitle(null)
					.setMessage(R.string.message_end_edit_desktop)
					.setPositiveButton(android.R.string.ok,
							new DialogInterface.OnClickListener() {
						public void onClick(
								DialogInterface dialog,
								int which) {
							stopDesktopEdit();
						}
					})
					.setNegativeButton(android.R.string.cancel, null)
					.create();
					mAlertDialog.show();
					return true;
				}
			});
			gallery.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view,
						int position, long id) {
					screenEditorText.startAnimation(fadeAnimation);
					screenEditorText.setVisibility(View.INVISIBLE);
				}
			});
			gallery.setOnItemSelectedListener(new OnItemSelectedListener() {
				public void onItemSelected(AdapterView<?> parent, View view,
						int position, long id) {
					screenEditorText.setText("" + (position + 1));
					screenEditorText.startAnimation(fadeAnimation);
					screenEditorText.setVisibility(View.INVISIBLE);

					if (position <= 0) {
						swapLeftButton.setVisibility(View.GONE);
					} else {
						swapLeftButton.setVisibility(View.VISIBLE);
					}
					if (position < parent.getCount() - 1) {
						swapRightButton.setVisibility(View.VISIBLE);
					} else {
						swapRightButton.setVisibility(View.GONE);
					}
				}

				/*
				 * (non-Javadoc)
				 * 
				 * @see android.widget.AdapterView.OnItemSelectedListener#
				 * onNothingSelected(android.widget.AdapterView)
				 */
				@Override
				public void onNothingSelected(AdapterView<?> arg0) {
					// do nothing
				}

			});
			mDragLayer.addView(mScreensEditor);
		}
	}

	private void showSimpleAlertDialog(int msg_id) {
		mAlertDialog = new AlertDialog.Builder(Launcher.this).setTitle(null)
				.setMessage(msg_id)
				.setPositiveButton(android.R.string.ok, null).create();
		mAlertDialog.show();
	}

	private void stopDesktopEdit() {
		mIsEditMode = false;
		hideDesktop(false);
		for (int i = 0; i < mWorkspace.getChildCount(); i++) {
			mWorkspace.getChildAt(i).setDrawingCacheEnabled(false);
		}
		mWorkspace.clearChildrenCache();
		mWorkspace.unlock();
		if (mScreensEditor != null) {
			mDragLayer.removeView(mScreensEditor);
			mScreensEditor = null;
		}
		// dumb workaround to force desktop indicator redraw
		mWorkspace.snapToScreen(sScreen);
	}

	protected boolean isEditMode() {
		return mIsEditMode;
	}

	private LauncherAppWidgetInfo mLauncherAppWidgetInfo = null;

	protected void editWidget(final View widget) {
		if (mWorkspace != null) {
			mIsWidgetEditMode = true;
			final CellLayout screen = (CellLayout) mWorkspace
					.getChildAt(mWorkspace.getCurrentScreen());

			if (screen != null) {
				mLauncherAppWidgetInfo = (LauncherAppWidgetInfo) widget
						.getTag();

				final Intent motosize = new Intent(
						"com.motorola.blur.home.ACTION_SET_WIDGET_SIZE");
				final int appWidgetId = ((AppWidgetHostView) widget)
						.getAppWidgetId();
				final AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager
						.getAppWidgetInfo(appWidgetId);
				if (appWidgetInfo != null) {
					motosize.setComponent(appWidgetInfo.provider);
				}
				motosize.putExtra("appWidgetId", appWidgetId);
				motosize.putExtra("com.motorola.blur.home.EXTRA_NEW_WIDGET",
						true);
				final int minw = (mWorkspace.getWidth()
						- screen.getLeftPadding() - screen.getRightPadding())
						/ screen.getCountX();
				final int minh = (mWorkspace.getHeight()
						- screen.getBottomPadding() - screen.getTopPadding())
						/ screen.getCountY();
				mScreensEditor = new ResizeViewHandler(this);
				// Create a default HightlightView if we found no face in the
				// picture.
				int width = (mLauncherAppWidgetInfo.spanX * minw);
				int height = (mLauncherAppWidgetInfo.spanY * minh);

				final Rect screenRect = new Rect(0, 0, mWorkspace.getWidth()
						- screen.getRightPadding(), mWorkspace.getHeight()
						- screen.getBottomPadding());
				final int x = mLauncherAppWidgetInfo.cellX * minw;
				final int y = mLauncherAppWidgetInfo.cellY * minh;
				final int[] spans = new int[] { 1, 1 };
				final int[] position = new int[] { 1, 1 };
				final CellLayout.LayoutParams lp = (CellLayout.LayoutParams) widget
						.getLayoutParams();
				RectF widgetRect = new RectF(x, y, x + width, y + height);
				((ResizeViewHandler) mScreensEditor).setup(null, screenRect,
						widgetRect, false, false, minw - 10, minh - 10);

				mDragLayer.addView(mScreensEditor);

				final Rect checkRect = new Rect();
				((ResizeViewHandler) mScreensEditor)
				.setOnValidateSizingRect(new ResizeViewHandler.OnSizeChangedListener() {
					@Override
					public void onTrigger(RectF r) {
						if (r != null) {

							int[] tmpspans = {
									Math.max(
											Math.round(r.width()
													/ (minw)), 1),
													Math.max(
															Math.round(r.height()
																	/ (minh)), 1) };
							int[] tmpposition = {
									Math.round(r.left / minw),
									Math.round(r.top / minh) };
							checkRect.set(tmpposition[0],
									tmpposition[1], tmpposition[0]
											+ tmpspans[0],
											tmpposition[1] + tmpspans[1]);
							final boolean widgetCollision = getLauncherModel()
									.isWidgetOverlapping(
											screen.getScreen(),
											appWidgetId, checkRect);
							if (!widgetCollision) {
								((ResizeViewHandler) mScreensEditor)
								.setColliding(false);
							} else {
								((ResizeViewHandler) mScreensEditor)
								.setColliding(true);
							}
							if (tmpposition[0] != position[0]
									|| tmpposition[1] != position[1]
											|| tmpspans[0] != spans[0]
													|| tmpspans[1] != spans[1]) {
								if (!widgetCollision) {
									position[0] = tmpposition[0];
									position[1] = tmpposition[1];
									spans[0] = tmpspans[0];
									spans[1] = tmpspans[1];
									lp.cellX = position[0];
									lp.cellY = position[1];
									lp.cellHSpan = spans[0];
									lp.cellVSpan = spans[1];
									widget.setLayoutParams(lp);
									mLauncherAppWidgetInfo.cellX = lp.cellX;
									mLauncherAppWidgetInfo.cellY = lp.cellY;
									mLauncherAppWidgetInfo.spanX = lp.cellHSpan;
									mLauncherAppWidgetInfo.spanY = lp.cellVSpan;
									widget.setTag(mLauncherAppWidgetInfo);
									// send the broadcast
									motosize.putExtra("spanX", spans[0]);
									motosize.putExtra("spanY", spans[1]);
									Launcher.this
									.sendBroadcast(motosize);
								}
							}

							final float left = Math
									.round(r.left / minw) * minw;
							final float top = Math.round(r.top / minh)
									* minh;
							final float right = left
									+ (Math.max(
											Math.round(r.width()
													/ (minw)), 1) * minw);
							final float bottom = top
									+ (Math.max(
											Math.round(r.height()
													/ (minh)), 1) * minh);

							r.set(left, top, right, bottom);
						}
					}
				});

				((ResizeViewHandler) mScreensEditor)
				.setOnSizeChangedListener(new ResizeViewHandler.OnSizeChangedListener() {
					@Override
					public void onTrigger(RectF r) {

						int[] tmpspans = {
								Math.max(
										Math.round(r.width() / (minw)),
										1),
										Math.max(
												Math.round(r.height() / (minh)),
												1) };
						int[] tmpposition = {
								Math.round(r.left / minw),
								Math.round(r.top / minh) };
						checkRect.set(tmpposition[0], tmpposition[1],
								tmpposition[0] + tmpspans[0],
								tmpposition[1] + tmpspans[1]);
						final boolean widgetCollision = getLauncherModel()
								.isWidgetOverlapping(
										screen.getScreen(),
										appWidgetId, checkRect);
						if (!widgetCollision) {
							((ResizeViewHandler) mScreensEditor)
							.setColliding(false);
						} else {
							((ResizeViewHandler) mScreensEditor)
							.setColliding(true);
						}
						/*
						 * if (tmpposition[0] != position[0] ||
						 * tmpposition[1] != position[1] || tmpspans[0]
						 * != spans[0] || tmpspans[1] != spans[1]) { if
						 * (!widgetCollision) { position[0] =
						 * tmpposition[0]; position[1] = tmpposition[1];
						 * spans[0] = tmpspans[0]; spans[1] =
						 * tmpspans[1]; lp.cellX = position[0]; lp.cellY
						 * = position[1]; lp.cellHSpan = spans[0];
						 * lp.cellVSpan = spans[1];
						 * widget.setLayoutParams(lp);
						 * mLauncherAppWidgetInfo.cellX = lp.cellX;
						 * mLauncherAppWidgetInfo.cellY = lp.cellY;
						 * mLauncherAppWidgetInfo.spanX = lp.cellHSpan;
						 * mLauncherAppWidgetInfo.spanY = lp.cellVSpan;
						 * widget.setTag(mLauncherAppWidgetInfo); //
						 * send the broadcast motosize.putExtra("spanX",
						 * spans[0]); motosize.putExtra("spanY",
						 * spans[1]);
						 * Launcher.this.sendBroadcast(motosize); } }
						 */
					}
				});
			}
		}
	}

	private void stopWidgetEdit() {
		mIsWidgetEditMode = false;
		if (mLauncherAppWidgetInfo != null) {
			LauncherModel.resizeItemInDatabase(this, mLauncherAppWidgetInfo,
					LauncherSettings.Favorites.CONTAINER_DESKTOP,
					mLauncherAppWidgetInfo.screen,
					mLauncherAppWidgetInfo.cellX, mLauncherAppWidgetInfo.cellY,
					mLauncherAppWidgetInfo.spanX, mLauncherAppWidgetInfo.spanY);
			mLauncherAppWidgetInfo = null;
		}
		// Remove the resizehandler view
		if (mScreensEditor != null) {
			mDragLayer.removeView(mScreensEditor);
			mScreensEditor = null;
		}
	}

	void navigateCatalogs(final int direction) {
		ApplicationsAdapter drawerAdapter = sLauncherModel
				.getApplicationsAdapter();
		if (drawerAdapter == null)
			return;

		List<Integer> filterIndexes = AppCatalogueFilters.getInstance()
				.getGroupsAndSpecialGroupIndexes();
		final AppCatalogueFilter filter = drawerAdapter.getCatalogueFilter();
		int currentFIndex = filter.getCurrentFilterIndex();
		// Translate to index of the list
		currentFIndex = filterIndexes.contains(currentFIndex) ? filterIndexes
				.indexOf(currentFIndex) : filterIndexes
				.indexOf(AppGroupAdapter.APP_GROUP_ALL);

				switch (direction) {
				case ACTION_CATALOG_PREV:
					currentFIndex--;
					break;
				case ACTION_CATALOG_NEXT:
					currentFIndex++;
					break;
				default:
					break;
				}

				if (currentFIndex < 0)
					currentFIndex = filterIndexes.size() - 1;
				else if (currentFIndex >= filterIndexes.size())
					currentFIndex = 0;

				// Translate to "filter index"
				final int setIndex = filterIndexes.get(currentFIndex);

				if (mUseDrawerUngroupCatalog) {
					filter.setCurrentGroupIndex(setIndex);
					if (currentFIndex == 0
							&& filterIndexes.size() > 1
							&& ((ApplicationsAdapter.CatalogueFilter) drawerAdapter
									.getFilter()).isEmpty()) {
						navigateCatalogs(direction);
						return;
					}
				}

				mAllAppsGrid.switchGroups(new Runnable() {

					@Override
					public void run() {
						filter.setCurrentGroupIndex(setIndex);
						if (filter == AppCatalogueFilters.getInstance()
								.getDrawerFilter())
							MyLauncherSettingsHelper.setCurrentAppCatalog(
									Launcher.this, setIndex);

						mAllAppsGrid.updateAppGrp();
						((View) mAllAppsGrid).invalidate();
					}
				});

				// Uncomment this to show a toast with the name of the new group...
				// String name = currentFIndex == AppGroupAdapter.APP_GROUP_ALL ?
				// getString(R.string.app_group_un) :
				// AppCatalogueFilters.getInstance().getGroupTitle(currentFIndex);
				// if (name != null) {
				// ((LauncherPlus)this).groupTitlePopupWindow(this, mAllAppsGrid, name);
				//
				// //Toast t=Toast.makeText(this, name, Toast.LENGTH_SHORT);
				// //t.show();
				// }
	}

	private void updateCounters(View view, String packageName, int counter,
			int color) {
		Object tag = view.getTag();
		if (tag instanceof ApplicationInfo) {
			ApplicationInfo info = (ApplicationInfo) tag;
			// We need to check for ACTION_MAIN otherwise getComponent() might
			// return null for some shortcuts (for instance, for shortcuts to
			// web pages.)
			final Intent intent = info.intent;
			final ComponentName name = intent.getComponent();
			if ((info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION || info.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT)
					&& Intent.ACTION_MAIN.equals(intent.getAction())
					&& name != null
					&& packageName.equals(name.getPackageName())) {
				if (view instanceof CounterImageView)
					((CounterImageView) view).setCounter(counter, color);
				// else if
				view.invalidate();
				sLauncherModel.updateCounterDesktopItem(info, counter, color);
			}
		}
	}

	private void updateCountersForPackage(String packageName, int counter,
			int color) {
		if (packageName != null && packageName.length() > 0) {
			mWorkspace.updateCountersForPackage(packageName, counter, color);
			// ADW: Update ActionButtons icons
			updateCounters(mMAB, packageName, counter, color);
			updateCounters(mLAB, packageName, counter, color);
			updateCounters(mRAB, packageName, counter, color);
			updateCounters(mLAB2, packageName, counter, color);
			updateCounters(mRAB2, packageName, counter, color);
			sLauncherModel.updateCounterForPackage(this, packageName, counter,
					color);
		}
	}

	@Override
	public void startActivity(Intent intent) {
		final ComponentName name = intent.getComponent();
		if (name != null)
			updateCountersForPackage(name.getPackageName(), 0, 0);
		try {
			super.startActivity(intent);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(this, R.string.activity_not_found,
					Toast.LENGTH_SHORT).show();
		} catch (SecurityException e) {
			Toast.makeText(this, R.string.activity_security_exception,
					Toast.LENGTH_SHORT).show();
			e(LOG_TAG,
					"Launcher does not have the permission to launch "
							+ intent
							+ ". Make sure to create a MAIN intent-filter for the corresponding activity "
							+ "or use the exported attribute for this activity.",
							e);
		}
	}

	private QuickActionWindow mQaw;

	void dismissQuickActionWindow() {
		if (mQaw != null) {
			if (mQaw.isShowing()) {
				mQaw.dismiss();
			}
			mQaw = null;
		}
	}

	void showQuickActionWindow(final ItemInfo info, final View view,
			PopupWindow.OnDismissListener onDismissListener) {
		QuickActionWindow existingQA = (QuickActionWindow) view.getTag(R.id.TAG_PREVIEW);
		if (existingQA != null && existingQA.isShowing()) {
			return;
		}

		int[] xy = new int[2];
		// fills the array with the computed coordinates
		view.getLocationInWindow(xy);
		// rectangle holding the clicked view area
		Rect rect = new Rect(xy[0], xy[1], xy[0] + view.getWidth(), xy[1]
				+ view.getHeight());

		// a new QuickActionWindow object
		final QuickActionWindow qa = new QuickActionWindow(this, view, rect);
		view.setTag(R.id.TAG_PREVIEW, qa);
		mQaw = qa;

		if (onDismissListener != null) {
			qa.setOnDismissListener(onDismissListener);
		}

		if (info.container != ItemInfo.NO_ID) {
			// adds an item to the badge and defines the quick action to be
			// triggered
			// when the item is clicked on

			// remove icon
			qa.addItem(
					getResources().getDrawable(
							android.R.drawable.ic_menu_delete),
							R.string.menu_delete, new OnClickListener() {
						public void onClick(View v) {
							final LauncherModel model = Launcher
									.getLauncherModel();
							boolean removedFromFolder = false;
							if (info.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
								if (info instanceof LauncherAppWidgetInfo) {
									model.removeDesktopAppWidget((LauncherAppWidgetInfo) info);
								} else {
									model.removeDesktopItem(info);
								}
							} else {
								// in a folder?
								FolderInfo source = sLauncherModel
										.getFolderById(Launcher.this,
												info.container);
								if (source instanceof UserFolderInfo) {
									final UserFolderInfo userFolderInfo = (UserFolderInfo) source;
									model.removeUserFolderItem(userFolderInfo,
											info);
									removedFromFolder = true;
								}
							}
							if (info instanceof UserFolderInfo) {
								final UserFolderInfo userFolderInfo = (UserFolderInfo) info;
								LauncherModel
								.deleteUserFolderContentsFromDatabase(
										Launcher.this, userFolderInfo);
								model.removeUserFolder(userFolderInfo);
							} else if (info instanceof LauncherAppWidgetInfo) {
								final LauncherAppWidgetInfo launcherAppWidgetInfo = (LauncherAppWidgetInfo) info;
								final LauncherAppWidgetHost appWidgetHost = Launcher.this
										.getAppWidgetHost();
								Launcher.this
								.getWorkspace()
								.unbindWidgetScrollableId(
										launcherAppWidgetInfo.appWidgetId);
								if (appWidgetHost != null) {
									appWidgetHost
									.deleteAppWidgetId(launcherAppWidgetInfo.appWidgetId);
								}
							}
							LauncherModel.deleteItemFromDatabase(Launcher.this,
									info);

							if (removedFromFolder) {
								final Folder folder = mWorkspace
										.getOpenFolder();
								if (folder != null)
									folder.notifyDataSetChanged();
							} else {
								try {
									if (view instanceof ActionButton)
										((ActionButton) view)
										.UpdateLaunchInfo(null);
									else {
										((ViewGroup) view.getParent())
										.removeView(view);
									}
								} catch (NullPointerException e) {}
							}
							v.post(new Runnable() {
								@Override
								public void run() {
									qa.dismiss();

								}
							});
						}
					});
			if (info instanceof ApplicationInfo) {
				qa.addItem(
						getResources().getDrawable(
								android.R.drawable.ic_menu_edit),
								R.string.menu_edit, new OnClickListener() {
							public void onClick(View v) {
								editShortcut((ApplicationInfo) info);
								v.post(new Runnable() {
									@Override
									public void run() {
										qa.dismiss();

									}
								});
							}
						});
			} else if (info instanceof LauncherAppWidgetInfo) {
				qa.addItem(getResources()
						.getDrawable(R.drawable.ic_menu_resize),
						R.string.menu_resize_widget, new OnClickListener() {
					public void onClick(View v) {
						editWidget(view);
						v.post(new Runnable() {
							@Override
							public void run() {
								qa.dismiss();

							}
						});
					}
				});
			}
		}

		String infoPackage = null;
		boolean isADWShortcut = false;
		if (info instanceof FolderInfo) {
			qa.addItem(
					getResources().getDrawable(android.R.drawable.ic_menu_edit),
					R.string.menu_edit, new OnClickListener() {
						public void onClick(View v) {
							if (info instanceof LiveFolderInfo) {
								mFolderInfo = (FolderInfo) info;
								mWaitingForResult = true;
								mWorkspace.lock();
								showDialog(DIALOG_RENAME_FOLDER);
							} else {
								editShortcut(info);
							}
							v.post(new Runnable() {
								@Override
								public void run() {
									qa.dismiss();

								}
							});
						}
					});
			if (info instanceof LiveFolderInfo) {
				LiveFolderInfo lfi = (LiveFolderInfo) info;
				if (lfi.baseIntent != null) {
					Uri data = lfi.baseIntent.getData();
					if (data != null) {
						String host = data.getHost();
						PackageManager mgr = getPackageManager();
						ProviderInfo packageInfo = mgr.resolveContentProvider(
								host, 0);
						if (packageInfo != null)
							infoPackage = packageInfo.packageName;
					}
				}
			}
		} else if (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION
				|| info.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT
				|| info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET) {
			if (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION
					|| info.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT) {
				ApplicationInfo appInfo = (ApplicationInfo) info;
				ComponentName component = appInfo.intent.getComponent();
				if (component != null) {
					infoPackage = component.getPackageName();
					isADWShortcut = (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT && component
							.getPackageName().equals(
									Launcher.class.getPackage().getName()));
				}
			} else if (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET) {
				LauncherAppWidgetInfo appwidget = (LauncherAppWidgetInfo) info;
				final AppWidgetProviderInfo appWidgetInfo = AppWidgetManager
						.getInstance(this).getAppWidgetInfo(
								appwidget.appWidgetId);
				if (appWidgetInfo != null) {
					infoPackage = appWidgetInfo.provider.getPackageName();
				}
			}
		}

		// found the package and it's not an ADW internal shortcut
		if (infoPackage != null && !isADWShortcut) {
			final String appPackage = infoPackage;
			// get the application info label
			if (mAppInfoLabel == null) {
				try {
					Resources resources = createPackageContext(
							ANDROID_SETTINGS_PACKAGE,
							Context.CONTEXT_IGNORE_SECURITY).getResources();
					int nameID = resources.getIdentifier(
							"application_info_label", "string",
							ANDROID_SETTINGS_PACKAGE);
					if (nameID != 0) {
						mAppInfoLabel = resources.getString(nameID);
					}
				} catch (Exception e) {
					// can't lookup the name!
				}
			}
			// if application info label loaded show the option
			if (mAppInfoLabel != null) {
				qa.addItem(
						getResources().getDrawable(
								android.R.drawable.ic_menu_info_details),
								mAppInfoLabel, new OnClickListener() {
							public void onClick(View v) {
								v.post(new Runnable() {
									@Override
									public void run() {
										qa.dismiss();

									}
								});
								try {
									Intent intent = new Intent();
									final int apiLevel = Build.VERSION.SDK_INT;
									if (apiLevel >= 9) { // above 2.3
										intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
										Uri uri = Uri.fromParts("package",
												appPackage, null);
										intent.setData(uri);
									} else { // below 2.3
										final String appPkgName = (apiLevel == 8 ? "pkg"
												: "com.android.settings.ApplicationPkgName");
										intent.setAction(Intent.ACTION_VIEW);
										intent.setClassName(
												ANDROID_SETTINGS_PACKAGE,
												"com.android.settings.InstalledAppDetails");
										intent.putExtra(appPkgName, appPackage);
									}
									Launcher.this.startActivity(intent);
								} catch (Exception e) {
									// failed to start app info
								}
							}
						});
			}

			// get the market icon
			if (mMarketIcon == null && mMarketLabel == null) {
				try {
					PackageManager packageManager = getPackageManager();
					android.content.pm.ApplicationInfo applicationInfo = packageManager
							.getApplicationInfo(ANDROID_MARKET_PACKAGE, 0);
					mMarketIcon = applicationInfo.loadIcon(packageManager);
					mMarketLabel = applicationInfo.loadLabel(packageManager);
					if (mMarketLabel == null) {
						mMarketLabel = applicationInfo.name;
					}
				} catch (Exception e) {
					// would appear there is no market
					mMarketIcon = null;
					mMarketLabel = ""; // empty string so we don't try to load
					// this again
				}
			}
			// if market is available, show it as an option
			if (mMarketIcon != null && mMarketLabel != null) {
				qa.addItem(mMarketIcon, (String) mMarketLabel,
						new OnClickListener() {
					public void onClick(View v) {
						v.post(new Runnable() {
							@Override
							public void run() {
								qa.dismiss();

							}
						});
						try {
							Intent intent = new Intent(
									Intent.ACTION_VIEW);
							intent.setData(Uri
									.parse("market://search?q=pname:"
											+ appPackage));
							startActivity(intent);
						} catch (Exception e) {
							// failed to tell market to find the app
						}
					}
				});
			}
			// application catalogs
			if (info instanceof ApplicationInfo) {
				qa.addItem(
						getResources().getDrawable(
								android.R.drawable.ic_menu_agenda),
								R.string.app_group_choose, new OnClickListener() {
							public void onClick(View v) {
								mPickGroupInfo = (ApplicationInfo) info;
								mWaitingForResult = true;
								mWorkspace.lock();
								showDialog(DIALOG_PICK_GROUPS);
								v.post(new Runnable() {
									@Override
									public void run() {
										qa.dismiss();

									}
								});
							}
						});
			}
		}
		// rearrange user info folder item
		if (info instanceof UserFolderInfo) {
			final UserFolderInfo ufi = (UserFolderInfo) info;
			if (!ufi.contents.isEmpty()) {
				qa.addItem(
						getResources().getDrawable(R.drawable.ic_menu_grabber),
						R.string.menu_folder_icon_reorder,
						new OnClickListener() {
							public void onClick(View v) {
								Intent i = new Intent(Launcher.this,
										FolderIconReorderActivity.class);
								i.putExtra(
										FolderIconReorderActivity.EXTRA_FOLDER_INFO_ID,
										ufi.id);
								Launcher.this.startActivity(i);
							}
						});
			}
		}
		// shows the quick action window on the screen
		qa.show();
	}

	public void setDockPadding(int pad) {
		if (pad < 0) {
			mDrawerToolbar.setPadding(0, -pad, 0, 0);
		} else {
			mDrawerToolbar.setPadding(0, 0, 0, pad);

		}
	}

	// CCHOW CHANGE START
	/*
	public void createManageApplications(Menu menu, int menuGroupAlmostnexus) {
		// get the application info label and if found show the option
		if (mAppManageLabel == null) {
			try {
				Resources resources = createPackageContext(
						ANDROID_SETTINGS_PACKAGE,
						Context.CONTEXT_IGNORE_SECURITY).getResources();
				int nameID = resources.getIdentifier(
						"manageapplications_settings_title", "string",
						ANDROID_SETTINGS_PACKAGE);
				if (nameID != 0) {
					mAppManageLabel = resources.getString(nameID);
				}
			} catch (Exception e) {
				// can't find the settings label
			}
		}
		if (mAppManageLabel != null) {
			final Intent settings = new Intent(Intent.ACTION_MAIN);
			settings.setComponent(ANDROID_MANAGE_COMPONENT);
			settings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
					| Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

			menu.add(menuGroupAlmostnexus, MENU_MANAGE_APPS, 0, mAppManageLabel)
			.setIcon(android.R.drawable.ic_menu_manage)
			.setAlphabeticShortcut('M').setIntent(settings);
		}
	}
	 */
	// CCHOW CHANGE END

	protected class PickGrpDialog implements
	DialogInterface.OnMultiChoiceClickListener,
	DialogInterface.OnCancelListener,
	DialogInterface.OnDismissListener, DialogInterface.OnClickListener {
		private List<Catalog> mAllGroups;
		private boolean[] inGroup;

		Dialog createDialog() {
			mWaitingForResult = true;

			final AlertDialog.Builder builder = new AlertDialog.Builder(
					Launcher.this);
			builder.setTitle(getString(R.string.app_group_choose));

			mAllGroups = AppCatalogueFilters.getInstance().getAllGroups();

			int count = mAllGroups.size();
			CharSequence[] groups = new CharSequence[count];
			inGroup = new boolean[count];
			for (int i = 0; i < count; i++) {
				groups[i] = mAllGroups.get(i).getTitle();
				inGroup[i] = mAllGroups
						.get(i)
						.getPreferences()
						.getBoolean(
								mPickGroupInfo.intent.getComponent()
								.flattenToString(), false);
			}
			builder.setMultiChoiceItems(groups, inGroup, this);
			builder.setInverseBackgroundForced(true);
			builder.setPositiveButton(android.R.string.ok, this);
			builder.setNeutralButton(R.string.app_group_add,
					new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int arg1) {
					dialog.dismiss();
					removeDialog(DIALOG_PICK_GROUPS);
					showDialog(DIALOG_NEW_GROUP);
				}
			});

			AlertDialog dialog = builder.create();
			dialog.setOnCancelListener(this);
			dialog.setOnDismissListener(this);
			return dialog;
		}

		public void onCancel(DialogInterface dialog) {
			mPickGroupInfo = null;
			cleanup();
		}

		public void onDismiss(DialogInterface dialog) {
			cleanup();
		}

		private void cleanup() {
			mWaitingForResult = false;
			try {
				mAllAppsGrid.updateAppGrp();
				getWorkspace().unlock();
				removeDialog(DIALOG_PICK_GROUPS);
			} catch (Exception e) {
				// Restarted while dialog or whatever causes
				// IllegalStateException???
			}
		}

		public void onClick(DialogInterface dialog, int which) {
			for (int i = 0; i < inGroup.length; i++) {
				Editor edit = mAllGroups.get(i).getPreferences().edit();
				if (inGroup[i]) {
					edit.putBoolean(mPickGroupInfo.intent.getComponent()
							.flattenToString(), true);
				} else {
					edit.remove(mPickGroupInfo.intent.getComponent()
							.flattenToString());
				}
				edit.commit();
			}
			cleanup();
		}

		@Override
		public void onClick(DialogInterface arg0, int which, boolean checked) {
			inGroup[which] = checked;
		}
	}

	// Apps Watcher
	public void drawerOpacity(int bgAlpha) {
		if (bgAlpha >= 255) {
			mWorkspace.setVisibility(View.GONE);
		}
	}

	// gives screen orientation wrt display dimensions
	protected boolean isScreenLandscape() {
		/*
		return getWindow().getDecorView().getWidth() > mDisplay.getHeight();
		 */
		return mDisplay.getWidth() > mDisplay.getHeight();
	}
}
