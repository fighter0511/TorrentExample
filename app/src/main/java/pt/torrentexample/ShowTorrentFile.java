package pt.torrentexample;
import java.util.List;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;

import frostwire.uxstats.UXAction;
import frostwire.uxstats.UXStats;
import pt.torrentexample.core.ConfigurationManager;
import pt.torrentexample.core.Constants;
import pt.torrentexample.core.FileDescriptor;
import pt.torrentexample.gui.adapter.FileListAdapter;
import pt.torrentexample.gui.adapter.ObjectLoader;
import pt.torrentexample.gui.dialog.YesNoDialog;
import pt.torrentexample.gui.services.Engine;
import pt.torrentexample.gui.view.AbstractDialog;


public class ShowTorrentFile extends FragmentActivity implements LoaderManager.LoaderCallbacks<Object>, AbstractDialog.OnDialogClickListener {

    private static final String SHUTDOWN_DIALOG_ID = "shutdown_dialog";
    private FileListAdapter adapter;
    private android.widget.ListView listView;
    private Toolbar toolbar;
    private List<FileDescriptor> items;
    private FloatingActionButton fab;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_torrent_file);
        onNewIntent(getIntent());
        initComponent();
        getSupportLoaderManager().initLoader(1, null, this).forceLoad();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ConfigurationManager.instance().setBoolean(Constants.PREF_KEY_GUI_TOS_ACCEPTED, true);
    }

    public void initComponent(){
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Torrent Download");
        toolbar.setTitleTextColor(Color.WHITE);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        listView = (android.widget.ListView) findViewById(R.id.fragment_browse_peer_list);
        fab.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ShowTorrentFile.this, TorrentDownloadActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public Loader<Object> onCreateLoader(int id, Bundle args) {
        return new ObjectLoader(ShowTorrentFile.this);
    }

    @Override
    public void onLoadFinished(Loader<Object> loader, Object data) {
        items = (List<FileDescriptor>) data;
        adapter = new FileListAdapter(this, items, Constants.FILE_TYPE_TORRENTS);
        listView.setAdapter(adapter);
    }

    @Override
    public void onLoaderReset(Loader<Object> loader) {
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if(intent == null){
            return;
        }
        String action = intent.getAction();
        if(action != null){
            if(action.equals(Constants.ACTION_REQUEST_SHUTDOWN)){
                UXStats.instance().log(UXAction.MISC_NOTIFICATION_EXIT);
                showShutdownDialog();
            }
        }
    }

    private void showShutdownDialog() {
        YesNoDialog dlg = YesNoDialog.newInstance(SHUTDOWN_DIALOG_ID, R.string.app_shutdown_dlg_title, R.string.app_shutdown_dlg_message);
        dlg.show(getFragmentManager()); //see onDialogClick
    }

    @Override
    public void onDialogClick(String tag, int which) {
        if(tag.equals(SHUTDOWN_DIALOG_ID) && which == AbstractDialog.BUTTON_POSITIVE){
            shutdown();
        }
    }

    public void shutdown() {
        UXStats.instance().flush(true); // sends data and ends 3rd party APIs sessions.
        finish();
        Engine.instance().shutdown();
    }
}

