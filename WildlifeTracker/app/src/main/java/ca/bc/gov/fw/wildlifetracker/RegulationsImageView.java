package ca.bc.gov.fw.wildlifetracker;

import android.content.Context;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import it.sephiroth.android.library.imagezoom.ImageViewTouch;

public class RegulationsImageView extends ImageViewTouch {
	
	public RegulationsImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void zoomAndCenter(float desiredZoom, float centerX, float centerY) {
		zoomTo(desiredZoom);
		centerOn(centerX, centerY);
	}

	public RectF getBitmapRect() {
		return super.getBitmapRect();
	}

	public void centerOn( float x, float y ) {
        Drawable d = getDrawable();
        if (d == null) {
            return; //nothing to do
        }
	    //get the bitmap rect, which represents the rectangle of the view at the specified scale
	    //NOTE: this currently has no relation to the actual bitmap...we will provide that relation in this method
        RectF rect = getBitmapRect();
        float screenMidX = getWidth() / 2;
        float screenMidY = getHeight() / 2;
        //determine the centerX and centerY of the image on screen (these coordinates are scaled image coordinates)
        float viewCenterX  = -(rect.left - screenMidX);
        float viewCenterY  = -(rect.top  - screenMidY);
        
        //NOTE: postTranslate expects - numbers to pull the image right and + to pull it left
        float xp = viewCenterX - (x / d.getIntrinsicWidth())  * (rect.right - rect.left);
        //NOTE: postTranslate expects - numbers to pull the image down, and + to pull it up
        float yp = viewCenterY - (y / d.getIntrinsicHeight()) * (rect.bottom - rect.top);
	    postTranslate(xp, yp);
	}
}
