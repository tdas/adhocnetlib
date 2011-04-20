package android.adhocnetlib;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import android.app.Activity;
import android.content.Context;
import android.content.IntentSender.SendIntentException;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.Toast;

public final class NetworkManager {
	
	// ---------------- Enumerations ----------------  
	public static enum NetworkStates { DISABLED, ADHOC_SERVER, ADHOC_CLIENT };
	
	public enum NetworkSwitchModes { AUTOMATIC, MANUAL }

	// ---------------- Subclasses ----------------	
	
	public class NetworkManagerState {
		public boolean started = false;
		public boolean initialized = false;
		public boolean newMessage = false;
		
		public Timestamp lastSwitchTime = new Timestamp(0);
		public Timestamp lastActivityTime = new Timestamp(0);
		private boolean adhocClientModeStarted = false;
	}

	public static interface ReceivedDataListener {
		public void onReceiveData(byte[] data);
	}
	
	private class StateChangeTimerTask extends TimerTask {
		@Override
		public void run() {			
			NetworkManager.getInstance().changeState();			
		}
		
	}
	
	private class ListeningThread implements Runnable {
		private static final String TAG = "NetworkManager.ListeningThread";

		@Override
		public void run() {
			Log.d(TAG, "ListeningThread started.");
			ServerSocket serverSocket = null;
			String message = null;
			try {
				String wifiIP = netUitls.getIP();
				serverSocket = new ServerSocket(adhocServerListeningPort, 10, InetAddress.getByName(wifiIP));
				while (true) {
					Log.d(TAG, "Started listening.");
					Toast ("Started listening.");
					Socket clientSocket = serverSocket.accept();
					new Thread(new ReceivingThread(clientSocket)).start();
				}
				
			}  catch (UnknownHostException uhe) {
				message= "ListeningThread exception: "+ uhe.toString();
				Log.d(TAG, message);
			} catch (IOException ioe) {
				message= "ListeningThread exception: "+ ioe.toString();
				Log.d(TAG, message);
			} catch (Exception e) {
				message= "ListeningThread exception: "+ e.toString();
				Log.d(TAG, message);
			}
			
			try {
				if (serverSocket != null) serverSocket.close();
			} catch (IOException e) {
				
			}
		}
	}
	
	private class ReceivingThread implements Runnable {
		
		private static final String TAG = "NetworkManager.ReceivingThread";
		private Socket socket = null;
		
		public ReceivingThread (Socket s) {
			socket = s;
		}
		
		@Override
		public void run() {
			Log.d(TAG, "ReceivingThread started.");
			String message = null;
			try {
				
				// Receive client.id
				// Get buffer items that have not been already sent to client.id
				// Send server.id and the buffer items
				// Receive buffer items and add it to the buffer
				
				BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				PrintWriter output = new PrintWriter(socket.getOutputStream(),true);
				
				String synStr = input.readLine();
				Log.d(TAG, "Received " + synStr);
				output.println("SYNACK");
				Log.d(TAG, "Sent SYNACK");
				String ackStr = input.readLine();
				Log.d(TAG, "Received " + ackStr);
				
				Toast("Data successfully received.");
				
			} catch (IOException ioe) {
				message= "ReceivingThread exception: "+ ioe.toString();
				Log.d(TAG, message);
			} catch (Exception e) {
				message= "ReceivingThread exception: "+ e.toString();
				Log.d(TAG, message);
			}
			try {
				socket.close();
			} catch (IOException e) {
				
			}
		}
		
	}
	
	private class SendingThread implements Runnable {

		private static final String TAG = "NetworkManager.SendingThread";
		
		@Override
		public void run() {
			Log.d(TAG, "SendingThread started.");
			String message = null;
			try {
				
				// Send client.id
				// Receive server.id and buffer items and add them to the buffer
				// Get buffer items that have not been already sent to server.id
				// Send the buffer items to server
				
				Thread.sleep(1000);
				Socket socket = new Socket(netUitls.getWifiAdhocServerIP(),adhocServerListeningPort);
				BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				PrintWriter output = new PrintWriter(socket.getOutputStream(),true);
				output.println("SYN");
				Log.d(TAG, "Sent SYN");
				String synackStr = input.readLine();
				Log.d(TAG, "Received " + synackStr);
				output.println("ACK");
				Log.d(TAG, "Sent ACK");
				
				Toast("Data successfully sent.");
				
				
			}  catch (UnknownHostException uhe) {
				message= "SendingThread exception: "+ uhe.toString();
				Log.e(TAG, message);
			} catch (IOException ioe) {
				message= "SendingThread exception: "+ ioe.toString();
				Log.e(TAG, message);
			} catch (Exception e) {
				message= "SendingThread exception: "+ e.toString();
				Log.e(TAG, message);
			}
		}
		
	}

	// ---------------- Instance Fields ----------------  
	
	private NetworkStates state = NetworkStates.DISABLED;
	private NetworkSwitchModes switchMode = NetworkSwitchModes.AUTOMATIC;	
	private NetworkSwitchPolicy switchPolicy = NetworkSwitchPolicy.Default; 
	
	
	
