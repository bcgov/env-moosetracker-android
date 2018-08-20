package ca.bc.gov.fw.wildlifetracker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

public class RegulationsFragment extends Fragment implements RegulationsPageFragment.RegulationsPageFragmentContainer, OnPageChangeListener, RegulationsIndexDialogFragment.RegulationsIndexDialogListener {

    private static final String REGS_PDF_FILENAME = "regs_2018.pdf";

    public static File getRegulationsFile(Context context) throws IOException {
        File extDir = context.getExternalFilesDir(null);
        File tempFile = new File(extDir, RegulationsFragment.REGS_PDF_FILENAME);
        Log.i(MainActivity.LOG_TAG, "Regs temp file: " + tempFile);
        if (!tempFile.exists()) {
            Log.i(MainActivity.LOG_TAG, "Temp file does not exist - copying from assets");
            InputStream is = context.getAssets().open("regulations_2018_2020.pdf");
            FileOutputStream os = new FileOutputStream(tempFile);
            byte[] buffer = new byte[1024];
            int read;
            while((read = is.read(buffer)) != -1){
                os.write(buffer, 0, read);
            }
            is.close();
            os.close();
        }
        return tempFile;
    }

    public interface RegulationsFragmentContainer {
        void setRegulationsPageFragmentContainer(RegulationsPageFragment.RegulationsPageFragmentContainer container);
        RegulationsPageFragment.RegulationsPageFragmentContainer getRegulationsPageFragmentContainer();
    }

    private static boolean didShowDisclaimer__ = false;

    private RegulationsViewPager viewPager_;
	private RegulationsPagerAdapter pagerAdapter_;
	private RegulationsFragmentContainer fragmentContainer_;
	private HashMap<String, RegulationsPageFragment> activeFragments_ = new HashMap<>();
	private RegulationsPageFragment currentFragment_;
	private Toast toast_;
    private int pageToDisplay_ = -1;
    private Button pdfButton_;
	
	@SuppressLint("ShowToast")
	@Override
	public void onAttach(Context context) {
		if (!(context instanceof RegulationsFragmentContainer))
			throw new IllegalStateException("RegulationsFragment must be attached to RegulationsFragmentContainer");
        fragmentContainer_ = (RegulationsFragmentContainer) context;
        fragmentContainer_.setRegulationsPageFragmentContainer(this);
		toast_ = Toast.makeText(context, null, Toast.LENGTH_SHORT);
		super.onAttach(context);
	}

	@Override
	public void onDetach() {
        fragmentContainer_.setRegulationsPageFragmentContainer(null);
        fragmentContainer_ = null;
		super.onDetach();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View result = inflater.inflate(R.layout.regulations_pager, container, false);
		
		viewPager_ = (RegulationsViewPager) result.findViewById(R.id.regulationsPager);
        View noPdfView = result.findViewById(R.id.noInlinePdf);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            noPdfView.setVisibility(View.GONE);
            pagerAdapter_ = new RegulationsPagerAdapter(getChildFragmentManager());
            viewPager_.setAdapter(pagerAdapter_);
            viewPager_.addOnPageChangeListener(this);
        } else {
            viewPager_.setVisibility(View.GONE);
            pdfButton_ = (Button) result.findViewById(R.id.btnViewPdf);
        }

