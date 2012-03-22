package com.wordpress.chislonchow.legacylauncher.ui;

import com.wordpress.chislonchow.legacylauncher.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class DialogSeekBarPreference extends DialogPreference implements
SeekBar.OnSeekBarChangeListener {
	private static final String androidns = "http://schemas.android.com/apk/res/android";

	private SeekBar mSeekBar;

	private TextView mValueText;

	private String mSuffix;

	private View mView;

	private int mMax, mMin, mValue = 0;

	final private static int DEFAULT_INTERVAL = 1;
	private int mInterval = DEFAULT_INTERVAL;
	private int mTmpValue;

	public DialogSeekBarPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setPersistent(true);

		mSuffix = attrs.getAttributeValue(androidns, "text");
		mMin = attrs.getAttributeIntValue(androidns, "min", 0);
		mMax = attrs.getAttributeIntValue(androidns, "max", 100);

		setDialogLayoutResource(R.layout.preference_seekbar_dialog);
	}

	@Override
	protected void onBindDialogView(View v) {
		super.onBindDialogView(v);
		TextView dialogMessage = (TextView) v.findViewById(R.id.dialogMessage);
		dialogMessage.setText(getDialogMessage());

		mValueText = (TextView) v.findViewById(R.id.actualValue);

		mSeekBar = (SeekBar) v.findViewById(R.id.myBar);
		mSeekBar.setOnSeekBarChangeListener(this);
		mSeekBar.setMax(mMax);
		mSeekBar.setProgress(mValue);

		if (mInterval > DEFAULT_INTERVAL) {
			mTmpValue = mValue;
		}
		String t = String.valueOf(mValue + mMin);
		mValueText.setText(mSuffix == null ? t : t.concat(mSuffix));
	}

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return a.getInt(index, 0);
	}

	@Override
	protected void onSetInitialValue(boolean restore, Object defaultValue) {
		mValue = getPersistedInt(defaultValue == null ? 0 : (Integer) defaultValue);
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);

		if (positiveResult) {
			int value = mSeekBar.getProgress();
			if (callChangeListener(value)) {
				setValue(value);
				updateWidgetFrameView(mView);
			}
		}
	}

	public void setValue(int value) {
		if (value > mMax) {
			value = mMax;
		} else if (value < 0) {
			value = 0;
		}
		mValue = value;
		persistInt(value);
	}

	public void setMax(int max) {
		mMax = max;
		if (mValue > mMax) {
			setValue(mMax);
		}
	}

	public void setMin(int min) {
		if (min < mMax) {
			mMin = min;
		}
	}

	public void onProgressChanged(SeekBar seek, int value, boolean fromTouch) {
		// interval logic		
		if(mInterval > DEFAULT_INTERVAL) {
			value = Math.round(((float)value)/mInterval) * mInterval;
			// update value if it changed
			if (mTmpValue != value) {
				mTmpValue = value;	// save value to snap to when we let go
				String t = String.valueOf(value + mMin);
				mValueText.setText(mSuffix == null ? t : t.concat(mSuffix));
			}
		} else {
			String t = String.valueOf(value + mMin);
			mValueText.setText(mSuffix == null ? t : t.concat(mSuffix));
		}
	}

	public void onStartTrackingTouch(SeekBar seek) {
	}

	public void onStopTrackingTouch(SeekBar seek) {
		// keep seekbar in sync with value stored
		if(mInterval > DEFAULT_INTERVAL) {
			mSeekBar.setProgress(mTmpValue);	// snap to last progress we know of
		}
	}

	public boolean setInterval(int interval) {
		// only allow intervals of 1 or greater
		if (interval > 0) {
			mInterval = interval;
			return true; 
		}
		return false;
	}

	@Override
	protected void onBindView(View view) {
		super.onBindView(view);
		updateWidgetFrameView(view);
	}

	private void updateWidgetFrameView(View view) {
		if (view == null) return;
		mView = view;

		// prepare widget frame
		LinearLayout widgetFrameView = ((LinearLayout)view.findViewById(android.R.id.widget_frame));
		if (widgetFrameView == null) return;

		// remove already created widgets
		int count = widgetFrameView.getChildCount();
		if (count > 0) {
			widgetFrameView.removeViews(0, count);
		}

		// add our view only if the preference is active
		if (isEnabled()) {
			widgetFrameView.setVisibility(View.VISIBLE);
			final boolean preApi14 = android.os.Build.VERSION.SDK_INT < 14;
			final int rightPadding = (int) (getContext().getResources().getDisplayMetrics().density * (preApi14 ? 10 : 7));
			widgetFrameView.setPadding(
					widgetFrameView.getPaddingLeft(),
					widgetFrameView.getPaddingTop(),
					rightPadding,
					widgetFrameView.getPaddingBottom()
					);

			TextView textView = new TextView(getContext());
			textView.setText(Integer.toString(mValue + mMin));
			textView.setTextAppearance(getContext(), android.R.style.TextAppearance_Medium);
			textView.setTypeface(null, Typeface.BOLD);
			widgetFrameView.addView(textView);
		}
	}
}
