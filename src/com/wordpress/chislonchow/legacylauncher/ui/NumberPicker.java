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

package com.wordpress.chislonchow.legacylauncher.ui;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.wordpress.chislonchow.legacylauncher.R;

/**
 * This class has been pulled from the Android platform source code, its an internal widget that hasn't been
 * made public so its included in the project in this fashion for use with the preferences screen; I have made
 * a few slight modifications to the code here, I simply put a MAX and MIN default in the code but these values
 * can still be set publically by calling code.
 *
 * @author Google
 */
public class NumberPicker extends LinearLayout implements OnClickListener, OnLongClickListener {

	private static final int DEFAULT_MAX = 200;
	private static final int DEFAULT_MIN = 0;

	public interface OnChangedListener {
		void onChanged(NumberPicker picker, int oldVal, int newVal);
	}

	public interface Formatter {
		String toString(int value);
	}

	private final Handler mHandler;
	private final Runnable mRunnable = new Runnable() {
		public void run() {
			if (mIncrement) {
				changeCurrent(mCurrent + 1);
				mHandler.postDelayed(this, mSpeed);
			} else if (mDecrement) {
				changeCurrent(mCurrent - 1);
				mHandler.postDelayed(this, mSpeed);
			}
		}
	};

	private final EditText mText;

	private String[] mDisplayedValues;
	protected int mStart;
	protected int mEnd;
	protected int mCurrent;
	protected int mPrevious;
	private OnChangedListener mListener;
	private Formatter mFormatter;
	private long mSpeed = 300;

	private boolean mIncrement;
	private boolean mDecrement;

	public NumberPicker(Context context) {
		this(context, null);
	}

	public NumberPicker(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public NumberPicker(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs);
		setOrientation(VERTICAL);
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.number_picker, this, true);
		mHandler = new Handler();

		mIncrementButton = (NumberPickerButton) findViewById(R.id.increment);
		mIncrementButton.setOnClickListener(this);
		mIncrementButton.setOnLongClickListener(this);
		mIncrementButton.setNumberPicker(this);
		mDecrementButton = (NumberPickerButton) findViewById(R.id.decrement);
		mDecrementButton.setOnClickListener(this);
		mDecrementButton.setOnLongClickListener(this);
		mDecrementButton.setNumberPicker(this);

		mText = (EditText) findViewById(R.id.timepicker_input);
		mText.setCursorVisible(false);
		mText.setFocusable(false);
		mText.setFocusableInTouchMode(false);

		if (!isEnabled()) {
			setEnabled(false);
		}

		mStart = DEFAULT_MIN;
		mEnd = DEFAULT_MAX;
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		mIncrementButton.setEnabled(enabled);
		mDecrementButton.setEnabled(enabled);
		mText.setEnabled(enabled);
	}

	public void setOnChangeListener(OnChangedListener listener) {
		mListener = listener;
	}

	public void setFormatter(Formatter formatter) {
		mFormatter = formatter;
	}

	/**
	 * Set the range of numbers allowed for the number picker. The current
	 * value will be automatically set to the start.
	 *
	 * @param start the start of the range (inclusive)
	 * @param end the end of the range (inclusive)
	 */
	public void setRange(int start, int end) {
		mStart = start;
		mEnd = end;
		mCurrent = start;
		updateView();
	}

	/**
	 * Set the range of numbers allowed for the number picker. The current
	 * value will be automatically set to the start. Also provide a mapping
	 * for values used to mDisplay to the user.
	 *
	 * @param start the start of the range (inclusive)
	 * @param end the end of the range (inclusive)
	 * @param displayedValues the values displayed to the user.
	 */
	public void setRange(int start, int end, String[] displayedValues) {
		mDisplayedValues = displayedValues;
		mStart = start;
		mEnd = end;
		mCurrent = start;
		updateView();
	}

	public void setCurrent(int current) {
		mCurrent = current;
		updateView();
	}

	/**
	 * The speed (in milliseconds) at which the numbers will scroll
	 * when the the +/- buttons are longpressed. Default is 300ms.
	 */
	public void setSpeed(long speed) {
		mSpeed = speed;
	}

	public void onClick(View v) {
		// now perform the increment/decrement
		if (R.id.increment == v.getId()) {
			changeCurrent(mCurrent + 1);
		} else if (R.id.decrement == v.getId()) {
			changeCurrent(mCurrent - 1);
		}
	}

	private String formatNumber(int value) {
		return (mFormatter != null)
				? mFormatter.toString(value)
						: String.valueOf(value);
	}

	protected void changeCurrent(int current) {

		// Wrap around the values if we go past the start or end
		if (current > mEnd) {
			current = mStart;
		} else if (current < mStart) {
			current = mEnd;
		}
		mPrevious = mCurrent;
		mCurrent = current;

		notifyChange();
		updateView();
	}

	protected void notifyChange() {
		if (mListener != null) {
			mListener.onChanged(this, mPrevious, mCurrent);
		}
	}

	protected void updateView() {

		/* If we don't have displayed values then use the
		 * current number else find the correct value in the
		 * displayed values for the current number.
		 */
		if (mDisplayedValues == null) {
			mText.setText(formatNumber(mCurrent));
		} else {
			mText.setText(mDisplayedValues[mCurrent - mStart]);
		}
	}

	/**
	 * We start the long click here but rely on the {@link NumberPickerButton}
	 * to inform us when the long click has ended.
	 */
	public boolean onLongClick(View v) {

		if (R.id.increment == v.getId()) {
			mIncrement = true;
			mHandler.post(mRunnable);
		} else if (R.id.decrement == v.getId()) {
			mDecrement = true;
			mHandler.post(mRunnable);
		}
		return true;
	}

	public void cancelIncrement() {
		mIncrement = false;
	}

	public void cancelDecrement() {
		mDecrement = false;
	}

	private NumberPickerButton mIncrementButton;
	private NumberPickerButton mDecrementButton;

	/**
	 * @return the current value.
	 */
	public int getCurrent() {
		return mCurrent;
	}
}
