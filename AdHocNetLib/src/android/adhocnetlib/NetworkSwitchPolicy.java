package android.adhocnetlib;

import java.sql.Timestamp;
import java.util.Date;

import android.adhocnetlib.NetworkManager.NetworkStates;
import android.util.Log;

public class NetworkSwitchPolicy {
	private static final String TAG = "NetworkSwitchPolicy";
	public long disabledTime;
	public long adhocServerTime;
	public long adhocClientTime;
	public double clientToServerProb;
	public boolean jumpBackToClient;
	
	public NetworkSwitchPolicy (long dTime, long asTime, long acTime, double prob) {
		disabledTime = dTime;
		adhocServerTime = asTime;
		adhocClientTime = acTime;
		clientToServerProb = prob;
		jumpBackToClient = false;
	}

	public NetworkManager.NetworkStates getNextState(NetworkManager.NetworkStates curState, NetworkManager.NetworkManagerState state) {
		NetworkManager.NetworkStates nextState = curState; 
		Date now = new Date();
		Logd("lastSwitchTime = " + state.lastSwitchTime.getTime());
		Logd("now = " + now.getTime());
		Logd("curState = " + curState);
		Logd("adhocClientTime = " + adhocClientTime);
		Logd("adhocServerTime = " + adhocServerTime);
		switch (curState) {
		case DISABLED:
			if (jumpBackToClient && now.getTime() - state.lastSwitchTime.getTime() > 3000) {
				jumpBackToClient = false;
				nextState = NetworkManager.NetworkStates.ADHOC_CLIENT;
			} else if (!jumpBackToClient && now.getTime() - state.lastSwitchTime.getTime() > disabledTime) {
				nextState = NetworkManager.NetworkStates.ADHOC_CLIENT;
			}
			break;
		case ADHOC_SERVER:			
			if (now.getTime() - state.lastSwitchTime.getTime() > adhocServerTime && now.getTime() - state.lastActivityTime.getTime() > adhocServerTime) 
				nextState = NetworkManager.NetworkStates.ADHOC_CLIENT;
			break;
		case ADHOC_CLIENT:
			boolean flipCoin = false;
			if (now.getTime() - state.lastActivityTime.getTime() > 10000) {				
				if (state.successfullySent) {
					NetworkManager.getInstance().Toast("Client successfully sent");
					nextState = NetworkManager.NetworkStates.DISABLED;
					state.successfullySent = false;
				} else if (now.getTime() - state.lastSwitchTime.getTime() > adhocClientTime) {
					NetworkManager.getInstance().Toast("Client time soft check");
					flipCoin = true;
				}
			} else if (now.getTime() - state.lastSwitchTime.getTime() > adhocClientTime * 3) {
				NetworkManager.getInstance().Toast("Client time hard check");
				flipCoin = true;
			}
			if (flipCoin) {
				if (Math.random() <= clientToServerProb) {
					nextState = NetworkManager.NetworkStates.ADHOC_SERVER;
					NetworkManager.getInstance().Toast("Flipped coin and switching to server");
					Logd("Flipped coin and switching to server");
				} else {
					jumpBackToClient = true;
					nextState = NetworkManager.NetworkStates.DISABLED;
					Logd("Flipped coin and staying in client");
				}
			}
			break;
		default: Loge("Unexpected state: "+ curState.toString());
		}
		Logd("Next state: " + nextState.toString());
		return nextState;
	}
	
	public static final NetworkSwitchPolicy Default = new NetworkSwitchPolicy(10000, (long)(120000+(Math.random()*30000)), 30000, 0.5);
	
	private static void Logd(String msg) {
		Log.d(TAG, msg);
	}
	
	private static void Loge(String msg) {
		Log.e(TAG, msg);
	}
}
