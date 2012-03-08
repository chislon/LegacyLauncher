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

//import com.wordpress.chislonchow.legacylauncher.catalogue.CataGridView;

import com.wordpress.chislonchow.legacylauncher.catalogue.AppCatalogueFilters;
import com.wordpress.chislonchow.legacylauncher.catalogue.AppGroupAdapter;
import com.wordpress.chislonchow.legacylauncher.R;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.Transformation;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.PopupWindow;
import android.widget.TextView;

public class AllAppsGridView extends GridView implements
AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener,
DragSource, Drawer {

	private DragController mDragger;
	private Launcher mLauncher;
	private Paint mPaint;
	// ADW: Animation vars
	private final static int CLOSED = 1;
	private final static int OPEN = 2;
	private final static int CLOSING = 3;
	private final static int OPENING = 4;
	private int mStatus = CLOSED;
	private boolean isAnimating;
	private long startTime;
	private float mScaleFactor;
	private int mIconSize = 0;
	private int mBgAlpha = 255;
	private int mTargetAlpha = 255;
	private Paint mLabelPaint;
	private boolean shouldDrawLabels = false;
	private int mAnimationDuration = 800;
	private int mBgColor = 0xFF000000;
	private boolean mDrawLabels = true;
	private boolean mFadeDrawLabels = false;
	private boolean mDrawerZoom = false;

	private float mLabelFactor;

	private Rect rl1=new Rect();
	private Rect rl2=new Rect();

	private int mLastIndexDraw = -99;
	private String mGroupTitle = null;
	private TextView mGroupTitleText;
	private int mGroupTextX;
	private int mGroupTextY;
	private Paint mGroupPaint;
	private boolean mShouldDrawGroupText = false;
	Transformation mTransformation = new Transformation();

	private DisplayMetrics dm = getResources().getDisplayMetrics();
	private int SWIPE_MIN_DISTANCE = (int)(ABS_SWIPE_MIN_DISTANCE * dm.densityDpi / 160.0f);
	private int SWIPE_MAX_OFF_PATH = (int)(ABS_SWIPE_MAX_OFF_PATH * dm.densityDpi / 160.0f);
	private int SWIPE_THRESHOLD_VELOCITY = (int)(ABS_SWIPE_THRESHOLD_VELOCITY * dm.densityDpi / 160.0f);
	private GestureDetector gestureDetector;
	private View.OnTouchListener gestureListener;
	private boolean mFling = false;

	public AllAppsGridView(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.gridViewStyle);
	}

	public AllAppsGridView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		mPaint = new Paint();
		mPaint.setDither(false);
		mLabelPaint = new Paint();
		mLabelPaint.setDither(false);
	}

	@Override
	public boolean isOpaque() {
		if (mBgAlpha >= 255)
			return true;
		else
			return false;
	}

	@Override
	protected void onFinishInflate() {
		setOnItemClickListener(this);
		setOnItemLongClickListener(this);
	}

	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		if ( !mFling )
		{
			ApplicationInfo app = (ApplicationInfo) parent
					.getItemAtPosition(position);
			mLauncher.startActivitySafely(app.intent);
		}
	}

	public boolean onItemLongClick(AdapterView<?> parent, View view,
			int position, long id) {
		if (!view.isInTouchMode()) {
			return false;
		}

		// consume long-press event if locked
		if (mLauncher.isLauncherIconsLocked()) {
			return true;
		}

		ApplicationInfo app = (ApplicationInfo) parent
				.getItemAtPosition(position);
		app = new ApplicationInfo(app);

		mLauncher.showActions(app, view, new PopupWindow.OnDismissListener()
		{
			@Override
			public void onDismiss()
			{
				mLauncher.closeAllApplications();
			}
		});
		mDragger.startDrag(view, this, app, DragController.DRAG_ACTION_COPY);

		return true;
	}

	public void setDragger(DragController dragger) {
		mDragger = dragger;
	}

	public void onDropCompleted(View target, boolean success) {
	}

	public void setLauncher(Launcher launcher) {
		mLauncher = launcher;
		setSelector(IconHighlights.getDrawable(mLauncher,
				IconHighlights.TYPE_DESKTOP));
	}

	/**
	 * ADW: easing functions for animation
	 */
	static float easeOut(float time, float begin, float end, float duration) {
		float change = end - begin;
		return change * ((time = time / duration - 1) * time * time + 1)
				+ begin;
	}

	static float easeIn(float time, float begin, float end, float duration) {
		float change = end - begin;
		return change * (time /= duration) * time * time + begin;
	}

	static float easeInOut(float time, float begin, float end, float duration) {
		float change = end - begin;
		if ((time /= duration / 2.0f) < 1)
			return change / 2.0f * time * time * time + begin;
		return change / 2.0f * ((time -= 2.0f) * time * time + 2.0f) + begin;
	}

	/**
	 * ADW: Override drawing methods to do animation
	 */
	@Override
	public void draw(Canvas canvas) {
		int saveCount = canvas.save();
		if (isAnimating) {
			long currentTime;
			if (startTime == 0) {
				startTime = SystemClock.uptimeMillis();
				currentTime = 0;
			} else {
				currentTime = SystemClock.uptimeMillis() - startTime;
			}
			if (mStatus == OPENING) {
				mScaleFactor = easeOut(currentTime, 3.0f, 1.0f, mAnimationDuration);
				mLabelFactor = easeOut(currentTime, -1.0f, 1.0f, mAnimationDuration);
			} else if (mStatus == CLOSING) {
				mScaleFactor = easeIn(currentTime, 1.0f, 3.0f, mAnimationDuration);
				mLabelFactor = easeIn(currentTime, 1.0f, -1.0f, mAnimationDuration);
			}
			if (mLabelFactor < 0)
				mLabelFactor = 0;
			if (currentTime >= mAnimationDuration) {
				isAnimating = false;
				if (mStatus == OPENING) {
					mStatus = OPEN;
				} else if (mStatus == CLOSING) {
					mStatus = CLOSED;
					mLauncher.getWorkspace().clearChildrenCache();
					setVisibility(View.GONE);
				}
			}
		}
		shouldDrawLabels = mFadeDrawLabels && mDrawLabels
				&& (mStatus == OPENING || mStatus == CLOSING);
		float percentageScale = 1.0f;
		if (isAnimating) {
			percentageScale = 1.0f - ((mScaleFactor - 1) / 3.0f);
			if (percentageScale > 0.9f)
				percentageScale = 1f;
			if (percentageScale < 0)
				percentageScale = 0;
			mBgAlpha = (int) (percentageScale * 255);
		}
		mPaint.setAlpha(mBgAlpha);
		if (getVisibility() == View.VISIBLE) {
			canvas
			.drawARGB((int) (percentageScale * mTargetAlpha), Color
					.red(mBgColor), Color.green(mBgColor), Color
					.blue(mBgColor));
			int index = ((ApplicationsAdapter) getAdapter()).getCatalogueFilter().getCurrentFilterIndex();
			if (mLastIndexDraw != index)
			{
				mLastIndexDraw = index;
				int title = isUngroupMode?R.string.AppGroupUn:R.string.AppGroupAll;
				mGroupTitle = (index == AppGroupAdapter.APP_GROUP_ALL ? mLauncher.getString(title) : AppCatalogueFilters.getInstance()
						.getGroupTitle(index));

				// ((LauncherPlus)mLauncher).groupTitlePopupWindow(mLauncher,
				// AllAppsGridView.this, name);
				mGroupTitleText = new TextView(getContext());
				mGroupTitleText.setTextSize(15);
				mGroupTitleText.setText(mGroupTitle);
				mGroupTitleText.setTextColor(Color.WHITE);

				int textWidth = (int) mGroupTitleText.getPaint().measureText(mGroupTitle);
				mGroupTextX = (getWidth() / 2) - (textWidth / 2);

				int textSize = (int) mGroupTitleText.getPaint().getTextSize();
				mGroupTextY = textSize;

				mGroupPaint = new Paint();
				mGroupPaint.setColor(Color.WHITE);
				mGroupPaint.setTextSize(mGroupTitleText.getTextSize());
				mGroupPaint.setAntiAlias(true);

				mShouldDrawGroupText = mLauncher.mUseDrawerTitleCatalog 
						&& AppCatalogueFilters.getInstance().getAllGroups().size() > 0;
			}

			if ( mShouldDrawGroupText )
			{
				if (mStatus == OPEN )
				{
					mGroupPaint.setAlpha(255);
					canvas.drawText(mGroupTitle, mGroupTextX, mGroupTextY, mGroupPaint);
				}
				else if ( isAnimating && mStatus == OPENING)
				{
					mGroupPaint.setAlpha((int) (mLabelFactor * 255));
					canvas.drawText(mGroupTitle, mGroupTextX, mGroupTextY, mGroupPaint);
				}
				canvas.translate(0, mGroupTextY);
			}

			super.draw(canvas);
			canvas.restoreToCount(saveCount);
		}

	}

	@Override
	protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
		int saveCount = canvas.save();
		Drawable[] tmp = ((TextView) child).getCompoundDrawables();
		Bitmap b = null;
		if (mIconSize == 0) {
			mIconSize = tmp[1].getIntrinsicHeight() + child.getPaddingTop();
		}
		int childLeft = child.getLeft();
		int childWidth = child.getWidth();
		int childTop = child.getTop();
		if (isAnimating) {
			postInvalidate();
			float distH = (childLeft + (childWidth / 2))
					- (getWidth() / 2);
			float distV = (childTop + (child.getHeight() / 2))
					- (getHeight() / 2);
			float scaleFactor;
			if (mDrawerZoom) {
				scaleFactor = mScaleFactor;
			} else {
				scaleFactor = 1;
			}
			float x = childLeft + (distH * (scaleFactor - 1)) * scaleFactor;
			float y = childTop + (distV * (scaleFactor - 1)) * scaleFactor;
			float width = childWidth * scaleFactor;
			if (shouldDrawLabels) {
				child.setDrawingCacheEnabled(true);
				child.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
				b = child.getDrawingCache();
				if ( b != null) {
					// ADW: try to manually draw labels
					int bHeight = b.getHeight();
					int bWidth = b.getWidth();
					rl1.set(0, mIconSize, bWidth, bHeight);
					rl2.set(childLeft, childTop + mIconSize, childLeft + bWidth,childTop + bHeight);
					mLabelPaint.setAlpha((int) (mLabelFactor * 255));
					canvas.drawBitmap(b, rl1, rl2, mLabelPaint);
				}
			}
			float scale = ((width) / childWidth);
			int xx = (childWidth / 2) - (tmp[1].getBounds().width() / 2);
			canvas.translate(x + xx, y + child.getPaddingTop());
			canvas.scale(scale, scale);

			tmp[1].draw(canvas);
		} else {
			int alpha = 255;
			if ( mStatusTransformation )
			{
				getChildStaticTransformation( child, mTransformation);
				alpha = (int) (mTransformation.getAlpha() * 255);
			}
			if (mDrawLabels) {
				child.setDrawingCacheEnabled(true);
				b = child.getDrawingCache();
				if (b != null) {
					mPaint.setAlpha(alpha);
					canvas.drawBitmap(b, childLeft, childTop, mPaint);
				} else {
					canvas.saveLayerAlpha(childLeft, childTop, childLeft + childWidth, childTop + child.getHeight(), (int) (mTransformation.getAlpha() * 255),
							Canvas.HAS_ALPHA_LAYER_SAVE_FLAG | Canvas.CLIP_TO_LAYER_SAVE_FLAG);
					canvas.translate(childLeft, childTop);
					child.draw(canvas);
				}
			} else {
				int xx = (childWidth / 2) - (tmp[1].getBounds().width() / 2);
				canvas.translate(childLeft + xx, childTop + child.getPaddingTop());
				tmp[1].draw(canvas);
			}
		}
		canvas.restoreToCount(saveCount);
		return true;
	}

	/**
	 * Open/close public methods
	 */
	public void open(boolean animate) {
		mLastIndexDraw = -99;
		mBgColor = AlmostNexusSettingsHelper.getDrawerColor(mLauncher);
		mTargetAlpha = Color.alpha(mBgColor);
		mDrawLabels = AlmostNexusSettingsHelper.getDrawerLabels(mLauncher);
		mFadeDrawLabels = AlmostNexusSettingsHelper
				.getFadeDrawerLabels(mLauncher);
		mDrawerZoom = AlmostNexusSettingsHelper
				.getDrawerZoom(mLauncher);
		if(getAdapter()==null)
			animate=false;
		else if(getAdapter().getCount()<=0)
			animate=false;
		if (animate) {

			if (mFadeDrawLabels && mDrawLabels) {
				ListAdapter adapter = getAdapter();
				if (adapter instanceof ApplicationsAdapter)
					((ApplicationsAdapter)adapter).setChildDrawingCacheEnabled(true);
			}
			mBgAlpha = 0;
			isAnimating = true;
			mStatus = OPENING;
		} else {
			mBgAlpha = mTargetAlpha;
			isAnimating = false;
			mStatus = OPEN;
		}
		startTime = 0;
		this.setVisibility(View.VISIBLE);
		invalidate();
	}

	public void close(boolean animate) {
		if(getAdapter()==null)
			animate=false;
		else if(getAdapter().getCount()<=0)
			animate=false;
		if (animate) {
			mStatus = CLOSING;
			isAnimating = true;
		} else {
			mStatus = CLOSED;
			isAnimating = false;
			mLauncher.getWorkspace().clearChildrenCache();
			setVisibility(View.GONE);
		}
		startTime = 0;
		invalidate();
	}
	public void setAnimationSpeed(int speed) {
		mAnimationDuration = speed;
	}

	public void updateAppGrp() {
		if(getAdapter()!=null){
			((ApplicationsAdapter) getAdapter()).updateDataSet();
			mLastIndexDraw = -99;
		}
	}

	public void setAdapter(ApplicationsAdapter adapter) {
		setAdapter((ListAdapter)adapter);
	}

	public void setNumRows(int numRows) {}

	public void setPageHorizontalMargin(int margin) {}

	int FADE_OFF = 0;
	int FADE_IN = 1;
	int FADE_CHANGE = 2;
	int FADE_OUT = 3;
	long mFadeEnd;
	int mFadeType = FADE_OFF;
	DataSetObserver mLastDSObserver;
	Runnable mSwitchGroups;
	boolean mStatusTransformation = false;

	private boolean isUngroupMode = false;

	public void switchGroups(Runnable switchGroups)
	{
		// just in case :)
		if ( mLastDSObserver != null )
		{
			getAdapter().unregisterDataSetObserver(mLastDSObserver);
			mLastDSObserver = null;
		}

		this.mSwitchGroups = switchGroups;
		mFadeEnd = System.currentTimeMillis() + 150;
		mFadeType = FADE_OUT;

		if ( getAdapter().getCount() == 0 )
		{
			// nothing to fade so we can't use draw events :( 
			setStaticTransformationsEnabled(false);
			switchGroups.run();
		}
		else
		{
			setStaticTransformationsEnabled(true);
			this.postInvalidate();
		}
	}

	@Override
	protected boolean getChildStaticTransformation(View child, Transformation t)
	{
		long time = mFadeEnd - System.currentTimeMillis();
		if (mFadeType != FADE_OFF)
		{
			if (mFadeType == FADE_IN)
			{
				if (time > 0)
				{
					t.setAlpha(1 - (time / 150f));
					this.postInvalidate();
					return true;
				}
			}
			else
			{
				if (time > 0)
				{
					t.setAlpha(time / 150f);
					this.postInvalidate();
					return true;
				}
				else if ( mFadeType == FADE_CHANGE)
				{
					t.setAlpha(0);
					return true;
				}
				else 
				{
					mFadeType = FADE_CHANGE;

					mLastDSObserver = new DataSetObserver()
					{
						@Override
						public void onChanged()
						{
							// TODO Auto-generated method stub
							super.onChanged();
							postInvalidate();
							long time = mFadeEnd - System.currentTimeMillis();
							mFadeType = FADE_IN;
							mFadeEnd = System.currentTimeMillis() + (150 + time);
						}
						@Override
						public void onInvalidated()
						{
							super.onInvalidated();
							postInvalidate();
							long time = mFadeEnd - System.currentTimeMillis();
							mFadeType = FADE_IN;
							mFadeEnd = System.currentTimeMillis() + (150 + time);
						}
					};
					getAdapter().registerDataSetObserver(mLastDSObserver);
					mSwitchGroups.run();
					t.setAlpha(0);
					this.postInvalidate();
					return true;
				}
			}
		}

		// clean up!
		if ( mLastDSObserver != null )
		{
			getAdapter().unregisterDataSetObserver(mLastDSObserver);
			mLastDSObserver = null;
		}
		mFadeType = FADE_OFF;
		t.setAlpha(255);
		setStaticTransformationsEnabled(false);

		this.postInvalidate();
		return true;
	}

	@Override
	protected void setStaticTransformationsEnabled(boolean enabled)
	{
		mStatusTransformation = enabled;
		super.setStaticTransformationsEnabled(enabled);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b)
	{
		super.onLayout(changed, l, t, r, b);

		setupGestures();

		this.setOnTouchListener(gestureListener);

		final int count = getChildCount();
		for (int i = 0; i < count; i++) {
			final View child = getChildAt(i);
			child.setOnTouchListener(gestureListener);
		}
	}

	void setupGestures()
	{
		// Gesture detection
		gestureDetector = new GestureDetector(getContext(), new MyGestureDetector());
		gestureListener = new View.OnTouchListener()
		{
			public boolean onTouch(View v, MotionEvent event)
			{
				if (gestureDetector.onTouchEvent(event))
				{
					return true;
				}
				return false;
			}
		};
	}

	class MyGestureDetector extends SimpleOnGestureListener
	{
		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
		{
			return super.onScroll(e1, e2, distanceX, distanceY);
		}

		float downX;
		@Override
		public boolean onDown(MotionEvent e)
		{
			mFling = false;
			downX = e.getX();
			// TODO Auto-generated method stub
			return super.onDown(e);
		}

		@Override
		public boolean onSingleTapUp(MotionEvent e)
		{
			if (Math.abs(downX - e.getX()) > SWIPE_MAX_OFF_PATH)
				return false;
			// TODO Auto-generated method stub
			return super.onSingleTapUp(e);
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
		{
			mFling = true;
			try {
				// not up down swipe
				if (Math.abs(e1.getY() - e2.getY()) <= SWIPE_MAX_OFF_PATH) {
					if (mLauncher.getUseDrawerCatalogFlingNavigation()) {

						if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY)
						{
							mLauncher.navigateCatalogs(Launcher.ACTION_CATALOG_PREV);
						}
						else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY)
						{
							mLauncher.navigateCatalogs(Launcher.ACTION_CATALOG_NEXT);
						}
					}
				}
			} catch (Exception e) {
				// nothing
			}
			return super.onFling(e1, e2, velocityX, velocityY);
		}
	}

	@Override
	public void setUngroupMode(boolean setUngroupMode)
	{
		isUngroupMode = setUngroupMode;
	}
}
