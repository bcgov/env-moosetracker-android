package ca.bc.gov.fw.wildlifetracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;

/**
 * Database wrapper for Sightings database
 */
public class SightingsDBHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Sightings.db";

    public SightingsDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SightingsDBContract.SQL_CREATE_SIGHTINGS);
        for (String sql: SightingsDBContract.SQL_CREATE_INDICES) {
            db.execSQL(sql);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void writeNewSighting(SightingData sightingData) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(SightingsDBContract.SightingEntry.COLUMN_NAME_NUM_BULLS, sightingData.numBulls);
        values.put(SightingsDBContract.SightingEntry.COLUMN_NAME_NUM_COWS, sightingData.numCows);
        values.put(SightingsDBContract.SightingEntry.COLUMN_NAME_NUM_CALVES, sightingData.numCalves);
        values.put(SightingsDBContract.SightingEntry.COLUMN_NAME_NUM_UNKNOWN, sightingData.numUnknown);
        values.put(SightingsDBContract.SightingEntry.COLUMN_NAME_HOURS, sightingData.numHours);
        values.put(SightingsDBContract.SightingEntry.COLUMN_NAME_MU, sightingData.managementUnit);
        values.put(SightingsDBContract.SightingEntry.COLUMN_NAME_DATE, sightingData.date.getTime());
        String region = ManagementUnitHelper.findRegionForMU(sightingData.managementUnit);
        if (region == null) {
            Log.println(Log.ERROR, getClass().getName(), "Couldn't find region for MU: " + sightingData.managementUnit);
            region = "";
        }
        values.put(SightingsDBContract.SightingEntry.COLUMN_NAME_REGION, region);
        values.put(SightingsDBContract.SightingEntry.COLUMN_NAME_UPLOADED, SubmitDataAsyncTaskResultEvent.SubmitStatus.NotSubmitted.getCode());

        sightingData.databaseId = db.insert(SightingsDBContract.SightingEntry.TABLE_NAME, null, values);
        if (sightingData.databaseId < 0) {
            Log.println(Log.ERROR, getClass().getName(), "Failed to insert sightings record");
        }
        db.close();
    }

    public void updateStatus(long id, SubmitDataAsyncTaskResultEvent.SubmitStatus status) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(SightingsDBContract.SightingEntry.COLUMN_NAME_UPLOADED, status.getCode());

        String selection = SightingsDBContract.SightingEntry.COLUMN_NAME_ID + " = ?";
        String[] selectionArgs = { String.valueOf(id) };

        int count = db.update(
                SightingsDBContract.SightingEntry.TABLE_NAME,
                values,
                selection,
                selectionArgs);

        Log.println(Log.DEBUG, getClass().getName(), count + " rows updated");
        db.close();
    }

    @NonNull
    public SightingData[] queryUnsubmittedSightings() {
        String selection = SightingsDBContract.SightingEntry.COLUMN_NAME_UPLOADED + " = ?";
        String[] selectionArgs = { String.valueOf(SubmitDataAsyncTaskResultEvent.SubmitStatus.NotSubmitted) };
        return querySightings(selection, selectionArgs, null);
    }

    /**
     * Query sightings by date and optionally by region / MU.
     * @param fromDate Must not be null.
     * @param toDate Must not be null.
     * @param region Region name. May be null. Ignored if MU is specified.
     * @param managementUnit May be null. Takes precedence over region - if an MU is specified, only results for that MU are queried.
     * @return Array of sighting data.
     */
    @NonNull
    public SightingData[] querySightings(Date fromDate, Date toDate, String region, String managementUnit) {
        assert fromDate != null;
        assert toDate != null;
        String selection = SightingsDBContract.SightingEntry.COLUMN_NAME_DATE + " >= ? AND " + SightingsDBContract.SightingEntry.COLUMN_NAME_DATE + " < ?";
        ArrayList<String> args = new ArrayList<>();
        args.add(String.valueOf(fromDate.getTime()));
        args.add(String.valueOf(toDate.getTime()));
        if (managementUnit != null) {
            selection = selection + " AND " + SightingsDBContract.SightingEntry.COLUMN_NAME_MU + " = ?";
            args.add(managementUnit);
        } else if (region != null) {
            selection = selection + " AND " + SightingsDBContract.SightingEntry.COLUMN_NAME_REGION + " = ?";
            args.add(region);
        }
        return querySightings(selection, args.toArray(new String[args.size()]), SightingsDBContract.SightingEntry.COLUMN_NAME_DATE);
    }

    @NonNull
    private SightingData[] querySightings(String selection, String[] selectionArgs, String orderBy) {
        SQLiteDatabase db = getReadableDatabase();
        String[] projection = {
                SightingsDBContract.SightingEntry.COLUMN_NAME_ID,
                SightingsDBContract.SightingEntry.COLUMN_NAME_NUM_BULLS,
                SightingsDBContract.SightingEntry.COLUMN_NAME_NUM_COWS,
                SightingsDBContract.SightingEntry.COLUMN_NAME_NUM_CALVES,
                SightingsDBContract.SightingEntry.COLUMN_NAME_NUM_UNKNOWN,
                SightingsDBContract.SightingEntry.COLUMN_NAME_HOURS,
                SightingsDBContract.SightingEntry.COLUMN_NAME_MU,
                SightingsDBContract.SightingEntry.COLUMN_NAME_DATE,
        };

        Cursor cursor = db.query(
                SightingsDBContract.SightingEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null, // Group by
                null, // Having
                orderBy  // Order by
        );

        ArrayList<SightingData> list = new ArrayList<>();
        try {
            while (cursor.moveToNext()) {
                SightingData data = new SightingData();
                data.databaseId = cursor.getLong(cursor.getColumnIndex(SightingsDBContract.SightingEntry.COLUMN_NAME_ID));
                data.numBulls = cursor.getInt(cursor.getColumnIndex(SightingsDBContract.SightingEntry.COLUMN_NAME_NUM_BULLS));
                data.numCows = cursor.getInt(cursor.getColumnIndex(SightingsDBContract.SightingEntry.COLUMN_NAME_NUM_COWS));
                data.numCalves = cursor.getInt(cursor.getColumnIndex(SightingsDBContract.SightingEntry.COLUMN_NAME_NUM_CALVES));
                data.numUnknown = cursor.getInt(cursor.getColumnIndex(SightingsDBContract.SightingEntry.COLUMN_NAME_NUM_UNKNOWN));
                data.numHours = cursor.getInt(cursor.getColumnIndex(SightingsDBContract.SightingEntry.COLUMN_NAME_HOURS));
                data.managementUnit = cursor.getString(cursor.getColumnIndex(SightingsDBContract.SightingEntry.COLUMN_NAME_MU));
                Long millisecondsSince1970 = cursor.getLong(cursor.getColumnIndex(SightingsDBContract.SightingEntry.COLUMN_NAME_DATE));
                data.date = new Date(millisecondsSince1970);
                list.add(data);
            }
        } finally {
            cursor.close();
        }
        db.close();
        return list.toArray(new SightingData[list.size()]);
    }
}
