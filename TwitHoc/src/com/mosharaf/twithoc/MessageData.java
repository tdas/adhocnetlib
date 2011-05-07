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

public class MessageData extends SQLiteOpenHelper {
  public static final String TABLE_NAME = "messages";
  
  public static final String _ID = BaseColumns._ID;
  public static final String MESSAGE_ID = "message_id";
  public static final String MESSAGE = "message";
  public static final String GROUP_ID = "group_id";
  public static final String POSTED_AT = "posted_at";
  public static final String EXPIRE_AFTER = "expire_after";

  public MessageData(Context context) {
    super(context, TwitHocActivity.DATABASE_NAME, null, TwitHocActivity.DATABASE_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    String sql =
      "CREATE TABLE " + TABLE_NAME + " ("
        + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
        + MESSAGE_ID + " TEXT NOT NULL, "
        + MESSAGE + " TEXT NOT NULL, "
        + GROUP_ID + " TEXT NOT NULL, "
        + POSTED_AT + " INTEGER NOT NULL,"
        + EXPIRE_AFTER + " INTEGER NOT NULL"
        + ");";

    db.execSQL(sql);
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
        + MESSAGE_ID + " TEXT NOT NULL, "
        + MESSAGE + " TEXT NOT NULL, "
        + GROUP_ID + " TEXT NOT NULL, "
        + POSTED_AT + " INTEGER NOT NULL,"
        + EXPIRE_AFTER + " INTEGER NOT NULL"
        + ");";

    db.execSQL(sql);
  }

  public void dropTable() {
    SQLiteDatabase db = getReadableDatabase();
    db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
  }
  
  // Creates a new record in the database
  public void createNew(String message, String groupID, int expireAfter) {
    SQLiteDatabase db = getWritableDatabase();

    ContentValues values = new ContentValues();
    values.put(MESSAGE_ID, UUID.randomUUID().toString());
    values.put(MESSAGE, message);
    values.put(GROUP_ID, groupID);
    values.put(POSTED_AT, System.currentTimeMillis());
    values.put(EXPIRE_AFTER, expireAfter);

    try{
      db.insertOrThrow(TABLE_NAME, null, values);
    } catch (Exception e) {
      System.out.println(e.toString());
    }
  }

  // Creates a new record in the database
  public void createNew(Message message) {
    SQLiteDatabase db = getWritableDatabase();

    ContentValues values = new ContentValues();
    values.put(MESSAGE_ID, message.messageID);
    values.put(MESSAGE, message.message);
    values.put(GROUP_ID, message.groupID);
    values.put(POSTED_AT, message.postedAt);
    values.put(EXPIRE_AFTER, Message.expireAfter);

    try{
      db.insertOrThrow(TABLE_NAME, null, values);
    } catch (Exception e) {
      System.out.println(e.toString());
    }
  }

  // Get all the records ordered by time
  public Cursor all(Activity activity) {
    String[] from = { _ID, MESSAGE_ID, MESSAGE, GROUP_ID, POSTED_AT, EXPIRE_AFTER };
    String order = POSTED_AT;

    SQLiteDatabase db = getReadableDatabase();
    Cursor cursor = db.query(TABLE_NAME, from, null, null, null, null, order + " DESC");
    activity.startManagingCursor(cursor);

    return cursor;
  }

  // Count the number of records
  public long count() {
    SQLiteDatabase db = getReadableDatabase();
    return DatabaseUtils.queryNumEntries(db, TABLE_NAME);
  }
}