	private WifiManager wifiManager = null;
	private Context context = null;
	private NetworkUtilities netUitls = NetworkUtilities.getInstance();
	private UUID uniqueID = UUID.randomUUID();
	private BufferManager bufferManager = new BufferManager();
	
	private NetworkManagerState managerState = new NetworkManagerState();	
	private Timer stateChangeTimer = new Timer("autoStateChangeTimer", true);
	private TimerTask stateChangeTimerTask = new StateChangeTimerTask();
	private long stateChangeTimerDelay = 500;
	private int adhocServerListeningPort = 12345;
	private ReceivedDataListener receivedDataListener = null;
	
	private Thread listeningThread = null;
	private Thread sendingThread = null;
	
	// --------------- Class Fields --------------------
	private static final String TAG = "NetworkManager";
	private static NetworkManager singleInstance = new NetworkManager();
	
	// ---------------- Instance Methods ----------------  
	private NetworkManager() {
		
	}
	
	public boolean initialize (Context c) {
		return initialize(c, null);
	}

	public boolean initialize (Context c, ReceivedDataListener listener) {
		context = c;
		wifiManager = (WifiManager) c.getSystemService(Context.WIFI_SERVICE); ;
		netUitls.initialize(c);
		receivedDataListener = listener;
		setState(NetworkStates.DISABLED);
		managerState.initialized = true;
		return true;
	}
	
	public void setSwitchModeToManual() {
		switchMode = NetworkSwitchModes.MANUAL;
	}
	
	public void setSwitchModeToAutomaticl() {
		switchMode = NetworkSwitchModes.AUTOMATIC;
	}
	
	public boolean setSwitchPolicy(NetworkSwitchPolicy policy) {
		if (!checkIfInitialized()) return false;
		switchPolicy = policy;
		return true;
	}
	
	public synchronized boolean start() {
		if (!checkIfStarted()) {
			managerState.started = true;
			Logd("Started");
		}
		if (switchMode == NetworkSwitchModes.AUTOMATIC) {
			stateChangeTimer.schedule(stateChangeTimerTask, stateChangeTimerDelay);		
		}
		return managerState.started;
	}
	
	public synchronized boolean stop() {
		if (checkIfStarted()) {
			managerState.started = false;
			Logd("Stopped");
		}
		return !managerState.started;
	}
	
	public boolean sendData(byte[] data, long ttl) {
		if (!checkIfStarted()) return false;
		return bufferManager.createNewItem(data, 120 * 1000, uniqueID);
	}

	public boolean registerCallBackForReceivedData() {
		if (!checkIfInitialized()) return false;
		return false;
	}
	
	private boolean checkIfInitialized() {
		if (!managerState.initialized) {
			Loge("NetworkManager not yet intialized");
		}
		return managerState.initialized;
	}
	
	private boolean checkIfStarted() {
		if (!managerState.started) {
			Loge("NetworkManager not yet started");
		}
		return managerState.started;
	}
	
	public synchronized void setState (NetworkStates newState) {
		if (state != newState) {
			managerState.lastSwitchTime = new Timestamp(new Date().getTime());
			managerState.lastActivityTime = managerState.lastSwitchTime;
			
			switch (state) {
			case ADHOC_SERVER: 
				if (listeningThread != null) {
					listeningThread.stop();
				}
				netUitls.stopAdhocServerMode(); 
				break;
			case ADHOC_CLIENT: 
				if (sendingThread != null) {
					sendingThread.stop();
				}
				netUitls.stopAdhocClientMode(); 
				managerState.adhocClientModeStarted = false; 
				break;
			}
			
			switch (newState) {
			case DISABLED: netUitls.stopWifi(); break;
			case ADHOC_SERVER: 
				netUitls.startAdhocServerMode(); 
				listeningThread = new Thread(new ListeningThread());
				listeningThread.start();
				break;
			case ADHOC_CLIENT: 
				managerState.adhocClientModeStarted = false;
				netUitls.initiateAdhocClientMode(new NetworkUtilities.AdhocClientModeStartListener() {				
					@Override
					public void onAdhocClientModeReady() {
						OnAdhocClientModeStarted();
					}				
				});
				break;
			default: Loge("Unexpected new state: " + newState);
			}
			state = newState;		
		}
	}
	
	private  void changeState() {
		if (!checkIfStarted()) return;
		stateChangeTimer.cancel();
		
		NetworkStates newState =  switchPolicy.getNextState(state, managerState.lastSwitchTime, managerState.lastActivityTime);
		setState(newState);
		
		if (switchMode == NetworkSwitchModes.AUTOMATIC) {
			stateChangeTimer.schedule(stateChangeTimerTask, stateChangeTimerDelay);		
		}
	}
	
	private void OnAdhocClientModeStarted() {
		managerState.adhocClientModeStarted = true;
		// get ip address and then connect at a predefined port
		sendingThread = new Thread(new SendingThread());
		sendingThread.start();
	}

	private void Toast(final String message) {
		final Activity a = (Activity)context;
		a.runOnUiThread(new Runnable() {
		    public void run() {
		        Toast.makeText(a, message, 500).show();
		    }
		});
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
