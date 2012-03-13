package com.wordpress.chislonchow.legacylauncher;

public interface Drawer {

	public int getVisibility();

	public boolean isOpaque();

	public boolean hasFocus();

	public boolean requestFocus();

	public void setTextFilterEnabled(boolean textFilterEnabled);
	public void clearTextFilter();


	public void setDragger(DragController dragger);
	public void setLauncher(Launcher launcher);
	public void updateAppGrp();
	public void setNumColumns(int numColumns);
	public void setNumRows(int numRows);
	public void setPageHorizontalMargin(int margin);
	public void setAdapter(ApplicationsAdapter adapter);
	public void setAnimationSpeed(int speed);
	public void open(boolean animate);
	public void close(boolean animate);
	public void setPadding(int left, int top, int right, int bottom);
	
	public void setSpeed(int value);
	public void setSnap(int value);

	public void switchGroups(Runnable switchGroups);
	
	public void setUngroupMode( boolean setUngroupMode );
	
    static final int ABS_SWIPE_MIN_DISTANCE = 120;
    static final int ABS_SWIPE_MAX_OFF_PATH = 100;
    static final int ABS_SWIPE_THRESHOLD_VELOCITY = 100;
}
