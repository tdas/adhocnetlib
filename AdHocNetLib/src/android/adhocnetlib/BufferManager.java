package android.adhocnetlib;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;

public class BufferManager {
	
	public static class Data implements Serializable {
		public byte[] bytes = null;
		public long time = 0;		
		public Data(byte[] b, long t) {
			bytes = b;
			time = t;
		}
	}
	
	public static class BufferItem implements Serializable {
		public Data data = null;
		Integer hash = 0;
		long ttl = 0;
		HashSet<UUID> nodeIDs = new HashSet<UUID>();
	
		public BufferItem (byte[] bytes, long ttl, UUID nodeID) {
			data = new Data(bytes, new Date().getTime());
			hash = new Integer(data.hashCode());
			this.ttl = ttl;
			nodeIDs.add(nodeID);
		}
		
		public static void serialize (BufferItem item, OutputStream out) 
		throws IOException {
			ObjectOutputStream oos = new ObjectOutputStream(out);
			oos.writeObject(item);
		}
		
		public static BufferItem deserialize (InputStream in) 
		throws StreamCorruptedException, IOException, ClassNotFoundException {
			ObjectInputStream ois = new ObjectInputStream(in);
			BufferItem item = (BufferItem) ois.readObject();
			return item;
		}
	}
	
	//private static BufferManager singleInstance = new BufferManager();
	
	private Hashtable<Integer, BufferItem> bufferedItems = new Hashtable<Integer, BufferItem>();
	
	public BufferManager() {
		
	}
	
	public synchronized boolean createNewItem  (byte[] bytes, long ttl, UUID nodeID) {
		BufferItem newItem = new BufferItem(bytes, ttl, nodeID);
		return addItem_(newItem);		
	}
	
	public synchronized boolean addItem (BufferItem item) {
		return addItem_(item);
	}
	
	public synchronized boolean addItems (List<BufferItem> items) {
		for (BufferItem item: items) {
			addItem_(item);
		}		
		return true;
	}
	
	public synchronized Collection<BufferItem> addItemsAndReturnNewItems (Collection<BufferItem> items) {
		ArrayList<BufferItem> newItems = new ArrayList<BufferItem>();
		
		for (BufferItem item: items) {
			if (addItem_(item)) {
				newItems.add(item);
			}
		}		
		return newItems;
	}
	
	public synchronized boolean addNodeIDToItems (Collection<BufferItem> items, UUID nodeID) {
		for (BufferItem item: items) {
			BufferItem bufferedItem = getDuplicateItem(item);
			if (bufferedItem == null) return false;
			bufferedItem.nodeIDs.add(nodeID);
		}
		return true;
	}
	
	public synchronized ArrayList<BufferItem> getAllItemsForNodeID  (UUID nodeID) {
		ArrayList<BufferItem> filteredItems = new ArrayList<BufferItem>();
		for (BufferItem bufferedItem : bufferedItems.values()) {
			if (nodeID == null || !bufferedItem.nodeIDs.contains(nodeID)) {
				filteredItems.add(bufferedItem);
			}
		}		
		return filteredItems;
	}
	
	public synchronized Collection<BufferItem> getAllItems () {
		return bufferedItems.values();
	}

	private boolean addItem_ (BufferItem item) {
		if (isDuplicate(item)) {
			// merge the node id list
			BufferItem oldItem = getDuplicateItem(item);
			oldItem.nodeIDs.addAll(item.nodeIDs);
			return false;
		} else {
			// add to the items list
			bufferedItems.put(item.hash,item);
			return true;
		}		
	}
	
	private boolean isDuplicate (Data data) {
		if (data != null && bufferedItems.contains(new Integer(data.hashCode()))) {
			return true;
		}
		return false;
	}
	
	private boolean isDuplicate (BufferItem item) {
		return bufferedItems.contains(item.hash);
	}
	
	private BufferItem getDuplicateItem (BufferItem item) {
		return (BufferItem)bufferedItems.get(item.hash);
	}
	
	/*
	public static BufferManager getInstance() {
		return singleInstance;
	}
	*/
	
	/*public static void serialize (Collection<BufferItem> items, OutputStream out) throws IOException {
		ObjectOutputStream oos = new ObjectOutputStream(out);
		for(BufferItem item : items){
			oos.writeObject(item);
		}
	}
	
	public static Collection<BufferItem> deserialize (byte[] bytes, InputStream in) {
		ObjectInputStream ois = new ObjectInputStream(in);
		BufferItem item = (BufferItem) ois.readObject();
		return item;
	}*/
}
