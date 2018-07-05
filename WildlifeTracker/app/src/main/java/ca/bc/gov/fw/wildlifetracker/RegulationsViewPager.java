package ca.bc.gov.fw.wildlifetracker;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;
import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.View;

public class RegulationsViewPager extends ViewPager {

	public RegulationsViewPager(Context context) {
		super(context);
	}

	public RegulationsViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected boolean canScroll(View v, boolean checkV, int dx, int x, int y) {
		if (v instanceof ImageViewTouch) {
			return ((ImageViewTouch) v).canScroll(dx);
		}
		return super.canScroll(v, checkV, dx, x, y);
	}

}
