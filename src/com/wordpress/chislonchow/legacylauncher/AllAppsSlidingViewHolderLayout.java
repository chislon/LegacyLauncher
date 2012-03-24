package com.wordpress.chislonchow.legacylauncher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class AllAppsSlidingViewHolderLayout extends ViewGroup {
	//ADW: Animation vars
	protected final static int HOLDER_CLOSED 	= 1;
	protected final static int HOLDER_OPENED 	= 2;
	protected final static int HOLDER_CLOSING 	= 3;
	protected final static int HOLDER_OPENING 	= 4;
	private int mStatus = HOLDER_OPENED;
	private boolean isAnimating;
	private int mIconSize=0;
	private Paint mPaint;
	private Paint mLabelPaint;

	private boolean mDrawLabels=true;

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
		if (mStatus != HOLDER_CLOSED) {
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
		child.setDrawingCacheEnabled(true);
		// lower quality when drawing
		if(isAnimating) {
			//postInvalidate();

			if(mDrawLabels) {
				child.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
				b = child.getDrawingCache();

				if(b != null) {
					//ADW: try to manually draw labels
					final Rect rl1=new Rect(0,mIconSize,b.getWidth(),b.getHeight());
					final Rect rl2=new Rect(child.getLeft(),child.getTop()+mIconSize,child.getLeft()
							+b.getWidth(),child.getTop()+b.getHeight());
					canvas.drawBitmap(b, rl1, rl2, mLabelPaint);
				}
			}

			final Rect r3 = tmp[1].getBounds();
			final int xx=(child.getWidth()/2)-(r3.width()/2);
			canvas.save();
			canvas.translate(child.getLeft()+xx, child.getTop()+child.getPaddingTop());

			tmp[1].draw(canvas);			
			canvas.restore();
		} else {
			// no drawing cache for icons that are just being displayed
			if(mDrawLabels){
				canvas.save();
				canvas.translate(child.getLeft(), child.getTop());
				child.draw(canvas);
				canvas.restore();
			} else { 
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
	public void open(boolean animate){
		if(mStatus!=HOLDER_OPENING){
			if(animate){
				isAnimating=true;
				mStatus=HOLDER_OPENING;
			}else{
				isAnimating=false;
				mStatus=HOLDER_OPENED;
			}
			invalidate();
		}
	}
	public void close(boolean animate){
		if(mStatus!=HOLDER_CLOSING){
			if(animate){
				mStatus=HOLDER_CLOSING;
				isAnimating=true;
			}else{
				mStatus=HOLDER_CLOSED;
				isAnimating=false;
			}
			invalidate();
		}
	}

	public void updateLabelVars(Context context){
		mDrawLabels=MyLauncherSettingsHelper.getDrawerLabels(context);
	}
	
	public void setHolderStatus(int status) {
		mStatus = status;
	}
}
