package ca.bc.gov.fw.wildlifetracker;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;

import com.squareup.otto.Bus;

import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;


public class DataController {

//	public static final String uploadURL = "http://huntbuddybc.com/api/upload_moose.php";
    public static final String uploadURL = "http://moose.nprg.ca/upload_moose.php";

	private static DataController instance = null;

	private DateFormat jsonDateFormat_;

	private Context context;
    private  SightingsDBHelper dbHelper;

	public DataController(Context context) {
		this.context = context;

		this.jsonDateFormat_ = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.CANADA);
		this.jsonDateFormat_.setTimeZone(TimeZone.getTimeZone("UTC"));

        this.dbHelper = new SightingsDBHelper(context);
	}

	public static void createSingleton(Context context) {
		instance = new DataController(context);
	}

    public static DataController getInstance() {
		if (instance == null) {
			throw new RuntimeException("DataController.getInstance() called before createSingleton()");
		}
		return instance;
    }

	public SightingsDBHelper getDatabaseHelper() {
        return dbHelper;
    }

	public void submitData(final SightingData sighting, boolean notify) {
		System.out.println("submitData called");

        if (sighting.databaseId <= 0) {
            dbHelper.writeNewSighting(sighting);
        }

        JSONObject dataObject = convertSighting(sighting);

		if (isConnectionAvailable()) {
			System.out.println("Connection is available");
            SubmitDataAsyncTask task = new SubmitDataAsyncTask(uploadURL, dataObject, notify);
            task.listener = new SubmitDataAsyncTask.SubmitDataListener() {
                @Override
                public void submitCompleted(SubmitDataAsyncTaskResultEvent.SubmitStatus status) {
                    dbHelper.updateStatus(sighting.databaseId, status);
                }
            };
            task.execute();
        }
		else {
			System.out.println("Connection is not available");
            if (notify) {
                Bus bus = WTOttoBus.getInstance();
                bus.post(new SubmitDataAsyncTaskResultEvent("Network not available. BC Moose Tracker will try again later.",
                        SubmitDataAsyncTaskResultEvent.SubmitStatus.NotSubmitted));
            }
		}
	}

    private JSONObject convertSighting(SightingData sighting) {
		HashMap<String, Object> map = new HashMap<>();

        map.put("numBulls", sighting.numBulls);
        map.put("numCows", sighting.numCows);
        map.put("numCalves", sighting.numCalves);
        map.put("numUnknown", sighting.numUnknown);
        map.put("numHours", sighting.numHours);
        map.put("managementUnit", sighting.managementUnit);
        map.put("date", jsonDateFormat_.format(sighting.date));
        map.put("platform", "android");
		map.put("installation", context.getSharedPreferences(MainActivity.PREFS_NAME, Activity.MODE_PRIVATE).getString(MainActivity.INSTALLATION_ID_PREFS_KEY, ""));

		JSONObject jsonObject;

		try {
			jsonObject = new JSONObject(map);
		} catch (NullPointerException e) {
			// what to do here...map is missing a key?
			System.out.println("Error in converting sighting data to JSON");
			e.printStackTrace();
			return null;
		}

		return jsonObject;
	}

	private boolean isConnectionAvailable() {
		ConnectivityManager conManager = (ConnectivityManager) this.context
				.getSystemService(Context.CONNECTIVITY_SERVICE);

		try {
			if (!conManager.getActiveNetworkInfo().isConnected()) {
				System.out.println("got false");
				return false;
			}
			//conManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState()==NetworkInfo.State.CONNECTED

			return true;

		}
		catch (NullPointerException e) {
			//if we get a null pointer exception, it means we are in airplane mode
			return false;
		}

	}

    public void resubmitUnsentData() {
        if (!isConnectionAvailable())
            return;

        SightingData[] unsubmittedSightings = dbHelper.queryUnsubmittedSightings();

        for (SightingData sighting: unsubmittedSightings) {
            submitData(sighting, false);
        }
    }
}
