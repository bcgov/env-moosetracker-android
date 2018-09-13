package ca.bc.gov.fw.wildlifetracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;
import java.io.IOException;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements RegulationsFragment.RegulationsFragmentContainer {
    public static final boolean DEBUG = true;
    public static final String LOG_TAG = "BCMooseTracker";

    public static final String PREFS_NAME = "profile_prefs";

    public static final String LICENSE_READ_KEY = "profileRead";
    public static final String INSTALLATION_ID_PREFS_KEY = "installationId";
    public static final String ALERTS_ENABLED_PREFS_KEY = "alertsEnabled";
    public static final String MANAGEMENT_UNIT_PREFS_KEY = "managementUnit";

    private SharedPreferences profile_;

    private RegulationsPageFragment.RegulationsPageFragmentContainer pageFragmentContainer_;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main);

        try {
            RegionManager.loadRegions(getAssets());
        } catch (IOException e) {
            Log.e(getClass().getName(), "Failed to load configuration files", e);
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        profile_ = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        boolean alertsEnabled = profile_.getBoolean(ALERTS_ENABLED_PREFS_KEY, true);
        WTSettingsActivity.setupNotifications(this, alertsEnabled);

        // Kick off background thread to ensure all PDF page thumbnails are generated if needed
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    RegulationsPageFragment.createThumbnails(MainActivity.this);
                }
            }).start();
        }

        // Get the ViewPager and set its PagerAdapter so that it can display items
        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        assert viewPager != null;
        viewPager.setAdapter(new WTFragmentPagerAdapter(getSupportFragmentManager()));

        // Give the TabLayout the ViewPager
        TabLayout tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        assert tabLayout != null;
        tabLayout.setupWithViewPager(viewPager);

        if (profile_.getString(INSTALLATION_ID_PREFS_KEY, null) == null) {
            // If no installation ID yet (first run), create one.
            UUID uuid = UUID.randomUUID();
            SharedPreferences.Editor editor = profile_.edit();
            editor.putString(INSTALLATION_ID_PREFS_KEY, uuid.toString());
            editor.apply();
        }

        if (!isLicenseAgreementRead()) {
            System.out.println("License agreement was not read!");
            this.startLicenseAgreementActivity();
            // check if license agreement has been viewed and agreed to
        }

        // Submit unsent data only on first activity create, not on configuration change
        if (savedInstanceState == null) {
            DataController.createSingleton(getApplicationContext());
            DataController.getInstance().resubmitUnsentData();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settingsMenuItem:
                startSettingsActivity();
                return true;
            case R.id.aboutMenuItem:
                startCreditsActivity();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean isLicenseAgreementRead() {
        return profile_.getBoolean(LICENSE_READ_KEY, false);
    }

    // ///////////////////////////////////////////////////////
    // /////////////methods to call different activities//////
    // //////////////////////////////////////////////////////
    private void startLicenseAgreementActivity() {
        Intent myIntent = new Intent(this, LicenseAgreementActivity.class);
        startActivity(myIntent);
    }

    private void startCreditsActivity() {
        Intent myIntent = new Intent(this, CreditsActivity.class);
        startActivity(myIntent);
    }

    private void startSettingsActivity() {
        Intent intent = new Intent(this, WTSettingsActivity.class);
        startActivity(intent);
    }

    @Override
    public void setRegulationsPageFragmentContainer(RegulationsPageFragment.RegulationsPageFragmentContainer container) {
        pageFragmentContainer_ = container;
    }

    @Override
    public RegulationsPageFragment.RegulationsPageFragmentContainer getRegulationsPageFragmentContainer() {
        return pageFragmentContainer_;
    }

    public class WTFragmentPagerAdapter extends FragmentPagerAdapter {
        final int PAGE_COUNT = 4;
        private String tabTitles[] = new String[] { "Sightings", "Map", "Statistics", "Regulations" };

        public WTFragmentPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return PAGE_COUNT;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new SightingsFragment();
                case 1:
                    return MapFragment.newInstance();
                case 2:
                    return StatisticsFragment.newInstance();
                case 3:
                    return new RegulationsFragment();
                default:
                    break;
            }
            return null;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            // Generate title based on item position
            return tabTitles[position];
        }
    }

}
