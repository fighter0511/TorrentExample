package pt.torrentexample.torrrent.manager;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;

import java.util.List;

import pt.torrentexample.R;
import pt.torrentexample.core.Constants;
import pt.torrentexample.core.FileDescriptor;
import pt.torrentexample.gui.adapter.FileListAdapter;
import pt.torrentexample.gui.adapter.ObjectLoader;

/**
 * Created by PhucThanh on 1/11/2016.
 */
public class ShowTorrentFragment extends Fragment implements LoaderManager.LoaderCallbacks<Object> {

    private FileListAdapter adapter;
    private ListView listView;
    private List<FileDescriptor> items;

    public ShowTorrentFragment(){

    }

    public static ShowTorrentFragment newInstance(){
        ShowTorrentFragment showTorrentFragment = new ShowTorrentFragment();
        return showTorrentFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_show_torrent, container, false);
        listView = (ListView) view.findViewById(R.id.list_view);
        getActivity().getSupportLoaderManager().initLoader(1, null, this).forceLoad();
        return view;
    }

    @Override
    public Loader<Object> onCreateLoader(int id, Bundle args) {
        return new ObjectLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<Object> loader, Object data) {
        items = (List<FileDescriptor>) data;
        adapter = new FileListAdapter(getActivity(), items, Constants.FILE_TYPE_TORRENTS);
        listView.setAdapter(adapter);
    }

    @Override
    public void onLoaderReset(Loader<Object> loader) {

    }
}
