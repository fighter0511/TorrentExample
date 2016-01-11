package pt.torrentexample.gui.adapter;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import pt.torrentexample.core.Constants;
import pt.torrentexample.gui.Peer;


public class ObjectLoader extends AsyncTaskLoader<Object> {
    public ObjectLoader(Context context) {
		super(context);
    }
	@Override 
    public Object loadInBackground() {
		Peer peer = new Peer();
//    	List<Object> list = new ArrayList<Object>();
		Object[] data = new Object[]{Constants.FILE_TYPE_TORRENTS, peer.browse(Constants.FILE_TYPE_TORRENTS)};
//		list.add(data[1]);
        return data[1];
    }
}