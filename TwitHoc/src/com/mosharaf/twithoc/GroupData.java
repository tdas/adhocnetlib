package com.mosharaf.twithoc;

import java.util.UUID;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public class GroupData extends SQLiteOpenHelper {
  public static final String TABLE_NAME = "groups";
  
  public static final String _ID = BaseColumns._ID;
  public static final String GROUP_ID = "group_id";
  public static final String NAME = "name";
  public static final String GROUP_KEY = "group_key";
  public static final String IS_SELECTED = "is_selected";

  public GroupData(Context context) {
    super(context, TwitHocActivity.DATABASE_NAME, null, TwitHocActivity.DATABASE_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
	  createTable();
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
    onCreate(db);
  }  
  
  public void createTable() {
    SQLiteDatabase db = getReadableDatabase();
    String sql =
      "CREATE TABLE " + TABLE_NAME + " ("
        + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
        + GROUP_ID + " TEXT UNIQUE NOT NULL, "
        + NAME + " TEXT UNIQUE NOT NULL, "
        + GROUP_KEY + " TEXT NOT NULL, "
        + IS_SELECTED + " BOOLEAN NOT NULL"
        + ");";

    db.execSQL(sql);
  }

  public void dropTable() {
    SQLiteDatabase db = getReadableDatabase();
    db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
  }
  
  // Insert new entry to the database
  public boolean insert(String groupId, String name, String groupKey) {
    SQLiteDatabase db = getWritableDatabase();

    ContentValues values = new ContentValues();
    values.put(GROUP_ID, groupId);
    values.put(NAME, name);
    values.put(GROUP_KEY, groupKey);
    values.put(IS_SELECTED, false);

    return db.insert(TABLE_NAME, null, values) >= 0;
  }
  
  // Insert new entry to the database
  // GROUP_ID is a random UUID
  public void insert(String name, String groupKey) {
    insert(UUID.randomUUID().toString(), name, groupKey);
  }

  // Insert new entry to the database
  // GROUP_ID is a random UUID
  // GROUP_KEY is a random UUID
  public void insert(String name) {
    // TODO: Replace GROUP_KEY with some key thing!
    insert(UUID.randomUUID().toString(), name, UUID.randomUUID().toString());
  }

  // Get all the records ordered by NAME
  public Cursor all(Activity activity) {
    String[] from = { _ID, GROUP_ID, NAME, GROUP_KEY, IS_SELECTED };
    String order = NAME;

    SQLiteDatabase db = getReadableDatabase();
    Cursor cursor = db.query(TABLE_NAME, from, null, null, null, null, order);
    activity.startManagingCursor(cursor);

    return cursor;
  }
  
  // Count the number of records
  public long count() {
    SQLiteDatabase db = getReadableDatabase();
    return DatabaseUtils.queryNumEntries(db, TABLE_NAME);
  }
  
  // Return if update was successful
  public boolean updateGroup(long rowId, String gName, String gID, String gKey) {
    SQLiteDatabase db = getWritableDatabase();
    
    ContentValues args = new ContentValues();
    args.put(NAME, gName);
    args.put(GROUP_ID, gID);
    args.put(GROUP_KEY, gKey);

    return db.update(TABLE_NAME, args, _ID + "=" + rowId, null) > 0;
  }
  
  // Returns number of rows deleted
  public int deleteGroup(long rowId) {
    SQLiteDatabase db = getWritableDatabase();
    return db.delete(TABLE_NAME, _ID + "=" + rowId, null);
  }
  
  // Toggle selection given _ID
  public boolean toggleSelection(int _id) {
    SQLiteDatabase db = getReadableDatabase();
    String sql = "SELECT " + IS_SELECTED + " FROM " + TABLE_NAME + " WHERE _ID = " + _id;
    db.execSQL(sql);
    
    db.close();    
    return false;
  }
}