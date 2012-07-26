package com.wordpress.chislonchow.legacylauncher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.List;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.WallpaperManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.wordpress.chislonchow.legacylauncher.ui.DialogSeekBarPreference;

public class MyLauncherSettings extends PreferenceActivity implements
OnPreferenceChangeListener {

	public static final boolean IsDebugVersion = false;
	private static final String LEGACY_PREFERENCES = "launcher.preferences.almostnexus";
	private boolean shouldRestart = false;
	private Context mContext;

	static final String ANDROID_SETTINGS_PACKAGE = "com.android.settings";

	private static final String PREF_BACKUP_FILENAME = "legacy_launcher_settings.xml";
	private static final String CONFIG_BACKUP_FILENAME = "legacy_launcher.db";
	private static final String NAMESPACE = "com.wordpress.chislonchow.legacylauncher";
	private static final String LAUNCHER_DB_BASE = "/data/" + NAMESPACE
			+ "/databases/launcher.db";

	// Request codes for onResultActivity. That way we know the request donw
	// when startActivityForResult was fired
	private static final int REQUEST_SWIPE_DOWN_APP_CHOOSER = 0;
	private static final int REQUEST_HOME_BINDING_APP_CHOOSER = 1;
	private static final int REQUEST_SWIPE_UP_APP_CHOOSER = 2;

	private AlertDialog mAlertDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO: ADW should i read stored values after
		// addPreferencesFromResource?
		super.onCreate(savedInstanceState);
		getPreferenceManager()
		.setSharedPreferencesName(LEGACY_PREFERENCES);

		addPreferencesFromResource(R.xml.launcher_settings);

		// Icon preferences
		/*
		IconPreferenceScreen icPref;
		icPref = (IconPreferenceScreen) findPreference("desktopPreferences");
		icPref.setIcon(getResources().getDrawable(R.drawable.ic_settings_desktop));
		 */
		// dialog seek bar preferences
		DialogSeekBarPreference dialogSeekBarPref = (DialogSeekBarPreference) findPreference("desktopColumns");
		dialogSeekBarPref.setMin(3);
		dialogSeekBarPref = (DialogSeekBarPreference) findPreference("desktopRows");
		dialogSeekBarPref.setMin(3);

		dialogSeekBarPref = (DialogSeekBarPreference) findPreference("drawerColumnsPortrait");
		dialogSeekBarPref.setMin(1);
		DialogSeekBarPreference rowsPortrait = (DialogSeekBarPreference) findPreference("drawerRowsPortrait");
		rowsPortrait.setMin(1);
		DialogSeekBarPreference columnsLandscape = (DialogSeekBarPreference) findPreference("drawerColumnsLandscape");
		columnsLandscape.setMin(1);
		DialogSeekBarPreference rowsLandscape = (DialogSeekBarPreference) findPreference("drawerRowsLandscape");
		rowsLandscape.setMin(1);

		DialogSeekBarPreference dSPref;
		dSPref = (DialogSeekBarPreference) findPreference("animationSpeed");
		dSPref.setMin(300);
		dSPref.setInterval(50);
		dSPref =  (DialogSeekBarPreference) findPreference("drawerLabelTextSize");
		dSPref.setMin(8);
		dSPref = (DialogSeekBarPreference) findPreference("desktopLabelSize");
		dSPref.setMin(8);
		dSPref = (DialogSeekBarPreference) findPreference("desktopSpeed");
		dSPref.setInterval(50);
		dSPref = (DialogSeekBarPreference) findPreference("desktopBounce");
		dSPref.setInterval(10);
		dSPref = (DialogSeekBarPreference) findPreference("desktopSnap");
		dSPref.setInterval(50);

		dSPref = (DialogSeekBarPreference) findPreference("pageHorizontalMargin");
		dSPref.setInterval(5);
		dSPref = (DialogSeekBarPreference) findPreference("notifSize");
		dSPref.setMin(10);
		dSPref = (DialogSeekBarPreference) findPreference("uiScaleAB");
		dSPref.setMin(1);
		dSPref = (DialogSeekBarPreference) findPreference("folderTextSize");
		dSPref.setMin(8);

		ListPreference desktopIndicator = (ListPreference) findPreference("uiDesktopIndicatorType");
		desktopIndicator.setOnPreferenceChangeListener(this);
		desktopIndicator.setSummary(desktopIndicator.getEntry());
		// enable/disable slider color customization based on indicator type
		if (Integer.valueOf(desktopIndicator.getValue()) > 1) {
			(findPreference("uiDesktopIndicatorColorAllow")).setEnabled(true);
			(findPreference("uiDesktopIndicatorColor")).setEnabled(true);
		} else {
			(findPreference("uiDesktopIndicatorColorAllow")).setEnabled(false);
			(findPreference("uiDesktopIndicatorColor")).setEnabled(false);
		}

		ListPreference listPref = (ListPreference) findPreference("deleteZoneLocation");
		listPref.setOnPreferenceChangeListener(this);
		listPref.setSummary(listPref.getEntry());

		listPref = (ListPreference) findPreference("desktopTransitionStyle");
		listPref.setOnPreferenceChangeListener(this);
		listPref.setSummary(listPref.getEntry());

		// wjax. Listen for changes in those ListPreference as if their values
		// are BINDING_APP, then an app shall be selected via
		// startActivityForResult
		listPref = (ListPreference) findPreference("swipedownActions");
		listPref.setOnPreferenceChangeListener(this);
		listPref.setSummary(listPref.getEntry());
		listPref = (ListPreference) findPreference("swipeupActions");
		listPref.setOnPreferenceChangeListener(this);
		listPref.setSummary(listPref.getEntry());
		listPref = (ListPreference) findPreference("homeBinding");
		listPref.setOnPreferenceChangeListener(this);
		listPref.setSummary(listPref.getEntry());

		// dock style
		listPref = (ListPreference) findPreference("main_dock_style");
		listPref.setOnPreferenceChangeListener(this);
		listPref.setSummary(listPref.getEntry());
		int val = Integer.valueOf(listPref.getValue());

		CheckBoxPreference checkPref;
		checkPref = (CheckBoxPreference) findPreference("uiDots");

		switch (val) {
		case Launcher.DOCK_STYLE_NONE:
			checkPref.setChecked(false);
			checkPref.setEnabled(false);

			findPreference("mainDockLockMAB").setEnabled(false);

			findPreference("mainDockDrawerHide").setEnabled(false);
			break;
		case Launcher.DOCK_STYLE_5:
			checkPref.setChecked(false);
			checkPref.setEnabled(false);

			findPreference("mainDockLockMAB").setEnabled(true);

			findPreference("mainDockDrawerHide").setEnabled(true);
			break;
		default:	//styles 1, 3
			checkPref.setEnabled(true);

			findPreference("mainDockLockMAB").setEnabled(true);

			findPreference("mainDockDrawerHide").setEnabled(true);
			break;
		}

		// system persistent
		listPref = (ListPreference) findPreference("homeOrientation");
		listPref.setOnPreferenceChangeListener(this);
		listPref.setSummary(listPref.getEntry());

		listPref = (ListPreference) findPreference("drawerStyle");
		listPref.setOnPreferenceChangeListener(this);
		listPref.setSummary(listPref.getEntry());
		Preference margin = findPreference("pageHorizontalMargin");
		val = Integer.valueOf(listPref.getValue());
		if (val == 1) {
			rowsPortrait.setEnabled(true);
			rowsLandscape.setEnabled(true);
			margin.setEnabled(true);
			dSPref = (DialogSeekBarPreference) findPreference("drawerSpeed");
			dSPref.setInterval(50);
			dSPref.setEnabled(true);
			dSPref = (DialogSeekBarPreference) findPreference("drawerSnap");
			dSPref.setEnabled(true);
			dSPref.setInterval(50);
			findPreference("drawerOvershoot").setEnabled(true);
		} else {
			rowsPortrait.setEnabled(false);
			rowsLandscape.setEnabled(false);
			margin.setEnabled(false);
			dSPref = (DialogSeekBarPreference) findPreference("drawerSpeed");
			dSPref.setInterval(50);
			dSPref.setEnabled(false);
			dSPref = (DialogSeekBarPreference) findPreference("drawerSnap");
			dSPref.setEnabled(false);
			dSPref.setInterval(50);
			findPreference("drawerOvershoot").setEnabled(false);
		}
		mContext = this;

		// launcher lock password implementation
		Preference launcherLockPassword = findPreference("launcherLockPassword");
		launcherLockPassword.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				final EditText input = new EditText(mContext);
				input.setMaxLines(1);
				input.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
				input.setHint(getString(R.string.hint_password_new));
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
				mAlertDialog = new AlertDialog.Builder(mContext)
				.setMessage(R.string.dialog_lock_password_set)
				.setView(input)
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,
							int which) {
						Editable value = input.getText();
						final String password =  value.toString();
						final ObscuredSharedPreferences oPrefs = new ObscuredSharedPreferences( 
								mContext, mContext.getSharedPreferences("secure", Context.MODE_PRIVATE) );
						oPrefs.edit().putString("pw", password).commit();
						if (password.length() > 0) {
							Toast.makeText(mContext, getString(R.string.toast_password_set) + password, Toast.LENGTH_SHORT).show();
						} else {
							Toast.makeText(mContext, R.string.toast_password_clear, Toast.LENGTH_SHORT).show();
						}
					}
				})
				.setNegativeButton(android.R.string.cancel, null).create();
				mAlertDialog.show();
				return false;
			}
		});

		// launcher lock password implementation
		Preference launcherLockPasswordClear = findPreference("launcherLockPasswordClear");
		launcherLockPasswordClear.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				final ObscuredSharedPreferences oPrefs = new ObscuredSharedPreferences( 
						mContext, mContext.getSharedPreferences("secure", Context.MODE_PRIVATE) );
				oPrefs.edit().putString("pw", "").commit();
				Toast.makeText(mContext, R.string.toast_password_clear, Toast.LENGTH_SHORT).show();
				return false;
			}
		});

		// ADW: restart and reset preferences
		Preference restart = findPreference("launcherRestart");
		restart.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				mAlertDialog = new AlertDialog.Builder(mContext)
				.setMessage(R.string.pref_summary_launcher_restart)
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,
							int which) {
						shouldRestart = true;
						finish();
					}
				})
				.setNegativeButton(android.R.string.cancel, null).create();
				mAlertDialog.show();
				return false;
			}
		});

		Preference launcherManage = findPreference("launcherManage");
		launcherManage.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				try {
					String appPackage = getApplicationContext().getPackageName();
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
					mContext.startActivity(intent);
				} catch (Exception e) {
					// failed to start app info
				}
				return false;
			}
		});


		if (IsDebugVersion) {
			// Debugging options
			addPreferencesFromResource(R.xml.debugging_settings);
		}

		// Guide screen
		Preference pref = findPreference("userGuide");
		pref
		.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				startActivity(new Intent(mContext, GuideActivity.class));
				return false;
			}
		});

		// Changelog screen
		pref = findPreference("app_version");
		pref
		.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				try {
					AlertDialog builder = MyLauncherSettingsHelper.ChangelogDialogBuilder
							.create(mContext, false);
					builder.show();
				} catch (Exception e) {
					e.printStackTrace();
				}
				return false;
			}
		});
		// End restart/reset
		pref = findPreference("xml_export");
		pref
		.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				mAlertDialog = new AlertDialog.Builder(mContext)
				.setTitle(R.string.title_dialog_xml)
				.setMessage(R.string.message_dialog_export)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,
							int which) {
						new ExportPrefsTask().execute();
					}
				})
				.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,
							int which) {

					}
				})
				.create();
				mAlertDialog.show();
				return true;
			}
		});

		pref = findPreference("xml_import");
		pref
		.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				mAlertDialog = new AlertDialog.Builder(
						mContext)
				.setTitle(getResources().getString(R.string.title_dialog_xml))
				.setMessage(R.string.message_dialog_import)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,
							int which) {
						new ImportPrefsTask().execute();
					}
				})
				.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,
							int which) {

					}
				})
				.create();
				mAlertDialog.show();
				return true;
			}
		});

		pref = findPreference("db_export");
		pref
		.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				mAlertDialog = new AlertDialog.Builder(mContext)
				.setTitle(R.string.title_dialog_xml)
				.setMessage(R.string.message_dialog_export_config)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,
							int which) {
						new ExportDatabaseTask().execute();
					}
				})
				.setNegativeButton(android.R.string.cancel,new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,
							int which) {

					}
				})
				.create();
				mAlertDialog.show();
				return true;
			}
		});

		pref = findPreference("db_import");
		pref
		.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				mAlertDialog = new AlertDialog.Builder(mContext)
				.setTitle(R.string.title_dialog_xml)
				.setMessage(R.string.message_dialog_import_config)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,
							int which) {
						new ImportDatabaseTask().execute();
					}
				})
				.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,
							int which) {

					}
				})
				.create();
				mAlertDialog.show();
				return true;
			}
		});
		// TODO: ADW, theme settings
		SharedPreferences sp = getPreferenceManager().getSharedPreferences();
		final String themePackage = sp.getString("themePackageName",
				Launcher.THEME_DEFAULT);
		ListPreference themeListPref = (ListPreference) findPreference("themePackageName");
		themeListPref.setOnPreferenceChangeListener(this);
		Intent intent = new Intent("org.adw.launcher.THEMES");
		intent.addCategory("android.intent.category.DEFAULT");
		PackageManager pm = getPackageManager();
		List<ResolveInfo> themes = pm.queryIntentActivities(intent, 0);
		String[] entries = new String[themes.size() + 1];
		String[] values = new String[themes.size() + 1];
		entries[0] = Launcher.THEME_DEFAULT;
		values[0] = Launcher.THEME_DEFAULT;
		for (int i = 0; i < themes.size(); i++) {
			String appPackageName = (themes.get(i)).activityInfo.packageName
					.toString();
			String themeName = (themes.get(i)).loadLabel(pm).toString();
			entries[i + 1] = themeName;
			values[i + 1] = appPackageName;
		}
		themeListPref.setEntries(entries);
		themeListPref.setEntryValues(values);
		PreviewPreference themePreview = (PreviewPreference) findPreference("themePreview");
		themePreview.setTheme(themePackage);
	}

	private ProgressDialog mProgressDialog;
	@Override
	protected void onStop() {
		super.onStop();
		// prevent memory leak and dismiss any progress dialogs
		if (mProgressDialog != null) {
			if (mProgressDialog.isShowing()) {
				mProgressDialog.dismiss();
			}
		}
	}

	public void applyTheme(View v) {
		// we need to load the theme here
		if (mProgressDialog == null || !mProgressDialog.isShowing()) {
			mProgressDialog = ProgressDialog.show(this, null, getString(R.string.dialog_please_wait), true, false);
		}

		PreviewPreference themePreview = (PreviewPreference) findPreference("themePreview");
		final String packageName = themePreview.getValue().toString();
		// this time we really save the themepackagename
		final SharedPreferences sp = getPreferenceManager().getSharedPreferences();
		final SharedPreferences.Editor editor = sp.edit();
		editor.putString("themePackageName", packageName);	// save this last so we alert the onshared preferences to toggle restart right away, or else drawer_color is the first to be committed since it is an integer
		editor.commit();

		new Thread(new Runnable() {
			@Override
			public void run() {
				// and update the preferences from the theme
				// TODO:ADW maybe this should be optional for the user
				if (!packageName.equals(Launcher.THEME_DEFAULT)) {
					Resources themeResources = null;
					try {
						themeResources = getPackageManager()
								.getResourcesForApplication(packageName.toString());
					} catch (NameNotFoundException e) {
						// e.printStackTrace();
					}

					if (themeResources != null) {
						int config_ui_ab_hide_bgId = themeResources.getIdentifier(
								"config_ui_ab_hide_bg", "bool", packageName.toString());
						if (config_ui_ab_hide_bgId != 0) {
							boolean config_ui_ab_hide_bg = themeResources
									.getBoolean(config_ui_ab_hide_bgId);
							editor.putBoolean("uiABBg", config_ui_ab_hide_bg);
						}
						int config_new_selectorsId = themeResources.getIdentifier(
								"config_new_selectors", "bool", packageName.toString());
						if (config_new_selectorsId != 0) {
							boolean config_new_selectors = themeResources
									.getBoolean(config_new_selectorsId);
							editor.putBoolean("uiNewSelectors", config_new_selectors);
						}
						int config_drawer_labelsId = themeResources.getIdentifier(
								"config_drawer_labels", "bool", packageName.toString());
						if (config_drawer_labelsId != 0) {
							boolean config_drawer_labels = themeResources
									.getBoolean(config_drawer_labelsId);
							editor.putBoolean("drawerLabels", config_drawer_labels);
						}
						int config_fade_drawer_labelsId = themeResources.getIdentifier(
								"config_fade_drawer_labels", "bool",
								packageName.toString());
						if (config_fade_drawer_labelsId != 0) {
							boolean config_fade_drawer_labels = themeResources
									.getBoolean(config_fade_drawer_labelsId);
							editor.putBoolean("fadeDrawerLabels",
									config_fade_drawer_labels);
						}
						int config_desktop_indicatorId = themeResources.getIdentifier(
								"config_desktop_indicator", "bool",
								packageName.toString());
						if (config_desktop_indicatorId != 0) {
							boolean config_desktop_indicator = themeResources
									.getBoolean(config_desktop_indicatorId);
							editor.putBoolean("uiDesktopIndicator",
									config_desktop_indicator);
						}
						int config_highlights_colorId = themeResources.getIdentifier(
								"config_highlights_color", "integer",
								packageName.toString());
						if (config_highlights_colorId != 0) {
							int config_highlights_color = themeResources
									.getInteger(config_highlights_colorId);
							editor.putInt("highlights_color", config_highlights_color);
						}
						int config_highlights_color_focusId = themeResources
								.getIdentifier("config_highlights_color_focus",
										"integer", packageName.toString());
						if (config_highlights_color_focusId != 0) {
							int config_highlights_color_focus = themeResources
									.getInteger(config_highlights_color_focusId);
							editor.putInt("highlights_color_focus",
									config_highlights_color_focus);
						}
						int config_drawer_colorId = themeResources.getIdentifier(
								"config_drawer_color", "integer",
								packageName.toString());
						if (config_drawer_colorId != 0) {
							int config_drawer_color = themeResources
									.getInteger(config_drawer_colorId);
							editor.putInt("drawer_color", config_drawer_color);
						}
						int config_desktop_indicator_typeId = themeResources
								.getIdentifier("config_desktop_indicator_type",
										"string", packageName.toString());
						if (config_desktop_indicator_typeId != 0) {
							String config_desktop_indicator_type = themeResources
									.getString(config_desktop_indicator_typeId);
							editor.putString("uiDesktopIndicatorType",
									config_desktop_indicator_type);
						}
						int config_ab_scale_factorId = themeResources.getIdentifier(
								"config_ab_scale_factor", "integer",
								packageName.toString());
						if (config_ab_scale_factorId != 0) {
							int config_ab_scale_factor = themeResources
									.getInteger(config_ab_scale_factorId);
							editor.putInt("uiScaleAB", config_ab_scale_factor);
						}
						int dock_styleId = themeResources.getIdentifier(
								"main_dock_style", "string", packageName.toString());
						if (dock_styleId != 0) {
							String dock_style = themeResources.getString(dock_styleId);
							editor.putString("main_dock_style", dock_style);
							if (Integer.valueOf(dock_style) == Launcher.DOCK_STYLE_5
									|| Integer.valueOf(dock_style) == Launcher.DOCK_STYLE_NONE)
								editor.putBoolean("uiDots", false);
						}
						// TODO:ADW We set the theme wallpaper. We should add this as
						// optional...
						int wallpaperId = themeResources.getIdentifier(
								"theme_wallpaper", "drawable", packageName.toString());
						if (wallpaperId != 0) {
							Options mOptions = new BitmapFactory.Options();
							mOptions.inDither = false;
							mOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
							Bitmap wallpaper = null;
							try {
								wallpaper = BitmapFactory.decodeResource(
										themeResources, wallpaperId, mOptions);
							} catch (OutOfMemoryError e) {
							}
							if (wallpaper != null) {
								try {
									final WallpaperManager wpm = (WallpaperManager) getSystemService(WALLPAPER_SERVICE);
									// wpm.setResource(mImages.get(position));
									wpm.setBitmap(wallpaper);
									wallpaper.recycle();
								} catch (Exception e) {
								}
							}
						}
					}
				}
				editor.commit();
				finish();
			}
		}).start();
	}

	@Override
	protected void onPause() {
		if (shouldRestart) {
			// this just force closes all the time, because Android 2.1 doesn't even support killBackgroundProcesses
			/*
			if (Build.VERSION.SDK_INT <= 7) {
				Intent intent = new Intent(getApplicationContext(),
						Launcher.class);
				PendingIntent sender = PendingIntent.getBroadcast(
						getApplicationContext(), 0, intent, 0);

				// We want the alarm to go off 30 seconds from now.
				Calendar calendar = Calendar.getInstance();
				calendar.setTimeInMillis(System.currentTimeMillis());
				calendar.add(Calendar.SECOND, 1);

				// Schedule the alarm!
				AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
				am.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
						sender);
				ActivityManager acm = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
				acm.killBackgroundProcesses("com.wordpress.chislonchow.legacylauncher");
			} else 
			 */
			{
				android.os.Process.killProcess(android.os.Process.myPid());
			}
		}
		super.onPause();
	}

	public boolean onPreferenceChange(Preference preference, Object newValue) {		
		final String key = preference.getKey();
		if (key.equals("themePackageName")) {
			PreviewPreference themePreview = (PreviewPreference) findPreference("themePreview");
			themePreview.setTheme(newValue.toString());
			return false;
		} else if (key.equals("swipedownActions")) {
			// lets launch app picker if the user selected to launch an app on
			// gesture
			CharSequence[] entries = ((ListPreference)preference).getEntries();
			preference.setSummary(entries[((ListPreference)preference).findIndexOfValue(newValue.toString())]);
			if (newValue.equals(String.valueOf(Launcher.BIND_APP_LAUNCHER))) {
				Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
				mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

				Intent pickIntent = new Intent(Intent.ACTION_PICK_ACTIVITY);
				pickIntent.putExtra(Intent.EXTRA_INTENT, mainIntent);
				startActivityForResult(pickIntent,
						REQUEST_SWIPE_DOWN_APP_CHOOSER);
			}
		} else if (key.equals("homeBinding")) {
			CharSequence[] entries = ((ListPreference)preference).getEntries();
			preference.setSummary(entries[((ListPreference)preference).findIndexOfValue(newValue.toString())]);
			// lets launch app picker if the user selected to launch an app on
			// gesture
			if (newValue.equals(String.valueOf(Launcher.BIND_APP_LAUNCHER))) {
				Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
				mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

				Intent pickIntent = new Intent(Intent.ACTION_PICK_ACTIVITY);
				pickIntent.putExtra(Intent.EXTRA_INTENT, mainIntent);
				startActivityForResult(pickIntent,
						REQUEST_HOME_BINDING_APP_CHOOSER);
			}
		} else if (key.equals("swipeupActions")) {
			CharSequence[] entries = ((ListPreference)preference).getEntries();
			preference.setSummary(entries[((ListPreference)preference).findIndexOfValue(newValue.toString())]);
			// lets launch app picker if the user selected to launch an app on
			// gesture
			if (newValue.equals(String.valueOf(Launcher.BIND_APP_LAUNCHER))) {
				Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
				mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

				Intent pickIntent = new Intent(Intent.ACTION_PICK_ACTIVITY);
				pickIntent.putExtra(Intent.EXTRA_INTENT, mainIntent);
				startActivityForResult(pickIntent, REQUEST_SWIPE_UP_APP_CHOOSER);
			}
		} else if (key.equals("main_dock_style")) {
			CheckBoxPreference dots = (CheckBoxPreference) findPreference("uiDots");
			CheckBoxPreference lockMAB = (CheckBoxPreference) findPreference("mainDockLockMAB");
			CheckBoxPreference hide = (CheckBoxPreference)findPreference("mainDockDrawerHide");

			int val = Integer.valueOf(newValue.toString());

			switch (val) {
			case Launcher.DOCK_STYLE_NONE:
				dots.setChecked(false);
				dots.setEnabled(false);

				lockMAB.setEnabled(false);

				hide.setEnabled(false);
				break;
			case Launcher.DOCK_STYLE_5:
				dots.setChecked(false);
				dots.setEnabled(false);

				lockMAB.setEnabled(true);

				hide.setEnabled(true);
				break;
			default:	//styles 1, 3
				dots.setEnabled(true);

				lockMAB.setEnabled(true);

				hide.setEnabled(true);
				break;
			}

			CharSequence[] entries = ((ListPreference)preference).getEntries();
			preference.setSummary(entries[((ListPreference)preference).findIndexOfValue(newValue.toString())]);
		} else if (key.equals("drawerStyle")) {
			boolean drawerHorizontal = (Integer.valueOf(newValue.toString()) == 1);
			findPreference("drawerRowsPortrait").setEnabled(drawerHorizontal);
			findPreference("drawerRowsLandscape").setEnabled(drawerHorizontal);
			findPreference("pageHorizontalMargin").setEnabled(drawerHorizontal);
			findPreference("drawerSpeed").setEnabled(drawerHorizontal);
			findPreference("drawerSnap").setEnabled(drawerHorizontal);
			findPreference("drawerOvershoot").setEnabled(drawerHorizontal);

			CharSequence[] entries = ((ListPreference)preference).getEntries();
			preference.setSummary(entries[((ListPreference)preference).findIndexOfValue(newValue.toString())]);
		} else if (key.equals("uiDesktopIndicatorType")) {
			// enable/disable slider color customization based on indicator type
			int val = Integer.valueOf(newValue.toString());
			if (val > 1) {
				(findPreference("uiDesktopIndicatorColorAllow")).setEnabled(true);
				(findPreference("uiDesktopIndicatorColor")).setEnabled(true);
			} else {
				(findPreference("uiDesktopIndicatorColorAllow")).setEnabled(false);
				(findPreference("uiDesktopIndicatorColor")).setEnabled(false);
			}
			CharSequence[] entries = ((ListPreference)preference).getEntries();
			preference.setSummary(entries[((ListPreference)preference).findIndexOfValue(newValue.toString())]);
		} else if (key.equals("deleteZoneLocation") || 
				key.equals("desktopTransitionStyle") || 
				key.equals("homeOrientation") ||
				key.equals("mainDockDrawerHide")) {

			CharSequence[] entries = ((ListPreference)preference).getEntries();
			preference.setSummary(entries[((ListPreference)preference).findIndexOfValue(newValue.toString())]);
		}
		return true;
	}

	// wjax: Get the App chosen as to be launched upon gesture completion. And
	// store it in SharedPreferences via AlmostNexusSettingsHelper!!!
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			switch (requestCode) {
			case REQUEST_SWIPE_DOWN_APP_CHOOSER:
				MyLauncherSettingsHelper.setSwipeDownAppToLaunch(this,
						infoFromApplicationIntent(this, data));
				break;
			case REQUEST_HOME_BINDING_APP_CHOOSER:
				MyLauncherSettingsHelper.setHomeBindingAppToLaunch(this,
						infoFromApplicationIntent(this, data));
				break;
			case REQUEST_SWIPE_UP_APP_CHOOSER:
				MyLauncherSettingsHelper.setSwipeUpAppToLaunch(this,
						infoFromApplicationIntent(this, data));
				break;
			}
		}

	}

	// Extracts useful information from Intent containing app information
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
		}

		if (activityInfo != null) {
			ApplicationInfo itemInfo = new ApplicationInfo();
			itemInfo.title = activityInfo.loadLabel(packageManager);
			if (itemInfo.title == null) {
				itemInfo.title = activityInfo.name;
			}

			itemInfo.setActivity(component, Intent.FLAG_ACTIVITY_NEW_TASK
					| Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
			itemInfo.icon = activityInfo.loadIcon(packageManager);
			itemInfo.container = ItemInfo.NO_ID;

			return itemInfo;
		}
		return null;
	}

	// Wysie: Adapted from
	// http://code.google.com/p/and-examples/source/browse/#svn/trunk/database/src/com/totsp/database
	private class ExportPrefsTask extends AsyncTask<Void, Void, String> {

		// can use UI thread here
		@Override
		protected void onPreExecute() {
			mProgressDialog = new ProgressDialog(mContext);
			mProgressDialog.setMessage(getResources().getString(
					R.string.xml_export_dialog));
			mProgressDialog.show();
		}

		// automatically done on worker thread (separate from UI thread)
		@Override
		protected String doInBackground(final Void... args) {
			if (!Environment.MEDIA_MOUNTED.equals(Environment
					.getExternalStorageState())) {
				return getResources().getString(
						R.string.import_export_sdcard_unmounted);
			}

			File prefFile = new File(Environment.getDataDirectory() + "/data/"
					+ NAMESPACE
					+ "/shared_prefs/launcher.preferences.almostnexus.xml");
			File file = new File(Environment.getExternalStorageDirectory(),
					PREF_BACKUP_FILENAME);

			try {
				file.createNewFile();
				copyFile(prefFile, file);
				return getResources().getString(R.string.xml_export_success);
			} catch (IOException e) {
				return getResources().getString(R.string.xml_export_error);
			} 
		}

		// can use UI thread here
		@Override
		protected void onPostExecute(final String msg) {
			if (mProgressDialog.isShowing()) {
				mProgressDialog.dismiss();
			}
			Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
		}
	}

	// Wysie: Adapted from
	// http://code.google.com/p/and-examples/source/browse/#svn/trunk/database/src/com/totsp/database
	private class ImportPrefsTask extends AsyncTask<Void, Void, String> {

		@Override
		protected void onPreExecute() {
			mProgressDialog = new ProgressDialog(mContext);
			mProgressDialog.setMessage(getResources().getString(
					R.string.xml_import_dialog));
			mProgressDialog.show();
		}

		// could pass the params used here in AsyncTask<String, Void, String> -
		// but not being re-used
		@Override
		protected String doInBackground(final Void... args) {
			if (!Environment.MEDIA_MOUNTED.equals(Environment
					.getExternalStorageState())) {
				return getResources().getString(
						R.string.import_export_sdcard_unmounted);
			}

			File prefBackupFile = new File(
					Environment.getExternalStorageDirectory(),
					PREF_BACKUP_FILENAME);

			if (!prefBackupFile.exists()) {
				return getResources().getString(R.string.xml_file_not_found);
			} else if (!prefBackupFile.canRead()) {
				return getResources().getString(R.string.xml_not_readable);
			}

			File prefFile = new File(Environment.getDataDirectory() + "/data/"
					+ NAMESPACE
					+ "/shared_prefs/launcher.preferences.almostnexus.xml");

			if (prefFile.exists()) {
				prefFile.delete();
			}

			try {
				prefFile.createNewFile();
				copyFile(prefBackupFile, prefFile);
				shouldRestart = true;
				return getResources().getString(R.string.xml_import_success);
			} catch (IOException e) {
				return getResources().getString(R.string.xml_import_error);
			}
		}

		@Override
		protected void onPostExecute(final String msg) {
			if (mProgressDialog.isShowing()) {
				mProgressDialog.dismiss();
			}

			Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
		}
	}

	// Wysie: Adapted from
	// http://code.google.com/p/and-examples/source/browse/#svn/trunk/database/src/com/totsp/database
	private class ExportDatabaseTask extends AsyncTask<Void, Void, String> {

		// can use UI thread here
		@Override
		protected void onPreExecute() {
			mProgressDialog = new ProgressDialog(mContext);
			mProgressDialog.setMessage(getResources().getString(
					R.string.dbfile_export_dialog));
			mProgressDialog.show();
		}

		// automatically done on worker thread (separate from UI thread)
		@Override
		protected String doInBackground(final Void... args) {
			if (!Environment.MEDIA_MOUNTED.equals(Environment
					.getExternalStorageState())) {
				return getResources().getString(
						R.string.import_export_sdcard_unmounted);
			}

			File dbFile = new File(Environment.getDataDirectory()
					+ LAUNCHER_DB_BASE);
			File dbFile_shm = new File(Environment.getDataDirectory()
					+ LAUNCHER_DB_BASE + "-shm");
			File dbFile_wal = new File(Environment.getDataDirectory()
					+ LAUNCHER_DB_BASE + "-wal");
			File file = new File(Environment.getExternalStorageDirectory(),
					CONFIG_BACKUP_FILENAME);
			File file_shm = new File(Environment.getExternalStorageDirectory(),
					CONFIG_BACKUP_FILENAME + "-shm");
			File file_wal = new File(Environment.getExternalStorageDirectory(),
					CONFIG_BACKUP_FILENAME + "-wal");

			//workaround for SQLITE 3.7
			if (dbFile_shm.exists() && dbFile_wal.exists()) {
				try {
					copyFile(dbFile_shm, file_shm);
					copyFile(dbFile_wal, file_wal);
				} catch (IOException e) {
				} catch (NullPointerException e) {
				}
			}
			try {
				file.createNewFile();
				copyFile(dbFile, file);
				exportCategories();
				return getResources().getString(R.string.dbfile_export_success);
			} catch (IOException e) {
				return getResources().getString(R.string.dbfile_export_error);
			} catch (NullPointerException e) {
				return getResources().getString(R.string.dbfile_export_error);
			}
		}

		private void exportCategories() throws IOException, NullPointerException {
			File prefFolder = new File(Environment.getDataDirectory()
					+ "/data/" + NAMESPACE + "/shared_prefs");
			String[] list = prefFolder.list();
			for (String fileName : list) {
				if (fileName.startsWith("APP_CATALOG_")) {
					File prefFile = new File(Environment.getDataDirectory()
							+ "/data/" + NAMESPACE + "/shared_prefs/"
							+ fileName);
					File exportedFile = new File(
							Environment.getExternalStorageDirectory(), fileName);
					copyFile(prefFile, exportedFile);
				}
			}
		}

		// can use UI thread here
		@Override
		protected void onPostExecute(final String msg) {
			if (mProgressDialog.isShowing()) {
				mProgressDialog.dismiss();
			}
			Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
		}
	}

	// Wysie: Adapted from
	// http://code.google.com/p/and-examples/source/browse/#svn/trunk/database/src/com/totsp/database
	private class ImportDatabaseTask extends AsyncTask<Void, Void, String> {
		private final ProgressDialog dialog = new ProgressDialog(mContext);

		@Override
		protected void onPreExecute() {
			this.dialog.setMessage(getResources().getString(
					R.string.dbfile_import_dialog));
			this.dialog.show();
		}

		// could pass the params used here in AsyncTask<String, Void, String> -
		// but not being re-used
		@Override
		protected String doInBackground(final Void... args) {
			if (!Environment.MEDIA_MOUNTED.equals(Environment
					.getExternalStorageState())) {
				return getResources().getString(
						R.string.import_export_sdcard_unmounted);
			}

			File dbBackupFile = new File(
					Environment.getExternalStorageDirectory(),
					CONFIG_BACKUP_FILENAME);

			if (!dbBackupFile.exists()) {
				return getResources().getString(R.string.dbfile_not_found);
			} else if (!dbBackupFile.canRead()) {
				return getResources().getString(R.string.dbfile_not_readable);
			}

			// If you don't also delete the '-shm' and '-wal' files the copy of
			// the '.db' file
			// doesn't work. The home screens will not be changed.
			File dbFile = new File(Environment.getDataDirectory()
					+ LAUNCHER_DB_BASE);
			File dbFile_shm = new File(Environment.getDataDirectory()
					+ LAUNCHER_DB_BASE + "-shm");
			File dbFile_wal = new File(Environment.getDataDirectory()
					+ LAUNCHER_DB_BASE + "-wal");
			File file = new File(Environment.getExternalStorageDirectory(),
					CONFIG_BACKUP_FILENAME);
			File file_shm = new File(Environment.getExternalStorageDirectory(),
					CONFIG_BACKUP_FILENAME + "-shm");
			File file_wal = new File(Environment.getExternalStorageDirectory(),
					CONFIG_BACKUP_FILENAME + "-wal");

			if (dbFile.exists()) {
				dbFile.delete();
			}
			if (dbFile_shm.exists()) {
				dbFile_shm.delete();
			}
			if (dbFile_wal.exists()) {
				dbFile_wal.delete();
			}

			//workaround for SQLITE 3.7
			if (file_shm.exists() && file_wal.exists()) {
				try {
					dbFile_shm.createNewFile();
					dbFile_wal.createNewFile();
					copyFile(file_shm, dbFile_shm);
					copyFile(file_wal, dbFile_wal);
				} catch (IOException e) {
				} catch (NullPointerException e) {
				}
			}

			try {
				dbFile.createNewFile();
				copyFile(dbBackupFile, dbFile);
				importCategories();
				shouldRestart = true;
				return getResources().getString(R.string.dbfile_import_success);
			} catch (IOException e) {
				return getResources().getString(R.string.dbfile_import_error);
			} catch (NullPointerException e) {
				return getResources().getString(R.string.dbfile_import_error);
			}
		}

		private void importCategories() throws IOException, NullPointerException {
			File prefFolder = Environment.getExternalStorageDirectory();
			String[] list = prefFolder.list();
			for (String fileName : list) {
				if (fileName.startsWith("APP_CATALOG_")) {
					File importFile = new File(
							Environment.getExternalStorageDirectory(), fileName);
					File prefFile = new File(Environment.getDataDirectory()
							+ "/data/" + NAMESPACE + "/shared_prefs/"
							+ fileName);

					if (!importFile.canRead()) {
						throw new IOException();
					}

					if (prefFile.exists()) {
						prefFile.delete();
					}
					prefFile.createNewFile();
					copyFile(importFile, prefFile);
				}
			}
		}

		@Override
		protected void onPostExecute(final String msg) {
			if (this.dialog.isShowing()) {
				this.dialog.dismiss();
			}

			Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
		}
	}

	public static void copyFile(File src, File dst) throws IOException {
		FileChannel inChannel = new FileInputStream(src).getChannel();
		FileChannel outChannel = new FileOutputStream(dst).getChannel();

		try {
			inChannel.transferTo(0, inChannel.size(), outChannel);
		} finally {

			if (inChannel != null)
				inChannel.close();
			if (outChannel != null)
				outChannel.close();
		}
	}

	public void getThemes(View v) {
		// TODO:warn theme devs to use "ADWTheme" as keyword.
		Uri marketUri = Uri.parse("market://search?q=ADWTheme");
		Intent marketIntent = new Intent(Intent.ACTION_VIEW).setData(marketUri);
		try {
			startActivity(marketIntent);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(this, R.string.activity_not_found,
					Toast.LENGTH_SHORT).show();
		} catch (SecurityException e) {
			Toast.makeText(this, R.string.activity_security_exception,
					Toast.LENGTH_SHORT).show();
			Log.e("ADW",
					"Launcher does not have the permission to launch "
							+ marketIntent
							+ ". Make sure to create a MAIN intent-filter for the corresponding activity "
							+ "or use the exported attribute for this activity.",
							e);
		}
		finish();
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
}
