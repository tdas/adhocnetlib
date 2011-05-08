package com.mosharaf.twithoc;

import java.util.HashMap;

import android.adhocnetlib.NetworkManager;
import android.adhocnetlib.NetworkManager.NetworkStates;
import android.app.Activity;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class TimelineActivity extends ListActivity 
  implements OnClickListener {
  
  protected GroupData groupData;
  protected MessageData messageData;
  
  protected TextView tvConnectionMode;
  protected ImageButton btRefresh;
  
  private HashMap<String, String> uuidToGroupName = new HashMap<String, String>();
    
  // Cursor containing the data
  Cursor groupCursor;
  Cursor messageCursor;
  
  private String[] displayFields;
  private int[] displayViews;

  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.timeline);
    
    // Connect interface elements
    tvConnectionMode = (TextView) this.findViewById(R.id.tv_connection_mode);
    btRefresh = (ImageButton) this.findViewById(R.id.bt_refresh_timeline);
    
    // Setup listeners
    btRefresh.setOnClickListener(this);
    
    // Create database objects
    groupData = new GroupData(this);
    messageData = new MessageData(this);

    displayFields = new String[] { MessageData.GROUP_ID, MessageData.MESSAGE, MessageData.POSTED_AT };
    displayViews = new int[] { R.id.tv_group_name, R.id.tv_message, R.id.tv_time_message_posted };
    
    messageCursor = messageData.all(this);
    setListAdapter(new TimelineCursorAdapter(this, R.layout.timeline_row, messageCursor, displayFields, displayViews));
    
    groupCursor = groupData.all(this);
    
    // Add NetworkManager status listener
    NetworkManager.getInstance().registerCallBackForNetworkStateChange(new NetworkManager.NetworkStateChangeListener() {			
		@Override
		public void onNetworkStateChange(NetworkStates state, String detail) {
			if (detail != null && detail.length() > 0) {
				detail = " - " + detail; 
			} else {
				detail = "";
			}
			switch (state) {			
			case ADHOC_CLIENT: setModeOnUIThread("Client"+ detail); break;
			case ADHOC_SERVER: setModeOnUIThread("Server"); break;
			case DISABLED: setModeOnUIThread("Off"); break;
			}				
		}
	});
  }
  
  public void setModeOnUIThread(final String mode) {
    final Activity a = this;
    a.runOnUiThread(new Runnable() {
	  public void run() {
        tvConnectionMode.setText(mode);
      }
    });		
  }

  @Override
  public void onResume() {
    super.onResume();
    messageCursor.requery();
  }
  
  @Override
  public void onClick(View v) {
    if (v == btRefresh) {
      messageCursor.requery();
      Toast.makeText(this,"" +messageData.count(), Toast.LENGTH_SHORT).show();
      messageCursor.requery();
  	  messageCursor.moveToFirst();
      while (messageCursor.isAfterLast() == false) {
  		String mID = messageCursor.getString(messageCursor.getColumnIndex(MessageData.MESSAGE_ID));
  		String m = messageCursor.getString(messageCursor.getColumnIndex(MessageData.MESSAGE));
  		
  		messageCursor.moveToNext();
  	  }
    }
  }
  
  public void refreshTimeline() {
      messageCursor.requery();
  }

  private Cursor getMessagesToDisplay() {
    String[] projectionIn = { GroupData.NAME, MessageData.MESSAGE, MessageData.POSTED_AT };
    String sortOrder = MessageData.POSTED_AT;

    String sql = 
      GroupData.TABLE_NAME + " gdTable INNER JOIN "
      + MessageData.TABLE_NAME + " mdtable ON (gdTable."
      + GroupData.GROUP_ID + "=mdTable."
      + MessageData.GROUP_ID + ")";

    SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
    queryBuilder.setTables(sql);
    
    return null;
    // return queryBuilder.query(db, projectionIn, selection, null, null, null, sortOrder);
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.timeline, menu);

    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
	  
    switch(item.getItemId()) {
      case R.id.exit_twithoc:
    	NetworkManager.getInstance().destroy();
    	System.exit(0);
        return true;

      case R.id.toggle_mode:
        return true;
        
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  private class TimelineCursorAdapter extends SimpleCursorAdapter {
    private Cursor cursor;
    private Context context;
    
    public TimelineCursorAdapter(Context context, int layout, Cursor cursor,
        String[] from, int[] to) {
      super(context, layout, cursor, from, to);
      this.cursor = cursor;
      this.context = context;
    }
    
    @Override
    public View getView(int pos, View inView, ViewGroup parent) {
      View v = inView;
      if (v == null) {
           LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
           v = inflater.inflate(R.layout.timeline_row, null);
      }
      
      this.cursor.moveToPosition(pos);
      
      // TODO groupName has to be fixed 
      String groupID = this.cursor.getString(this.cursor.getColumnIndex(MessageData.GROUP_ID));
      if (uuidToGroupName.containsKey(groupID) == false) {
    	  refreshUUIDToGroupName();
      }
      String groupName = uuidToGroupName.get(groupID);
      String message = this.cursor.getString(this.cursor.getColumnIndex(MessageData.MESSAGE));
      
      String postedAtStr = this.cursor.getString(this.cursor.getColumnIndex(MessageData.POSTED_AT));
      long postedAt = Long.parseLong(postedAtStr);
      // long curTime = System.currentTimeMillis();
      // String postedTimeAgo = getTimeAgo((curTime - postedAt) / 1000);

      TextView tvGroupName = (TextView) v.findViewById(R.id.tv_group_name);
      tvGroupName.setText(groupName);
      
      TextView tvMessage = (TextView) v.findViewById(R.id.tv_message);
      tvMessage.setText(message);
      
      TextView tvMessagePosted = (TextView) v.findViewById(R.id.tv_time_message_posted);
      tvMessagePosted.setText(formatTimeToShow(postedAt));
      
      return(v);
    }
    
    private void refreshUUIDToGroupName() {
    	groupCursor.requery();
    	groupCursor.moveToFirst();
    	while (groupCursor.isAfterLast() == false) {
    		String gID = groupCursor.getString(groupCursor.getColumnIndex(GroupData.GROUP_ID));
    		String gName = groupCursor.getString(groupCursor.getColumnIndex(GroupData.NAME));
    		if (uuidToGroupName.containsKey(gID) ==  false) {
    			uuidToGroupName.put(gID, gName);
    		}
    		groupCursor.moveToNext();
    	}
    }
    
    // Expects time in milliseconds
    private String formatTimeToShow(long postedAt) {
    	String toRet = "";
    	
        long curTime = System.currentTimeMillis();
        long postedTimeAgo = (curTime - postedAt) / 1000;
    	
        // If more than a day, return date
        if (postedTimeAgo >= 86400) {
        	toRet = new java.text.SimpleDateFormat("MMM d").format(new java.util.Date (postedAt));
        } 
        // Otherwise, return time
        else {
        	toRet = new java.text.SimpleDateFormat("h:mm a").format(new java.util.Date (postedAt));
        }
    	
    	return toRet;
    }
    
    // Expects time difference in seconds
    private String getTimeAgo(long timeDiff) {
      String toRet = "";

      // Days
      if (timeDiff >= 86400) {
        long t = timeDiff / 86400;
        if (t > 1) {
          toRet += (t + "days"); 
        } else if (t == 1) {
          toRet += (t + "day");        
        }
        toRet += " ago";
      }
      // Hours
      else if (timeDiff >= 3600) {
        long t = timeDiff / 3600;
        if (t > 1) {
          toRet += (t + " hours"); 
        } else if (t == 1) {
          toRet += (t + " hour");        
        }
        toRet += " ago";
      }
      // Minute
      else if (timeDiff >= 60) {
        long t = timeDiff / 60;
        if (t > 1) {
          toRet += (t + " minutes"); 
        } else if (t == 1) {
          toRet += (t + " minute");        
        }
        toRet += " ago";
      }
      // Seconds
      else if (timeDiff > 0) {
        long t = timeDiff;
        if (t > 1) {
          toRet += (t + " seconds"); 
        } else if (t == 1) {
          toRet += (t + " second");        
        }
        toRet += " ago";
      }
      else {
        toRet += "just now";
      }
      
      return toRet;
    }
  }
}