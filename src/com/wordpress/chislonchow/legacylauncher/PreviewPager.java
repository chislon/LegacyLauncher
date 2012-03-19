package com.wordpress.chislonchow.legacylauncher;

import com.wordpress.chislonchow.legacylauncher.catalogue.AppCatalogueFilters;
import com.wordpress.chislonchow.legacylauncher.catalogue.AppGroupAdapter;
import com.wordpress.chislonchow.legacylauncher.R;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.TransitionDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class PreviewPager extends ViewGroup {
	private int mTotalItems;
	private int mCurrentItem;
	private int mDotDrawableId;
	private int mTextSize = -1;
	
	private boolean mEnableGroupText;
    private boolean isUngroupMode = false;
    private boolean mShowGroupText;
	
	public PreviewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		initPager();
	}

	public PreviewPager(Context context) {
		super(context);
		initPager();
		// TODO Auto-generated constructor stub
	}
	private void initPager(){
		setFocusable(false);
		setWillNotDraw(false);
		mDotDrawableId=R.drawable.pager_dots;
	}
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		if(mTotalItems<=0) return;
		createLayout();
	}
	private void updateLayout(){
		int offset = 0;
		for(int i=0;i<getChildCount();i++){
			View child = getChildAt(i);
			if ( child instanceof ImageView )
			{
				TransitionDrawable tmp=(TransitionDrawable)((ImageView) child).getDrawable();
				if(i-offset==mCurrentItem){
					tmp.startTransition(50);
				}else{
					tmp.resetTransition();
				}
			}
			else
			{
				offset ++;
			}
		}
	}
	
	private void createLayout(){
		detachAllViewsFromParent();
    	//ADW: Load the specified theme
    	String themePackage=MyLauncherSettingsHelper.getThemePackageName(getContext(), Launcher.THEME_DEFAULT);
    	PackageManager pm=getContext().getPackageManager();
    	Resources themeResources=null;
    	if(!themePackage.equals(Launcher.THEME_DEFAULT)){
	    	try {
				themeResources=pm.getResourcesForApplication(themePackage);
			} catch (NameNotFoundException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
    	}
    	int resource_id=0;
		if(themeResources!=null){
			resource_id=themeResources.getIdentifier ("pager_dots", "drawable", themePackage);
		}
		
		int dotWidth=getResources().getDrawable(mDotDrawableId).getIntrinsicWidth();
		int separation=dotWidth;
		int marginLeft=((getWidth())/2)-(((mTotalItems*dotWidth)/2)+(((mTotalItems-1)*separation)/2));
		int dotMarginTop=((getHeight())/2)-(dotWidth/2);

		TextView groupName = null;
		if ( mEnableGroupText && mShowGroupText )
		{
			String groupTitle = getCurrentTitle();
			if ( groupTitle != null )
			{
				groupName=new TextView(getContext());
				groupName.setText(groupTitle);
				
		        ViewGroup.LayoutParams p;
		        p = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
		        		ViewGroup.LayoutParams.WRAP_CONTENT);
		        groupName.setLayoutParams(p);
				
		        groupName.getPaint().setAntiAlias(true);
				int textSize = mTextSize;
				if ( textSize == -1 ) {
					if ( getHeight() > 0 ) {
						// start here
						textSize = 12;
						groupName.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
						
						int targetSize = getHeight() - (getHeight() - dotWidth)/2;
						int textHeight = 99999;
						while ( textHeight > targetSize)
						{
							groupName.setTextSize(TypedValue.COMPLEX_UNIT_SP, --textSize);
							textHeight = (int) groupName.getPaint().getTextSize();
							if ( textSize < 7 )	// 7 dip
							{
								textSize = 7;
								break;
							}
						}
						mTextSize = textSize;
					}
				}
				
				groupName.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
				int textWidth = (int) groupName.getPaint().measureText(groupTitle);
				marginLeft -= ( textWidth / 2); 
				
	            int childHeightSpec = getChildMeasureSpec(
	                    MeasureSpec.makeMeasureSpec(dotWidth, MeasureSpec.UNSPECIFIED), 0, p.height);
	            int childWidthSpec = getChildMeasureSpec(
	                    MeasureSpec.makeMeasureSpec(textWidth, MeasureSpec.EXACTLY), 0, p.width);
	            groupName.measure(childWidthSpec, childHeightSpec);
				
	            int left=marginLeft;
	            
	            groupName.layout(left, 0, left+textWidth,getHeight() );
	            addViewInLayout(groupName, getChildCount(), p, true);
	            
	            marginLeft += textWidth+separation;
			}
		}
		
		for(int i=0;i<mTotalItems;i++){
			ImageView dot=new ImageView(getContext());
			TransitionDrawable td;
			if(themeResources!=null && resource_id!=0){
				td=(TransitionDrawable)themeResources.getDrawable(resource_id);
			}else{
				td=(TransitionDrawable)getResources().getDrawable(mDotDrawableId);
			}
			td.setCrossFadeEnabled(true);
			dot.setImageDrawable(td);
	        ViewGroup.LayoutParams p;
	        p = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
	        		ViewGroup.LayoutParams.FILL_PARENT);
            dot.setLayoutParams(p);
            int childHeightSpec = getChildMeasureSpec(
                    MeasureSpec.makeMeasureSpec(dotWidth, MeasureSpec.UNSPECIFIED), 0, p.height);
            int childWidthSpec = getChildMeasureSpec(
                    MeasureSpec.makeMeasureSpec(dotWidth, MeasureSpec.EXACTLY), 0, p.width);
            dot.measure(childWidthSpec, childHeightSpec);
			
            int left=marginLeft+(i*(dotWidth+separation));
            
            
			dot.layout(left, dotMarginTop, left+dotWidth,dotMarginTop+dotWidth );
            addViewInLayout(dot, getChildCount(), p, true);
            if(i==mCurrentItem){
            	TransitionDrawable tmp=(TransitionDrawable)dot.getDrawable();
            	tmp.startTransition(200);
            }
		}
		if (groupName != null )
		{
		    groupName.bringToFront();
		}
		postInvalidate();
	}

	private String getCurrentTitle()
	{
		String groupTitle = null;
		final ApplicationsAdapter drawerAdapter = Launcher.getModel().getApplicationsAdapter();
		if (drawerAdapter != null)
		{
		    int index = drawerAdapter.getCatalogueFilter().getCurrentFilterIndex();
		    int title = isUngroupMode?R.string.app_group_un:R.string.app_group_all;
		    groupTitle = (index ==  AppGroupAdapter.APP_GROUP_ALL ?
		    		getContext().getString(title) :
		    		AppCatalogueFilters.getInstance().getGroupTitle(index));
		}
		return groupTitle;
	}
	protected int getTotalItems() {
		return mTotalItems;
	}

	protected void setTotalItems(int totalItems) {
		if(totalItems!=mTotalItems || mEnableGroupText){
			this.mTotalItems = totalItems;
			createLayout();
		}
	}

	protected int getCurrentItem() {
		return mCurrentItem;
	}

	protected void setCurrentItem(int currentItem) {
		if(currentItem!=mCurrentItem){
			this.mCurrentItem = currentItem;
			updateLayout();
		}
	}
	protected void setPagerLeft(int value){
		int width=this.mRight-this.mLeft;
		this.mLeft=value;
		this.mRight=this.mLeft+width;
	}

	public void enableGroupText(boolean show)
	{
		mEnableGroupText = show;
	}

    public void setUngroupMode(boolean setUngroupMode)
    {
        isUngroupMode = setUngroupMode;
    }
    
    public void showGroupText( boolean show )
    {
        mShowGroupText = show;
    }

}
