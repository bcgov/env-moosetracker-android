package ca.bc.gov.fw.wildlifetracker;

import com.squareup.otto.Bus;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Async task to post sighting data to the server.
 */
public class SubmitDataAsyncTask extends PostJSONAsyncTask {

    public interface SubmitDataListener {
        void submitCompleted(SubmitDataAsyncTaskResultEvent.SubmitStatus status);
    }

    /**
     * The listener will always be called with the status of the submit, regardless of the notify parameter.
     */
    public SubmitDataListener listener;

    private boolean notify_;

    public SubmitDataAsyncTask(String urlStr, JSONObject jsonObject, boolean notify) {
        super(urlStr, jsonObject);
        notify_ = notify;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        boolean success = false;
        SubmitDataAsyncTaskResultEvent.SubmitStatus status = SubmitDataAsyncTaskResultEvent.SubmitStatus.NotSubmitted;
        if (responseJSON_ != null) {
            try {
                success = responseJSON_.getBoolean("success");
                if (success) {
                    status = SubmitDataAsyncTaskResultEvent.SubmitStatus.Success;
                } else {
                    // Valid JSON with failure result code... something must have been wrong with the data
                    status = SubmitDataAsyncTaskResultEvent.SubmitStatus.Failed;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (notify_) {
            String message;
            if (success) {
                message = "Data submitted successfully.";
            } else {
                message = "Submit data failed!";
            }
            Bus bus = WTOttoBus.getInstance();
            bus.post(new SubmitDataAsyncTaskResultEvent(message, status));
        }

        if (listener != null) {
            listener.submitCompleted(status);
        }
        listener = null;
    }
}
