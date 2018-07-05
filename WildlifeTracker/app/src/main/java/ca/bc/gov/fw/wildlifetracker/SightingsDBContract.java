package ca.bc.gov.fw.wildlifetracker;

import android.provider.BaseColumns;

/**
 * Defines database schema for Sightings SQLite database
 */
public final class SightingsDBContract {

    private SightingsDBContract() {}

    public static abstract class SightingEntry implements BaseColumns {
        public static final String TABLE_NAME = "sightings";
        public static final String COLUMN_NAME_ID = "id";
        public static final String COLUMN_NAME_DATE = "date";
        public static final String COLUMN_NAME_REGION = "region";
        public static final String COLUMN_NAME_MU = "mu";
        public static final String COLUMN_NAME_NUM_BULLS = "num_bulls";
        public static final String COLUMN_NAME_NUM_COWS = "num_cows";
        public static final String COLUMN_NAME_NUM_CALVES = "num_calves";
        public static final String COLUMN_NAME_NUM_UNKNOWN = "num_unknown";
        public static final String COLUMN_NAME_HOURS = "hours";
        public static final String COLUMN_NAME_COMMENTS = "comments";
        public static final String COLUMN_NAME_UPLOADED = "uploaded";
    }

    private static final String TEXT_TYPE = " TEXT";
    private static final String INTEGER_TYPE = " INTEGER";
    private static final String COMMA_SEP = ",";

    public static final String SQL_CREATE_SIGHTINGS =
            "CREATE TABLE IF NOT EXISTS " + SightingEntry.TABLE_NAME + " (" +
                    SightingEntry.COLUMN_NAME_ID + INTEGER_TYPE + " PRIMARY KEY" + COMMA_SEP +
                    SightingEntry.COLUMN_NAME_DATE + INTEGER_TYPE + COMMA_SEP +
                    SightingEntry.COLUMN_NAME_REGION + TEXT_TYPE + COMMA_SEP +
                    SightingEntry.COLUMN_NAME_MU + TEXT_TYPE + COMMA_SEP +
                    SightingEntry.COLUMN_NAME_NUM_BULLS + INTEGER_TYPE + COMMA_SEP +
                    SightingEntry.COLUMN_NAME_NUM_COWS + INTEGER_TYPE + COMMA_SEP +
                    SightingEntry.COLUMN_NAME_NUM_CALVES + INTEGER_TYPE + COMMA_SEP +
                    SightingEntry.COLUMN_NAME_NUM_UNKNOWN + INTEGER_TYPE + COMMA_SEP +
                    SightingEntry.COLUMN_NAME_HOURS + INTEGER_TYPE + COMMA_SEP +
                    SightingEntry.COLUMN_NAME_COMMENTS + TEXT_TYPE + COMMA_SEP +
                    SightingEntry.COLUMN_NAME_UPLOADED + INTEGER_TYPE +
                    " )";

    public static final String[] SQL_CREATE_INDICES = {
            "CREATE INDEX IF NOT EXISTS date_index ON " + SightingEntry.TABLE_NAME + "(" + SightingEntry.COLUMN_NAME_DATE + ")",
            "CREATE INDEX IF NOT EXISTS region_index ON " + SightingEntry.TABLE_NAME + "(" + SightingEntry.COLUMN_NAME_REGION + ")",
            "CREATE INDEX IF NOT EXISTS mu_index ON " + SightingEntry.TABLE_NAME + "(" + SightingEntry.COLUMN_NAME_MU + ")",
            "CREATE INDEX IF NOT EXISTS uploaded_index ON " + SightingEntry.TABLE_NAME + "(" + SightingEntry.COLUMN_NAME_UPLOADED + ")"
    };
}
