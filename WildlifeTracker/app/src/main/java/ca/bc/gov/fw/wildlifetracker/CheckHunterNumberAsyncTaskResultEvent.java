package ca.bc.gov.fw.wildlifetracker;

import org.json.JSONObject;

/**
 * Event to hold the result of the CheckHunterNumberAsyncTask JSON post
 */
public class CheckHunterNumberAsyncTaskResultEvent {
    private JSONObject resultJSON_;
    private int hunterNumber_;

    public CheckHunterNumberAsyncTaskResultEvent(JSONObject json, int hunterNumber) {
        resultJSON_ = json;
        hunterNumber_ = hunterNumber;
    }

    public JSONObject getResultJSON() {
        return resultJSON_;
    }

    public int getHunterNumber() {
        return hunterNumber_;
    }
}
