package ca.bc.gov.fw.wildlifetracker;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.pdf.PdfRenderer;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

import it.sephiroth.android.library.imagezoom.ImageViewTouchBase.DisplayType;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase.OnDrawableChangeListener;

public class RegulationsPageFragment extends Fragment implements OnDrawableChangeListener {

	public interface RegulationsPageFragmentContainer {
        void registerPageFragment(String pageName, RegulationsPageFragment fragment);
	}

	private static final float PDF_PAGE_SIZE_X = 585.0f;
	private static final float PDF_PAGE_SIZE_Y = 756.0f;
	
	private String pageName_;
    private int pageNumber_;
	private RegulationsImageView imageView_;
	private Bitmap bitmap_;
	private Rect zoomRect_ = null;
	private boolean zoomFitMax_ = false; // If true, in zoomToRect, fit the maximum dimension of the rect.
	private RegulationsFragment.RegulationsFragmentContainer fragmentContainer_;
	private boolean hiResAvailable_;
	private RegImageLoader loader_ = new RegImageLoader();
	
	@Override
	public void onAttach(Context context) {
		if (!(context instanceof RegulationsFragment.RegulationsFragmentContainer))
			throw new IllegalStateException("RegulationsPageFragment must be attached to a RegulationsFragmentContainer");
        fragmentContainer_ = (RegulationsFragment.RegulationsFragmentContainer) context;
		hiResAvailable_ = false;
		super.onAttach(context);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Bundle args = getArguments();
		pageName_ = args.getString("page");
        pageNumber_ = args.getInt("pageNumber");
		if (savedInstanceState != null) {
			zoomRect_ = savedInstanceState.getParcelable("zoomRect");
			// If true, we want to fit the *maximum* dimension of the zoom rect,
			// such as the case when initially zooming to a particular rect via a PDFLink.
			// On rotation, we want to fit the *minimum* so the perceived zoom level stays roughly the same.
			zoomFitMax_ = savedInstanceState.getBoolean("zoomFitMax", false);
		}
		if (MainActivity.DEBUG)
			Log.d(MainActivity.LOG_TAG, "RegulationsPageFragment " + pageName_ + " onCreate saved zoomRect=" +
					zoomRect_ + ", zoomFitMax_=" + zoomFitMax_);
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		if ((zoomRect_ == null) && (imageView_ != null) && hiResAvailable_) {
			Matrix invert = new Matrix();
			if (imageView_.getImageViewMatrix().invert(invert)) {
				RectF bitmapRect = imageView_.getBitmapRect();
				if (bitmapRect != null) {
					float bitmapScaleX = PDF_PAGE_SIZE_X / (bitmapRect.right - bitmapRect.left);
					float bitmapScaleY = PDF_PAGE_SIZE_Y / (bitmapRect.bottom - bitmapRect.top);
					float viewCenterX = ((float) imageView_.getWidth()) / 2.0f;
					float viewCenterY = ((float) imageView_.getHeight()) / 2.0f;
					float centerX = (viewCenterX - bitmapRect.left) * bitmapScaleX;
					float centerY = (viewCenterY - bitmapRect.top) * bitmapScaleY;
					float squareHalfSize = Math.min(((float)imageView_.getWidth()) * bitmapScaleX, ((float)imageView_.getHeight()) * bitmapScaleY) / 2.0f;
					zoomRect_ = new Rect((int)(centerX - squareHalfSize),
							(int)(centerY - squareHalfSize),
							(int)(centerX + squareHalfSize),
							(int)(centerY + squareHalfSize));
				}
			}
		}
		if (zoomRect_ != null) {
    		if (MainActivity.DEBUG)
    			Log.d(MainActivity.LOG_TAG, pageName_ + " saving zoomRect_ = " + zoomRect_ + ", zoomFitMax_=false");
			// Set this to false to ensure the view is preserved correctly on rotation.
			outState.putBoolean("zoomFitMax", false);
			outState.putParcelable("zoomRect", zoomRect_);
		}
		
		super.onSaveInstanceState(outState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		imageView_ = new RegulationsImageView(inflater.getContext(), null);
//		imageView_.setId(42);
//		AssetManager assets = getActivity().getAssets();
		if (MainActivity.DEBUG)
			Log.d(MainActivity.LOG_TAG, "RegulationsPageFragment " + pageName_ + " creating lo-res bitmap for page " + pageNumber_);
		if (bitmap_ != null)
			bitmap_.recycle();
		bitmap_ = getThumbnail(getActivity(), pageNumber_, true);
/*
        BitmapFactory.Options options = new BitmapFactory.Options();
		options.inSampleSize = 4;
		options.inPurgeable = true;
		options.inInputShareable = true;
		options.inPreferredConfig = Bitmap.Config.RGB_565;
		try {
			InputStream is = assets.open("regulations/" + pageName_);
			bitmap_ = BitmapFactory.decodeStream(is, null, options);
		} catch (IOException e) {
    		if (MainActivity.DEBUG)
    			Log.e(MainActivity.LOG_TAG, "Failed to load lo-res regulations page bitmap for " + pageName_, e);
		}
		*/
		if (bitmap_ != null) {
			imageView_.setDisplayType(DisplayType.FIT_TO_SCREEN);
			imageView_.setImageBitmap(bitmap_);
		}
		
		// Bitmap doesn't actually get set until the view receives an onLayout(). So we need a callback.
		imageView_.setOnDrawableChangedListener(this);
		
		return imageView_;
	}

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void createThumbnails(Activity activity) {
        ParcelFileDescriptor fd = null;
        PdfRenderer renderer = null;
        try {
            File tempFile = RegulationsFragment.getRegulationsFile(activity);
            fd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY);
            renderer = new PdfRenderer(fd);
        } catch (IOException e) {
            Log.e(MainActivity.LOG_TAG, "Failed to open regulations PDF", e);
        }
        if (renderer != null) {
            int pageCount = renderer.getPageCount();
            renderer.close();
            Log.d(MainActivity.LOG_TAG, "Check and generate thumbnails for " + pageCount + " pages");
            for (int i = 0; i < pageCount; i++) {
                getThumbnail(activity, i, false);
            }
            Log.d(MainActivity.LOG_TAG, "Done generating thumbnails");
        } else if (fd != null) {
            try {
                fd.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

	private static Bitmap getThumbnail(Activity activity, int pageNumber, boolean loadImage) {
        File cacheDir = activity.getExternalCacheDir();
        File thumbnailsDir = null;
        if (cacheDir != null) {
            thumbnailsDir = new File(cacheDir, "thumbnails");
            if (!(thumbnailsDir.mkdirs() || thumbnailsDir.isDirectory())) {
                Log.e(MainActivity.LOG_TAG, "Failed to create thumbnails directory");
                thumbnailsDir = null;
            }
            if ((thumbnailsDir != null) && !thumbnailsDir.canWrite()) {
                Log.e(MainActivity.LOG_TAG, "Thumbnails cache dir is not writable");
                thumbnailsDir = null;
            }
        }
        File thumbFile = null;
        Bitmap image = null;
        if (thumbnailsDir != null) {
            // Got a thumbnails dir. Check for existing thumbnail image first.
            thumbFile = new File(thumbnailsDir, "thumb-" + pageNumber + ".png");
            if (thumbFile.canRead()) {
                if (!loadImage) {
                    Log.d(MainActivity.LOG_TAG, "Readable thumbnail file exists " + thumbFile);
                    return null;
                }
                image = BitmapFactory.decodeFile(thumbFile.getAbsolutePath());
                if (image != null) {
                    Log.d(MainActivity.LOG_TAG, "Loaded thumbnail from disk for regs page " + pageNumber);
                }
            }
        }

        if (image == null) {
            // No thumbnails dir or existing thumbnail file on disk, so generate one
            Log.d(MainActivity.LOG_TAG, "Generating thumbnail for regs page " + pageNumber);
            image = loadBitmap(false, pageNumber, activity);
            if ((image != null) && (thumbFile != null) && (!thumbFile.exists() || thumbFile.canWrite())) {
                // We have a bitmap and a writable path to store it
                FileOutputStream out = null;
                try {
                    // Add a loop for the unlikely case that a temp file already exists
                    Random r = new Random(System.currentTimeMillis());
                    int suffix = Math.abs(r.nextInt());
                    File tempThumbFile;
                    do {
                        tempThumbFile = new File(thumbnailsDir, "temp" + suffix + ".png");
                        suffix = Math.abs(r.nextInt());
                    } while (tempThumbFile.exists());
                    out = new FileOutputStream(tempThumbFile);
                    if (!image.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                        Log.e(MainActivity.LOG_TAG, "Failed to compress thumbnail to file " + thumbFile);
                    } else if (!tempThumbFile.renameTo(thumbFile)) {
                        Log.e(MainActivity.LOG_TAG, "Failed to rename temp thumbnail file " + tempThumbFile + " to " + thumbFile);
                    } else {
                        Log.d(MainActivity.LOG_TAG, "Created thumbnail file " + thumbFile);
                    }
                } catch (FileNotFoundException e) {
                    Log.e(MainActivity.LOG_TAG, "Failed to open temp thumbnail file for writing", e);
                } finally {
                    try {
                        if (out != null) {
                            out.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return image;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static Bitmap loadBitmap(boolean hiRes, int pageNumber, Activity activity) {
        Bitmap bitmap = null;
        ParcelFileDescriptor fd;
        PdfRenderer renderer = null;
        try {
            File tempFile = RegulationsFragment.getRegulationsFile(activity);
            fd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY);
            renderer = new PdfRenderer(fd);
        } catch (IOException e) {
            Log.e(MainActivity.LOG_TAG, "Failed to open regulations PDF", e);
        }
        if (renderer != null) {
            PdfRenderer.Page page = renderer.openPage(pageNumber);
            float scale;
            if (hiRes) {
                ActivityManager am = (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
                int memoryClass = am.getMemoryClass();
                Log.i(MainActivity.LOG_TAG, "Device memory class: " + memoryClass);
                if (memoryClass >= 256) {
                    scale = 4.0f;
                } else if (memoryClass >= 64) {
                    scale = 3.0f;
                } else {
                    scale = 2.0f;
                }
            } else {
                float scaleX = 200.0f / (float) page.getWidth();
                float scaleY = 300.0f / (float) page.getHeight();
                scale = scaleX < scaleY ? scaleX : scaleY;
            }
            bitmap = Bitmap.createBitmap((int)(scale * (float)page.getWidth()),
                    (int)(scale * (float)page.getHeight()),
                    Bitmap.Config.ARGB_8888);
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            page.close();
            renderer.close();
        }
        return bitmap;
    }

    @Override
	public void onDestroyView() {
		loader_.cancel();
		if (bitmap_ != null) {
    		if (MainActivity.DEBUG)
    			Log.d(MainActivity.LOG_TAG, "RegulationsPageFragment " + pageName_ + " recycling bitmap");
			bitmap_.recycle();
			bitmap_ = null;
		}
		hiResAvailable_ = false;
		imageView_ = null;
		super.onDestroyView();
	}

	@Override
	public void onResume() {
		super.onResume();
		RegulationsPageFragmentContainer viewer = fragmentContainer_.getRegulationsPageFragmentContainer();
		if (viewer == null) {
			// Should never happen - if we're active, our parent RegulationsFragment should be registered
			throw new IllegalStateException("RegulationsPageFragmentContainer is null");
		}
		viewer.registerPageFragment(pageName_, this);
	}

	@Override
	public void onPause() {
		loader_.cancel();
		super.onPause();
		RegulationsPageFragmentContainer viewer = fragmentContainer_.getRegulationsPageFragmentContainer();
		if (viewer == null) {
			// Should never happen - if we're active, our parent RegulationsFragment should be registered
			throw new IllegalStateException("RegulationsPageFragmentContainer is null");
		}
		viewer.registerPageFragment(pageName_, null);
	}
	
//	private void zoomToRect(Rect rect) {
//		zoomToRect(rect, true);
//	}

	private void zoomToRect(final Rect rect, final boolean fitMax) {
		if (!isResumed() || !hiResAvailable_) {
			zoomRect_ = rect;
			zoomFitMax_ = fitMax;
			return;
		}
		final Drawable bitmap = imageView_.getDrawable();
		if (bitmap == null) {
			zoomRect_ = rect;
			zoomFitMax_ = fitMax;
			return;
		}
		zoomRect_ = null;
		zoomFitMax_ = true;
		
		// Scroll and zoom
		// Use post() because we may have gotten here via the onBitmapChanged() callback. But the
		// ImageViewTouch code may not be finished setting itself up at this point (onBitmapChanged()
		// is invoked by the ImageViewTouchBase superclass, but ImageViewTouch does some more stuff
		// after calling super). So post a Runnable to do the work after the dust has settled.
		imageView_.post(new Runnable() {
			public void run() {
				RegulationsImageView imageView = imageView_;
				if (imageView == null)
					return; // One would think this could never happen, but the monkey proves otherwise
				int rectWidth = rect.right - rect.left;
				int rectHeight = rect.bottom - rect.top;
				// rect_ coordinates are based on fixed size PDF pages.
				float desiredZoomX = PDF_PAGE_SIZE_X / (float)rectWidth;
				float desiredZoomY = PDF_PAGE_SIZE_Y / (float)rectHeight;
				float desiredZoom;
				if (fitMax)
					desiredZoom = Math.min(desiredZoomX, desiredZoomY);
				else
					desiredZoom = Math.max(desiredZoomX, desiredZoomY);
				zoomFitMax_ = true; // In case zoomToRect is next called NOT as a result of a save/restore state
				desiredZoom = Math.max(desiredZoom, imageView.getMinScale());
				final float desiredZoomF = Math.min(desiredZoom, imageView.getMaxScale());
				// Translate the center coordinates to bitmap coordinates.
				float pdfCenterX = ((float)rect.left) + ((float)rectWidth) / 2.0f;
				float pdfCenterY = ((float)rect.top) + ((float)rectHeight) / 2.0f;
				// Now scale the center to be in bitmap coordinates
				final float centerX = (bitmap.getIntrinsicWidth() / PDF_PAGE_SIZE_X) * pdfCenterX;
				final float centerY = (bitmap.getIntrinsicHeight() / PDF_PAGE_SIZE_Y) * pdfCenterY;
				imageView.zoomAndCenter(desiredZoomF, centerX, centerY);
			}
		});
	}

	@Override
	public void onDrawableChanged(Drawable drawable) {
		if (hiResAvailable_) {
    		if (MainActivity.DEBUG)
    			Log.d(MainActivity.LOG_TAG, pageName_ + " onDrawableChanged to hi res - zoomRect_=" +
    					zoomRect_ + ", zoomFitMax_=" + zoomFitMax_);
			if (zoomRect_ != null) {
				zoomToRect(zoomRect_, zoomFitMax_);
			}
		}
	}

	public void loadHiResBitmap() {
		loader_.loadHiRes();
	}

	public void onPageDeselected() {
		loader_.cancel();
		if (imageView_ != null) {
			// Reset image view to default scale (fill view exactly)
			Drawable drawable = imageView_.getDrawable();
			if (drawable != null) {
				float centerX = ((float) drawable.getIntrinsicWidth()) / 2.0f;
				float centerY = ((float) drawable.getIntrinsicHeight()) / 2.0f;
				imageView_.zoomAndCenter(1.0f, centerX, centerY);
			}
		}
	}

	private class RegImageLoader implements Runnable {
		private boolean cancel_ = false;
		private Thread thread_ = null;
		
		public void cancel() {
			cancel_ = true;
			if (thread_ != null)
				thread_.interrupt();
		}
		
		public void loadHiRes() {
			cancel_ = false;
			thread_ = new Thread(this);
			thread_.start();
		}
		
		public void run() {
			if (hiResAvailable_)
				return;
			final Activity activity = getActivity();
			if (activity == null) // We've been detached
				return;
			
			// Short delay to allow page swipe animation to hopefully complete
			try { Thread.sleep(200); } catch (InterruptedException ignored) {}
			
			if (cancel_ || isDetached())
				return;
			
			final RegulationsImageView imageView = imageView_; // atomic access in case onDestroyView is called in UI thread
//			final AssetManager assets = activity.getAssets();
			Bitmap newBitmap;
			if (imageView != null) {
	    		if (MainActivity.DEBUG)
	    			Log.d(MainActivity.LOG_TAG, "RegulationsPageFragment " + pageName_ + " creating hi-res bitmap");
                /*
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inPurgeable = true;
				options.inInputShareable = true;
				options.inPreferredConfig = Bitmap.Config.RGB_565;
				try {
					InputStream is = assets.open("regulations/" + pageName_);
					newBitmap = BitmapFactory.decodeStream(is, null, options);
				} catch (IOException e) {
		    		if (MainActivity.DEBUG)
		    			Log.e(MainActivity.LOG_TAG, "Failed to load hi-res regulations page bitmap for " + pageName_, e);
				}
				*/
                newBitmap = loadBitmap(true, pageNumber_, getActivity());
				if (newBitmap != null) {
					final Bitmap oldBitmap = bitmap_;
					final Bitmap newBitmapF = newBitmap;
					imageView.post(new Runnable() {
						public void run() {
							hiResAvailable_ = true;
							bitmap_ = newBitmapF;
							imageView.setImageBitmap(newBitmapF);
							if (oldBitmap != null) {
					    		if (MainActivity.DEBUG)
					    			Log.d(MainActivity.LOG_TAG, "RegulationsPageFragment " + pageName_ + " recycling lo-res bitmap");
								oldBitmap.recycle();
							}
						}
					});
				}
			}
		}
	}
}
