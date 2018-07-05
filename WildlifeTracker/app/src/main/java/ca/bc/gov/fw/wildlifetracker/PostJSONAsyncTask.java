package ca.bc.gov.fw.wildlifetracker;

import android.os.AsyncTask;

import com.squareup.otto.Bus;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * An AsyncTask that posts a JSON object to a server URL and retrieves a JSON response.
 */
public abstract class PostJSONAsyncTask extends AsyncTask<Void, Void, Void> {

    protected JSONObject responseJSON_;

    private String urlString_;
    protected JSONObject postData_;

    public PostJSONAsyncTask(String urlString, JSONObject postData) {
        super();
        urlString_ = urlString;
        postData_ = postData;
    }

    /** Subclasses must set up postData_ before execute() is called!
     *
     * @param urlString The string representation of the target URL
     */
    protected PostJSONAsyncTask(String urlString) {
        super();
        urlString_ = urlString;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            URL url = new URL(urlString_);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Content-Type", "application/json");

            // Starts the query
            conn.connect();

            // Send POST data
            System.out.println("POST JSON: " + postData_.toString());
            System.out.println("POST URL: " + url.toString());
            DataOutputStream outStream = new DataOutputStream(conn.getOutputStream());
            outStream.writeBytes(postData_.toString());
            outStream.flush();
            outStream.close();

            // Check response
            int responseCode = conn.getResponseCode();
            System.out.println("The response code is: " + responseCode);
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(),"utf-8"));
                String line;
                StringBuilder buf = new StringBuilder();
                while ((line = br.readLine()) != null) {
                    buf.append(line);
                }
                br.close();

                System.out.println("Response string: " + buf);
                responseJSON_ = new JSONObject(buf.toString());
                System.out.println("Response as parsed JSON: " + responseJSON_.toString());
            } else {
                System.out.println(conn.getResponseMessage());
            }

        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

//        try { Thread.sleep(5000); } catch (InterruptedException ex) { System.out.println("Interrupted"); }
        System.out.println("PostJSONAsyncTask.doInBackground exiting");
        return null;
    }
}
