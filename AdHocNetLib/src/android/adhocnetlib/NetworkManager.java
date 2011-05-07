package android.adhocnetlib;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import android.adhocnetlib.BufferManager.BufferItem;
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
		
		public Timestamp lastSwitchTime = new Timestamp(new Date().getTime());
		public Timestamp lastActivityTime = new Timestamp(new Date().getTime());
		private boolean adhocClientModeStarted = false;
		public boolean successfullySent = false;
	}

	public static interface ReceivedDataListener {
		public void onReceiveData(byte[] data);
	}
	
	public static interface NetworkStateChangeListener {
		public void onNetworkStateChange(NetworkStates state);
	}
	
	private class StateChangeTimerTask extends TimerTask {
		@Override
		public void run() {			
			NetworkManager.getInstance().changeState();			
		}
		
	}
	
	private class ListeningThread extends Thread implements Runnable {
		private static final String TAG = "NetworkManager.ListeningThread";
		ServerSocket serverSocket = null;
		Socket clientSocket = null;
		@Override
		public void run() {
			Log.d(TAG, "ListeningThread started.");
			String message = null;
			try {
				String wifiIP = netUtils.getIP();
				serverSocket = new ServerSocket(adhocServerListeningPort, 10, InetAddress.getByName(wifiIP));
				serverSocket.setSoTimeout((int)(NetworkSwitchPolicy.Default.adhocServerTime - 100));
				Log.d(TAG, "Started listening.");
				Toast ("Started listening.");
				while (state == NetworkStates.ADHOC_SERVER) {
					clientSocket = serverSocket.accept();
					managerState.lastActivityTime = new Timestamp(new Date().getTime());
					new Thread(new ReceivingThread(clientSocket)).start();
				}				
			}  catch (UnknownHostException uhe) {
				message= "ListeningThread exception: "+ uhe.toString();
				Log.e(TAG, message);
			} catch (IOException ioe) {
				message= "ListeningThread exception: "+ ioe.toString();
				Log.e(TAG, message);
			} catch (Exception e) {
				message= "ListeningThread exception: "+ e.toString();
				Log.e(TAG, message);
			} finally {
				try {
					if (serverSocket != null) {
						while(!serverSocket.isClosed()) serverSocket.close();
					}
				} catch (IOException e) {}
			}
		}
		
		public void closeAllSockets() {
			if(serverSocket != null) {
				try {
					serverSocket.close();
				} catch (IOException e) {
					Loge("Couldn't close server socket in listening thread: " + e);
				}
			}
			if(clientSocket != null){
				try {
					clientSocket.close();
				} catch (IOException e) {
					Loge("Couldn't close client socket in listening thread: " + e);
				}
			}
		}
	}
	
	private class ReceivingThread extends Thread implements Runnable {
		
		private static final String TAG = "NetworkManager.ReceivingThread";
		private Socket socket = null;
		
		public ReceivingThread (Socket s) {
			socket = s;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public void run() {
			Log.d(TAG, "ReceivingThread started.");
			String message = null;
			try {
				// Receive client.id
				// Get buffer items that have not been already sent to client.id
				// Send server.id and the buffer items
				// Receive buffer items and add it to the buffer
				
				UUID clientid = null;
				try {
					ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
					clientid = (UUID) ois.readObject();
					Toast("Received client id: " + clientid.toString());
					Logd("Received client id: " + clientid.toString());
				} catch (Exception e) {
					Loge("Failed in receiving clientid: " + e);
					throw e; 
				}
				
				try {
					ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
					oos.writeObject(NetworkManager.getInstance().uniqueID);
					Log.d(TAG, "Sent server UUID");
				} catch (Exception e) {
					Loge("Failed in serializing server UUID: " + e);
					throw e;
				}
				
				ArrayList<BufferItem> receivedBufferItems = null;
				try {
					ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
					receivedBufferItems = (ArrayList<BufferItem>) ois.readObject();
					bufferManager.addItems(receivedBufferItems);
					Toast("Received buffer items");
					Log.d(TAG, "Received buffer items");
				} catch (Exception e) {
					Loge("Failed in deserializing buffer items: " + e);
					Toast("Failed in deserializing buffer items: " + e);
					throw e;
				}
				
				Toast("Data successfully received.");
				
			} catch (Exception e) {
				message= "ReceivingThread exception: "+ e.toString();
				Log.d(TAG, message);
			} finally {
				try {
					socket.close();
				} catch (IOException e) {}
			}
		}
	}
	
	private class SendingThread extends Thread implements Runnable {

		private static final String TAG = "NetworkManager.SendingThread";
		private Socket socket = null;
		
		@Override
		public void run() {
			Log.d(TAG, "SendingThread started.");
			String message = null;
			try {
				managerState.successfullySent = false;
				int attempts = 20;
				managerState.lastActivityTime = new Timestamp(new Date().getTime());
				// Send client.id
				// Receive server.id and buffer items and add them to the buffer
				// Get buffer items that have not been already sent to server.id
				// Send the buffer items to server
				
				Thread.sleep(1000);
				
				while (attempts > 0) {
					try {
						try{
							socket = new Socket(netUtils.getWifiAdhocServerIP(),adhocServerListeningPort);
							Logd("Socket created in sending thread");
						} catch (Exception e) {
							Loge("Exception while creating socket in sending thread: " + e);
							throw e;
						}
						
						try {
							ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
							oos.writeObject(NetworkManager.getInstance().uniqueID);
							Logd("Sent client id");							
						} catch (Exception e) {
							Loge("Exception in sending client id: " + e);
							throw e;
						}
					
						UUID serverid = null;
						try {
							ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
							serverid = (UUID) ois.readObject();
							Toast("Received " + serverid);
							Logd("Received " + serverid);
						} catch (Exception e) {
							Loge("Exception in deserializing serverid: " + e);
							throw e;
						}					
					
						try {
							ArrayList<BufferItem> toSend = bufferManager.getAllItemsForNodeID(serverid);
							ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
							oos.writeObject(toSend);
							Logd("Sent buffer items");
							
						} catch (Exception e) {
							Loge("Exception in serializing buffer items: " + e);
							throw e;
						} 
						break;
					} catch (Exception e) { 
						Loge("Exception in SendingThread at " + attempts + " attempt: " + e);
						attempts--;
						if (attempts == 0) {
							throw new Exception ("Finally gave up!");
						}
					}					
				}
				
				managerState.successfullySent = true;
				Toast("Data successfully sent.");
			} catch (Exception e) {
				message= "SendingThread exception: "+ e.toString();
				Loge(message);
			} finally {
				try{
					socket.close();
				} catch (Exception e) {
					Logd("Error while trying to close socket in sending thread: " + e);
				}
			}
		}
	}

	// ---------------- Instance Fields ----------------  
	
	private NetworkStates state = NetworkStates.DISABLED;
	private NetworkSwitchModes switchMode = NetworkSwitchModes.AUTOMATIC;	
	private NetworkSwitchPolicy switchPolicy = NetworkSwitchPolicy.Default; 
	
	private WifiManager wifiManager = null;
	private Context context = null;
	private NetworkUtilities netUtils = NetworkUtilities.getInstance();
	public UUID uniqueID = UUID.randomUUID(); //CHANGE THIS TO PRIVATE
	private BufferManager bufferManager = new BufferManager();
	
	private NetworkManagerState managerState = new NetworkManagerState();	
	private Timer stateChangeTimer = new Timer("autoStateChangeTimer", true);
	private TimerTask stateChangeTimerTask = new StateChangeTimerTask();
	private long stateChangeTimerDelay = 500;
	private int adhocServerListeningPort = 12345;
	private ReceivedDataListener receivedDataListener = null;
	private NetworkStateChangeListener networkStateChangeListener = null;
	
	private ListeningThread listeningThread = null;
	private SendingThread sendingThread = null;
	
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
		netUtils.initialize(c);
		receivedDataListener = listener;
		setState(NetworkStates.DISABLED);
		managerState.initialized = true;
		return true;
	}
	
	public void setSwitchModeToManual() {
		switchMode = NetworkSwitchModes.MANUAL;
	}
	
	public void setSwitchModeToAutomatic() {
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
			if (switchMode == NetworkSwitchModes.AUTOMATIC) {
				stateChangeTimer.schedule(stateChangeTimerTask, stateChangeTimerDelay, stateChangeTimerDelay);		
			}
			if (networkStateChangeListener != null) {
				networkStateChangeListener.onNetworkStateChange(state);
			}
			Toast("Started");
			Logd("Started");
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
		boolean result =  bufferManager.createNewItem(data, ttl, uniqueID);
		Toast("BufferManager has "+ bufferManager.getBufferSize()+" items.");
		return result;
	}

	public boolean registerCallBackForReceivedData(ReceivedDataListener listener) {
		if (!checkIfInitialized()) return false;
		receivedDataListener = listener;
		return false;
	}
	
	public boolean registerCallBackForNetworkStateChange(NetworkStateChangeListener listener ) {
		if (!checkIfInitialized())  return false;
		networkStateChangeListener = listener;
		return true;
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
								
			switch (state) {
			case ADHOC_SERVER: 
				if (listeningThread != null) {
					listeningThread.interrupt();
					listeningThread = null;
				}
				netUtils.stopAdhocServerMode();
				Logd("Server mode stopped");
				//Toast("Server mode stopped");
				break;
			case ADHOC_CLIENT: 
				netUtils.stopAdhocClientMode(); 
				managerState.adhocClientModeStarted = false; 
				Logd("Client mode stopped");
				//Toast("Client mode stopped");
				break;
			}
			
			switch (newState) {
			case DISABLED: 
				netUtils.stopWifi();
				Logd("All modes disabled");
				//Toast("All modes disabled");
				break;
			case ADHOC_SERVER: 
				netUtils.startAdhocServerMode(); 
				if(listeningThread == null) {
					listeningThread = new ListeningThread();
				}	
				Logd("Server mode started");
				//Toast("Server mode started");
				listeningThread.start();
				break;
			case ADHOC_CLIENT: 
				managerState.adhocClientModeStarted = false;
				netUtils.initiateAdhocClientMode(new NetworkUtilities.AdhocClientModeStartListener() {				
					@Override
					public void onAdhocClientModeReady() {
						OnAdhocClientModeStarted();
					}				
				});
				Logd("Client mode started");
				//Toast("Client mode started");
				break;
			default: Loge("Unexpected new state: " + newState);
			}
			state = newState;
			if (networkStateChangeListener != null) {
				networkStateChangeListener.onNetworkStateChange(state);
			}
		}
	}
	
	public NetworkManager.NetworkStates getState () {
		return state;
	}
	
	public void destroy() {
		if (state == NetworkStates.ADHOC_SERVER) {
			netUtils.stopAdhocServerMode();
		} else if (state == NetworkStates.ADHOC_CLIENT) {
			netUtils.stopAdhocClientMode();
		}
	}
	
	private void changeState() {
		if(!checkIfStarted()) return;		
		NetworkStates newState =  switchPolicy.getNextState(state, managerState);
		if (state != newState) setState(newState);
	}
	
	private void OnAdhocClientModeStarted() {
		managerState.adhocClientModeStarted = true;
		// get ip address and then connect at a predefined port
		sendingThread = new SendingThread();
		sendingThread.start();
	}

	public void Toast(final String message) {
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
