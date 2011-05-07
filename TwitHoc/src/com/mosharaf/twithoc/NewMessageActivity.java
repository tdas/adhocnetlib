package com.mosharaf.twithoc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import android.adhocnetlib.NetworkManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText; 
import android.widget.Toast;

public class NewMessageActivity extends Activity 
  implements OnClickListener {
  
  private static final int DIALOG_SELECT_GROUPS = 0;
  
  protected GroupData groupData;
  protected MessageData messageData;
  
  protected EditText etRecipients;
  protected EditText etMessage;
  protected Button btPost;
  protected Button btCancel;
  
  // Variable used to keep track of which recipient was selected.
  private String recipientGroup;
  private String recipientGroupID;
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.new_message);

    // Connect interface elements
    etRecipients = (EditText) this.findViewById(R.id.et_send_to);
    etMessage = (EditText) this.findViewById(R.id.et_message);
    btPost = (Button) this.findViewById(R.id.button_post_message);
    btCancel = (Button) this.findViewById(R.id.button_cancel_message);
    
    // Disable soft-keyboard for the recipients list
    etRecipients.setInputType(0);
    
    // Setup listeners
    btPost.setOnClickListener(this);
    btCancel.setOnClickListener(this);
    etRecipients.setOnClickListener(this);
    
    // Create database objects
    groupData = new GroupData(this);
    messageData = new MessageData(this);
  }
  
  protected Dialog onCreateDialog(int id) {
    Dialog dialog = null;
    
    switch(id) {
      case DIALOG_SELECT_GROUPS:
        dialog = createGroupSelectionDialog();
        break;
      default:
        dialog = null;
    }
    return dialog;
  }
  
  protected void onPrepareDialog(int id, Dialog dialog) { 
    switch(id) {
      case DIALOG_SELECT_GROUPS:
        
        break;
      default:
        break;
    }
  }
  
  protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
    Toast.makeText(this, getString(R.string.post_message_succeeded), Toast.LENGTH_SHORT).show();
  }
  
  @Override
  public void onClick(View v) {
    if (v == btPost) {
      if (etRecipients.getText().length() != 0) {
        if (etMessage.getText().length() != 0) {
          // If there is a message and a recipient, try to post
          if (postMessage(etMessage.getText().toString(), recipientGroupID)) {        	
            // Notify success
            Toast.makeText(this, getString(R.string.post_message_succeeded), Toast.LENGTH_SHORT).show();
            
            // Go back to the timeline tab
            TwitHocActivity thA = (TwitHocActivity) this.getParent();
            thA.switchTab(getString(R.string.timeline_label));
          } else {
            Toast.makeText(this, getString(R.string.post_message_failed), Toast.LENGTH_SHORT).show();
          }
        } else {
          Toast.makeText(this, getString(R.string.no_message_to_send), Toast.LENGTH_SHORT).show();
        }
      } else {
        Toast.makeText(this, getString(R.string.no_recipients_selected), Toast.LENGTH_SHORT).show();
      }
    } else if (v == btCancel) {
      // Go back to the timeline tab
      TwitHocActivity thA = (TwitHocActivity) this.getParent();
      thA.switchTab(getString(R.string.timeline_label));
    } else if (v == etRecipients) {
      showDialog(DIALOG_SELECT_GROUPS);
    }
  }
  
  private boolean postMessage(String messageToSend, String groupID) { 
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    Message message = new Message(messageToSend, groupID);
    ObjectOutputStream oos;
	
    try {
		oos = new ObjectOutputStream(bos);
		oos.writeObject(message);
	} catch (IOException e1) {
		e1.printStackTrace();
	}
	
    NetworkManager.getInstance().sendData(bos.toByteArray(), Message.expireAfter); 
    
    return false;    
  }
  
  private Dialog createTestDialog() {
    final CharSequence[] items = {"Red", "Green", "Blue"};

    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("Pick a color");
    builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int item) {
            Toast.makeText(getApplicationContext(), items[item], Toast.LENGTH_SHORT).show();
        }
    });
    return builder.create();
  }
  
  private Dialog createGroupSelectionDialog() {
    final Cursor cursor = groupData.all(this);

    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle(R.string.select_groups_dialog_title);
    builder.setSingleChoiceItems(cursor, -1, GroupData.NAME, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichItem) {
          cursor.moveToPosition(whichItem);
          recipientGroup = cursor.getString(cursor.getColumnIndex(GroupData.NAME));
          recipientGroupID = cursor.getString(cursor.getColumnIndex(GroupData.GROUP_ID));
          Toast.makeText(getApplicationContext(), "Clicked " + whichItem + " which was " + recipientGroupID, Toast.LENGTH_SHORT).show();
        }
    });

    builder.setPositiveButton("Ok",
      new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
          etRecipients.setText(recipientGroup);
          // Toast.makeText(NewMessageActivity.this, "Success", Toast.LENGTH_SHORT).show();
        }
      });
    builder.setNegativeButton("Cancel",
      new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
          // Toast.makeText(NewMessageActivity.this, "Fail", Toast.LENGTH_SHORT).show();
        }
      });
    
    return builder.create();
  }
}