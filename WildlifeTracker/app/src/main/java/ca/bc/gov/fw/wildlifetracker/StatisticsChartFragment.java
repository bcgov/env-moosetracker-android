package ca.bc.gov.fw.wildlifetracker;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * A Fragment to display the bar chart and its title for a particular year.
 */
public class StatisticsChartFragment extends Fragment {

    public interface StatisticsChartFragmentListener {
        void chartDidBecomeVisible();
    }

    private static final String YEAR_KEY = "year";
    private static final String REGION_KEY = "region";
    private static String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    // Should really have an API to enforce the fact these are read-only...
    public int totalBulls = 0;
    public int totalCows = 0;
    public int totalCalves = 0;
    public int totalUnknown = 0;
    public int totalHours = 0;
    public int totalDays = 0;

    private BarChart chart;
    ArrayList<BarEntry> hoursDataList;
    ArrayList<BarEntry> mooseDataList;
    ArrayList<String> dataLabels;

    private int year;
    private String region = null;

    public StatisticsChartFragment() {
        // Required empty public constructor
    }

    /**
     * Creates an instance to display statistics for the given year and (optional) region.
     * @param year The calendar year, e.g. 2015
     * @param region The region string, e.g. "All", "7A", "8-15"
     * @return The new fragment instance.
     */
    public static StatisticsChartFragment newInstance(int year, String region) {
        Bundle args = new Bundle();
        args.putInt(YEAR_KEY, year);
        if (region != null) {
            args.putString(REGION_KEY, region);
        }
        StatisticsChartFragment frag = new StatisticsChartFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args == null) {
            Log.e(MainActivity.LOG_TAG, "No arguments to StatisticsChartFragment! Using default year 2015.");
            year = 2015;
        } else {
            year = args.getInt(YEAR_KEY, 0);
            if (year == 0) {
                Log.e(MainActivity.LOG_TAG, "Invalid or missing year for StatisticsChartFragment! Using default year 2015.");
                year = 2015;
            }
            region = args.getString(REGION_KEY);
        }
        queryChartData();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && (getParentFragment() instanceof StatisticsChartFragmentListener)) {
            ((StatisticsChartFragmentListener) getParentFragment()).chartDidBecomeVisible();
        }
    }

    /**
     * Performs database queries to populate hoursDataList, mooseDataList and dataLabels arrays,
     * and calculate totalBulls, totalCows etc. This should be called upon fragment creation,
     * since the parent fragment may access totalBulls etc. before the view is created.
     */
    private void queryChartData() {
        Date[] queryDates = new Date[13];
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("America/Vancouver"), Locale.CANADA);
        calendar.clear();
        calendar.set(year, Calendar.JANUARY, 1);
        queryDates[0] = calendar.getTime();
        for (int i = 1; i <= 12; i++) {
            calendar.add(Calendar.MONTH, 1);
            queryDates[i] = calendar.getTime();
        }

        int lastDay;
        hoursDataList = new ArrayList<>();
        mooseDataList = new ArrayList<>();
        dataLabels = new ArrayList<>();
        int nextX = 0;
        boolean includeMonth = false;
        SightingsDBHelper db = DataController.getInstance().getDatabaseHelper();
        String queryRegion = null;
        String queryMu = null;
        if ((region != null) && !region.equals("All")) {
            if (region.contains("-")) {
                queryMu = region;
            } else {
                queryRegion = region;
            }
        }
        for (int i = 0; i < 12; i++) {
            lastDay = -1;
            SightingData[] data = db.querySightings(queryDates[i], queryDates[i + 1], queryRegion, queryMu);
            if ((i >= 7) || (data.length > 0))
                includeMonth = true;
            if (includeMonth) {
                int moose = 0;
                int hours = 0;
                for (SightingData sighting : data) {
                    calendar.setTime(sighting.date);
                    int day = calendar.get(Calendar.DAY_OF_MONTH);
                    if (day != lastDay) {
                        lastDay = day;
                        totalDays++;
                    }
                    totalBulls += sighting.numBulls;
                    totalCows += sighting.numCows;
                    totalCalves += sighting.numCalves;
                    totalUnknown += sighting.numUnknown;
                    totalHours += sighting.numHours;
                    moose += (sighting.numBulls + sighting.numCows + sighting.numCalves + sighting.numUnknown);
                    hours += sighting.numHours;
                }
                hoursDataList.add(new BarEntry(hours, nextX));
                mooseDataList.add(new BarEntry(moose, nextX));
                dataLabels.add(monthNames[i]);
                nextX++;
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_statistics_chart, container, false);

        TextView titleTextView = (TextView) view.findViewById(R.id.tvChartTitle);
        String yearStr = year + " Summary";
        titleTextView.setText(yearStr);

        chart = (BarChart) view.findViewById(R.id.chart);
        chart.setDescription("");

        setupChartView();

        return view;
    }

    private void setupChartView() {
        Log.d(MainActivity.LOG_TAG, "setupChartView");
        int hoursColor = ContextCompat.getColor(getContext(), R.color.bc_yellow);
        int mooseColor = ContextCompat.getColor(getContext(), R.color.bc_blue);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setAxisMinValue(0);
        leftAxis.setDrawZeroLine(false);
        leftAxis.setDrawGridLines(false);
        leftAxis.setSpaceTop(15.0f);
        leftAxis.setTextColor(hoursColor);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setAxisMinValue(0);
        rightAxis.setDrawZeroLine(false);
        rightAxis.setDrawGridLines(false);
        rightAxis.setSpaceTop(15.0f);
        rightAxis.setTextColor(mooseColor);

        LimitLine zeroLine = new LimitLine(0.0f);
        zeroLine.setLineColor(Color.GRAY);
        zeroLine.setLineWidth(0.5f);
        leftAxis.addLimitLine(zeroLine);

        BarDataSet hoursDataSet = new BarDataSet(hoursDataList, "Hours");
        hoursDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        hoursDataSet.setColor(hoursColor);

        BarDataSet mooseDataSet = new BarDataSet(mooseDataList, "Moose");
        mooseDataSet.setAxisDependency(YAxis.AxisDependency.RIGHT);
        mooseDataSet.setColor(mooseColor);

        ArrayList<IBarDataSet> dataSets = new ArrayList<>();
        dataSets.add(hoursDataSet);
        dataSets.add(mooseDataSet);

        BarData data = new BarData(dataLabels, dataSets);
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
                return String.valueOf((int) (value + 0.5f));
            }
        });

        chart.setData(data);
        chart.invalidate();

    }
}
