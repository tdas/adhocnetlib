package android.adhocnetlib;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.http.MethodNotSupportedException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.Toast;


public class NetworkUtilities {
	
	private class ScanReceiver extends BroadcastReceiver {
		  private static final String TAG = "NetworkUtilities.ScanningThread.ScanReceiver";
		  
		  boolean done = false;
		  boolean found = false;
		  String message = null;
		  private List<ScanResult> scanResults = null;
		  private String requiredSSID = "AndroidTether";
		  
		  @Override
		  public void onReceive(Context c, Intent intent) {
			  try {
				  
				  synchronized (NetworkUtilities.instance) {	
					  context.unregisterReceiver(this);
					  scanResults = wifiManager.getScanResults();
					  if (!scanStarted) {
						  //context.unregisterReceiver(this);
						  return;
					  }
					  if (scanResults!= null) {
							// select AndroidTether and connect
					    	message =  "Wifi scan complete, "+scanResults.size()+" results found.";
						    //Toast(message);
					    	found = false;
							for (ScanResult result: scanResults) {
								if (result.SSID.contains(requiredSSID)) {
									found = true;
									message = "Found Wifi = [ " + result.toString()+" ].";
									Logd(message);
									//Toast(message);
									/*
									WifiConfiguration wc = new WifiConfiguration();
									wc.SSID = "\"" + result.SSID+"\"";
									wc.BSSID = null;
									wc.hiddenSSID = false;
									wc.allowedKeyManagement.set(KeyMgmt.NONE);
									
									int netID = wifiManager.addNetwork(wc);
									if (netID >= 0) {
										message = "Added WifiConfiguration, InetID = " + netID + ".";								
										Logd(message);
										Toast(message);
									} else  {
										message = "Could not add WifiConfiguration, InetID = " + netID + ".";								
										Loge(message);
										Toast(message);
										continue;
									}
									*/
									int netID = -1;
									List<WifiConfiguration> wifiConfigs = wifiManager.getConfiguredNetworks();
							    	for (WifiConfiguration config : wifiConfigs) {
							    		if (config.SSID.contains(result.SSID)) {
							    			netID = config.networkId;
							    		}
							    	}
							    									
									if (!wifiManager.enableNetwork(netID, false)) {
										message = "Connection unsuccessful.";
										Loge(message);
										Toast(message);
									} else {	
										message = "Waiting for IP address.";
										Toast(message);
										Date scanStartTime = new Date();
										while (!hasWifiIP() && new Date().getTime() - scanStartTime.getTime() < 10 * 1000) {
											Thread.sleep(500);
										} 
										if (!hasWifiIP()) {
											message = "Connected, but could not get IP address.";
											Logd(message);
											Toast(message);
										} else {
											message = "Successfully connected.";
											Logd(message);
											Toast(message);
											done = true;
											//context.unregisterReceiver(this);
											adhocClientModeStartListener.onAdhocClientModeReady();
											return;		
										}
																		
									}								
								}
							}
							if (!found) {
								message = requiredSSID +" not found.";
								Logd(message);
								Toast(message);
							}
						} else {
							message = "No wifi networks found.";
							Logd(message);
							Toast(message);
						}
				  }
				  context.registerReceiver(this, scanIntentFilter);
			} catch (InterruptedException e) {
				Logd("ScanReceiver thread interrupted.");
			}
		  }
	}
	
	public static interface AdhocClientModeStartListener {
		public void onAdhocClientModeReady (); 
	}
	
	private static final String TAG = "NetworkUtilities";
	private static NetworkUtilities instance = new NetworkUtilities();
	
	private Context context = null;
	private WifiManager wifiManager = null;
	private boolean initialized = false;
	private boolean tetherStarted = false;
	private boolean scanStarted = false;
	private AdhocClientModeStartListener adhocClientModeStartListener = null;
	private ScanReceiver scanReceiver = null;
	private IntentFilter scanIntentFilter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
	
	private NetworkUtilities () {
		
	}
	
	public synchronized boolean initialize(Context c) {
		context = c;
		wifiManager = (WifiManager) c.getSystemService(Context.WIFI_SERVICE);
		initialized = true;
		return true;
	}
	
	public boolean checkIfInitialized() {
		if (!initialized) {
			Loge("NetworkUtilities not yet initialized.");
		}
		return initialized;
	}
	   
