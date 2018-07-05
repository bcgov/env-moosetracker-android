package ca.bc.gov.fw.wildlifetracker;


import android.app.Activity;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.LeadingMarginSpan;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

public class CreditsActivity extends Activity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.credits);

        int selectionColor = 0x80a0b0c0;
        ImageView imageView = (ImageView) findViewById(R.id.ivBCLogo);
        Drawable drawable = imageView.getDrawable();
        imageView.setImageDrawable(new PressedEffectStateListDrawable(drawable, selectionColor));

        imageView = (ImageView) findViewById(R.id.ivHCTFLogo);
        drawable = imageView.getDrawable();
        imageView.setImageDrawable(new PressedEffectStateListDrawable(drawable, selectionColor));

        imageView = (ImageView) findViewById(R.id.ivBCWFLogo);
        drawable = imageView.getDrawable();
        imageView.setImageDrawable(new PressedEffectStateListDrawable(drawable, selectionColor));

        ImageView mooseImage = (ImageView) findViewById(R.id.ivCreditsImage);

        final TextView tvCredits2 = (TextView) findViewById(R.id.tvCredits2);
        final String text = tvCredits2.getText().toString();
        final ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) tvCredits2.getLayoutParams();
        Drawable mooseDrawable = mooseImage.getDrawable();
        final int width = mooseDrawable.getIntrinsicWidth() + (int) ((12.0 * getResources().getDisplayMetrics().density) + 0.5);
        final int height = mooseDrawable.getIntrinsicHeight();
        float textLineHeight = tvCredits2.getLineHeight();
        final int lines = (int) Math.ceil((float)height / textLineHeight);
        params.setMargins(width, 0, 0, 0);

        tvCredits2.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            @SuppressWarnings("deprecation")
            public void onGlobalLayout() {
                int linesCount = tvCredits2.getLayout().getLineCount();
                // restore the margin
                params.setMargins(0, 0, 0, 0);
                SpannableString spanS = new SpannableString(text);

                if (linesCount <= lines) {
                    spanS.setSpan(new MyLeadingMarginSpan2(lines, width), 0, spanS.length(), 0);
                    tvCredits2.setText(spanS);
                } else {
                    // find the breakpoint where to break the String.
                    int breakpoint = tvCredits2.getLayout().getLineEnd(lines - 1);

                    Spannable s1 = new SpannableStringBuilder(spanS, 0, breakpoint);
                    s1.setSpan(new MyLeadingMarginSpan2(lines, width), 0, s1.length(), 0);
                    Spannable s2 = null;
                    if (s1.charAt(s1.length() - 1) != '\n') {
                        s2 = new SpannableStringBuilder(System.getProperty("line.separator"));
                    }
                    Spannable s3 = new SpannableStringBuilder(spanS, breakpoint, spanS.length());
                    // It is needed to set a zero-margin span on for the text under the image to prevent the space on the right!
                    s3.setSpan(new MyLeadingMarginSpan2(0, 0), 0, s3.length(), 0);

                    if (s2 != null)
                        tvCredits2.setText(TextUtils.concat(s1, s2, s3));
                    else
                        tvCredits2.setText(TextUtils.concat(s1, s3));
                }

                // remove the GlobalLayoutListener
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                    tvCredits2.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    tvCredits2.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            }
        });
        tvCredits2.setText(text); // Force layout
    }

    private void startMainActivity() {
        Intent myIntent = new Intent (this, SightingsFragment.class);
        startActivity(myIntent);
    }


    public void bcLogoClicked(View view) {
        openURL("http://www.env.gov.bc.ca/fw/");
    }

    public void bcwfLogoClicked(View view) {
        openURL("http://bcwf.net/");
    }

    public void hctfLogoClicked(View view) {
        openURL("http://www.hctf.ca/");
    }

    public void moreInfoClicked(View view) {
        openURL("http://www.gov.bc.ca/wildlifehealth/moosetracker");
    }

    public void emailButtonClicked(View view) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        String[] addresses = {"moosetracker@gov.bc.ca"};
        intent.putExtra(Intent.EXTRA_EMAIL, addresses);
        intent.putExtra(Intent.EXTRA_SUBJECT, "App Question");
        intent.putExtra(Intent.EXTRA_TEXT, "\n\nPlatform: Android\nApp version: " + BuildConfig.VERSION_NAME +
                "\nDevice: " + Build.MODEL + " " + Build.VERSION.RELEASE +
                "\nInstallation: " + getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE).getString(MainActivity.INSTALLATION_ID_PREFS_KEY, ""));
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    private void openURL(String urlStr) {
        Uri webpage = Uri.parse(urlStr);
        Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    class PressedEffectStateListDrawable extends StateListDrawable {

        private int selectionColor;

        public PressedEffectStateListDrawable(Drawable drawable, int selectionColor) {
            super();
            this.selectionColor = selectionColor;
            addState(new int[] { android.R.attr.state_pressed }, drawable);
            addState(new int[] {}, drawable);
        }

        @Override
        protected boolean onStateChange(int[] states) {
            boolean isStatePressedInArray = false;
            for (int state : states) {
                if (state == android.R.attr.state_pressed) {
                    isStatePressedInArray = true;
                }
            }
            if (isStatePressedInArray) {
                super.setColorFilter(selectionColor, PorterDuff.Mode.MULTIPLY);
            } else {
                super.clearColorFilter();
            }
            return super.onStateChange(states);
        }

        @Override
        public boolean isStateful() {
            return true;
        }
    }

    class MyLeadingMarginSpan2 implements LeadingMarginSpan.LeadingMarginSpan2 {
        private int margin;
        private int lines;

        MyLeadingMarginSpan2(int lines, int margin) {
            this.margin = margin;
            this.lines = lines;
        }

        @Override
        public int getLeadingMargin(boolean first) {
            if (first) {
                return margin;
            } else {
                return 0;
            }
        }

        @Override
        public void drawLeadingMargin(Canvas c, Paint p, int x, int dir,
                                      int top, int baseline, int bottom, CharSequence text,
                                      int start, int end, boolean first, Layout layout) {}

        @Override
        public int getLeadingMarginLineCount() {
            return lines;
        }
    }
}
