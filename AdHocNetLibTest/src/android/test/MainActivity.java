package android.test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.http.conn.util.InetAddressUtils;

import android.adhocnetlib.NetworkManager;
import android.adhocnetlib.BufferManager.BufferItem;
import android.adhocnetlib.NetworkManager.NetworkStates;
import android.adhocnetlib.NetworkUtilities;
import android.adhocnetlib.NetworkUtilities.AdhocClientModeStartListener;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
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
import android.widget.TextView;
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
	TextView display = null;
	Spinner toggleDropDown = null;
	ArrayAdapter<CharSequence> dropDownAdapter = null;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        display = (TextView) findViewById(R.id.TextView02);
                
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
        
        networkManager.initialize(this);
        networkManager.setSwitchModeToAutomatic();
        networkManager.start();
        NetworkManager.NetworkStates state = networkManager.getState();
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
				
				new Thread(new Runnable () {
					
					@Override
					public void run() {
						String TAG = "TestThread";
						try 
						{				
							ServerSocket ss = new ServerSocket(2345, 10, InetAddress.getByName("localhost"));
							Socket cs = new Socket(InetAddress.getByName("localhost"), 2345);
							Socket socket = ss.accept();
							try {
								BufferItem.serialize(new BufferItem("SYN".getBytes(), 20000,
										NetworkManager.getInstance().uniqueID), 
										cs.getOutputStream());
								Log.d(TAG, "Sent SYN");
								Toast("Sent SYN");
							} catch (Exception e) {
								Log.e(TAG, "Exception in serializing syn: " + e);
							}
							
							byte[] synByteArray = null;
							String synStr = "";
							try {
								synByteArray = BufferItem.deserialize(socket.getInputStream()).data.bytes;
								synStr = new String(synByteArray);
								Log.d(TAG, "Received " + synStr);
								Toast("Received " + synStr);
							} catch (Exception e) {
								Log.e(TAG, "Failed in deserializing syn: " + e);
								Toast("Failed in deserializing syn: " + e);
							}
														
							Toast("Data successfully received.");
							ss.close();
							cs.close();
							socket.close();
						} catch (Exception ex) {
							Toast("Exception in receiving phase: " + ex);
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
	
	public void setModeClient () {
		networkManager.setState(NetworkStates.ADHOC_CLIENT);
		setUIModeClient();
	}
	
	public void setUIModeClient() {
		adhocServerToggleButton.setChecked(false);
		adhocClientToggleButton.setChecked(true);
		wifiToggleButton.setChecked(true);
		allModeButton.setText("Client");
		display.setText("Client");
		display.setTextColor(Color.WHITE);
	}
	
	public void setModeServer () {
		networkManager.setState(NetworkStates.ADHOC_SERVER);
		setUIModeServer();		
	}
	
	public void setUIModeServer() {
		adhocServerToggleButton.setChecked(true);
		adhocClientToggleButton.setChecked(false);
		wifiToggleButton.setChecked(true);
		allModeButton.setText("Server");
		display.setText("Server");
		display.setTextColor(Color.WHITE);
	}
	
	public void setModeDisabled () {
		networkManager.setState(NetworkStates.DISABLED);
		setUIModeDisabled();	
	}
	
	public void setUIModeDisabled () {
		adhocServerToggleButton.setChecked(false);
		adhocClientToggleButton.setChecked(false);
		wifiToggleButton.setChecked(false);
		allModeButton.setText("Disabled");
		display.setText("Disabled");
		display.setTextColor(Color.WHITE);	
	}
	
	public void setMode (String mode) {
		display.setText("Please wait...");
		display.setTextColor(Color.RED);
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