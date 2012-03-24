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

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.util.AttributeSet;

/**
 * TextView that draws a bubble behind the text. We cannot use a LineBackgroundSpan
 * because we want to make the bubble taller than the text and TextView's clip is
 * too aggressive.
 */
public class BubbleTextView extends CounterTextView {
	//private static final float CORNER_RADIUS = 8.0f;

	private final RectF mRect = new RectF();
	private Paint mPaint;

	private boolean mBackgroundSizeChanged;
	private Drawable mBackground;
	private float mCornerRadius;
	private float mPaddingH;
	private float mPaddingV;
	//adw custom corner radius themable
	public BubbleTextView(Context context) {
		super(context);
		init();
	}

	public BubbleTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public BubbleTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	private void init() {
		Context context = getContext();

		setFocusable(true);
		//mBackground = getBackground();
		mBackground = IconHighlights.getDrawable(context, IconHighlights.TYPE_DESKTOP);
		setBackgroundDrawable(null);
		mBackground.setCallback(this);

		int colorBg;
		boolean overrideColor = MyLauncherSettingsHelper.getDesktopLabelColorOverride(context);
		boolean overridePadding = MyLauncherSettingsHelper.getDesktopLabelPaddingOverride(context);

		//ADW: Load textcolor and bubble color from theme
		String themePackage = MyLauncherSettingsHelper.getThemePackageName(context, Launcher.THEME_DEFAULT);

		// load theme padding and colors 
		if(!themePackage.equals(Launcher.THEME_DEFAULT)){
			Resources themeResources = null;
			try {
				themeResources=context.getPackageManager().getResourcesForApplication(themePackage);
			} catch (NameNotFoundException e) {
				//e.printStackTrace();
			}
			if (themeResources != null) {
				if (!overrideColor) {
					int resourceId=themeResources.getIdentifier("bubble_color", "color", themePackage);
					if (resourceId != 0){
						colorBg = themeResources.getColor(resourceId);
					}
					int textColorId = themeResources.getIdentifier("bubble_text_color", "color", themePackage);
					if (textColorId != 0){
						setTextColor(themeResources.getColor(textColorId));
					}
				}
				if (!overridePadding) {
					int cornerId=themeResources.getIdentifier("bubble_radius", "integer", themePackage);
					if (cornerId != 0){
						mCornerRadius = (float)themeResources.getInteger(cornerId);
					}
				}
			}
		}

		// text size customization. no scaling because the entire canvas is scaled. 
		setTextSize(MyLauncherSettingsHelper.getDesktopLabelSize(context));

		// text size bold.
		if (MyLauncherSettingsHelper.getDesktopLabelBold(context)) {
			setTypeface(Typeface.DEFAULT_BOLD); 
		}

		// retrieve overridden or default values
		if (overrideColor) {
			// bubble background color
			colorBg = MyLauncherSettingsHelper.getDesktopLabelColorBg(context);	// set later in code
			// text color
			setTextColor(MyLauncherSettingsHelper.getDesktopLabelColorText(context));
			// set the shadow layer (overwrites the value from CounterTextView
			int colorShadow = MyLauncherSettingsHelper.getDesktopLabelColorShadow(context);	// set later in code
			setShadowLayer(2f, 1, 1, colorShadow);
		} else {
			colorBg = context.getResources().getInteger(R.integer.config_desktop_label_color_bg);
		}

		if (overridePadding) {
			mCornerRadius = MyLauncherSettingsHelper.getDesktopLabelPaddingRadius(context);
			mPaddingH = MyLauncherSettingsHelper.getDesktopLabelPaddingH(context);
			mPaddingV = MyLauncherSettingsHelper.getDesktopLabelPaddingV(context);
		} else {
			mCornerRadius = context.getResources().getInteger(R.integer.config_desktop_label_padding_radius);
			mPaddingH = context.getResources().getInteger(R.integer.config_desktop_label_padding_h);
			mPaddingV = context.getResources().getInteger(R.integer.config_desktop_label_padding_v);
		}


		// scale padding to mDisplay 
		final float scale = context.getResources().getDisplayMetrics().density;
		mCornerRadius *= scale;
		mPaddingH *= scale;
		mPaddingV *= scale;

		mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		//mPaint.setColor(getContext().getResources().getColor(R.color.bubble_dark_background));
		mPaint.setColor(colorBg);
	}

	@Override
	protected boolean setFrame(int left, int top, int right, int bottom) {
		if (mLeft != left || mRight != right || mTop != top || mBottom != bottom) {
			mBackgroundSizeChanged = true;
		}
		return super.setFrame(left, top, right, bottom);
	}

	@Override
	protected boolean verifyDrawable(Drawable who) {
		return who == mBackground || super.verifyDrawable(who);
	}

	@Override
	protected void drawableStateChanged() {
		Drawable d = mBackground;
		if (d != null && d.isStateful()) {
			d.setState(getDrawableState());
		}
		super.drawableStateChanged();
	}

	@Override
	public void draw(Canvas canvas) {
		final Drawable background = mBackground;
		if (background != null) {
			final int scrollX = mScrollX;
			final int scrollY = mScrollY;

			if (mBackgroundSizeChanged) {
				background.setBounds(0, 0,  mRight - mLeft, mBottom - mTop);
				mBackgroundSizeChanged = false;
			}

			if ((scrollX | scrollY) == 0) {
				background.draw(canvas);
			} else {
				canvas.translate(scrollX, scrollY);
				background.draw(canvas);
				canvas.translate(-scrollX, -scrollY);
			}
		}

		if(getText().length()>0){
			final Layout layout = getLayout();
			final RectF rect = mRect;
			final int left = getCompoundPaddingLeft();
			final int top = getExtendedPaddingTop();

			rect.set(left + layout.getLineLeft(0) - mPaddingH,
					top + layout.getLineTop(0) -  mPaddingV,
					Math.min(left + layout.getLineRight(0) + mPaddingH, getScrollX() + getRight() - getLeft()),
					top + layout.getLineBottom(0) + mPaddingV);
			canvas.drawRoundRect(rect, mCornerRadius, mCornerRadius, mPaint);
		}
		super.draw(canvas);
	}
}
