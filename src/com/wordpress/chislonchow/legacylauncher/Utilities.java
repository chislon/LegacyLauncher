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
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;

/**
 * Various utilities shared amongst the Launcher's classes for drawing icon drawables, selectors
 */
final class Utilities {
	private static int sIconWidth = -1;
	private static int sIconHeight = -1;

	private static final Paint sPaint = new Paint();
	private static final Rect sBounds = new Rect();
	private static final Rect sOldBounds = new Rect();
	private static Canvas sCanvas = new Canvas();

	static {
		sCanvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.DITHER_FLAG,
				Paint.FILTER_BITMAP_FLAG));
	}

	/**
	 * Returns a Drawable representing the thumbnail of the specified Drawable.
	 * The size of the thumbnail is defined by the dimension
	 * android.R.dimen.launcher_application_icon_size.
	 * 
	 * This method is not thread-safe and should be invoked on the UI thread
	 * only.
	 * 
	 * @param icon
	 *            The icon to get a thumbnail of.
	 * @param context
	 *            The application's context.
	 * 
	 * @return A thumbnail for the specified icon or the icon itself if the
	 *         thumbnail could not be created.
	 */
	static Drawable createIconThumbnail(Drawable icon, Context context) {
		if (sIconWidth == -1) {
			final Resources resources = context.getResources();
			sIconWidth = sIconHeight = (int) resources
					.getDimension(android.R.dimen.app_icon_size);
		}

		int width = sIconWidth;
		int height = sIconHeight;

		if (icon instanceof PaintDrawable) {
			PaintDrawable painter = (PaintDrawable) icon;
			painter.setIntrinsicWidth(width);
			painter.setIntrinsicHeight(height);
		} else if (icon instanceof BitmapDrawable) {
			// Ensure the bitmap has a density.
			BitmapDrawable bitmapDrawable = (BitmapDrawable) icon;
			Bitmap bitmap = bitmapDrawable.getBitmap();
			if (bitmap.getDensity() == Bitmap.DENSITY_NONE) {
				bitmapDrawable.setTargetDensity(context.getResources()
						.getDisplayMetrics());
			}
		}
		int iconWidth = icon.getIntrinsicWidth();
		int iconHeight = icon.getIntrinsicHeight();

		if (width > 0 && height > 0) {
			if (width < iconWidth || height < iconHeight) {
				final float ratio = (float) iconWidth / iconHeight;
				// It's too big, scale it down.
				if (iconWidth > iconHeight) {
					height = (int) (width / ratio);
				} else if (iconHeight > iconWidth) {
					width = (int) (height * ratio);
				}

				final Bitmap.Config c = icon.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
						: Bitmap.Config.RGB_565;
				final Bitmap thumb = Bitmap.createBitmap(sIconWidth,
						sIconHeight, c);
				final Canvas canvas = sCanvas;
				canvas.setBitmap(thumb);
				// Copy the old bounds to restore them later
				// If we were to do oldBounds = icon.getBounds(),
				// the call to setBounds() that follows would
				// change the same instance and we would lose the
				// old bounds
				sOldBounds.set(icon.getBounds());
				final int x = (sIconWidth - width) / 2;
				final int y = (sIconHeight - height) / 2;
				icon.setBounds(x, y, x + width, y + height);
				icon.draw(canvas);
				icon.setBounds(sOldBounds);
				icon = new FastBitmapDrawable(thumb);
			} else if (iconWidth < width && iconHeight < height) {
				final Bitmap.Config c = Bitmap.Config.ARGB_8888;
				final Bitmap thumb = Bitmap.createBitmap(sIconWidth,
						sIconHeight, c);
				final Canvas canvas = sCanvas;
				canvas.setBitmap(thumb);
				sOldBounds.set(icon.getBounds());
				final int x = (width - iconWidth) / 2;
				final int y = (height - iconHeight) / 2;
				icon.setBounds(x, y, x + iconWidth, y + iconHeight);
				icon.draw(canvas);
				icon.setBounds(sOldBounds);
				icon = new FastBitmapDrawable(thumb);
			}
		}

		return icon;
	}

	/**
	 * Returns a Bitmap representing the thumbnail of the specified Bitmap. The
	 * size of the thumbnail is defined by the dimension
	 * android.R.dimen.launcher_application_icon_size.
	 * 
	 * This method is not thread-safe and should be invoked on the UI thread
	 * only.
	 * 
	 * @param bitmap
	 *            The bitmap to get a thumbnail of.
	 * @param context
	 *            The application's context.
	 * 
	 * @return A thumbnail for the specified bitmap or the bitmap itself if the
	 *         thumbnail could not be created.
	 */
	static Bitmap createBitmapThumbnail(Bitmap bitmap, Context context) {
		if (sIconWidth == -1) {
			final Resources resources = context.getResources();
			sIconWidth = sIconHeight = (int) resources
					.getDimension(android.R.dimen.app_icon_size);
		}

		int width = sIconWidth;
		int height = sIconHeight;

		final int bitmapWidth = bitmap.getWidth();
		final int bitmapHeight = bitmap.getHeight();

		if (width > 0 && height > 0) {
			if (width < bitmapWidth || height < bitmapHeight) {
				final float ratio = (float) bitmapWidth / bitmapHeight;

				if (bitmapWidth > bitmapHeight) {
					height = (int) (width / ratio);
				} else if (bitmapHeight > bitmapWidth) {
					width = (int) (height * ratio);
				}

				final Bitmap.Config c = (width == sIconWidth
						&& height == sIconHeight && bitmap.getConfig() != null) ? bitmap
								.getConfig() : Bitmap.Config.ARGB_8888;
								final Bitmap thumb = Bitmap.createBitmap(sIconWidth,
										sIconHeight, c);
								final Canvas canvas = sCanvas;
								final Paint paint = sPaint;
								canvas.setBitmap(thumb);
								paint.setDither(false);
								paint.setFilterBitmap(true);
								sBounds.set((sIconWidth - width) / 2,
										(sIconHeight - height) / 2, width, height);
								sOldBounds.set(0, 0, bitmapWidth, bitmapHeight);
								canvas.drawBitmap(bitmap, sOldBounds, sBounds, paint);
								return thumb;
			} else if (bitmapWidth < width || bitmapHeight < height) {
				final Bitmap.Config c = Bitmap.Config.ARGB_8888;
				final Bitmap thumb = Bitmap.createBitmap(sIconWidth,
						sIconHeight, c);
				final Canvas canvas = sCanvas;
				final Paint paint = sPaint;
				canvas.setBitmap(thumb);
				paint.setDither(false);
				paint.setFilterBitmap(true);
				canvas.drawBitmap(bitmap, (sIconWidth - bitmapWidth) / 2,
						(sIconHeight - bitmapHeight) / 2, paint);
				return thumb;
			}
		}

		return bitmap;
	}

	/**
	 * Create drawable scaled, optionally with tint (used for ActionButtons)
	 * 
	 * @param icon 
	 * @param context
	 * @param tint
	 * @param tintColor
	 * @param scaleFactor
	 * @return 
	 */
	static Drawable createScaledTintedDrawable(Drawable icon, Context context,
			boolean tint, int tintColor, float scaleFactor) {
		// if no tint is applied and scaling factor is 1, no change is needed, return the original
		if (scaleFactor == 1.0f && !tint) {
			return icon;
		}

		if (sIconWidth == -1) {
			final Resources resources = context.getResources();
			sIconWidth = sIconHeight = (int) resources.getDimension(android.R.dimen.app_icon_size);
		}

		int width = sIconWidth;
		int height = sIconHeight;
		Bitmap original;
		try {
			original = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		} catch (OutOfMemoryError e) {
			return icon;
		}
		Canvas canvas = new Canvas(original);
		canvas.setBitmap(original);
		icon.setBounds(0, 0, width, height);
		icon.draw(canvas);

		if (tint) {
			Paint paint = new Paint();
			LinearGradient shader = new LinearGradient(width / 2, 0, width / 2, height, 
					Color.argb(220, Color.red(tintColor), Color.green(tintColor), Color.blue(tintColor)), 
					Color.argb(50, Color.red(tintColor), Color.green(tintColor), Color.blue(tintColor)), 
					TileMode.CLAMP);
			paint.setShader(shader);
			paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
			canvas.drawRect(0, 0, width, height, paint);
		}

		try {
			Bitmap endImage = Bitmap.createScaledBitmap(original,
					(int) (width * scaleFactor), (int) (height * scaleFactor), true);
			original.recycle();
			return new FastBitmapDrawable(endImage);
		} catch (OutOfMemoryError e) {
			return icon;
		}
	}

	/**
	 * Create a colored linear gradient selector drawable for the input drawable 
	 * 
	 * @param icon
	 * @param color
	 * @return
	 */
	static Drawable createSelectorDrawable(Drawable icon, int color) {

		int height = icon.getIntrinsicHeight();
		int width = icon.getIntrinsicWidth();

		Bitmap original;
		try {
			original = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		} catch (OutOfMemoryError e) {
			return icon;
		}
		Canvas canvas = new Canvas(original);
		canvas.setBitmap(original);
		icon.setBounds(0, 0, width, height);
		icon.draw(canvas);

		Paint paint = new Paint();
		LinearGradient shader = new LinearGradient(width / 2, 0, width / 2, height, 
				Color.argb(200, Color.red(color), Color.green(color), Color.blue(color)), 
				Color.argb(100, Color.red(color), Color.green(color), Color.blue(color)), 
				TileMode.CLAMP);
		paint.setShader(shader);
		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		canvas.drawRect(0, 0, width, height, paint);

		try {
			return new FastBitmapDrawable(original);
		} catch (OutOfMemoryError e) {
			return icon;
		}
	}
}