    public synchronized boolean startWifi() {
        if (!checkIfInitialized()) return false; 
        
        boolean done=false;
    	while (!done && !Thread.currentThread().isInterrupted()) {
    		wifiManager.setWifiEnabled(true);
    		// Waiting for interface-shutdown
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// nothing
			}
			if (!wifiManager.isWifiEnabled())
				done=false;
			else
				done=true;
    	}
    	if (!done) {
    		Loge("Wifi not started!");
    	} else {
    		Logd("Wifi started.");
    	}
    	return done;
    }
    
    public boolean stopWifi() {
        if (!checkIfInitialized()) return false; 
        
        boolean done=false;
    	while (!done && !Thread.currentThread().isInterrupted()) {
    		wifiManager.setWifiEnabled(false);
    		// Waiting for interface-shutdown
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// nothing
			}
			if (wifiManager.isWifiEnabled())
				done=false;
			else
				done=true;
    	}
    	if (!done) {
    		Loge("Wifi not started!");
    	} else {
    		Logd("Wifi started.");
    	}
		return done;
    }
   
    public synchronized boolean startAdhocServerMode() {
        if (!checkIfInitialized()) return false; 
    	
        if (tetherStarted) {
        	Logd("AdhocServer already started.");
        	return true;
        }
    	if (!stopWifi()) {
    		Loge("AdhocServer mode not enabled! Could not disable WiFi.");
    		return false;
    	}
    	if (!GeneralUtilities.runRootCommand("/data/data/android.tether/bin/tether start 1")) {    		
    		Loge("AdhocServer mode not started!");
    		return false;
    	} else {
    		tetherStarted = true;
    		Logd("AdhocServer mode started.");
    		return true;
    	}
    }
    
    public synchronized boolean stopAdhocServerMode() {
    	if (!checkIfInitialized()) return false; 
    	
    	if (!tetherStarted) {
    		Logd("AdhocServer not started.");
    		return true;
    	}
    	
    	if (!GeneralUtilities.runRootCommand("/data/data/android.tether/bin/tether stop 1")) {    		
    		Loge("AdhocServer mode not stopped!");
    		return false;
    	} else {
    		tetherStarted = false;
    		
    		if (!stopWifi()) {
    			Logd("AdhocServer mode stopped, but could not stop WiFi.");
    		} else {
    			Logd("AdhocServer mode stopped.");
    		}
    		return true;
    	}
    }
    
    public synchronized boolean initiateAdhocClientMode(AdhocClientModeStartListener listener ) {
    	if (!checkIfInitialized()) return false; 
    	
    	if (scanStarted) {
    		Logd("AdhocClient mode starting already initiated.");
    		return true;
    	}
    	
    	if (tetherStarted) {
    		if (!stopAdhocServerMode()) {
    			Loge("AdhocClient mode not started! Could not stop tether.");
    			return false;
    		} else {
    			Logd("Tether stopped to start AdhocClient mode.");
    		}
    	}
    	
    	if (listener == null) {
    		Loge("AdhocClient mode not started! Listener is null.");
    	}
    	
    	
    	// Option 1: start wifi, the phone will automatically connect to AndroidTether
    	// Option 2: start wifi, force the phone to connect to AndroidTether by starting a thread that repeatedly scans to find and connect to AndroidTether
    	if (!startWifi()) {
    		Loge("AdhocClient mode not started. Could not start WiFi.");
    	}
    	
    	adhocClientModeStartListener = listener;
    	//scanningThread = new Thread(new ScanningThread());
    	//scanningThread.start();
    	scanReceiver = new ScanReceiver(); 
    	context.registerReceiver(scanReceiver, scanIntentFilter);
    	if (wifiManager.startScan()) {
    		scanStarted = true;
    		Logd("Scan started");
    		Toast("Scan started");
    		return true;
    	} else {
    		Logd("Scan not started");
    		Toast("Scan not started");
    		return false;
    	}
    }
    
    public synchronized boolean stopAdhocClientMode() {
    	if (!checkIfInitialized()) return false; 
    	
    	// Option 1: start wifi, the phone will automatically connect to AndroidTether
    	// Option 2: start wifi, force the phone to connect to AndroidTether
    	/*if (!scanStarted) {
    		Logd("AdhocClient mode has not been started.");
    		return true;
    	}*/
    	scanStarted = false;
    	try {
    		context.unregisterReceiver(scanReceiver);    	
    	} catch (Exception e) {
    		
    	}

    	if (!stopWifi()) {
    		Loge("AdhocClient mode not stopped. Could not stop WiFi.");
    	} else {    	
    		Logd("AdhocClient mode stopped.");
    		Toast("AdhocClient mode stopped.");
    	}
    	return true;
    }
    
    public List<WifiConfiguration> getWifiConfigurations() {
    	List<WifiConfiguration> wifiConfigs = wifiManager.getConfiguredNetworks();
    	for (WifiConfiguration config : wifiConfigs) {
    		Toast(config.toString());
    	}
    	return wifiConfigs;
    }
    
    public String getWifiIP() {
    	
    	WifiInfo myWifiInfo = wifiManager.getConnectionInfo();
        int myIp = myWifiInfo.getIpAddress();
    	int intMyIp3 = myIp/0x1000000;
        int intMyIp3mod = myIp%0x1000000;
       
        int intMyIp2 = intMyIp3mod/0x10000;
        int intMyIp2mod = intMyIp3mod%0x10000;
       
        int intMyIp1 = intMyIp2mod/0x100;
        int intMyIp0 = intMyIp2mod%0x100;
        
        return String.valueOf(intMyIp0) + "." + String.valueOf(intMyIp1) + "." + String.valueOf(intMyIp2) + "." + String.valueOf(intMyIp3);
    }
    
    public boolean hasWifiIP() {
    	if (getWifiIP().startsWith("192")) {
    		return true;
    	}
    	return false;
    }
    
    private void Toast(String message) {
		Toast.makeText(context, message, 500).show();
	}
    
    public static NetworkUtilities getInstance() {
    	return instance;
    }
    
    private static void Logd(String msg) {
		Log.d(TAG, msg);
	}
	
	private static void Loge(String msg) {
		Log.e(TAG, msg);
	}

}
