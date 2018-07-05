package ca.bc.gov.fw.wildlifetracker;

import com.squareup.otto.Bus;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * AsyncTask to check the hunter number.
 */
public class CheckHunterNumberAsyncTask extends PostJSONAsyncTask {

    public static boolean checkInProgress__ = false;

    private int hunterNumber_;

    public CheckHunterNumberAsyncTask(String urlString, int hunterNumberInt) {
        super(urlString);
        hunterNumber_ = hunterNumberInt;
        JSONObject json = null;
        try {
            postData_ = new JSONObject("{\"hunterNumber\":" + hunterNumberInt + "}");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPreExecute() {
        checkInProgress__ = true;
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        Bus bus = WTOttoBus.getInstance();
        bus.post(new CheckHunterNumberAsyncTaskResultEvent(responseJSON_, hunterNumber_));
        checkInProgress__ = false;
    }
}
