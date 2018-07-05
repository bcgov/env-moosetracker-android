package ca.bc.gov.fw.wildlifetracker;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link StatisticsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class StatisticsFragment extends Fragment
        implements WTMUPickerFragment.RegionPickerListener, StatisticsChartFragment.StatisticsChartFragmentListener {

    private static final String SELECTED_REGION_KEY = "selectedRegion";

    private ViewPager chartPager;
    private StatisticsChartPagerAdapter chartPagerAdapter;
    private TextView hoursTextView;
    private TextView daysTextView;
    private TextView mooseTextView;
    private TextView mooseDescriptionTextView;
    private TextView moosePerHourTextView;
    private TextView regionTextView;

    private String selectedRegion = "All";

    public StatisticsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment StatisticsFragment.
     */
    public static StatisticsFragment newInstance() {
        return new StatisticsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            selectedRegion = savedInstanceState.getString(SELECTED_REGION_KEY);
            if (selectedRegion == null) {
                selectedRegion = "All";
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SELECTED_REGION_KEY, selectedRegion);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_statistics, container, false);

        chartPager = (ViewPager) view.findViewById(R.id.chartPager);
        chartPagerAdapter = new StatisticsChartPagerAdapter(getChildFragmentManager());
        chartPager.setAdapter(chartPagerAdapter);
        chartPager.setCurrentItem(chartPagerAdapter.getInitialItemPosition());
        chartPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                // Required implementation
            }

            @Override
            public void onPageSelected(int position) {
                StatisticsChartFragment chartFragment = StatisticsFragment.this.chartPagerAdapter.getRegisteredFragment(position);
                Log.d(MainActivity.LOG_TAG, "onPageSelected position=" + position + ", chartFragment=" + chartFragment);
                if (chartFragment != null) {
                    setupTextFields(chartFragment);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                // Required implementation
            }
        });

        hoursTextView = (TextView) view.findViewById(R.id.tvEffortHours);
        daysTextView = (TextView) view.findViewById(R.id.tvEffortDays);
        mooseTextView = (TextView) view.findViewById(R.id.tvSightingsTotal);
        mooseDescriptionTextView = (TextView) view.findViewById(R.id.tvSightingsDetail);
        moosePerHourTextView = (TextView) view.findViewById(R.id.tvMoosePerHour);
        regionTextView = (TextView) view.findViewById(R.id.tvRegion);
        Button regionButton = (Button) view.findViewById(R.id.btnChangeRegion);
        regionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment newFragment = new WTMUPickerFragment();
                Bundle fragmentArgs = new Bundle();
                fragmentArgs.putString(WTMUPickerFragment.INITIAL_REGION, selectedRegion);
                fragmentArgs.putBoolean(WTMUPickerFragment.INCLUDE_ALL_OPTION, true);
                newFragment.setArguments(fragmentArgs);
                newFragment.show(getChildFragmentManager(), "muPicker");
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        setupRegionTextField();
        chartPagerAdapter.notifyDataSetChanged();
    }

    private void setupTextFields(StatisticsChartFragment chartFragment) {
        String hoursStr = String.valueOf(chartFragment.totalHours) + " hour";
        if (chartFragment.totalHours != 1) {
            hoursStr = hoursStr + "s";
        }
        hoursTextView.setText(hoursStr);

        String daysStr = String.valueOf(chartFragment.totalDays) + " day";
        if (chartFragment.totalDays != 1) {
            daysStr = daysStr + "s";
        }
        daysTextView.setText(daysStr);

        int totalMoose = chartFragment.totalBulls + chartFragment.totalCows + chartFragment.totalCalves + chartFragment.totalUnknown;
        String mooseStr = String.valueOf(totalMoose) + " moose";
        mooseTextView.setText(mooseStr);

        String desc = "";
        if (chartFragment.totalBulls > 0) {
            desc = String.valueOf(chartFragment.totalBulls) + " bull";
            if (chartFragment.totalBulls > 1) {
                desc = desc + "s";
            }
        }
        if (chartFragment.totalCows > 0) {
            if (desc.length() > 0) {
                desc = desc + ", ";
            }
            desc = desc + String.valueOf(chartFragment.totalCows) + " cow";
            if (chartFragment.totalCows > 1) {
                desc = desc + "s";
            }
        }
        if (chartFragment.totalCalves > 0) {
            if (desc.length() > 0) {
                desc = desc + ", ";
            }
            desc = desc + String.valueOf(chartFragment.totalCalves) + " cal";
            if (chartFragment.totalCalves > 1) {
                desc = desc + "ves";
            } else {
                desc = desc + "f";
            }
        }
        if (chartFragment.totalUnknown > 0) {
            if (desc.length() > 0) {
                desc = desc + ", ";
            }
            desc = desc + String.valueOf(chartFragment.totalUnknown) + " unknown moose";
        }
        mooseDescriptionTextView.setText(desc);

        if (chartFragment.totalHours > 0) {
            float rate = (float) totalMoose / (float) chartFragment.totalHours;
            moosePerHourTextView.setText(String.format("%1.2f moose per hour", rate));
        } else {
            moosePerHourTextView.setText("");
        }
    }

    @Override
    public void regionPicked(String region) {
        Log.i(MainActivity.LOG_TAG, "Selected region = " + region);
        selectedRegion = region;
        setupRegionTextField();
        chartPagerAdapter.notifyDataSetChanged();
    }

    private void setupRegionTextField() {
        if (selectedRegion == null)
            selectedRegion = "All";
        if (selectedRegion.equals("All") || selectedRegion.contains("-")) {
            regionTextView.setText(selectedRegion);
        } else {
            String text = "Region " + selectedRegion + " (all MUs)";
            regionTextView.setText(text);
        }
    }

    @Override
    public void chartDidBecomeVisible() {
        int item = chartPager.getCurrentItem();
        StatisticsChartFragment fragment = chartPagerAdapter.getRegisteredFragment(item);
        Log.d(MainActivity.LOG_TAG, "chartDidBecomeVisible position=" + item + ", fragment=" + fragment);
        if (fragment != null) {
            setupTextFields(fragment);
        }
    }

    private class StatisticsChartPagerAdapter extends FragmentStatePagerAdapter {
        private int currentYear;
        SparseArray<StatisticsChartFragment> registeredFragments = new SparseArray<>();

        public StatisticsChartPagerAdapter(FragmentManager fm) {
            super(fm);
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("America/Vancouver"), Locale.CANADA);
            currentYear = calendar.get(Calendar.YEAR);
            if (currentYear < 2015)
                currentYear = 2015;
        }

        @Override
        public Fragment getItem(int position) {
            return StatisticsChartFragment.newInstance(2015 + position, StatisticsFragment.this.selectedRegion);
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @Override
        public int getCount() {
            return (currentYear - 2015) + 1;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            StatisticsChartFragment fragment = (StatisticsChartFragment) super.instantiateItem(container, position);
            registeredFragments.put(position, fragment);
            return fragment;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            registeredFragments.remove(position);
            super.destroyItem(container, position, object);
        }

        public StatisticsChartFragment getRegisteredFragment(int position) {
            return registeredFragments.get(position);
        }

        public int getInitialItemPosition() {
            return currentYear - 2015;
        }
    }
}
