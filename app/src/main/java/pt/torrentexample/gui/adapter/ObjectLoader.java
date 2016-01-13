package pt.torrentexample.gui.adapter;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;
import android.widget.Toast;

import pt.torrentexample.core.Constants;
import pt.torrentexample.gui.Peer;


public class ObjectLoader extends AsyncTaskLoader<Object> {
    private Context context;
    public ObjectLoader(Context context) {
		super(context);
        this.context = context;
    }
	@Override 
    public Object loadInBackground() {
		Peer peer = new Peer();
		Object[] data = new Object[]{Constants.FILE_TYPE_TORRENTS, peer.browse(Constants.FILE_TYPE_TORRENTS)};
        return data[1];
    }
}