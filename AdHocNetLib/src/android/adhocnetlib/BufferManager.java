package android.adhocnetlib;

import java.sql.Timestamp;
import java.util.List;


public class BufferManager {
	
	static class BufferItem {
		Timestamp creationTime = null;
		Byte[] dataBytes = null;
		long TimeToLive = 0;
	}
	
	
	
}
