package com.mosharaf.twithoc;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class ManageGroupsActivity extends ListActivity 
  implements OnClickListener {
  
  private static final int DIALOG_ADD_GROUP = 0;
  private static final int DIALOG_CREATE_GROUP = 1;
  private static final int DIALOG_EDIT_GROUP = 2;
  
  protected GroupData groupData;
  
  private Cursor listCursor;
  private SimpleCursorAdapter listAdapter;

  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Create database objects
    groupData = new GroupData(this);

    // Fetch and display data
    getOrRefreshData();
    
    ListView lv = getListView();
    lv.setTextFilterEnabled(true);

    // Handle item click event
    lv.setOnItemClickListener(new OnItemClickListener() {
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        listCursor.moveToPosition(position);
        
        int _ID = listCursor.getInt(listCursor.getColumnIndex(GroupData._ID));
        String gName = listCursor.getString(listCursor.getColumnIndex(GroupData.NAME));
        String gID = listCursor.getString(listCursor.getColumnIndex(GroupData.GROUP_ID));
        String gKey = listCursor.getString(listCursor.getColumnIndex(GroupData.GROUP_KEY));
        
        // TODO This isn't the recommended way. Should use showDialog instead.
        Dialog dialog = createEditGroupDialog(_ID, gName, gID, gKey);
        dialog.show();
      }
    });
  }
  
  private void getOrRefreshData() {
    String[] displayFields = new String[] { GroupData.NAME };
    int[] displayViews = new int[] { android.R.id.text1 };
    
    listCursor = groupData.all(this);
    listAdapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1, listCursor, displayFields, displayViews);
    setListAdapter(listAdapter);
  }
  
  protected Dialog onCreateDialog(int id) {
    Dialog dialog = null;
    
    switch(id) {
      case DIALOG_ADD_GROUP:
        dialog = createAddGroupDialog();
        break;
      case DIALOG_EDIT_GROUP:
        dialog = createEditGroupDialog(0, "", "", "");
        break;
      case DIALOG_CREATE_GROUP:
        dialog = createCreateGroupDialog();
        break;
      default:
        dialog = null;
    }
    return dialog;
  }
  
  @Override
  protected void onPrepareDialog(int id, Dialog dialog) { 
    super.onPrepareDialog(id, dialog);
    
    switch(id) {
      case DIALOG_ADD_GROUP:        
        break;
      case DIALOG_EDIT_GROUP:
        break;
      case DIALOG_CREATE_GROUP:        
        break;
      default:
        break;
    }
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.manage_groups, menu);

    return true;
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
	Dialog dialog = null;
	  
    switch(item.getItemId()) {
      case R.id.add_group:
		// TODO This isn't the recommended way. Should use showDialog instead.
		dialog = createAddGroupDialog();
		dialog.show();
        // showDialog(DIALOG_ADD_GROUP);
        return true;

      case R.id.create_group:
  		// TODO This isn't the recommended way. Should use showDialog instead.
  		dialog = createCreateGroupDialog();
  		dialog.show();
        // showDialog(DIALOG_CREATE_GROUP);
        return true;
        
      default:
        return super.onOptionsItemSelected(item);
    }
  }
  
  private Dialog createAddGroupDialog() {
    EditGroupDialog.Builder builder = new EditGroupDialog.Builder(this);
    builder
      .setTitle(R.string.add_group_label)
      .setWandButton(null)
      .setPositiveButton(R.string.add_group_label, onClickListenerForAddCreate);
    
    return builder.create();
  }

  private Dialog createCreateGroupDialog() {
    EditGroupDialog.Builder builder = new EditGroupDialog.Builder(this);
    builder
      .setTitle(R.string.create_group_label)
      .setPositiveButton(R.string.create_group_label, onClickListenerForAddCreate);
    
    return builder.create();
  }

  private DialogInterface.OnClickListener onClickListenerForAddCreate = 
	  new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			// Get the data from the dialog box. This might not be the best way	
			AlertDialog alert = ((AlertDialog) dialog);
			
	        String gA = ((TextView) alert.findViewById(R.id.et_group_alias)).getText().toString();
	        String gID = ((TextView) alert.findViewById(R.id.et_group_id)).getText().toString();
	        String gK = ((TextView) alert.findViewById(R.id.et_group_key)).getText().toString();
			
		  	if (groupData.insert(gID, gA, gK)) {
				Toast.makeText(getApplicationContext(), getString(R.string.add_group_succeeded), Toast.LENGTH_SHORT).show();
				getOrRefreshData();
			} else {
				Toast.makeText(getApplicationContext(), getString(R.string.add_group_failed), Toast.LENGTH_SHORT).show();
			}
		}
	  };
  
  private Dialog createEditGroupDialog(final int _ID, final String gName, final String gID, final String gKey) {
    EditGroupDialog.Builder builder = new EditGroupDialog.Builder(this);
    builder
      .setTitle(R.string.delete_group_label)
      .setGroupAlias(gName)
      .setGroupID(gID)
      .setGroupKey(gKey)
      .setWandButton(null)
      // .setPositiveButton(R.string.update_group_label, new DialogInterface.OnClickListener() {
        // @Override
        // public void onClick(DialogInterface dialog, int id) {
          // boolean x = groupData.updateGroup(_ID, gName, gID, gKey);
          // TODO Update isn't working :(
          // getOrRefreshData();
          // listCursor.requery();
          // listAdapter.notifyDataSetChanged();
        // }
      // })
      .setNeutralButton(R.string.delete_group_label, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int id) {
          groupData.deleteGroup(_ID);
          getOrRefreshData();
        }
      });
    
    return builder.create();
  }

  @Override
  public void onClick(View v) {

  }
}