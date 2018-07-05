package ca.bc.gov.fw.wildlifetracker;

import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.DatePicker;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Date picker dialog
 */
public class WTDatePickerFragment extends android.support.v4.app.DialogFragment implements OnDateSetListener {

    public interface WTDatePickerFragmentListener {
        void didSelectDate(Date date);
    }

    @Override @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the current date as the default if not specified in arguments
        final Calendar c = Calendar.getInstance(TimeZone.getTimeZone("America/Vancouver"), Locale.CANADA);
        Date now = c.getTime(); // max allowed date = now
        long dateLong = getArguments().getLong("date");
        if (dateLong != 0)
            c.setTime(new Date(dateLong));
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        // Create a new instance of DatePickerDialog and return it
        DatePickerDialog pickerDialog = new DatePickerDialog(getActivity(), this, year, month, day);

        // These APIs are only available starting with v11 for some reason... Gingerbread is SOL
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            c.setTime(now);
            c.roll(Calendar.YEAR, false);
            Date minDate = c.getTime();
            DatePicker picker = pickerDialog.getDatePicker();
            picker.setMinDate(minDate.getTime());
            picker.setMaxDate(now.getTime());
        }

        return pickerDialog;
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        WTDatePickerFragmentListener listener = null;

        if (getActivity() instanceof WTDatePickerFragmentListener) {
            listener = (WTDatePickerFragmentListener) getActivity();
        } else if (getParentFragment() instanceof  WTDatePickerFragmentListener) {
            listener = (WTDatePickerFragmentListener) getParentFragment();
        }

        if (listener != null) {
            final Calendar c = Calendar.getInstance(TimeZone.getTimeZone("America/Vancouver"), Locale.CANADA);
            c.clear();
            c.set(Calendar.YEAR, year);
            c.set(Calendar.MONTH, monthOfYear);
            c.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            c.set(Calendar.HOUR_OF_DAY, 12);
            listener.didSelectDate(c.getTime());
        } else {
            Log.println(Log.ERROR, getClass().getName(), "Date selected but no listener!");
        }
    }
}
