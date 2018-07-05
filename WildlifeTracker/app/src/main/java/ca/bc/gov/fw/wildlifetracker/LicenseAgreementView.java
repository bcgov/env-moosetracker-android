package ca.bc.gov.fw.wildlifetracker;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Display;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Button;

public class LicenseAgreementView extends WebView {

    private Context context;
    private Button agreementButton;

    public LicenseAgreementView (Context context) {
	super (context);
    }
    public LicenseAgreementView (Context context, AttributeSet attrs) {
	super (context, attrs);
    }
    public LicenseAgreementView(Context context, AttributeSet attrs,
	    int defStyle) {
	super(context, attrs, defStyle);
    }

    public void setContext(Context context) {
	this.context = context;
    }
    
    public void setButton(Button button) {
	this.agreementButton = button;
    }

    @Override
    public void onScrollChanged(int l, int t, int oldl, int oldt) {
//	System.out.println("Overriden on scroll changed method!");
	 //View view = (View) getChildAt(getChildCount()-1);
//	System.out.println("Top: " + this.getTop());
//	System.out.println("Bottom" + this.getBottom());
	System.out.println("height: "+ getHeight());
	System.out.println("Content height: " + getContentHeight());
	System.out.println("scroll y: " + getScrollY());

	System.out.println("T: " + t);
//	System.out.println("old T: " + oldt);
//	 int diff = (this.getTop()-(getHeight()+getScrollY()));// Calculate the scrolldiff
//	 System.out.println("Diff: "+diff);
	int contentHeight = this.getContentHeight();
	//Display display = getWindowToken().getDefaultDisplay();
	WindowManager wm = (WindowManager) this.context.getSystemService(Context.WINDOW_SERVICE);
	Display display = wm.getDefaultDisplay();
	int screenHeight = display.getHeight();
	int viewHeight = getHeight();
	int difference = screenHeight - viewHeight;
	System.out.println("Difference: " + difference);
	System.out.println("Screenheight: " + screenHeight);
	System.out.println("ViewHeight: " + viewHeight);
	//normally would be t + viewheight, but need to subtract differnece because
	//for some reason, android takes the entire screen for t instead of just for the
	//part of the screen that is filled by the view
	 if( t + viewHeight +20  >= contentHeight ){ //for some reason, the origin is different on different phones, so add 20 random pixel
	     //so that if the origin is too low, it will stil make hte button pop up.
	     // if diff is zero, then the bottom has been reached
	     //O dear this is horrid
	     System.out.println("MyScrollView: Bottom has been reached" );
	     //Button agreementButton = (Button) findViewById (R.id.licenseAgreeButton);
	     this.agreementButton.setVisibility(VISIBLE);
	 }
	super.onScrollChanged(l, t, oldl, oldt);
	//do here to check if overscrolled

    }

    public void onOverScrolled(int scrollX, int scrollY, boolean clampedX,
	    boolean clampedY) {
	//make visible
	this.agreementButton.setVisibility(VISIBLE);

    }
    

}
