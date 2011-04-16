package android.adhocnetlib;

import org.apache.http.MethodNotSupportedException;

import android.net.wifi.WifiManager;
import android.util.Log;


public class NetworkUtilities {
	// Wifi
	private static WifiManager wifiManager = null;
	private static boolean initialized = false;
	private static boolean tetherStarted = false;
	
	public static boolean initialize(WifiManager wifimngr) {
		wifiManager = wifimngr;
		initialized = true;
		return true;
	}
	
	public static boolean checkIfInitialized() {
		if (!initialized) {
			Loge("NetworkUtilities not yet initialized.");
		}
		return initialized;
	}
	
    public static boolean stopWifi() {
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
    
    public static boolean startWifi() {
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
    
    public static boolean startAdhocServerMode() {
        if (!checkIfInitialized()) return false; 
    	
        if (tetherStarted) {
        	Logd("AdhocServer alread started.");
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
    
    public static boolean stopAdhocServerMode() {
    	if (!checkIfInitialized()) return false; 
    	
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
    
    public static boolean startAdhocClientMode() {
    	if (!checkIfInitialized()) return false; 
    	
    	if (tetherStarted) {
    		if (!stopAdhocServerMode()) {
    			Loge("AdhocClient not started! Could not stop tether.");
    			return false;
    		} else {
    			Logd("Tether stopped to start AdhocClient mode.");
    		}
    	}
    	
    	// Option 1: start wifi, the phone will automatically connect to AndroidTether
    	// Option 2: start wifi, force the phone to connect to AndroidTether
    	if (!startWifi()) {
    		Loge("AdhocClient mode not started. Could not start WiFi.");
    	}
    	
    	return false;
    }
    
    public static boolean stopAdhocClientMode() {
    	if (!checkIfInitialized()) return false; 
    	
    	// Option 1: start wifi, the phone will automatically connect to AndroidTether
    	// Option 2: start wifi, force the phone to connect to AndroidTether
    	if (!stopWifi()) {
    		Loge("AdhocClient mode not started. Could not start WiFi.");
    	}    	
    	return false;
    }
    
    private static void Logd(String msg) {
		Log.d("NetworkUtilities", msg);
	}
	
	private static void Loge(String msg) {
		Log.e("NetworkUtilities", msg);
	}
	
}
