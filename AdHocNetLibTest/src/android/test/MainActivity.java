package android.test;

import android.adhocnetlib.NetworkUtilities;
import android.app.Activity;
import android.app.NotificationManager;
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
		if (arg0 == (View)button1) {
			//Toast.makeText(this, "Button 1 Works", 1000).show();
			try {
				NetworkUtilities.enableWifi();
			} catch (Exception e) {
				Log.e("Test", e.toString());
			}
		} else if (arg0 == (View)button2) {
			//Toast.makeText(this, "Button 2 Works", 1000).show();
			NetworkUtilities.disableWifi();
		} else if (arg0 == (View)button5) {
			Toast.makeText(this, "Button 3 Works", 1000).show();
			System.exit(0);
		}  
	}    
}