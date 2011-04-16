package android.test;

import android.adhocnetlib.NetworkUtilities;
import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;


public class MainActivity extends Activity implements OnClickListener {
    Button button1 = null;
    Button button2 = null;
	Button button3 = null;
    Button button4 = null;
	Button button5 = null;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        button1 = (Button) findViewById(R.id.button1);
        button2 = (Button) findViewById(R.id.button2);
        button3 = (Button) findViewById(R.id.button3);
        button4 = (Button) findViewById(R.id.button4);
        button5 = (Button) findViewById(R.id.button5);
        
        button1.setOnClickListener(this);
        button2.setOnClickListener(this);
        button3.setOnClickListener(this);
        button4.setOnClickListener(this);
        button5.setOnClickListener(this);
        
        WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE); 
        NetworkUtilities.initialize(wifiManager);
        
    }

	@Override
	public void onClick(View arg0) {
		try {
			
			if (arg0 == (View)button1) {
				if (NetworkUtilities.startWifi()) {
					showMessage("WiFi started.");
				} else {
					showMessage("WiFi not started.");
				}					
			} else if (arg0 == (View)button2) {
				if (NetworkUtilities.stopWifi()) {
					showMessage("WiFi stopped.");
				} else {
					showMessage("WiFi not stopped.");
				}
			} else if (arg0 == (View)button3) {
				if (NetworkUtilities.startAdhocServerMode()) {
					showMessage("AdHocServer started.");
				} else {
					showMessage("AdHocServer not started.");
				}
			} else if (arg0 == (View)button4) {
				if (NetworkUtilities.stopAdhocServerMode()) {
					showMessage("AdHocServer stopped.");
				} else {
					showMessage("AdHocServer not stopped.");
				}
			}else if (arg0 == (View)button5) {
				showMessage("Exiting");
				System.exit(0);
			}
		} catch (Exception e) {
			Log.e("Test", e.toString());
			showMessage("Error: " + e); 
					
		}
	}
	
	public void showMessage(String message) {
		Toast.makeText(this, message, 1000).show();
	}
}