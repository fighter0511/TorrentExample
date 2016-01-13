package pt.torrentexample.torrrent.manager;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import pt.torrentexample.R;
import pt.torrentexample.TorrentDownloadActivity;
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
    private FloatingActionButton fab;

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
        fab = (FloatingActionButton) view.findViewById(R.id.fab);
        getActivity().getSupportLoaderManager().initLoader(1, null, this).forceLoad();

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), TorrentDownloadActivity.class);
                getContext().startActivity(intent);
            }
        });
        return view;
    }

    @Override
    public Loader<Object> onCreateLoader(int id, Bundle args) {
        return new ObjectLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<Object> loader, Object data) {
        items = (List<FileDescriptor>) data;
        if(items.size() == 0){
            showDialog();
        }
        adapter = new FileListAdapter(getActivity(), items, Constants.FILE_TYPE_TORRENTS);
        listView.setAdapter(adapter);

    }

    @Override
    public void onLoaderReset(Loader<Object> loader) {

    }

    public void showDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Thông báo");
        builder.setMessage("Không có file Torrent, vui lòng download và lưu vào bộ nhớ máy");
        builder.show();
    }
}
