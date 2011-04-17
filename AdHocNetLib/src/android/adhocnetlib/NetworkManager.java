package android.adhocnetlib;

import java.sql.Timestamp;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

public final class NetworkManager {
	
	// ---------------- Enumerations ----------------  
	public static enum NetworkStates { DISABLED, ADHOC_SERVER, ADHOC_CLIENT };
	
	public enum NetworkSwitchModes { AUTOMATIC, MANUAL }

	// ---------------- Subclasses ----------------	
	private class StateChangeTimerTask extends TimerTask {
		@Override
		public void run() {			
			NetworkManager.getInstance().changeState();			
		}
		
	}

	
	// ---------------- Instance Fields ----------------  
	private NetworkStates state = NetworkStates.DISABLED;
	private NetworkSwitchModes switchMode = NetworkSwitchModes.AUTOMATIC;	
	private NetworkSwitchPolicy switchPolicy = NetworkSwitchPolicy.Default; 
	
	private WifiManager wifiManager = null;
	private Context context = null;
	private NetworkUtilities netUitls = NetworkUtilities.getInstance();
	
	private boolean started = false;
	private boolean initialized = false;
	private boolean newMessage = false;
	
	private Timestamp lastSwitchTime = new Timestamp(0);
	private Timestamp lastActivityTime = new Timestamp(0);
	private Timer stateChangeTimer = new Timer("autoStateChangeTimer", true);
	private TimerTask stateChangeTimerTask = new StateChangeTimerTask();
	private long stateChangeTimerDelay = 500;
	
	private boolean AdhocClientModeStarted = false;
	
	// --------------- Class Fields --------------------
	private static final String TAG = "NetworkManager";
	private static NetworkManager singleInstance = new NetworkManager();
	
	// ---------------- Instance Methods ----------------  
	private NetworkManager() {
		
	}

	public boolean initialize (Context c) {
		context = c;
		wifiManager = (WifiManager) c.getSystemService(Context.WIFI_SERVICE); ;
		netUitls.initialize(c);
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
		if (switchMode == NetworkSwitchModes.AUTOMATIC) {
			stateChangeTimer.schedule(stateChangeTimerTask, stateChangeTimerDelay);		
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
		stateChangeTimer.cancel();
		
		NetworkStates newState =  switchPolicy.getNextState(state, lastSwitchTime, lastActivityTime);
		if (state != newState) {
			lastActivityTime = lastSwitchTime;
			
			switch (state) {
			case ADHOC_SERVER: netUitls.stopAdhocServerMode(); break;
			case ADHOC_CLIENT: netUitls.stopAdhocClientMode(); AdhocClientModeStarted = false; break;
			}
			
			switch (newState) {
			case DISABLED: netUitls.stopWifi(); break;
			case ADHOC_SERVER: netUitls.startAdhocServerMode(); break;
			case ADHOC_CLIENT: 
				AdhocClientModeStarted = false;
				netUitls.initiateAdhocClientMode(new NetworkUtilities.AdhocClientModeStartListener() {
				@Override
				public void onAdhocClientModeReady() {
					OnAdhocClientModeStarted();
				}				
			});
			default: Loge("Unexpected new state: " + newState);
			}
			state = newState;
		}
		
		if (switchMode == NetworkSwitchModes.AUTOMATIC) {
			stateChangeTimer.schedule(stateChangeTimerTask, stateChangeTimerDelay);		
		}
	}
	
	private void OnAdhocClientModeStarted() {
		AdhocClientModeStarted = true;
		// get ip address and then connect at a predefined port
	}
	
	
	// ---------------- Class Methods ----------------  
	public static NetworkManager getInstance() {
		return singleInstance;
	}

	private static void Logd(String msg) {
		Log.d(TAG, msg);
	}
	
	private static void Loge(String msg) {
		Log.e(TAG, msg);
	}
	
}
