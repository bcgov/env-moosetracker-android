package ca.bc.gov.fw.wildlifetracker;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class LicenseAgreementActivity extends Activity implements
        OnClickListener {

    private Button agreementButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.licenseagreement);
        this.init();
    }

    private void init() {
        int licenseButtonId = R.id.licenseAgreeButton;
        this.agreementButton = (Button) findViewById(licenseButtonId);
        if (this.agreementButton == null) {
            System.out.println("License agreement Button id=" + licenseButtonId + " not found in view");
            return;
        }
        this.agreementButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.equals(agreementButton)) {
            // do stuff to save the fact that agreement is good
            setLicenseRead();
            setResult(RESULT_OK);
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        // Can't back out of this activity. We require the license to be accepted. If the user hits
        // Back, just return to the home screen by sending this task to the back of the stack.
        moveTaskToBack(true);
    }

    private void setLicenseRead() {
        System.out.println("Setting license read flag");
        SharedPreferences profile = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = profile.edit();
        editor.putBoolean(MainActivity.LICENSE_READ_KEY, true);
        editor.apply();
    }

}
