package android.adhocnetlib;

import java.sql.Timestamp;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

public final class NetworkManager {
	
	// ---------------- Enumerations ----------------  
	public static enum NetworkStates { DISABLED, ADHOC_SERVER, ADHOC_CLIENT };
	
	public enum NetworkSwitchModes { AUTOMATIC, MANUAL }

	// ---------------- Subclasses ----------------
	
	// ---------------- Instance Fields ----------------  
	private NetworkStates state = NetworkStates.DISABLED;
	//private NetworkSwitchModes switchMode = NetworkSwitchModes.AUTOMATIC;	
	private NetworkSwitchPolicy switchPolicy = NetworkSwitchPolicy.Default; 
	
	private WifiManager wifiManager = null;
	
	private boolean started = false;
	private boolean initialized = false;
	private boolean newMessage = false;
	private Timestamp lastSwitchTime = new Timestamp(0);
	private Timestamp lastActivityTime = new Timestamp(0);
	
	// --------------- Class Fields --------------------
	private static NetworkManager singleInstance = new NetworkManager();
	
	// ---------------- Instance Methods ----------------  
	private NetworkManager() {
		
	}

	public boolean initialize (Activity mainActivity) {
		wifiManager = (WifiManager) mainActivity.getSystemService(Context.WIFI_SERVICE); ;
		NetworkUtilities.initialize(wifiManager);
		initialized = true;
		return true;
	}
	
	public boolean setSwitchPolicy(NetworkSwitchPolicy policy) {
		if (!checkIfInitialized()) return false;
		switchPolicy = policy;
		return true;
	}
	
	public synchronized boolean start() {
		if (!checkIfStarted()) {
			started = true;
			Logd("Started");
		}

		return started;
	}
	
	public synchronized boolean stop() {
		if (checkIfStarted()) {
			started = false;
			Logd("Stopped");
		}
		return !started;
	}
	
	public boolean sendData(byte[] data, long ttl) {
		if (!checkIfStarted()) return false;
		
		return false;
	}

	public boolean registerCallBackForReceivedData() {
		if (!checkIfInitialized()) return false;
		return false;
	}
	
	private boolean checkIfInitialized() {
		if (!initialized) {
			Loge("NetworkManager not yet intialized");
		}
		return initialized;
	}
	
	private boolean checkIfStarted() {
		if (!started) {
			Loge("NetworkManager not yet started");
		}
		return started;
	
	}

	private synchronized void changeState() {
		if (!checkIfStarted()) return;
		
		NetworkStates newState =  switchPolicy.getNextState(state, lastSwitchTime, lastActivityTime);
		if (state != newState) {
			lastActivityTime = lastSwitchTime;
			
			switch (state) {
			case ADHOC_SERVER: NetworkUtilities.stopAdhocServerMode(); break;
			case ADHOC_CLIENT: NetworkUtilities.stopAdhocClientMode(); break;
			}
			
			switch (newState) {
			case DISABLED: NetworkUtilities.stopWifi(); break;
			case ADHOC_SERVER: NetworkUtilities.startAdhocServerMode(); break;
			case ADHOC_CLIENT: NetworkUtilities.startAdhocClientMode();
			default: Loge("Unexpected new state: " + newState);
			}
			state = newState;
		}
	}
	
	// ---------------- Class Methods ----------------  
	public static NetworkManager getInstance() {
		return singleInstance;
	}

	private static void Logd(String msg) {
		Log.d("NetworkManager", msg);
	}
	
	private static void Loge(String msg) {
		Log.e("NetworkManager", msg);
	}
	
}
