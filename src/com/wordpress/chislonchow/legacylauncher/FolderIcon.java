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

import com.wordpress.chislonchow.legacylauncher.R;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;

/**
 * An icon that can appear on in the workspace representing an {@link UserFolder}.
 */
public class FolderIcon extends BubbleTextView implements DropTarget {
    private UserFolderInfo mInfo;
    private Launcher mLauncher;
    private Drawable mCloseIcon;
    private Drawable mOpenIcon;

    public FolderIcon(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FolderIcon(Context context) {
        super(context);
    }

    static FolderIcon fromXml(int resId, Launcher launcher, ViewGroup group,
            UserFolderInfo folderInfo) {

        FolderIcon icon = (FolderIcon) LayoutInflater.from(launcher).inflate(resId, group, false);
        //TODO:ADW Load icon from theme/iconpack
        Drawable dclosed = folderInfo.icon;
        Drawable dopen = folderInfo.icon;
        if ( dopen == null )
        {
            final Resources resources = launcher.getResources();
            String themePackage=AlmostNexusSettingsHelper.getThemePackageName(launcher, Launcher.THEME_DEFAULT);
            if(themePackage.equals(Launcher.THEME_DEFAULT)){
            	dclosed = resources.getDrawable(R.drawable.ic_launcher_folder);
            	dopen = resources.getDrawable(R.drawable.ic_launcher_folder_open);
            }else{
            	Drawable tmpIcon1 = loadFolderFromTheme(launcher, launcher.getPackageManager(), themePackage,"ic_launcher_folder");
            	if(tmpIcon1==null){
            		dclosed = resources.getDrawable(R.drawable.ic_launcher_folder);
            	}else{
            		dclosed = tmpIcon1;
            	}
            	Drawable tmpIcon2 = loadFolderFromTheme(launcher, launcher.getPackageManager(), themePackage,"ic_launcher_folder_open");
            	if(tmpIcon2==null){
            		dopen = resources.getDrawable(R.drawable.ic_launcher_folder_open);
            	}else{
            		dopen = tmpIcon2;
            	}
            }
        }
        icon.mCloseIcon=Utilities.createIconThumbnail(dclosed, launcher);
        icon.mOpenIcon=dopen;
        /*final Resources resources = launcher.getResources();
        Drawable d = resources.getDrawable(R.drawable.ic_launcher_folder);
        d = Utilities.createIconThumbnail(d, launcher);
        icon.mCloseIcon = d;
        icon.mOpenIcon = resources.getDrawable(R.drawable.ic_launcher_folder_open);*/
        icon.setCompoundDrawablesWithIntrinsicBounds(null, dclosed, null, null);
        if(!AlmostNexusSettingsHelper.getUIHideLabels(launcher))icon.setText(folderInfo.title);
        icon.setTag(folderInfo);
        icon.setOnClickListener(launcher);
        icon.mInfo = folderInfo;
        icon.mLauncher = launcher;
        
        return icon;
    }

    public boolean acceptDrop(DragSource source, int x, int y, int xOffset, int yOffset,
            Object dragInfo) {
        final ItemInfo item = (ItemInfo) dragInfo;
        final int itemType = item.itemType;
        return (itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION ||
                itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT)
                && item.container != mInfo.id;
    }

    public Rect estimateDropLocation(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo, Rect recycle) {
        return null;
    }

    public void onDrop(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
        final ApplicationInfo item = (ApplicationInfo) dragInfo;
        // TODO: update open folder that is looking at this data
        mInfo.add(item);
        LauncherModel.addOrMoveItemInDatabase(mLauncher, item, mInfo.id, 0, 0, 0);
    }

    public void onDragEnter(DragSource source, int x, int y, int xOffset, int yOffset,
            Object dragInfo) {
        setCompoundDrawablesWithIntrinsicBounds(null, mOpenIcon, null, null);
    }

    public void onDragOver(DragSource source, int x, int y, int xOffset, int yOffset,
            Object dragInfo) {
    }

    public void onDragExit(DragSource source, int x, int y, int xOffset, int yOffset,
            Object dragInfo) {
        setCompoundDrawablesWithIntrinsicBounds(null, mCloseIcon, null, null);
    }
    /**
     * ADW: Load the floder icon drawables from the theme
     * @param context
     * @param manager
     * @param themePackage
     * @param resourceName
     * @return
     */
    static Drawable loadFolderFromTheme(Context context,
			PackageManager manager, String themePackage, String resourceName) {
		Drawable icon=null;
    	Resources themeResources=null;
    	try {
			themeResources=manager.getResourcesForApplication(themePackage);
		} catch (NameNotFoundException e) {
			//e.printStackTrace();
		}
		if(themeResources!=null){
			int resource_id=themeResources.getIdentifier (resourceName, "drawable", themePackage);
			if(resource_id!=0){
				icon=themeResources.getDrawable(resource_id);
			}
		}
		return icon;
	}
    
}
