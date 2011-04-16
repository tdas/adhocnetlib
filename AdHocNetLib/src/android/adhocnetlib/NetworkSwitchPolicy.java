package android.adhocnetlib;

import java.sql.Timestamp;
import java.util.Date;

import android.util.Log;

public class NetworkSwitchPolicy {
	public long disabledTime;
	public long adhocServerTime;
	public long adhocClientTime;
	
	public NetworkSwitchPolicy (long dTime, long asTime, long acTime) {
		disabledTime = dTime;
		adhocServerTime = asTime;
		adhocClientTime = acTime;
	}

	public NetworkManager.NetworkStates getNextState(NetworkManager.NetworkStates curState, Timestamp lastSwitchTime, Timestamp lastActivityTime) {
		NetworkManager.NetworkStates nextState = NetworkManager.NetworkStates.DISABLED; 
		Date now = new Date();
		switch (curState) {
		case DISABLED:
			if (now.getTime() - lastSwitchTime.getTime() > disabledTime) 
				nextState = NetworkManager.NetworkStates.ADHOC_SERVER;
			break;
		case ADHOC_SERVER:
			if (now.getTime() - lastSwitchTime.getTime() > adhocServerTime) 
				nextState = NetworkManager.NetworkStates.ADHOC_CLIENT;
			break;
		case ADHOC_CLIENT:
			if (now.getTime() - lastSwitchTime.getTime() > adhocClientTime) 
				nextState = NetworkManager.NetworkStates.ADHOC_CLIENT;
			break;
		default: Loge("Unexpected state: "+ curState.toString());
		}
		Logd("Next state: " + nextState.toString());
		return nextState;
	}
	
	public static final NetworkSwitchPolicy Default = new NetworkSwitchPolicy(0, 10, 12);
	
	private static void Logd(String msg) {
		Log.d("NetworkSwitchPolicy", msg);
	}
	
	private static void Loge(String msg) {
		Log.e("NetworkSwitchPolicy", msg);
	}
}
