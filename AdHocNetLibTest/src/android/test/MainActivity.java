package android.test;

import android.adhocnetlib.NetworkUtilities;
import android.adhocnetlib.NetworkUtilities.AdhocClientModeStartListener;
import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ToggleButton;


public class MainActivity extends Activity implements OnClickListener {
    NetworkUtilities netUtil = NetworkUtilities.getInstance();

    Button exitButton = null;
    Button randomButton = null;
	ToggleButton wifiToggleButton = null;
	ToggleButton adhocServerToggleButton = null;
	ToggleButton adhocClientToggleButton = null;
	
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        randomButton = (Button) findViewById(R.id.randomButton);        
        randomButton.setOnClickListener(this);
        
        exitButton = (Button) findViewById(R.id.exitButton);        
        exitButton.setOnClickListener(this);
        
        wifiToggleButton = (ToggleButton) findViewById(R.id.wifiToggleButton);
        adhocServerToggleButton = (ToggleButton) findViewById(R.id.adhocServerToggleButton);
        adhocClientToggleButton = (ToggleButton) findViewById(R.id.adhocClientToggleButton);
        
        wifiToggleButton.setOnClickListener(this);
        adhocServerToggleButton.setOnClickListener(this);
        adhocClientToggleButton.setOnClickListener(this);
        
        //WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE); 
        netUtil.initialize(this);        
    }

	@Override
	public void onClick(View arg0) {
		try {
			
			if (arg0 == (View)exitButton) {
				Toast("Exiting");
				System.exit(0);
			} else if (arg0 == (View)wifiToggleButton) {
				if (wifiToggleButton.isChecked()) {
					if (netUtil.startWifi()) {
						Toast("WiFi started.");
					} else {
						Toast("WiFi not started.");
					}
				} else {
					if (netUtil.stopWifi()) {
						Toast("WiFi stopped.");
					} else {
						Toast("WiFi not stopped.");
					}
				}
			} else if (arg0 == (View) adhocServerToggleButton) {
				if (adhocServerToggleButton.isChecked()) {
					if (netUtil.startAdhocServerMode()) {
						Toast("AdHocServer started.");
					} else {
						Toast("AdHocServer not started.");
					}
				} else {
					if (netUtil.stopAdhocServerMode()) {
						Toast("AdHocServer stopped.");
					} else {
						Toast("AdHocServer not stopped.");
					}
				}
			} else if (arg0 == (View) adhocClientToggleButton) {
				if (adhocClientToggleButton.isChecked()) {
					if (netUtil.initiateAdhocClientMode(new AdhocClientModeStartListener() {
						@Override
						public void onAdhocClientModeReady() {
							Toast("AdhocClient started.");
						}
					})) {
						Toast("AdHocClient initiated.");
					} else {
						Toast("AdHocClient not initiated.");
					}
				} else {
					if (netUtil.stopAdhocClientMode()) {
						Toast("AdHocClient stopped.");
					} else {
						Toast("AdHocClient not stopped.");
					}
				}
			} else if (arg0 == (View)randomButton) {
				netUtil.getWifiConfigurations();
			}
			
		} catch (Exception e) {
			Log.e("Test", e.toString());
			Toast("Error: " + e); 
					
		}
	}
	
	public void Toast(String message) {
		Toast.makeText(this, message, 500).show();
	}
}