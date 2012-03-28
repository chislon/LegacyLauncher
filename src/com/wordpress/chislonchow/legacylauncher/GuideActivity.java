package com.wordpress.chislonchow.legacylauncher;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;

public class GuideActivity extends Activity implements OnClickListener {

	private int mCurrentPage = 0;

	private ImageView mImageView;
	private TextView mTextView;
	private Button mButton;

	private static final int PAGE_NUMBER_LAST = 8;

	private int[] mGuideTitleIds = 
		{
			R.string.title_guide_0, 
			R.string.title_guide_1,
			R.string.title_guide_2,
			R.string.title_guide_3,
			R.string.title_guide_4,
			R.string.title_guide_5,
			R.string.title_guide_6,
			R.string.title_guide_7,
			R.string.title_guide_8
		};

	private int[] mGuideTextIds = 
		{
			R.string.text_guide_0,
			R.string.text_guide_1,
			R.string.text_guide_2,
			R.string.text_guide_3,
			R.string.text_guide_4,
			R.string.text_guide_5,
			R.string.text_guide_6,
			R.string.text_guide_7,
			R.string.text_guide_8
		};

	private int[] mGuideDrawableIds = 
		{
			R.drawable.ic_launcher_home, 
			R.drawable.screen_guide_1,
			R.drawable.screen_guide_2,
			R.drawable.screen_guide_3,
			R.drawable.screen_guide_4,
			R.drawable.screen_guide_5,
			R.drawable.screen_guide_6,
			R.drawable.screen_guide_7,
			0
		};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.guide);
		mButton = (Button) findViewById(R.id.button_dialog_guide_back);
		mButton.setOnClickListener(this);
		mButton = (Button) findViewById(R.id.button_dialog_guide_continue);
		mButton.setOnClickListener(this);
		mImageView = (ImageView) findViewById(R.id.dialog_guide_image);
		mTextView = (TextView) findViewById(R.id.dialog_guide_text);
		mTextView.setMovementMethod(LinkMovementMethod.getInstance());
		updateToPage(mCurrentPage);
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (!isFinishing()) {
			finish();
		}
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.button_dialog_guide_continue:
			mCurrentPage++;
			updateToPage(mCurrentPage);
			if (mCurrentPage > PAGE_NUMBER_LAST) {
				finish();
			}
			break;
		case R.id.button_dialog_guide_back:
			if (mCurrentPage > 0) {
				mCurrentPage --;
				updateToPage(mCurrentPage);
			} else {
				finish();
			}
			break;
		}
	}

	@Override
	public void onBackPressed() {
		if (mCurrentPage > 0) {
			mCurrentPage --;
			updateToPage(mCurrentPage);
		} else {
			finish();
		}
	}

	private void updateToPage(int page) {
		if (mCurrentPage == PAGE_NUMBER_LAST) {
			mButton.setText(R.string.button_finish);
		} else if (mCurrentPage > PAGE_NUMBER_LAST) {
			return;
		} else {
			mButton.setText(R.string.button_continue);
		}

		if (mGuideTitleIds[page] != 0) {
			setTitle(mGuideTitleIds[page]);
		} else {
			setTitle(R.string.application_name);
		}

		if (mGuideDrawableIds[page] != 0) {
			mImageView.setImageResource(mGuideDrawableIds[page]);
		} else {
			mImageView.setImageDrawable(null);
		}

		if (mGuideTextIds[page] != 0) {
			Spanned aboutText = Html.fromHtml(getString(mGuideTextIds[page], TextView.BufferType.SPANNABLE));
			mTextView.setText(aboutText);
		} else {
			mTextView.setText(null);
		}
	}
}