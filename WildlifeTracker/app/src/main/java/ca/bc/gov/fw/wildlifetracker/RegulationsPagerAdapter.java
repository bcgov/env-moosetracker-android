package ca.bc.gov.fw.wildlifetracker;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.ArrayList;

public class RegulationsPagerAdapter extends FragmentStatePagerAdapter {

//	private AssetManager assetManager_;
	private ArrayList<String> pages_ = new ArrayList<>();
	
	public RegulationsPagerAdapter(FragmentManager fm) {
		super(fm);
        for (int i = 0; i < 97; i++) {
            pages_.add(String.format("page-%03d", i));
        }
        /*
		assetManager_ = assetManager;
		try {
			String[] files = assetManager_.list("regulations");
			for (String file : files) {
//				int where = file.indexOf(".png");
//				if (where < 0)
//					throw new IOException("Invalid filename: " + file);
//				pages_.add(file.substring(0, where));
				pages_.add(file);
			}
		} catch (IOException e) {
			if (MainActivity.DEBUG)
    			Log.e(MainActivity.LOG_TAG, "Failed to load regulations page list ", e);
		}
		*/
	}

	@Override
	public Fragment getItem(int position) {
		RegulationsPageFragment frag = new RegulationsPageFragment();
		Bundle args = new Bundle();
		args.putString("page", pages_.get(position));
        args.putInt("pageNumber", position);
		frag.setArguments(args);
		return frag;
	}

	@Override
	public int getCount() {
		return pages_.size();
	}

	@Override
	public CharSequence getPageTitle(int position) {
		return pages_.get(position);
	}

}
