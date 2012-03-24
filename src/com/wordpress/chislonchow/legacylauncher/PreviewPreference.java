package com.wordpress.chislonchow.legacylauncher;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.preference.Preference;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class PreviewPreference extends Preference {
	private CharSequence themeName;
	private CharSequence themePackageName;
	private CharSequence themeDescription;
	private Drawable themePreview;
	public PreviewPreference(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	public PreviewPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	public PreviewPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void onBindView(View view) {
		// TODO Auto-generated method stub
		super.onBindView(view);
		if(view != null && themePackageName != null && themePackageName.toString().length() > 0){
			TextView text;
			if (themeDescription != null) {
				text = (TextView) view.findViewById(R.id.ThemeDescription);
				text.setMovementMethod(LinkMovementMethod.getInstance());
				text.setText(Html.fromHtml(themeDescription.toString()));
			}
			if (themeName != null) {
				text = (TextView) view.findViewById(R.id.ThemeTitle);
				text.setText(themeName);
			}
			ImageView image;
			if(themePreview != null) {
				image = (ImageView) view.findViewById(R.id.ThemeIcon);
				image.setImageDrawable(themePreview);
			} else {
				image = (ImageView) view.findViewById(R.id.ThemeIcon);
				image.setImageResource(R.drawable.ic_launcher_wallpaper);
			}

			Button applyButton= (Button) view.findViewById(R.id.ThemeApply);
			applyButton.setEnabled(true);
		} else {
			Button applyButton= (Button) view.findViewById(R.id.ThemeApply);
			applyButton.setEnabled(false);
		}
	}

	private class FetchThemeResourceTask extends AsyncTask<CharSequence, Void, Void> {
		@Override
		protected Void doInBackground(final CharSequence... args) {
			Resources themeResources = null;
			if (args.length > 0) {
				try {
					themeResources = getContext().getPackageManager().getResourcesForApplication(args[0].toString());
				} catch (NameNotFoundException e) {
					//e.printStackTrace();
				}
				if(themeResources != null) {
					int themeNameId=themeResources.getIdentifier("theme_title", "string", args[0].toString());
					if(themeNameId!=0){
						themeName=themeResources.getString(themeNameId);
					}
					int themeDescriptionId=themeResources.getIdentifier("theme_description", "string", args[0].toString());
					if(themeDescriptionId!=0){
						themeDescription=themeResources.getString(themeDescriptionId);
					}
					int themePreviewId=themeResources.getIdentifier("theme_preview", "drawable", args[0].toString());
					if(themePreviewId!=0){
						themePreview=themeResources.getDrawable(themePreviewId);
					}
				}
				if(themeName==null)themeName=getContext().getResources().getString(R.string.pref_title_theme_preview);
				if(themeDescription==null)themeDescription=getContext().getResources().getString(R.string.pref_summary_theme_preview);
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			notifyChanged();
		}
	}

	public void setTheme(final CharSequence packageName) {
		themePackageName=packageName;
		themeName=null;
		themeDescription=null;
		if(themePreview!=null)themePreview.setCallback(null);
		themePreview=null;
		if(!packageName.equals(Launcher.THEME_DEFAULT)) {
			FetchThemeResourceTask themeTask = new FetchThemeResourceTask();
			themeTask.execute(packageName);
		} else {
			themeName=getContext().getResources().getString(R.string.pref_title_theme_preview);
			themeDescription=getContext().getResources().getString(R.string.pref_summary_theme_preview);
			notifyChanged();
		}
	}

	public CharSequence getValue(){
		return themePackageName;
	}
	public CharSequence getThemeName(){
		if(themeName!=null) {
			return themeName;
		}
		return themeName=getContext().getResources().getString(R.string.pref_title_theme_preview);
	}
}
