package com.mosharaf.twithoc;
 
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

// Based on http://blog.androgames.net/10/custom-android-dialog/

public class EditGroupDialog extends AlertDialog {
  public EditGroupDialog(Context context, int theme) {
    super(context, theme);
  }
  
  public EditGroupDialog(Context context) {
    super(context);
  }

  public static class Builder {
    private Context context;
    
    private String title = "";
    
    private String groupAlias = "";
    private String groupID = "";
    private String groupKey = "";
    
    private String positiveButtonText = null;
    private String negativeButtonText = "Cancel";
    private String neutralButtonText = null;
    
    private DialogInterface.OnClickListener positiveButtonClickListener,
                                            negativeButtonClickListener,
                                            neutralButtonClickListener;
    
    public Builder(Context context) {
      this.context = context;          
    }
    
    public Builder setGroupAlias(String gA) {
      this.groupAlias = gA;
      return this;
    }
    
    public Builder setGroupAlias(int gA) {
      this.groupAlias = (String) context.getText(gA);
      return this;
    }
    
    public String getGroupAlias() {
      return groupAlias;
    }
    
    public Builder setGroupID(String gID) {
      this.groupID = gID;
      return this;
    }
    
    public Builder setGroupID(int gID) {
      this.groupID = (String) context.getText(gID);
      return this;
    }
    
    public String getGroupID() {
      return groupID;
    }
    
    public Builder setGroupKey(String gK) {
      this.groupKey = gK;
      return this;
    }
    
    public Builder setGroupKey(int gK) {
      this.groupKey = (String) context.getText(gK);
      return this;
    }
    
    public String getGroupKey() {
      return groupKey;
    }
    
    public Builder setTitle(String title) {
      this.title = title;
      return this;
    }
    
    public Builder setTitle(int title) {
      this.title = (String) context.getText(title);
      return this;
    }
    
    public Builder setPositiveButton(int positiveButtonText, DialogInterface.OnClickListener listener) {
        this.positiveButtonText = (String) context.getText(positiveButtonText);
        this.positiveButtonClickListener = listener;
        return this;
    }
   
    public Builder setPositiveButton(String positiveButtonText, DialogInterface.OnClickListener listener) {
        this.positiveButtonText = positiveButtonText;
        this.positiveButtonClickListener = listener;
        return this;
    }
   
    public Builder setNegativeButton(int negativeButtonText, DialogInterface.OnClickListener listener) {
        this.negativeButtonText = (String) context.getText(negativeButtonText);
        this.negativeButtonClickListener = listener;
        return this;
    }
   
    public Builder setNegativeButton(String negativeButtonText, DialogInterface.OnClickListener listener) {
        this.negativeButtonText = negativeButtonText;
        this.negativeButtonClickListener = listener;
        return this;
    }
  
    public Builder setNeutralButton(int neutralButtonText, DialogInterface.OnClickListener listener) {
      this.neutralButtonText = (String) context.getText(neutralButtonText);
      this.neutralButtonClickListener = listener;
      return this;
    }
   
    public Builder setNeutralButton(String neutralButtonText, DialogInterface.OnClickListener listener) {
        this.neutralButtonText = negativeButtonText;
        this.neutralButtonClickListener = listener;
        return this;
    }
    
    public AlertDialog create() {
        // Setup the AlertDialog builder
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
        alertBuilder.setTitle(title);
        alertBuilder.setPositiveButton(positiveButtonText, positiveButtonClickListener);
        alertBuilder.setNegativeButton(negativeButtonText, negativeButtonClickListener);
        alertBuilder.setNeutralButton(neutralButtonText, neutralButtonClickListener);
      
        // Create EditGroupDialog
        final AlertDialog dialog = alertBuilder.create();
  
        // Set custom view
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.edit_group_dialog, null);
        dialog.setView(layout);
  
        // Setup text boxes
        
        ((TextView) layout.findViewById(R.id.et_group_alias)).setText(groupAlias);
        ((TextView) layout.findViewById(R.id.et_group_id)).setText(groupID);
        ((TextView) layout.findViewById(R.id.et_group_key)).setText(groupKey);
        
        return dialog;
    }
  }
}