package ca.bc.gov.fw.wildlifetracker;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import java.util.Calendar;
import java.util.Date;

public class WTSettingsActivity extends AppCompatActivity {

    private SharedPreferences profile_;

    private static Activity targetActivity__ = null;
    private static PendingIntent pendingIntent__ = null;

    public static void setupNotifications(Activity activity, boolean alertsEnabled) {
        System.out.println("In set up notifications!");
        targetActivity__ = activity;

        Intent myIntent = new Intent(activity, NotificationService.class);
        pendingIntent__ = PendingIntent.getService(activity.getBaseContext(), 0, myIntent, 0);

        if (alertsEnabled) {
            // TODO: put the setting back in
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 20);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            // Make sure alarms start in the future
            if (calendar.getTime().before(new Date())) {
                calendar.roll(Calendar.DATE, 1);
            }
            System.out.println("Calendar time: " + calendar.getTime());

            AlarmManager alarmManager = (AlarmManager) activity.getSystemService(ALARM_SERVICE);
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(), 24 * 60 * 60 * 1000, pendingIntent__);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wtsettings);
        profile_ = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE);
        CheckBox remindersCheckBox = (CheckBox) findViewById(R.id.cbRemindersEnabled);
        assert remindersCheckBox != null;
        remindersCheckBox.setChecked(profile_.getBoolean(MainActivity.ALERTS_ENABLED_PREFS_KEY, true));
        remindersCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                WTSettingsActivity.this.setAlerts(isChecked);
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_wtsettings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.aboutMenuItem) {
            Intent myIntent = new Intent(this, CreditsActivity.class);
            startActivity(myIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    private void setAlerts(boolean enabled) {
        SharedPreferences.Editor editor = profile_.edit();
        editor.putBoolean(MainActivity.ALERTS_ENABLED_PREFS_KEY,
                enabled);
        editor.apply();
        if (enabled) {
            System.out.println("Turning on reminders");
            setupNotifications(targetActivity__, true);
        } else {
            System.out.println("Turning off reminders");
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            alarmManager.cancel(pendingIntent__);
        }
    }

}

