package android.adhocnetlib;

import java.sql.Timestamp;

import android.net.wifi.WifiManager;
import android.util.Log;

public final class NetworkManager {
	
	// ---------------- Enumerations ----------------  
	public static enum NetworkStates { DISABLED, ADHOC_SERVER, ADHOC_CLIENT };
	
	public enum NetworkSwitchModes { AUTOMATIC, MANUAL }

	// ---------------- Subclasses ----------------
	
	// ---------------- Instance Fields ----------------  
	private NetworkStates state = NetworkStates.DISABLED;
	private NetworkSwitchModes switchMode = NetworkSwitchModes.AUTOMATIC;	
	private NetworkSwitchPolicy switchPolicy = NetworkSwitchPolicy.Default; 
	
	private WifiManager wifiManager = null;
	
	private boolean started = false;
	private boolean newMessage = false;
	private Timestamp lastSwitchTime = new Timestamp(0);
	private Timestamp lastActivityTime = new Timestamp(0);
	
	// --------------- Class Fields --------------------
	private static NetworkManager singleInstance = new NetworkManager();
	
	// ---------------- Instance Methods ----------------  
	private NetworkManager() {
		
	}

	public boolean initialize (WifiManager wifiMngr) {
		wifiManager = wifiMngr;
		NetworkUtilities.initialize(wifiMngr);
		return true;
	}
	
	public boolean setSwitchPolicy(NetworkSwitchPolicy policy) {
		switchPolicy = policy;
		return true;
	}
	
	public synchronized boolean start() {
		if (!started) {
			started = true;
			Logd("Started");
		}

		return started;
	}
	
	public synchronized void changeState() {
		NetworkStates newState =  switchPolicy.getNextState(state, lastSwitchTime, lastActivityTime);
		if (state != newState) {
			lastActivityTime = lastSwitchTime;
			switch (newState) {
			case DISABLED: NetworkUtilities.stopWifi(); break;
			case ADHOC_SERVER: NetworkUtilities.enableAdhocServerMode(); break;
			case ADHOC_CLIENT: NetworkUtilities.enableAdhocClientMode(); break;
			default: Loge("Unexpected new state: " + newState);
			}
		}
	}

	public synchronized boolean stop() {
		if (started) {
			started = false;
			Logd("Stopped");
		}
		return !started;
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
