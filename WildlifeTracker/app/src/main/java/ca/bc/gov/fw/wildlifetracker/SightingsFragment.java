package ca.bc.gov.fw.wildlifetracker;

import android.Manifest;
import android.app.Activity;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.squareup.otto.Subscribe;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class SightingsFragment extends Fragment
        implements WTDatePickerFragment.WTDatePickerFragmentListener,
        WTMUPickerFragment.RegionPickerListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private SharedPreferences profile_;

    private DateFormat dateFormat_;

    private WTNumberPicker bullsPicker_;
    private WTNumberPicker cowsPicker_;
    private WTNumberPicker calvesPicker_;
    private WTNumberPicker unidentifiedPicker_;
    private WTNumberPicker hoursPicker_;

    private String managementUnit_;
    private TextView muTextView_;

    private Date selectedDate_;
    private TextView dateTextView_;

    private Bundle savedState_;

    private GoogleApiClient apiClient_;
    private LocationRequest locationRequest_;

    private boolean userHasChangedMU_;

    private static final int REQUEST_CHECK_SETTINGS = 1;
    private static final int LOCATIONS_PERMISSION_REQUEST_CODE = 2;

    private static final String USER_HAS_CHANGED_MU_KEY = "userHasChangedMU";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        profile_ = getActivity().getSharedPreferences(MainActivity.PREFS_NAME, Activity.MODE_PRIVATE);

        dateFormat_ = SimpleDateFormat.getDateInstance(SimpleDateFormat.LONG);
        dateFormat_.setTimeZone(TimeZone.getTimeZone("America/Vancouver"));

        long dateLong = 0;
        if (savedInstanceState != null) {
            dateLong = savedInstanceState.getLong("date");
            userHasChangedMU_ = savedInstanceState.getBoolean(USER_HAS_CHANGED_MU_KEY);
        }
        if (dateLong != 0)
            selectedDate_ = new Date(dateLong);
        else
            selectedDate_ = new Date();

        savedState_ = (savedInstanceState != null) ? new Bundle(savedInstanceState) : null;

        if (apiClient_ == null) {
            apiClient_ = new GoogleApiClient.Builder(getContext())
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sightings, container, false);

        dateTextView_ = (TextView) view.findViewById(R.id.dateView);
        setupDateView();
        Button dateButton = (Button) view.findViewById(R.id.btnChangeDate);
        dateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment newFragment = new WTDatePickerFragment();
                Bundle fragmentArgs = new Bundle();
                fragmentArgs.putLong("date", selectedDate_.getTime());
                newFragment.setArguments(fragmentArgs);
                newFragment.show(getChildFragmentManager(), "datePicker");
            }
        });

        managementUnit_ = profile_.getString(MainActivity.MANAGEMENT_UNIT_PREFS_KEY, "1-1");
        muTextView_ = (TextView) view.findViewById(R.id.tvManagementUnit);
        setupMUView();
        Button muButton = (Button) view.findViewById(R.id.btnChangeManagementUnit);
        muButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment newFragment = new WTMUPickerFragment();
                Bundle fragmentArgs = new Bundle();
                fragmentArgs.putString(WTMUPickerFragment.INITIAL_REGION, managementUnit_);
                newFragment.setArguments(fragmentArgs);
                newFragment.show(getChildFragmentManager(), "muPicker");
            }
        });

        // Get references to all the number pickers
        bullsPicker_ = (WTNumberPicker) view.findViewById(R.id.bullsNumberPicker);
        cowsPicker_ = (WTNumberPicker) view.findViewById(R.id.cowsNumberPicker);
        calvesPicker_ = (WTNumberPicker) view.findViewById(R.id.calvesNumberPicker);
        unidentifiedPicker_ = (WTNumberPicker) view.findViewById(R.id.unknownNumberPicker);
        hoursPicker_ = (WTNumberPicker) view.findViewById(R.id.hoursNumberPicker);
        hoursPicker_.maxValue = 24;
        if (savedInstanceState != null) {
            bullsPicker_.setValue(savedInstanceState.getInt("bulls"));
            cowsPicker_.setValue(savedInstanceState.getInt("cows"));
            calvesPicker_.setValue(savedInstanceState.getInt("calves"));
            unidentifiedPicker_.setValue(savedInstanceState.getInt("unidentified"));
            hoursPicker_.setValue(savedInstanceState.getInt("hours"));
        }

        Button resetButton = (Button) view.findViewById(R.id.resetButton);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetButtonPressed();
            }
        });
        Button submitButton = (Button) view.findViewById(R.id.submitButton);
        submitButton.setOnClickListener(new Button.OnClickListener() {

            @Override
            public void onClick(View v) {
                formButtonPressed();
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        WTOttoBus.getInstance().register(this);
        if (apiClient_ != null && !apiClient_.isConnected()) {
            apiClient_.connect();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        WTOttoBus.getInstance().unregister(this);
        if (apiClient_ != null && apiClient_.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(apiClient_, this);
            apiClient_.disconnect();
        }
    }

    private void setupMUView() {
        muTextView_.setText(managementUnit_);
    }

    private void setupDateView() {
        // Make sure selectedDate_ is noon Pacific time on the selected day
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("America/Vancouver"), Locale.CANADA);
        calendar.setTime(selectedDate_);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        calendar.clear();
        calendar.set(year, month, day, 12, 0);
        selectedDate_ = calendar.getTime();
        dateTextView_.setText(dateFormat_.format(selectedDate_));
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(USER_HAS_CHANGED_MU_KEY, userHasChangedMU_);
        if (bullsPicker_ != null) {
            // Only extract values from the UI if view has been created!
            outState.putLong("date", selectedDate_.getTime());
            outState.putInt("bulls", bullsPicker_.getValue());
            outState.putInt("cows", cowsPicker_.getValue());
            outState.putInt("calves", calvesPicker_.getValue());
            outState.putInt("unidentified", unidentifiedPicker_.getValue());
            outState.putInt("hours", hoursPicker_.getValue());
        } else if (savedState_ != null) {
            // Fragment is being recreated and destroyed without creating the view, so reuse
            // saved state from the last time instead of getting it from the view
            outState.putAll(savedState_);
        }
    }

    private void resetButtonPressed() {
        Log.d(MainActivity.LOG_TAG, "Reset button pressed");
        userHasChangedMU_ = false;
        startLocationUpdates();
        selectedDate_ = new Date();
        setupDateView();
        resetForm();
    }

    private void formButtonPressed() {
        Log.d(MainActivity.LOG_TAG, "Form button pressed");
        if (hoursPicker_.getValue() == 0) {
            Toast toast = Toast.makeText(getContext(), "Please enter the number of Hours Out.", Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        // grab all the data from the forms
        SightingData sighting = convertFormData();
        DataController.getInstance().submitData(sighting, true);

        resetForm();
    }

    private void resetForm() {
        bullsPicker_.setValue(0);
        cowsPicker_.setValue(0);
        calvesPicker_.setValue(0);
        unidentifiedPicker_.setValue(0);
        hoursPicker_.setValue(0);
    }

    private SightingData convertFormData() {

        SightingData result = new SightingData();
        result.numBulls = bullsPicker_.getValue();
        result.numCows = cowsPicker_.getValue();
        result.numCalves = calvesPicker_.getValue();
        result.numUnknown = unidentifiedPicker_.getValue();
        result.numHours = hoursPicker_.getValue();
        result.managementUnit = managementUnit_;
        result.date = selectedDate_;
        return result;
    }

    @Override
    public void didSelectDate(Date date) {
        selectedDate_ = date;
        setupDateView();
    }

    @Override
    public void regionPicked(String region) {
        managementUnit_ = region;
        userHasChangedMU_ = true;
        SharedPreferences.Editor editor = profile_.edit();
        editor.putString(MainActivity.MANAGEMENT_UNIT_PREFS_KEY, region);
        editor.apply();
        setupMUView();
    }

    @Subscribe
    public void handleSubmitDataEvent(SubmitDataAsyncTaskResultEvent event) {
        String message = event.getMessage();
        Toast toast = Toast.makeText(getContext(), message, Toast.LENGTH_LONG);
        toast.show();
    }

    private void startLocationUpdates() {
        if ((apiClient_ != null) &&
                (locationRequest_ != null) &&
                apiClient_.isConnected()) {
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[] { Manifest.permission.ACCESS_COARSE_LOCATION }, LOCATIONS_PERMISSION_REQUEST_CODE);
                return;
            }
            LocationServices.FusedLocationApi.requestLocationUpdates(apiClient_, locationRequest_, this);
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest_ = new LocationRequest();
        locationRequest_.setInterval(5000);
        locationRequest_.setFastestInterval(2000);
        locationRequest_.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest_);
        final PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(apiClient_, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
                final Status status = locationSettingsResult.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can
                        // initialize location requests here.
                        startLocationUpdates();
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied, but this can be fixed
                        // by showing the user a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(SightingsFragment.this.getActivity(),
                                    REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way
                        // to fix the settings so we won't show the dialog.
                        break;
                }

            }
        });
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(MainActivity.LOG_TAG, "Failed to connect to Google APIs: " + connectionResult);
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(MainActivity.LOG_TAG, "Got location: " + location);
        LocationServices.FusedLocationApi.removeLocationUpdates(apiClient_, this);
        if (userHasChangedMU_) {
            Log.d(MainActivity.LOG_TAG, "Ignoring location - userHasChangedMU_ is true");
            return;
        }
        RegionManager.RegionResult result = RegionManager.getInstance().regionsForLocation(location);
        if ((result.regions_ != null) && (result.regions_.length > 0) && !userHasChangedMU_) {
            Log.d(MainActivity.LOG_TAG, "Setting region from location: " + result.regions_[0].name_);
            regionPicked(result.regions_[0].name_);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != LOCATIONS_PERMISSION_REQUEST_CODE) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            //noinspection ResourceType
            LocationServices.FusedLocationApi.requestLocationUpdates(apiClient_, locationRequest_, this);
        }
    }
}
