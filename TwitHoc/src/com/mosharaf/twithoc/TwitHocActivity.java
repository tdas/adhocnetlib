package com.mosharaf.twithoc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;

import android.adhocnetlib.NetworkManager;
import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.TabHost;
import android.widget.Toast;

public class TwitHocActivity extends TabActivity {
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
    NetworkManager.getInstance().registerCallBackForReceivedData(new NetworkManager.ReceivedDataListener() {
		@Override
		public void onReceiveData(byte[] data) {
			ObjectInputStream ois;
			Message message = null;
			
			try {
				ois = new ObjectInputStream (new ByteArrayInputStream (data));
				message = (Message) ois.readObject();
				ois.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			Toast.makeText(getApplicationContext(), "Received Stuff " + message.message, Toast.LENGTH_SHORT ).show();
			// Add to local database
			messageData.createNew(message);
		}
	});
    
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
    
    // Create database objects
    groupData = new GroupData(this);
    messageData = new MessageData(this);
    
    // Add some default database values
    groupData.dropTable();
    groupData.createTable();
    if (groupData.count() == 0) {
      groupData.insert("1", "Earl Grey", "A");
      groupData.insert("2", "Assam", "B");
      groupData.insert("3", "Jasmine Green", "C");
      groupData.insert("4", "Darjeeling", "D");
    }

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
  }  
  
  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
//     if ((keyCode == KeyEvent.KEYCODE_BACK)) {
//    	 NetworkManager.getInstance().destroy();
//    	 System.exit(0);
//     }
//     Toast.makeText(this,"Exiting", Toast.LENGTH_SHORT ).show();
     return super.onKeyDown(keyCode, event);
  }
  
  public void switchTab(String tabTag){
    tabHost.setCurrentTabByTag(tabTag);
  }
}