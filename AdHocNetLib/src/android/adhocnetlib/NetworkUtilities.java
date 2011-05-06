package android.adhocnetlib;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.http.MethodNotSupportedException;

import android.app.Activity;
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
		private String requiredSSID = "AndroidTether";
		String message = null;
		
		@Override
		public void onReceive(Context c, Intent intent) {

			List<ScanResult> scanResults = null;			
			boolean done = false;
			boolean found = false;
			boolean configured = false;
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
							
							//find the required configured network
							int netID = -1;
							List<WifiConfiguration> wifiConfigs = wifiManager.getConfiguredNetworks();
					    	for (WifiConfiguration config : wifiConfigs) {
					    		if (config.SSID.contains(result.SSID)) {
					    			netID = config.networkId;
					    			break;
					    		}
					    	}
					    	
					    	// connect to the configured network
					    	if (netID >= 0) {
					    		configured = true;
					    		if (connect(netID)) {
					    			done = true;
									message = "Successfully connected.";
									Logd(message);
									Toast(message);
									if (adhocClientModeStartListener != null) {
										adhocClientModeStartListener.onAdhocClientModeReady();
									}
									return;
					    		}
					    	} 
					    	break;					    	
						}
					}
				
					if (!found) {
						message = requiredSSID +" not found.";
						Logd(message);
						Toast(message);
					} else if (found && !configured) {
						message = requiredSSID +" found, but not configured.";
						Logd(message);
						Toast(message);
					}
					
				} else {
					message = "No wifi networks found.";
					Logd(message);
					Toast(message);
				}
				
				  
				context.registerReceiver(this, scanIntentFilter);
				new Thread( new Runnable() {
					public void run() {
						try {
							Thread.sleep(1000 * 5);
							} catch (InterruptedException e) {
							
							}
							if (!scanStarted) {
								//context.unregisterReceiver(this);
								return;
							}
							wifiManager.startScan();
						}
					}).start(); 
				}
				
			} catch (Exception e) {
				Logd("ScanReceiver thread interrupted.");
			}
		}
	
		private boolean connect(int netID) throws InterruptedException {
			String ssid = "";
			if(hasWifiIP()) {
				Logd("Wifi already connected -- need not attempt again");
				Toast("Wifi already connected -- need not attempt again");
				return true;				
			}
			//attempting to associate with the required network
			int attempt = 10;			
			while (attempt > 0) {	
				if (!wifiManager.enableNetwork(netID, true)) {
					message = "Attempt "+attempt+": Could not enable network "+requiredSSID+".";
					Loge(message);
					//Toast(message);
				} else {
					WifiInfo wifiInfo = wifiManager.getConnectionInfo();
					ssid = wifiInfo.getSSID();
					if (ssid != null && !ssid.contains(requiredSSID)){
						message = "Attempt "+attempt+": Could not associate with "+requiredSSID+".";
						Loge(message);
						//Toast(message);						
					} else {
						break;
					}
				}
				attempt --;
				Thread.sleep(500);
			}			
			if (attempt <= 0) {
				Toast("Could not enable and connect to "+requiredSSID+".");
				return false;
			}
			
			// checking whether an IP address has been obtained
			attempt = 60;
			while (attempt >= 0 ) {
				if (!hasWifiIP()) {
					message = "Attempt "+attempt+": Connected, but could not get IP address.";
					Logd(message);
				} else {
					break;
				}
				attempt --;
				Thread.sleep(500);
			}
			if (attempt <= 0) {
				Toast("Connected to "+ssid+", but could not get IP address.");
				return false;
			}
			return true;
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
	
	
	public synchronized boolean initialize(Context c) {
		context = c;
		wifiManager = (WifiManager) c.getSystemService(Context.WIFI_SERVICE);
		initialized = true;
		return true;
	}
	
	public synchronized boolean startWifi() {    	
    	boolean done = startWifi_();
    	if (!done) {
    		Loge("Wifi not started!");
    	} else {
    		Logd("Wifi started.");
    	}
    	return done;
    }
    
    public synchronized boolean stopWifi() {
    	if (tetherStarted) {
    		if (stopTether()) {
    			Loge("Disabling tether before stopping Wifi failed.");
    		}
    	}
    	boolean done = stopWifi_();
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
    	if (!stopWifi_()) {
    		Loge("AdhocServer mode not enabled! Could not disable WiFi.");
    		return false;
    	}
    	if (!startTether()) {    		
    		Loge("AdhocServer mode not started!");
    		return false;
    	} else {
    		tetherStarted = true;
    		Logd("AdhocServer mode started.");
    		return true;
    	}
    }
    
    public synchronized boolean stopAdhocServerMode () {
    	if (!checkIfInitialized()) return false; 
    	
    	if (!tetherStarted) {
    		Logd("AdhocServer not started.");
    		return true;
    	}
    	
    	if (!stopTether()) {    		
    		Loge("AdhocServer mode not stopped!");
    		return false;
    	} else {
    		tetherStarted = false;
    		
    		if (!stopWifi_()) {
    			Logd("AdhocServer mode stopped, but could not stop WiFi.");
    		} else {
    			Logd("AdhocServer mode stopped.");
    		}
    		return true;
    	}
    }
    
    public synchronized boolean initiateAdhocClientMode (AdhocClientModeStartListener listener ) {
    	
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
    	if (!startWifi_()) {
    		Loge("AdhocClient mode not started. Could not start WiFi.");
    		Toast("AdhocClient mode not started. Could not start WiFi.");
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

    	if (!stopWifi_()) {
    		Loge("AdhocClient mode not stopped. Could not stop WiFi.");
    	} else {    	
    		Logd("AdhocClient mode stopped.");
    		//Toast("AdhocClient mode stopped.");
    	}
    	return true;
    }
	   
    public List<WifiConfiguration> getWifiConfigurations() {
    	List<WifiConfiguration> wifiConfigs = wifiManager.getConfiguredNetworks();
    	for (WifiConfiguration config : wifiConfigs) {
    		//Toast(config.toString());
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
    
    public String getIP() {
    	try {
    		List<NetworkInterface> intfs = Collections.list(NetworkInterface.getNetworkInterfaces());
    		for (NetworkInterface intf: intfs ) {
    			List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
    			for (InetAddress addr: addrs) {
    				if (!addr.isLoopbackAddress()) {
    					return addr.getHostAddress().toString();
    				}
    			}
    		}
    	} catch (SocketException ex) {
    		Loge(ex.toString());
    	}
    	return null;
    }
    
    public String getWifiAdhocServerIP() {
    	WifiInfo myWifiInfo = wifiManager.getConnectionInfo();
        int myIp = myWifiInfo.getIpAddress();
    	int intMyIp3 = myIp/0x1000000;
        int intMyIp3mod = myIp%0x1000000;
       
        int intMyIp2 = intMyIp3mod/0x10000;
        int intMyIp2mod = intMyIp3mod%0x10000;
       
        int intMyIp1 = intMyIp2mod/0x100;
        int intMyIp0 = intMyIp2mod%0x100;
        
        return String.valueOf(intMyIp0) + "." + String.valueOf(intMyIp1) + "." + String.valueOf(intMyIp2) + "." + String.valueOf(254);
    }
    
    public boolean hasWifiIP() {
    	if (getWifiIP().startsWith("192")) {
    		return true;
    	}
    	return false;
    }
    
    private NetworkUtilities () {
		
    }	
    
    private boolean checkIfInitialized() {
		if (!initialized) {
			Loge("NetworkUtilities not yet initialized.");
		}
		return initialized;
	}
    
    private boolean startWifi_() {
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
    	
    	return done;
    }
    
    private boolean stopWifi_() {
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
    	
		return done;
    }
    
    private boolean startTether() {
    	return GeneralUtilities.runRootCommand("/data/data/android.tether/bin/tether start 1");
    }
    
    private boolean stopTether() {
    	return GeneralUtilities.runRootCommand("/data/data/android.tether/bin/tether stop 1");
    }
    
    private void Toast(final String message) {
    	final Activity a = (Activity)context;
		a.runOnUiThread(new Runnable() {
		    public void run() {
		        Toast.makeText(a, message, 250).show();
		    }
		});
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
