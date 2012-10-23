package com.wordpress.chislonchow.legacylauncher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.wordpress.chislonchow.legacylauncher.DragController.DragListener;

public class ActionButton extends CounterImageView implements DropTarget,
DragListener, OnLongClickListener, DragSource {
	private Launcher mLauncher;
	private int mIdent = LauncherSettings.Favorites.CONTAINER_LAB;
	private ItemInfo mCurrentInfo;
	private Drawable bgResource;
	private Drawable bgEmpty;
	private Drawable mIconNormal;

	private int mIconSpecial_id;
	private Drawable mIconSpecial;
	private boolean specialMode = false;
	private boolean hiddenBg = false;
	private int specialAction = 0;
	public boolean mInterceptClicks = false;
	private DragController mDragger;

	private StateListDrawable mStateListDrawable;

	private int mSelectorColor = 0xFF82B600;

	public ActionButton(Context context) {
		super(context);
	}

	public ActionButton(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ActionButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setHapticFeedbackEnabled(true);
		TypedArray a = context.obtainStyledAttributes(attrs,
				R.styleable.ActionButton, defStyle, 0);
		mIdent = a.getInt(R.styleable.ActionButton_ident, mIdent);
		bgEmpty = context.getResources().getDrawable(
				R.drawable.lab_rab_empty_bg);
		a.recycle();
		this.setOnLongClickListener(this);
	}

	/* (non-Javadoc)
	 * @see com.wordpress.chislonchow.legacylauncher.DropTarget#acceptDrop(com.wordpress.chislonchow.legacylauncher.DragSource, int, int, int, int, java.lang.Object)
	 */
	public boolean acceptDrop(DragSource source, int x, int y, int xOffset,
			int yOffset, Object dragInfo) {

		// launcher lock do nothing if locked
		if (mCurrentInfo != null) {
			if (LauncherSettings.Favorites.CONTAINER_MAB == mCurrentInfo.container && 
					MyLauncherSettingsHelper.getDockLockMAB(mLauncher)) {
				return false;
			}
		}

		final ItemInfo info = (ItemInfo) dragInfo;
		if (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET) {
			// do not eat widgets
			return false;
		}

		return !specialMode;
	}

	public Rect estimateDropLocation(DragSource source, int x, int y,
			int xOffset, int yOffset, Object dragInfo, Rect recycle) {
		return null;
	}

	public void onDragEnter(DragSource source, int x, int y, int xOffset,
			int yOffset, Object dragInfo) {
		setPressed(true);
	}

	public void onDragExit(DragSource source, int x, int y, int xOffset,
			int yOffset, Object dragInfo) {
		setPressed(false);
	}

	public void onDragOver(DragSource source, int x, int y, int xOffset,
			int yOffset, Object dragInfo) {
	}

	/* (non-Javadoc)
	 * @see com.wordpress.chislonchow.legacylauncher.DropTarget#onDrop(com.wordpress.chislonchow.legacylauncher.DragSource, int, int, int, int, java.lang.Object)
	 */
	public void onDrop(DragSource source, int x, int y, int xOffset,
			int yOffset, Object dragInfo) {

		// default value
		boolean accept = true;

		ItemInfo info = (ItemInfo) dragInfo;
		switch (info.itemType) {
		case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
		case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
		case LauncherSettings.Favorites.ITEM_TYPE_LIVE_FOLDER:
		case LauncherSettings.Favorites.ITEM_TYPE_USER_FOLDER:
			// we do accept those
			break;
		case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:
			// this should never happen, but just in case
			accept = false;
			break;
		default:
			Toast t2 = Toast.makeText(getContext(),
					R.string.toast_unknown_item, Toast.LENGTH_SHORT);
			t2.show();
			accept = false;
			break;
		}
		final LauncherModel model = Launcher.getLauncherModel();
		// TODO:ADW check this carefully
		// We need to remove current item from database before adding the new
		// one
		if (info instanceof LauncherAppWidgetInfo) {
			model.removeDesktopAppWidget((LauncherAppWidgetInfo) info);
			final LauncherAppWidgetInfo launcherAppWidgetInfo = (LauncherAppWidgetInfo) info;
			final LauncherAppWidgetHost appWidgetHost = mLauncher
					.getAppWidgetHost();
			if (appWidgetHost != null) {
				appWidgetHost
				.deleteAppWidgetId(launcherAppWidgetInfo.appWidgetId);
			}
		}

		// accept drop logic
		if (accept) {
			if (mCurrentInfo != null) {
				model.removeDesktopItem(mCurrentInfo);
				LauncherModel.deleteItemFromDatabase(mLauncher, mCurrentInfo);
			}
			model.addDesktopItem(info);
			LauncherModel.addOrMoveItemInDatabase(mLauncher, info, mIdent, -1,
					-1, -1);
			UpdateLaunchInfo(info);
		} else {
			LauncherModel.deleteItemFromDatabase(mLauncher, info);
		}
	}

	protected void UpdateLaunchInfo(ItemInfo info) {
		// commit changes
		mCurrentInfo = info;
		// TODO:ADW extract icon and put it as the imageview src...
		Drawable myIcon = null;
		if (info != null) {
			switch (info.itemType) {
			case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
			case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
				if (info.container == NO_ID) {
					// Came from all apps -- make a copy
					info = new ApplicationInfo((ApplicationInfo) info);
				}
				setCounter(((ApplicationInfo) info).counter,
						((ApplicationInfo) info).counterColor);
				myIcon = mLauncher.createSmallActionButtonIcon(info);
				break;
			case LauncherSettings.Favorites.ITEM_TYPE_LIVE_FOLDER:
			case LauncherSettings.Favorites.ITEM_TYPE_USER_FOLDER:
				myIcon = mLauncher.createSmallActionButtonIcon(info);
				break;
			case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:
				// Toast t=Toast.makeText(getContext(),
				// "Widgets not supported... sorry :-)", Toast.LENGTH_SHORT);
				// t.show();
				return;
			default:
				// Toast t2=Toast.makeText(getContext(),
				// "Unknown item. We can't add unknown item types :-)",
				// Toast.LENGTH_SHORT);
				// t2.show();
				return;
				// throw new IllegalStateException("Unknown item type: " +
				// info.itemType);
			}
		}

		setIcon(myIcon);
		updateIcon();
		invalidate();
	}

	public void onDragEnd() {
	}

	public void onDragStart(View v, DragSource source, Object info,
			int dragAction) {

	}

	void setLauncher(Launcher launcher) {
		mLauncher = launcher;
	}

	@Override
	public Object getTag() {
		// TODO Auto-generated method stub
		if (!specialMode) {
			return mCurrentInfo;
		} else {
			return specialAction;
		}
	}

	public void updateIcon() {
		if (mCurrentInfo != null) {
			ItemInfo info = mCurrentInfo;
			Drawable myIcon = null;
			switch (info.itemType) {
			case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
			case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
				if (info.container == NO_ID) {
					// Came from all apps -- make a copy
					info = new ApplicationInfo((ApplicationInfo) info);
				}
				myIcon = mLauncher.createSmallActionButtonIcon(info);
				break;
			case LauncherSettings.Favorites.ITEM_TYPE_LIVE_FOLDER:
			case LauncherSettings.Favorites.ITEM_TYPE_USER_FOLDER:
				myIcon = mLauncher.createSmallActionButtonIcon(info);
				break;
			case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:
				// Toast t=Toast.makeText(getContext(),
				// "Widgets not supported... sorry :-)", Toast.LENGTH_SHORT);
				// t.show();
				return;
			default:
				// Toast t2=Toast.makeText(getContext(),
				// "Unknown item. We can't add unknown item types :-)",
				// Toast.LENGTH_SHORT);
				// t2.show();
				return;
				// throw new IllegalStateException("Unknown item type: " +
				// info.itemType);
			}
			setIcon(myIcon);
			invalidate();
		} else {
			Drawable myIcon = mLauncher.createSmallActionButtonIcon(null);
			setIcon(myIcon);
			invalidate();
		}
		if (mIconSpecial_id != 0) {
			setSpecialIcon(mIconSpecial_id);
		}
	}

	/**
	 * ADW: show/hide background
	 * 
	 * @param enable
	 */
	public void hideBg(boolean hide) {
		if (hide != hiddenBg) {
			hiddenBg = hide;
			if (!hide)
				this.setBackgroundDrawable(bgResource);
			else {
				this.setBackgroundDrawable(bgEmpty);
			}
		}
	}

	@Override
	public void setBackgroundDrawable(Drawable d) {
		// TODO Auto-generated method stub
		super.setBackgroundDrawable(d);
		if (d != bgEmpty) {
			if (bgResource != null)
				bgResource.setCallback(null);
			bgResource = d;
		}
	}

	/**
	 * ADW: Reload the proper icon This is mainly used when the apps from SDcard
	 * are available in froyo
	 */
	public void reloadIcon(String packageName) {
		if (mCurrentInfo == null)
			return;
		if (mCurrentInfo instanceof ApplicationInfo) {
			final ApplicationInfo info = (ApplicationInfo) mCurrentInfo;
			final Intent intent = info.intent;
			final ComponentName name = intent.getComponent();
			if ((info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION || info.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT)
					&& Intent.ACTION_MAIN.equals(intent.getAction())
					&& name != null
					&& packageName.equals(name.getPackageName())) {
				final Drawable icon = Launcher.getLauncherModel()
						.getApplicationInfoIcon(mLauncher.getPackageManager(),
								info, mLauncher);
				Drawable myIcon = null;
				if (icon != null) {
					info.icon.setCallback(null);
					info.icon = Utilities.createIconThumbnail(icon, mLauncher);
					info.filtered = true;
					myIcon = mLauncher.createSmallActionButtonIcon(info);
					setIcon(myIcon);
					invalidate();
				}
			}
		}
	}

	private void setIcon(Drawable d) {
		if (mIconNormal != null) {
			mIconNormal.setCallback(null);
			mIconNormal = null;
		}

		if (mStateListDrawable != null) {
			mStateListDrawable.setCallback(null);
			mStateListDrawable = null;
		}

		mIconNormal = d;
		if (!specialMode) {
			if (mIconNormal != null) {
				final Drawable iconSelected =  Utilities.selectorDrawable(mIconNormal, mIconNormal.getIntrinsicWidth(), mIconNormal.getIntrinsicHeight(), mSelectorColor);
				mStateListDrawable = new StateListDrawable();
				mStateListDrawable.addState(new int[] {android.R.attr.state_pressed}, iconSelected);
				mStateListDrawable.addState(new int[] {android.R.attr.state_selected}, iconSelected);
				mStateListDrawable.addState(new int[] {android.R.attr.state_focused}, iconSelected);
				mStateListDrawable.addState(new int[] {}, mIconNormal);
				setImageDrawable(mStateListDrawable);
			} else {
				setImageDrawable(null);
			}
		}
	}

	public void setSpecialIcon(int res_id) {
		Drawable d = null;
		try {
			d = getResources().getDrawable(res_id);
			if (mIconSpecial != null) {
				mIconSpecial.setCallback(null);
				mIconSpecial = null;
			}
			mIconSpecial_id = res_id;
			mIconSpecial = mLauncher.createSmallActionButtonDrawable(d);
			if (specialMode) {
				setImageDrawable(mIconSpecial);
			}
		} catch (Exception e) {
		}
	}

	public void setSpecialMode(boolean special) {
		if (special != specialMode) {
			specialMode = special;
			if (specialMode)
				setImageDrawable(mIconSpecial);
			else
				setImageDrawable(mIconNormal);
		}
	}

	public void setSpecialAction(int action) {
		specialAction = action;
	}

	@Override
	public boolean onLongClick(View v) {
		if (mDragger == null || !v.isInTouchMode() || mCurrentInfo == null
				|| specialMode || mLauncher.isLauncherLocked()) {
			return false;
		} else if (MyLauncherSettingsHelper.getDockLockMAB(mLauncher)
				&& LauncherSettings.Favorites.CONTAINER_MAB == mCurrentInfo.container) {
			return false;
		}

		mLauncher.showQuickActionWindow(mCurrentInfo, v,
				new PopupWindow.OnDismissListener() {
			@Override
			public void onDismiss() {
				// Close Drawer if it is open...
				mLauncher.closeAllApplications();
			}
		});
		mDragger.startDrag(v, this, mCurrentInfo,
				DragController.DRAG_ACTION_COPY);
		UpdateLaunchInfo(null);
		return true;
	}

	public void setDragger(DragController dragger) {
		mDragger = dragger;
	}

	@Override
	public void onDropCompleted(View target, boolean success) {
	}

	protected void setSelectorColor(int color) {
		mSelectorColor = color;
	}
}
