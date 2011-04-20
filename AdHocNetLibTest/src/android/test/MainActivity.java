package android.test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.http.conn.util.InetAddressUtils;

import android.adhocnetlib.NetworkManager;
import android.adhocnetlib.NetworkManager.NetworkStates;
import android.adhocnetlib.NetworkUtilities;
import android.adhocnetlib.NetworkUtilities.AdhocClientModeStartListener;
import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ToggleButton;


public class MainActivity extends Activity implements OnClickListener {
	NetworkManager networkManager = NetworkManager.getInstance();
	NetworkUtilities netUtil = NetworkUtilities.getInstance();

    Button exitButton = null;
    Button randomButton = null;
    Button allModeButton = null;
	ToggleButton wifiToggleButton = null;
	ToggleButton adhocServerToggleButton = null;
	ToggleButton adhocClientToggleButton = null;
	Spinner toggleDropDown = null;
	ArrayAdapter<CharSequence> dropDownAdapter = null;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        allModeButton = (Button) findViewById(R.id.allModeButton);
        randomButton = (Button) findViewById(R.id.randomButton);        
        exitButton = (Button) findViewById(R.id.exitButton);        
        
        allModeButton.setOnClickListener(this);
        randomButton.setOnClickListener(this);
        exitButton.setOnClickListener(this);
        
        wifiToggleButton = (ToggleButton) findViewById(R.id.wifiToggleButton);
        adhocServerToggleButton = (ToggleButton) findViewById(R.id.adhocServerToggleButton);
        adhocClientToggleButton = (ToggleButton) findViewById(R.id.adhocClientToggleButton);
        
        toggleDropDown = (Spinner) findViewById(R.id.toggleDropDown);
        dropDownAdapter = ArrayAdapter.createFromResource(this, R.array.toggle_array, 
        		android.R.layout.simple_spinner_item);
        
        dropDownAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        toggleDropDown.setAdapter(dropDownAdapter);
        toggleDropDown.setOnItemSelectedListener(new toggleDropDownSelectedListener());
        
        wifiToggleButton.setOnClickListener(this);
        adhocServerToggleButton.setOnClickListener(this);
        adhocClientToggleButton.setOnClickListener(this);
        
        //WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE); 
        networkManager.initialize(this);
        networkManager.setSwitchModeToManual();
        networkManager.start();
        
    }

	@Override
	public void onClick(View arg0) {
		try {
			
			if (arg0 == (View)exitButton) {
				if (adhocServerToggleButton.isChecked()) {
					Toast("Stopping AdhocServer.");
					netUtil.stopAdhocServerMode();
				}
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
				//netUtil.getWifiConfigurations();
				new Thread(new Runnable () {
					
					@Override
					public void run() {
						String TAG = "TestThread";
						try 
						{	
							String ip = netUtil.getIP();
							if (ip == null) {
								ip = netUtil.getWifiIP();
							}
							ServerSocket ss = new ServerSocket(2345, 10, InetAddress.getByName(ip));
							Socket cs = ss.accept();
							BufferedReader input = new BufferedReader(new InputStreamReader(cs.getInputStream()));
							PrintWriter output = new PrintWriter(cs.getOutputStream(),true);
							
							String synStr = input.readLine();
							Log.d(TAG, "Received " + synStr);
							output.println("SYNACK");
							Log.d(TAG, "Sent SYNACK");
							String ackStr = input.readLine();
							Log.d(TAG, "Received " + ackStr);
							
							Toast("Data successfully received.");
							
							cs.close();
							ss.close();
							return;
						} catch (Exception ex) {
							Toast("Already listening");
						}
						
						try {
							String ip = netUtil.getIP();
							Socket cs = new Socket(ip, 2345);
							
							BufferedReader input = new BufferedReader(new InputStreamReader(cs.getInputStream()));
							PrintWriter output = new PrintWriter(cs.getOutputStream(),true);
							output.println("SYN");
							Log.d(TAG, "Sent SYN");
							String synackStr = input.readLine();
							Log.d(TAG, "Received " + synackStr);
							output.println("ACK");
							Log.d(TAG, "Sent ACK");
							
							Toast("Data successfully sent.");

							cs.close();
						} catch (Exception ex) {
							
						}

					}
					
				}).start();
				
			} else if (arg0 == (View)allModeButton) {
				if (allModeButton.getText().toString().contains("Disabled")) {
					setModeClient();
				} else if (allModeButton.getText().toString().contains("Client")) {
					setModeServer();
				} else if (allModeButton.getText().toString().contains("Server")) {
					setModeDisabled();
				}
			}
			
		} catch (Exception e) {
			Log.e("Test", e.toString());
			Toast("Error: " + e); 
					
		}
	}
	
	public void Toast2(String message) {
		
	}
	
	public void setModeClient () {
		networkManager.setState(NetworkStates.ADHOC_CLIENT);
		adhocServerToggleButton.setChecked(false);
		adhocClientToggleButton.setChecked(true);
		wifiToggleButton.setChecked(true);
		allModeButton.setText("Client");
	}
	
	public void setModeServer () {
		networkManager.setState(NetworkStates.ADHOC_SERVER);
		adhocServerToggleButton.setChecked(true);
		adhocClientToggleButton.setChecked(false);
		wifiToggleButton.setChecked(true);
		allModeButton.setText("Server");
	}
	
	public void setModeDisabled () {
		networkManager.setState(NetworkStates.DISABLED);
		adhocServerToggleButton.setChecked(false);
		adhocClientToggleButton.setChecked(false);
		wifiToggleButton.setChecked(false);
		allModeButton.setText("Disabled");
	}
	
	public void setMode (String mode) {
		if (mode.contains("Client")) {
			setModeClient();			
		} else if (mode.contains("Server")) {
			setModeServer();
		} else if (mode.contains("Disabled")){
			setModeDisabled();
		} else {
			Log.e("Error: ", "unrecognized mode.");
		}
	}
	
	public void Toast(final String message) {
		final Activity a = this;
		a.runOnUiThread(new Runnable() {
		    public void run() {
		        Toast.makeText(a, message, Toast.LENGTH_SHORT).show();
		    }
		});
	}
	
	public class toggleDropDownSelectedListener implements OnItemSelectedListener {

	    public void onItemSelected(AdapterView<?> parent,
	        View view, int pos, long id) {
	    	String selected = parent.getItemAtPosition(pos).toString();
	      setMode(selected);
	    }

	    public void onNothingSelected(AdapterView parent) {
	      // Do nothing.
	    }
	}
}