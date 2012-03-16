package com.wordpress.chislonchow.legacylauncher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class AllAppsSlidingViewHolderLayout extends ViewGroup {
	//ADW: Animation vars
	private final static int CLOSED=1;
	private final static int OPEN=2;
	private final static int CLOSING=3;
	private final static int OPENING=4;
	private int mStatus=OPEN;
	private boolean isAnimating;
	private long startTime;
	private int mIconSize=0;
	private Paint mPaint;
	private Paint mLabelPaint;
	private boolean shouldDrawLabels=false;
	private int mAnimationDuration=800;
	private boolean mDrawLabels=true;
	private long mCurrentTime;
	//ADW: listener to dispatch open/close animation events
	private OnFadingListener mOnFadingListener;

	private float x;
	private float y;

	private int xx;
	public AllAppsSlidingViewHolderLayout(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		mPaint=new Paint();
		mPaint.setDither(false);
		mLabelPaint=new Paint();
		mLabelPaint.setDither(false);
		setWillNotDraw(false);
		updateLabelVars(context);
	}

	public AllAppsSlidingViewHolderLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		mPaint=new Paint();
		mPaint.setDither(false);
		mLabelPaint=new Paint();
		mLabelPaint.setDither(false);
		setWillNotDraw(false);
		updateLabelVars(context);
	}

	public AllAppsSlidingViewHolderLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
		mPaint=new Paint();
		mPaint.setDither(false);
		mLabelPaint=new Paint();
		mLabelPaint.setDither(false);
		setWillNotDraw(false);
		updateLabelVars(context);
	}
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		// TODO Auto-generated method stub
	}

	@Override
	protected boolean addViewInLayout(View child, int index,
			LayoutParams params, boolean preventRequestLayout) {
		// TODO Auto-generated method stub
		return super.addViewInLayout(child, index, params, preventRequestLayout);
	}

	@Override
	protected void attachViewToParent(View child, int index, LayoutParams params) {
		// TODO Auto-generated method stub
		super.attachViewToParent(child, index, params);
	}

	@Override
	protected void dispatchSetPressed(boolean pressed) {
		// TODO Auto-generated method stub
		//super.dispatchSetPressed(pressed);
	}

	@Override
	public void dispatchSetSelected(boolean selected) {
		// TODO Auto-generated method stub
		super.dispatchSetSelected(selected);
	}
	/*@Override
    public void requestChildFocus(View child, View focused) {
        super.requestChildFocus(child, focused);
        if (child != null) {
            Rect r = new Rect();
            child.getDrawingRect(r);
            requestRectangleOnScreen(r);
        }
    }*/
	@Override
	protected void setChildrenDrawingCacheEnabled(boolean enabled) {
		final int count = getChildCount();
		for (int i = 0; i < count; i++) {
			final View view = getChildAt(i);
			view.setDrawingCacheEnabled(enabled);
			// Update the drawing caches
			view.buildDrawingCache(true);
		}
	}

	@Override
	protected void setChildrenDrawnWithCacheEnabled(boolean enabled) {
		super.setChildrenDrawnWithCacheEnabled(enabled);
	}

	@Override
	protected void onFocusChanged(boolean gainFocus, int direction,
			Rect previouslyFocusedRect) {
		// TODO Auto-generated method stub
		//super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
	}
	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		//Log.d("HolderLayout","INTERCEPT");
		return true;
	}
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		//Log.d("HolderLayout","TOUCH");
		return true;
	}

	/**
	 * ADW: Override drawing methods to do animation
	 */
	@Override
	public void draw(Canvas canvas) {
		if (isAnimating) {
			if (startTime == 0) {
				startTime = SystemClock.uptimeMillis();
				mCurrentTime = 0;
			} else {
				mCurrentTime = SystemClock.uptimeMillis() - startTime;
			}

			if (mCurrentTime >= mAnimationDuration) {
				isAnimating = false;
				if (mStatus == OPENING) {
					mStatus = OPEN;
					dispatchFadingEvent(OnFadingListener.OPEN);
				} else if (mStatus == CLOSING) {
					mStatus = CLOSED;
					dispatchFadingEvent(OnFadingListener.CLOSE);
				}
			}
		}
		if(mStatus!=CLOSED){
			shouldDrawLabels = mDrawLabels
					&& (mStatus == OPENING || mStatus == CLOSING);
			super.draw(canvas);
		}
	}

	@Override
	protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
		int saveCount = canvas.save();
		Drawable[] tmp=((TextView)child).getCompoundDrawables();
		Bitmap b = null;
		if(mIconSize==0){
			mIconSize=tmp[1].getIntrinsicHeight()+child.getPaddingTop();
		}
		if(isAnimating) {
			postInvalidate();

			x=child.getLeft();
			y=child.getTop();

			if(shouldDrawLabels) {
				child.setDrawingCacheEnabled(true);
				child.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
				b = child.getDrawingCache();

				if(b != null){
					//ADW: try to manually draw labels
					final Rect rl1=new Rect(0,mIconSize,b.getWidth(),b.getHeight());
					final Rect rl2=new Rect(child.getLeft(),child.getTop()+mIconSize,child.getLeft()
							+b.getWidth(),child.getTop()+b.getHeight());
					canvas.drawBitmap(b, rl1, rl2, mLabelPaint);
				}
			}

			final Rect r3 = tmp[1].getBounds();
			xx=(child.getWidth()/2)-(r3.width()/2);
			canvas.save();
			canvas.translate(x+xx, y+child.getPaddingTop());

			tmp[1].draw(canvas);			
			canvas.restore();
		} else {
			if(mDrawLabels){
				canvas.save();
				canvas.translate(child.getLeft(), child.getTop());
				child.draw(canvas);
				canvas.restore();
			}else{
				final Rect r3 = tmp[1].getBounds();
				int xx=(child.getWidth()/2)-(r3.width()/2);
				canvas.save();
				canvas.translate(child.getLeft()+xx, child.getTop()+child.getPaddingTop());
				tmp[1].draw(canvas);
				canvas.restore();
			}
		}
		canvas.restoreToCount(saveCount);
		return true;
	}
	/**
	 * Open/close public methods
	 */
	public void open(boolean animate, int speed){
		if(mStatus!=OPENING){
			mAnimationDuration=speed;
			if(animate){
				isAnimating=true;
				mStatus=OPENING;
			}else{
				isAnimating=false;
				mStatus=OPEN;
				dispatchFadingEvent(OnFadingListener.OPEN);
			}
			startTime=0;
			invalidate();
		}
	}
	public void close(boolean animate, int speed){
		if(mStatus!=CLOSING){
			mAnimationDuration=speed;
			if(animate){
				mStatus=CLOSING;
				isAnimating=true;
			}else{
				mStatus=CLOSED;
				isAnimating=false;
				dispatchFadingEvent(OnFadingListener.CLOSE);
			}
			startTime=0;
			invalidate();
		}
	}
	/**
	 * Interface definition for a callback to be invoked when an open/close animation
	 * starts/ends
	 */
	public interface OnFadingListener {
		public static final int OPEN=1;
		public static final int CLOSE=2;
		void onUpdate(int Status);
	}
	public void setOnFadingListener(OnFadingListener listener) {
		mOnFadingListener = listener;
	}
	/**
	 * Dispatches a trigger event to listener. Ignored if a listener is not set.
	 * @param whichHandle the handle that triggered the event.
	 */
	private void dispatchFadingEvent(int status) {
		if (mOnFadingListener != null) {
			mOnFadingListener.onUpdate(status);
		}
	}
	public void updateLabelVars(Context context){
		mDrawLabels=MyLauncherSettingsHelper.getDrawerLabels(context);
	}

	public void setStartTime(long startTime)
	{
		this.startTime = startTime;
	}

	public long getStartTime()
	{
		return startTime;
	}
}
