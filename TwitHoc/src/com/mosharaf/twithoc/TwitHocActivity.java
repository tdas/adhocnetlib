package com.mosharaf.twithoc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;

import android.adhocnetlib.NetworkManager;
import android.adhocnetlib.NetworkManager.ReceivedDataListener;
import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.TabHost;
import android.widget.Toast;

public class TwitHocActivity extends TabActivity {
  public static class TwitReceivedDataListener implements ReceivedDataListener {

	public static TwitReceivedDataListener instance = null;
	
	public static TwitReceivedDataListener getInstance(TwitHocActivity activity_) {
		if (instance == null) {
			instance = new TwitReceivedDataListener(activity_);
		}
		return instance;
	}
	
	private TwitHocActivity activity;  
		
	private TwitReceivedDataListener ( TwitHocActivity activity_) {
		activity = activity_;
	}
	  
	@Override
	public void onReceiveData(byte[] data) {
		ObjectInputStream ois;
		Message message = null;
		
		try {
			ois = new ObjectInputStream (new ByteArrayInputStream (data));
			message = (Message) ois.readObject();
			ois.close();
		} catch (Exception e) {
			Log.e("TwitReceivedDataListener","Error deserialiing received data " + e.toString());
		}
		try {
			// Add to local database
			if (message != null) {
				final Message finalMessage = message;
				activity.runOnUiThread(new Runnable() {

					@Override
					public void run() {
						new MessageData(activity).createNew(finalMessage);
						if (TimelineActivity.instance!=null) {
							TimelineActivity.instance.refreshTimeline();
						}
						Toast.makeText(activity.getApplicationContext(), "Received message " + finalMessage.messageID, Toast.LENGTH_SHORT ).show();
					}
					
				});
			} else {
			}
		}
		catch (Exception e ) {
			String str = "Error creating message: " + e.toString();
			Log.e("TwitReceivedDataListener", str);
		}
	}  
  }	
	
  public static final String DATABASE_NAME = "twithoc.db";
  public static final int DATABASE_VERSION = 1;
  
  protected GroupData groupData;
  protected MessageData messageData;
  
  protected TabHost tabHost;
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    
    // Resource object to get Drawables
    Resources res = getResources(); 

    // Initialize NetworkManager
    NetworkManager.getInstance().initialize(this, null);
    NetworkManager.getInstance().start();

    // Add NetworkManager callback function
    NetworkManager.getInstance().registerCallBackForReceivedData(TwitReceivedDataListener.getInstance(this));

    // Create database objects
    groupData = new GroupData(this);
    groupData.createTable();
    messageData = new MessageData(this);
    messageData.createTable();
    
    fillTablesWithTestData();
    
    // Get the activity TabHost
    tabHost = getTabHost();
    // Reusable TabSpec for each tab
    TabHost.TabSpec spec;
    // Reusable Intent for each tab
    Intent intent;

    // Create an Intent to launch an Activity for the tab (to be reused)
    intent = new Intent().setClass(this, TimelineActivity.class);

    // Initialize a TabSpec for each tab and add it to the TabHost
    spec = tabHost.newTabSpec(getString(R.string.timeline_label))
        .setIndicator(getString(R.string.timeline_label), res.getDrawable(R.drawable.ic_tab_timeline))
        .setContent(intent);
    tabHost.addTab(spec);

    // Do the same for the other tabs
    intent = new Intent().setClass(this, NewMessageActivity.class);
    spec = tabHost.newTabSpec(getString(R.string.new_message_label))
        .setIndicator(getString(R.string.new_message_label), res.getDrawable(R.drawable.ic_tab_new_message))
        .setContent(intent);
    tabHost.addTab(spec);

    intent = new Intent().setClass(this, ManageGroupsActivity.class);
    spec = tabHost.newTabSpec(getString(R.string.manage_groups_label))
        .setIndicator(getString(R.string.manage_groups_label), res.getDrawable(R.drawable.ic_tab_manage_groups))
        .setContent(intent);
    tabHost.addTab(spec);

    // Make timeline the default tab
    switchTab(getString(R.string.timeline_label));    
  }  
  
//Add some default database values
  private void fillTablesWithTestData() {
    // groupData.dropTable();
    // groupData.createTable();
    if (groupData.count() == 0) {
      groupData.insert("1", "Family", "A");
      groupData.insert("2", "Friends", "B");
      groupData.insert("3", "Community", "C");
    }

    /*
    messageData.dropTable();
    messageData.createTable();
    if (messageData.count() == 0) {
      messageData.createNew("Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.", "1", 300000);
      messageData.createNew("Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.", "1", 300000);
      messageData.createNew("A quick brown fox jumped over the lazy dog.", "2", 300000);
      messageData.createNew("Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.", "1", 300000);
      messageData.createNew("The lazy dog didn't like it. Not at all.", "1", 300000);
      messageData.createNew("Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.", "3", 300000);
    }
    */
  }
  
  @Override
  public void onDestroy() {
	  super.onDestroy();
	  Toast.makeText(this,"Exiting", Toast.LENGTH_SHORT ).show();
	  NetworkManager.getInstance().destroy();
  }
  
  public void switchTab(String tabTag){
    tabHost.setCurrentTabByTag(tabTag);
  }
}