		return result;
	}

    @Override
    public void onDestroyView() {
        if (viewPager_ != null) {
            viewPager_.removeOnPageChangeListener(this);
        }
        super.onDestroyView();
    }

    @Override
	public void onResume() {
		super.onResume();
        if (pdfButton_ != null) {
            final Context context = getContext();
            final Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            boolean canView = false;
            try {
                File regsFile = RegulationsFragment.getRegulationsFile(context);
                intent.setDataAndType(Uri.fromFile(regsFile), "application/pdf");
                canView = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).size() > 0;
            } catch (IOException e) {
                Log.e(MainActivity.LOG_TAG, "Failed to check open PDF intent", e);
            }
            pdfButton_.setOnClickListener(null);
            pdfButton_.setVisibility(View.VISIBLE);
            if (canView) {
                pdfButton_.setText(R.string.open_button_title);
                pdfButton_.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        context.startActivity(intent);
                    }
                });
            } else {
                final Intent marketIntent = new Intent(Intent.ACTION_VIEW);
                marketIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                marketIntent.setData(Uri.parse("market://search?q=pdf&c=apps"));
                if (context.getPackageManager().queryIntentActivities(marketIntent, PackageManager.MATCH_DEFAULT_ONLY).size() > 0) {
                    pdfButton_.setText(R.string.install_button_title);
                    pdfButton_.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            context.startActivity(marketIntent);
                        }
                    });
                } else {
                    pdfButton_.setVisibility(View.INVISIBLE);
                }
            }
        } else if (pageToDisplay_ >= 0) {
			showPage(pageToDisplay_);
		}
		setupIndexButton(getUserVisibleHint());
    }

	public void showPage(int page) {
		if (!isResumed()) {
			pageToDisplay_ = page;
			return;
		}
		viewPager_.setCurrentItem(page - 1);
		pageToDisplay_ = -1;
	}

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (getActivity() == null)
            return;
        setupIndexButton(isVisibleToUser);
    }

    @Override
    public void indexPageSelected(int page) {
        showPage(page);
    }

    @Override
	public void registerPageFragment(String pageName, RegulationsPageFragment fragment) {
		boolean isCurrentPage = pageName.equals(pagerAdapter_.getPageTitle(viewPager_.getCurrentItem()));
		if (isCurrentPage)
			currentFragment_ = fragment;
		
		if (fragment == null) {
			activeFragments_.remove(pageName);
		} else {
			activeFragments_.put(pageName, fragment);
    		if (MainActivity.DEBUG)
    			Log.d(MainActivity.LOG_TAG, "registerPageFragment pageName=" + pageName + " current item=" + viewPager_.getCurrentItem());
			if (isCurrentPage) {
				showToast(viewPager_.getCurrentItem());
				fragment.loadHiResBitmap();
			}
		}
	}

	private void showToast(int pageNum) {
        // Don't need a toast to tell us we're looking at the cover
		if (pageNum != 0) {
			String pageNumStr = "Page " + pageNum;
            toast_.setText(pageNumStr);
            toast_.show();
		}
	}

	private void setupIndexButton(boolean visible) {
        ImageButton indexButton = (ImageButton) getActivity().findViewById(R.id.btnRegsIndex);
        if (indexButton != null) {
            boolean showIndex = visible;
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
                // Always hide index if we're not showing regulations pages in-app
                showIndex = false;
            }
            if (showIndex) {
                indexButton.setVisibility(View.VISIBLE);
                indexButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        RegulationsIndexDialogFragment frag = new RegulationsIndexDialogFragment();
                        frag.setStyle(DialogFragment.STYLE_NORMAL, R.style.MyListViewStyle);
                        frag.show(RegulationsFragment.this.getChildFragmentManager(), null);
                    }
                });
            } else {
                indexButton.setVisibility(View.GONE);
                indexButton.setOnClickListener(null);
            }
        }
        if (visible && !didShowDisclaimer__) {
            didShowDisclaimer__ = true;
            DialogFragment dialog = new RegulationsDisclaimerDialogFragment();
            dialog.show(getChildFragmentManager(), "RegulationsDisclaimerDialogFragment");
        }
    }

	@Override
	public void onPageScrollStateChanged(int arg0) {		
	}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {		
	}

	@Override
	public void onPageSelected(int newPage) {
		if (currentFragment_ != null)
			currentFragment_.onPageDeselected();
		String pageName = (String) pagerAdapter_.getPageTitle(newPage);
		currentFragment_ = activeFragments_.get(pageName);
		if (MainActivity.DEBUG)
			Log.d(MainActivity.LOG_TAG, "onPageSelected newPage=" + newPage + " new fragment=" + currentFragment_);
		if (currentFragment_ != null) {
			showToast(newPage);
			currentFragment_.loadHiResBitmap();
		}
	}
}
