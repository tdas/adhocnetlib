package com.mosharaf.twithoc;

import java.io.Serializable;
import java.util.UUID;

public class Message implements Serializable {
  public static long expireAfter = 3600000;

  public String messageID;
  public String message;
  public String groupID;
  public long postedAt;
  
  Message(String message, String groupID) {
    this.messageID = UUID.randomUUID().toString();
	this.message = message;
	this.groupID = groupID;
	this.postedAt = System.currentTimeMillis();	
  }
}