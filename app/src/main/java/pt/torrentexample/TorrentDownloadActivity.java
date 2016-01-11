/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2014, FrostWire(R). All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pt.torrentexample;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.TextView;

import frostwire.logging.Logger;
import pt.torrentexample.gui.NetworkManager;
import pt.torrentexample.gui.Util.UIUtils;
import pt.torrentexample.gui.adapter.TransferListAdapter;
import pt.torrentexample.gui.services.EngineService;
import pt.torrentexample.gui.transfers.Transfer;
import pt.torrentexample.gui.transfers.TransferManager;
import pt.torrentexample.gui.view.AbstractDialog;
import pt.torrentexample.gui.view.MainFragment;
import pt.torrentexample.gui.view.TimerObserver;
import pt.torrentexample.gui.view.TimerService;
import pt.torrentexample.gui.view.TimerSubscription;


import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * @author gubatron
 * @author aldenml
 */
public class TorrentDownloadActivity extends Activity implements TimerObserver, MainFragment, AbstractDialog.OnDialogClickListener {
    private static final Logger LOG = Logger.getLogger(TorrentDownloadActivity.class);
    private static final String SELECTED_STATUS_STATE_KEY = "selected_status";
    private static final int FROSTWIRE_STATUS_NOTIFICATION_UPDATE_INTERVAL_IN_SECS = 5;
    private Comparator<Transfer> transferComparator;
    private ExpandableListView list;
    private TextView textDHTPeers;
    private TextView textDownloads;
    private TextView textUploads;
    private TransferListAdapter adapter;
    private TransferStatus selectedStatus;
    private TimerSubscription subscription;
    private int androidNotificationUpdateTick;
    private static boolean isVPNactive;
    private Toolbar toolbar;
    private EngineService.CheckDHTUICallback onDHTCheckCallback;
    private String uri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_torrent_download);

        transferComparator = new TransferComparator();
        selectedStatus = TransferStatus.ALL;
        onDHTCheckCallback = new OnCheckDHTCallback();

        if (savedInstanceState != null) {
            selectedStatus = TransferStatus.valueOf(savedInstanceState.getString(SELECTED_STATUS_STATE_KEY, TransferStatus.ALL.name()));
        }

        subscription = TimerService.subscribe(this, 2);
        initComponents();
        onNewIntent(getIntent());
        if (uri != null)
            TransferManager.instance().downloadTorrent(uri);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null)
            if (intent.getDataString() != null)
                uri = intent.getDataString();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    @Override
    public void onResume() {
        super.onResume();
        onTime();
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SELECTED_STATUS_STATE_KEY, selectedStatus.name());
    }

    @Override
    public void onPause() {
        super.onPause();

        if (adapter != null) {
            adapter.dismissDialogs();
        }
    }

    @Override
    public void onTime() {
        if (adapter != null) {
            List<Transfer> transfers = filter(TransferManager.instance().getTransfers(), selectedStatus);
            Collections.sort(transfers, transferComparator);
            adapter.updateList(transfers);
        } else if (this != null) {
            setupAdapter();
        }

        //  format strings
        String sDown = UIUtils.rate2speed(TransferManager.instance().getDownloadsBandwidth() / 1024);
        String sUp = UIUtils.rate2speed(TransferManager.instance().getUploadsBandwidth() / 1024);

        // number of uploads (seeding) and downloads
        int downloads = TransferManager.instance().getActiveDownloads();
        int uploads = TransferManager.instance().getActiveUploads();

        updatePermanentStatusNotification(sDown, sUp, downloads, uploads);
        updateStatusBar(sDown, sUp, downloads, uploads);
    }

    private void updateStatusBar(String sDown, String sUp, int downloads, int uploads) {
        textDownloads.setText(downloads + " @ " + sDown);
        textUploads.setText(uploads + " @ " + sUp);
        EngineService.asyncCheckDHTPeers(getWindow().getDecorView().getRootView(), onDHTCheckCallback);
    }

    private void updatePermanentStatusNotification(String sDown, String sUp, int downloads, int uploads) {
        if (++androidNotificationUpdateTick >= FROSTWIRE_STATUS_NOTIFICATION_UPDATE_INTERVAL_IN_SECS) {
            androidNotificationUpdateTick = 0;
            EngineService.updatePermanentStatusNotification(
                    new WeakReference<Context>(this),
                    new WeakReference<>(getWindow().getDecorView().getRootView()),
                    downloads,
                    sDown,
                    uploads,
                    sUp);
        }
    }

    private class OnCheckDHTCallback implements EngineService.CheckDHTUICallback {
        @Override
        public void onCheckDHT(final boolean dhtEnabled, final int dhtPeers) {

            textDHTPeers.setVisibility(dhtEnabled ? View.VISIBLE : View.GONE);
            if (!dhtEnabled) {
                return;
            }
            textDHTPeers.setText(dhtPeers + " " + TorrentDownloadActivity.this.getString(R.string.dht_contacts));
        }
    }

    @Override
    public View getHeader(Activity activity) {
        LayoutInflater inflater = LayoutInflater.from(activity);

        View header = inflater.inflate(R.layout.view_transfers_header, null);

        TextView text = (TextView) header.findViewById(R.id.view_transfers_header_text_title);
        text.setText(R.string.transfers);

        return header;
    }


    protected void initComponents() {
        list = (ExpandableListView) findViewById(R.id.fragment_transfers_list);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Torrent Downloading");
        toolbar.setTitleTextColor(Color.WHITE);
        textDHTPeers = (TextView) findViewById(R.id.fragment_transfers_dht_peers);
        textDHTPeers.setVisibility(View.GONE);
        textDownloads = (TextView) findViewById(R.id.fragment_transfers_text_downloads);
        textUploads = (TextView) findViewById(R.id.fragment_transfers_text_uploads);

    }


    private void setupAdapter() {
        List<Transfer> transfers = filter(TransferManager.instance().getTransfers(), selectedStatus);
        Collections.sort(transfers, transferComparator);
        adapter = new TransferListAdapter(TorrentDownloadActivity.this, transfers);
        list.setAdapter(adapter);
    }

    private List<Transfer> filter(List<Transfer> transfers, TransferStatus status) {
        Iterator<Transfer> it;

        switch (status) { // replace this filter by a more functional style
            case DOWNLOADING:
                it = transfers.iterator();
                while (it.hasNext()) {
                    if (it.next().isComplete()) {
                        it.remove();
                    }
                }
                return transfers;
            case COMPLETED:
                it = transfers.iterator();
                while (it.hasNext()) {
                    if (!it.next().isComplete()) {
                        it.remove();
                    }
                }
                return transfers;
            default:
                return transfers;
        }
    }

    private static final String TRANSFERS_DIALOG_ID = "transfers_dialog";

    private static final int CLEAR_MENU_DIALOG_ID = 0;
    private static final int PAUSE_MENU_DIALOG_ID = 1;
    private static final int RESUME_MENU_DIALOG_ID = 2;

    @Override
    public void onDialogClick(String tag, int which) {
        if (tag.equals(TRANSFERS_DIALOG_ID)) {
            switch (which) {
                case CLEAR_MENU_DIALOG_ID:
                    TransferManager.instance().clearComplete();
                    break;
                case PAUSE_MENU_DIALOG_ID:
                    TransferManager.instance().stopHttpTransfers();
                    TransferManager.instance().pauseTorrents();
                    break;
                case RESUME_MENU_DIALOG_ID:
                    boolean bittorrentDisconnected = TransferManager.instance().isBittorrentDisconnected();
                    if (bittorrentDisconnected) {
                        UIUtils.showLongMessage(TorrentDownloadActivity.this, R.string.cant_resume_torrent_transfers);
                    } else {
                        if (NetworkManager.instance().isDataUp()) {
                            TransferManager.instance().resumeResumableTransfers();
                        } else {
                            UIUtils.showShortMessage(TorrentDownloadActivity.this, R.string.please_check_connection_status_before_resuming_download);
                        }
                    }
                    break;
            }
            setupAdapter();
        }
    }


    private static final class TransferComparator implements Comparator<Transfer> {
        public int compare(Transfer lhs, Transfer rhs) {
            try {
                return -lhs.getDateCreated().compareTo(rhs.getDateCreated());
            } catch (Throwable e) {
                // ignore, not really super important
            }
            return 0;
        }
    }

    public enum TransferStatus {
        ALL, DOWNLOADING, COMPLETED
    }


}
