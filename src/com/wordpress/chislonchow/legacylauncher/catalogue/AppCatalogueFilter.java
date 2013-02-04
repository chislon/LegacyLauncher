package com.wordpress.chislonchow.legacylauncher.catalogue;

import java.util.List;

import com.wordpress.chislonchow.legacylauncher.Launcher;
import com.wordpress.chislonchow.legacylauncher.catalogue.AppCatalogueFilters.Catalog;

import android.content.SharedPreferences;
import android.util.Log;

public class AppCatalogueFilter {

	private AppCatalogueFilters.Catalog mCatalogue;

	public AppCatalogueFilter() {
		this(AppGroupAdapter.APP_GROUP_ALL);
	}

	public AppCatalogueFilter(int index) {
		setCurrentGroupIndex(index);
	}

	public boolean checkAppInGroup(String className) {
		boolean result = true;
		if (mCatalogue != null) {
			final SharedPreferences prefs = mCatalogue.getPreferences();
			if (prefs != null)
				result = prefs.getBoolean(className, false);
		}
		else
		{
            AppCatalogueFilters instance = AppCatalogueFilters.getInstance();
    		if ( ((Launcher)instance.mContext).mUseDrawerUngroupCatalog )
    		{
                List<Catalog> allGroups = instance.getAllGroups();
    			for (Catalog catalogue : allGroups)
    			{
    				if ( catalogue.getPreferences().getBoolean(className, false) )
    				{
    					result = false;
    					break;
    				}
    			}
    		}
		}
		return result;
	}

	public boolean isUserGroup() {
		return mCatalogue != null;
	}

	public int getCurrentFilterIndex() {
		if (mCatalogue != null)
			return mCatalogue.getIndex();
		else
			return AppGroupAdapter.APP_GROUP_ALL;
	}

	public synchronized void setCurrentGroupIndex(int index) {
		if (index != getCurrentFilterIndex()) {
			mCatalogue = AppCatalogueFilters.getInstance().getCatalogue(index);
		}
	}


}
