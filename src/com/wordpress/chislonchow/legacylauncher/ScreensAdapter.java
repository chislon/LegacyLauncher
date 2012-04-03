package com.wordpress.chislonchow.legacylauncher;

import java.util.ArrayList;
import java.util.Collections;

import com.wordpress.chislonchow.legacylauncher.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;

public class ScreensAdapter extends BaseAdapter {
	private Context mContext;
	private float mWidth;
	private float mHeight;
	private ArrayList<CellLayout> mScreens;

	public ScreensAdapter(Context c,int width,int height) {
		mContext = c;
		mWidth=width * .65f;
		mHeight=height * .65f;
	}

	public void addScreen(CellLayout screen){
		if(mScreens==null)
			mScreens=new ArrayList<CellLayout>();
		mScreens.add(screen);
		notifyDataSetChanged();
	}

	public void addScreen(CellLayout screen, int position){
		if(mScreens==null)
			mScreens=new ArrayList<CellLayout>();
		mScreens.add(position, screen);
		notifyDataSetChanged();
	}

	public void removeScreen(int position){
		if(mScreens==null)
			return;
		mScreens.remove(position);
		notifyDataSetChanged();
	}

	public void swapScreens(int a, int b){
		if(mScreens==null)
			return;
		Collections.swap(mScreens, a, b);
		notifyDataSetChanged();
	}

	public int getCount() {
		return mScreens.size();
	}

	public Object getItem(int position) {
		return position;
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {

		if(convertView==null) {
			convertView=new ImageView(mContext);
			((ImageView)convertView).setLayoutParams(new Gallery.LayoutParams((int)mWidth,(int)mHeight));
			((ImageView)convertView).setBackgroundResource(R.drawable.preview_bg);
			mScreens.get(position).setDrawingCacheEnabled(true);
			Bitmap b=mScreens.get(position).getDrawingCache(true);
			if(b!=null){
				((ImageView)convertView).setScaleType(ImageView.ScaleType.CENTER_INSIDE);
				((ImageView)convertView).setImageBitmap(b);
			}
		}

		return convertView;
	}
}
