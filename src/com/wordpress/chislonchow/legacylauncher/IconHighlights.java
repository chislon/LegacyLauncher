package com.wordpress.chislonchow.legacylauncher;

import com.wordpress.chislonchow.legacylauncher.R;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;

public class IconHighlights {
	public static final int TYPE_DESKTOP=1;
	public static final int TYPE_DOCKBAR=2;
	public static final int TYPE_DRAWER=3;
	public IconHighlights(Context context) {
		// TODO Auto-generated constructor stub
	}
	private static Drawable newSelector(Context context){
		GradientDrawable mDrawPressed;
		GradientDrawable mDrawSelected;
		StateListDrawable drawable=new StateListDrawable();
		int selectedColor=MyLauncherSettingsHelper.getHighlightsColorFocus(context);
		int pressedColor=MyLauncherSettingsHelper.getHighlightsColor(context);
		int stateFocused = android.R.attr.state_focused;
		int statePressed = android.R.attr.state_pressed;
		int stateWindowFocused = android.R.attr.state_window_focused;

		mDrawSelected = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
				new int[] { 0x77FFFFFF, selectedColor,selectedColor,selectedColor,selectedColor, 0x77000000 });
		mDrawSelected.setShape(GradientDrawable.RECTANGLE);
		mDrawSelected.setGradientRadius((float)(Math.sqrt(2) * 60));
		mDrawSelected.setCornerRadius(8);
		mDrawPressed = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
				new int[] { 0x77FFFFFF, pressedColor,pressedColor,pressedColor,pressedColor, 0x77000000 });
		mDrawPressed.setShape(GradientDrawable.RECTANGLE);
		mDrawPressed.setGradientRadius((float)(Math.sqrt(2) * 60));
		mDrawPressed.setCornerRadius(8);

		drawable.addState(new int[]{ statePressed}, mDrawPressed);
		drawable.addState(new int[]{ stateFocused, stateWindowFocused}, mDrawSelected);
		drawable.addState(new int[]{stateFocused, -stateWindowFocused}, null);
		drawable.addState(new int[]{-stateFocused, stateWindowFocused}, null);
		drawable.addState(new int[]{-stateFocused, -stateWindowFocused}, null);
		return drawable;
	}
	private static Drawable oldSelector(Context context, int type){
		//int selectedColor=MyLauncherSettingsHelper.getHighlightsColorFocus(context);
		int pressedColor=MyLauncherSettingsHelper.getHighlightsColor(context);
		
		//ADW: Load the specified theme
		String themePackage=MyLauncherSettingsHelper.getThemePackageName(context, Launcher.THEME_DEFAULT);
		Resources themeResources=null;
		if(!themePackage.equals(Launcher.THEME_DEFAULT)){
			PackageManager pm=context.getPackageManager();
			try {
				themeResources=pm.getResourcesForApplication(themePackage);
			} catch (NameNotFoundException e) {
				// TODO Auto-generated catch block
				themeResources=context.getResources();
			}
		}else{
			themeResources=context.getResources();
		}
		Drawable drawable=null;
		//use_drawer_icons_bg
		if(themeResources!=null){
			boolean use_drawer_icons_bgs=false;
			if(type==TYPE_DRAWER){
				int use_drawer_icons_bgs_id=themeResources.getIdentifier("use_drawer_icons_bg", "bool", themePackage);
				if(use_drawer_icons_bgs_id!=0){
					use_drawer_icons_bgs=themeResources.getBoolean(use_drawer_icons_bgs_id);
				}
				if(use_drawer_icons_bgs){
					int resource_id=themeResources.getIdentifier("normal_application_background", "drawable", themePackage);
					if(resource_id!=0){
						drawable=themeResources.getDrawable(resource_id);
					}					
				}
			}else{
				int resource_id=themeResources.getIdentifier("shortcut_selector", "drawable", themePackage);
				if(resource_id!=0){
					drawable=themeResources.getDrawable(resource_id);
				}else{
					drawable=themeResources.getDrawable(R.drawable.shortcut_selector);
				}
				drawable.setColorFilter(pressedColor, Mode.SRC_ATOP);
			}
		}
		return drawable;
	}
	public static Drawable getDrawable(Context context, int type){
		if(MyLauncherSettingsHelper.getUINewSelectors(context)){
			return newSelector(context);
		}else{
			return oldSelector(context, type);
		}
	}
}
