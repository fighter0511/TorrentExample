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

package pt.torrentexample.gui.fragments;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipData.Item;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;



import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import frostwire.logging.Logger;
import frostwire.util.StringUtils;
import pt.torrentexample.R;
import pt.torrentexample.core.ConfigurationManager;
import pt.torrentexample.core.Constants;
import pt.torrentexample.gui.NetworkManager;
import pt.torrentexample.gui.Util.SystemUtils;
import pt.torrentexample.gui.Util.UIUtils;
import pt.torrentexample.gui.VPNStatusDetailActivity;
import pt.torrentexample.gui.adapter.TransferListAdapter;
import pt.torrentexample.gui.dialog.MenuDialog;
import pt.torrentexample.gui.services.EngineService;
import pt.torrentexample.gui.transfers.BittorrentDownload;
import pt.torrentexample.gui.transfers.HttpDownload;
import pt.torrentexample.gui.transfers.Transfer;
import pt.torrentexample.gui.transfers.TransferManager;
import pt.torrentexample.gui.view.AbstractDialog;
import pt.torrentexample.gui.view.AbstractFragment;
import pt.torrentexample.gui.view.ClickAdapter;
import pt.torrentexample.gui.view.MainFragment;
import pt.torrentexample.gui.view.TimerObserver;
import pt.torrentexample.gui.view.TimerService;
import pt.torrentexample.gui.view.TimerSubscription;

/**
 * 
 * @author gubatron
 * @author aldenml
 * 
 */
public class TransfersFragment extends AbstractFragment implements TimerObserver, MainFragment, AbstractDialog.OnDialogClickListener {
    private static final Logger LOG = Logger.getLogger(TransfersFragment.class);
    private static final String SELECTED_STATUS_STATE_KEY = "selected_status";
    private static final int FROSTWIRE_STATUS_NOTIFICATION_UPDATE_INTERVAL_IN_SECS = 5;
    private final Comparator<Transfer> transferComparator;
    private final ButtonMenuListener buttonMenuListener;
    private Button buttonSelectAll;
    private Button buttonSelectDownloading;
    private Button buttonSelectCompleted;
    private ExpandableListView list;
    private TextView textDHTPeers;
    private TextView textDownloads;
    private TextView textUploads;
    private TransferListAdapter adapter;
    private TransferStatus selectedStatus;
    private TimerSubscription subscription;
    private int androidNotificationUpdateTick;
    private static boolean isVPNactive;
    private final OnVPNStatusCallback onVPNStatusCallback;
    private final EngineService.CheckDHTUICallback onDHTCheckCallback;

    public TransfersFragment() {
        super(R.layout.fragment_transfers);
        this.transferComparator = new TransferComparator();
        this.buttonMenuListener = new ButtonMenuListener(this);
        selectedStatus = TransferStatus.ALL;
        this.onVPNStatusCallback = new OnVPNStatusCallback();
        this.onDHTCheckCallback = new OnCheckDHTCallback();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            selectedStatus = TransferStatus.valueOf(savedInstanceState.getString(SELECTED_STATUS_STATE_KEY, TransferStatus.ALL.name()));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        subscription = TimerService.subscribe(this, 2);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        onTime();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        subscription.unsubscribe();
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
        } else if (this.getActivity() != null) {
            setupAdapter();
        }

        //  format strings
        String sDown = UIUtils.rate2speed(TransferManager.instance().getDownloadsBandwidth() / 1024);
        String sUp = UIUtils.rate2speed(TransferManager.instance().getUploadsBandwidth()/1024);

        // number of uploads (seeding) and downloads
        int downloads = TransferManager.instance().getActiveDownloads();
        int uploads = TransferManager.instance().getActiveUploads();

        updatePermanentStatusNotification(sDown, sUp, downloads, uploads);
        updateStatusBar(sDown, sUp, downloads, uploads);
    }

    private void updateStatusBar(String sDown, String sUp, int downloads, int uploads) {
        textDownloads.setText(downloads + " @ " + sDown);
        textUploads.setText(uploads + " @ " + sUp);
        updateVPNButtonIfStatusChanged(TransfersFragment.isVPNactive);
        EngineService.asyncCheckVPNStatus(getView(), onVPNStatusCallback);
        EngineService.asyncCheckDHTPeers(getView(), onDHTCheckCallback);
    }

    private void updatePermanentStatusNotification(String sDown, String sUp, int downloads, int uploads) {
        if (++androidNotificationUpdateTick >= FROSTWIRE_STATUS_NOTIFICATION_UPDATE_INTERVAL_IN_SECS) {
            androidNotificationUpdateTick = 0;
            EngineService.updatePermanentStatusNotification(
                    new WeakReference<Context>(getActivity()),
                    new WeakReference<>(getView()),
                    downloads,
                    sDown,
                    uploads,
                    sUp);
        }
    }

    private void updateVPNButtonIfStatusChanged(boolean vpnActive) {
        TransfersFragment.isVPNactive = vpnActive;
        final ImageView view = findView(getView(), R.id.fragment_transfers_status_vpn_icon);
        if (view != null) {
            view.setImageResource(vpnActive ? R.drawable.notification_vpn_on : R.drawable.notification_vpn_off);
        }
    }

    private class OnVPNStatusCallback implements EngineService.VpnStatusUICallback {
        @Override
        public void onVpnStatus(final boolean vpnActive) {
            if (TransfersFragment.this.isAdded()) {
                TransfersFragment.this.updateVPNButtonIfStatusChanged(vpnActive);
            }
        }
    }

    private class OnCheckDHTCallback implements EngineService.CheckDHTUICallback {
        @Override
        public void onCheckDHT(final boolean dhtEnabled, final int dhtPeers) {
            if (textDHTPeers==null || !TransfersFragment.this.isAdded()) {
                return;
            }
            textDHTPeers.setVisibility(dhtEnabled ? View.VISIBLE : View.GONE);
            if (!dhtEnabled) {
                return;
            }
            textDHTPeers.setText(dhtPeers + " " + TransfersFragment.this.getString(R.string.dht_contacts));
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

    public void selectStatusTab(TransferStatus status) {
        selectedStatus = status;
        switch (selectedStatus) {
        case ALL:
            buttonSelectAll.performClick();
            break;
        case DOWNLOADING:
            buttonSelectDownloading.performClick();
            break;
        case COMPLETED:
            buttonSelectCompleted.performClick();
            break;
        }
    }

    @Override
    protected void initComponents(View v) {

        buttonSelectAll = findView(v, R.id.fragment_transfers_button_select_all);
        buttonSelectAll.setOnClickListener(new ButtonTabListener(this, TransferStatus.ALL));

        buttonSelectDownloading = findView(v, R.id.fragment_transfers_button_select_downloading);
        buttonSelectDownloading.setOnClickListener(new ButtonTabListener(this, TransferStatus.DOWNLOADING));

        buttonSelectCompleted = findView(v, R.id.fragment_transfers_button_select_completed);
        buttonSelectCompleted.setOnClickListener(new ButtonTabListener(this, TransferStatus.COMPLETED));

        list = findView(v, R.id.fragment_transfers_list);

        textDHTPeers = findView(v, R.id.fragment_transfers_dht_peers);
        textDHTPeers.setVisibility(View.GONE);
        textDownloads = findView(v, R.id.fragment_transfers_text_downloads);
        textUploads = findView(v, R.id.fragment_transfers_text_uploads);

        initVPNStatusButton(v);
    }

    private void initVPNStatusButton(View v) {
        final ImageView vpnStatusButton = findView(v, R.id.fragment_transfers_status_vpn_icon);
        vpnStatusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getActivity(),
                        VPNStatusDetailActivity.class).

                        setAction(TransfersFragment.isVPNactive ?
                                Constants.ACTION_SHOW_VPN_STATUS_PROTECTED :
                                Constants.ACTION_SHOW_VPN_STATUS_UNPROTECTED).
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                i.putExtra("from","transfers");
                startActivity(i);
            }
        });
    }



    private void setupAdapter() {
        List<Transfer> transfers = filter(TransferManager.instance().getTransfers(), selectedStatus);
        Collections.sort(transfers, transferComparator);
        adapter = new TransferListAdapter(TransfersFragment.this.getActivity(), transfers);
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
                if (bittorrentDisconnected){
                    UIUtils.showLongMessage(getActivity(), R.string.cant_resume_torrent_transfers);
                } else {
                    if (NetworkManager.instance().isDataUp()) {
                        TransferManager.instance().resumeResumableTransfers();
                    } else {
                        UIUtils.showShortMessage(getActivity(), R.string.please_check_connection_status_before_resuming_download);
                    }
                }
                break;
            }
            setupAdapter();
        }
    }

    private void showContextMenu() {
        MenuDialog.MenuItem clear = new MenuDialog.MenuItem(CLEAR_MENU_DIALOG_ID, R.string.transfers_context_menu_clear_finished, R.drawable.contextmenu_icon_remove_transfer);
        MenuDialog.MenuItem pause = new MenuDialog.MenuItem(PAUSE_MENU_DIALOG_ID, R.string.transfers_context_menu_pause_stop_all_transfers, R.drawable.contextmenu_icon_pause_transfer);
        MenuDialog.MenuItem resume = new MenuDialog.MenuItem(RESUME_MENU_DIALOG_ID, R.string.transfers_context_resume_all_torrent_transfers, R.drawable.contextmenu_icon_play);

        List<MenuDialog.MenuItem> dlgActions = new ArrayList<>();

        TransferManager tm = TransferManager.instance();
        boolean bittorrentDisconnected = tm.isBittorrentDisconnected();
        final List<Transfer> transfers = tm.getTransfers();

        if (transfers != null && transfers.size() > 0) {
            if (someTransfersComplete(transfers)) {
                dlgActions.add(clear);
            }

            if (!bittorrentDisconnected) {
                if (someTransfersActive(transfers)) {
                    dlgActions.add(pause);
                }
            }

            //let's show it even if bittorrent is disconnected
            //user should get a message telling them to check why they can't resume.
            //Preferences > Connectivity is disconnected.
            if (someTransfersInactive(transfers)) {
                dlgActions.add(resume);
            }
        }

        if (dlgActions.size() > 0) {
            MenuDialog dlg = MenuDialog.newInstance(TRANSFERS_DIALOG_ID, dlgActions);
            dlg.show(getFragmentManager());
        }
    }

    private boolean someTransfersInactive(List<Transfer> transfers) {
        for (Transfer t : transfers) {
            if (t instanceof BittorrentDownload) {
                BittorrentDownload bt = (BittorrentDownload) t;
                if (!bt.isDownloading() && !bt.isSeeding()) {
                    return true;
                }
            } else if (t instanceof HttpDownload) {
                HttpDownload ht = (HttpDownload) t;
                if (ht.isComplete() || !ht.isDownloading()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean someTransfersComplete(List<Transfer> transfers) {
        for (Transfer t : transfers) {
            if (t.isComplete()) {
                return true;
            }
        }
        return false;
    }

    private boolean someTransfersActive(List<Transfer> transfers) {
        for (Transfer t : transfers) {
            if (t instanceof BittorrentDownload) {
                BittorrentDownload bt = (BittorrentDownload) t;
                if (bt.isDownloading() || bt.isSeeding()) {
                    return true;
                }
            } else if (t instanceof HttpDownload) {
                HttpDownload ht = (HttpDownload) t;
                if (ht.isDownloading()) {
                    return true;
                }
            }
        }
        return false;
    }








    /**
     * Is it using the SD Card's private (non-persistent after uninstall) app folder to save
     * downloaded files?
     */
    public static boolean isUsingSDCardPrivateStorage() {
        String primaryPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        String currentPath = ConfigurationManager.instance().getStoragePath();

        return !primaryPath.equals(currentPath);
    }

    /**
     * Iterates over all the secondary external storage roots and returns the one with the most bytes available.
     */
    private static File getBiggestSDCardDir(Context context) {
        try {
            String primaryPath = context.getExternalFilesDir(null).getParent();

            long biggestBytesAvailable = -1;

            File result = null;

            for (File f : SystemUtils.getExternalFilesDirs(context)) {
                if (!f.getAbsolutePath().startsWith(primaryPath)) {
                    long bytesAvailable = SystemUtils.getAvailableStorageSize(f);
                    if (bytesAvailable > biggestBytesAvailable) {
                        biggestBytesAvailable = bytesAvailable;
                        result = f;
                    }
                }
            }
            //System.out.println("FW.SystemUtils.getSDCardDir() -> " + result.getAbsolutePath());
            // -> /storage/extSdCard/Android/data/com.frostwire.android/files
            return result;
        } catch (Throwable e) {
            // the context could be null due to a UI bad logic or context.getExternalFilesDir(null) could be null
            LOG.error("Error getting the biggest SD card", e);
        }

        return null;
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

    private static final class ButtonMenuListener extends ClickAdapter<TransfersFragment> {

        public ButtonMenuListener(TransfersFragment f) {
            super(f);
        }

        @Override
        public void onClick(TransfersFragment f, View v) {
            f.showContextMenu();
        }
    }



    private static final class ButtonTabListener extends ClickAdapter<TransfersFragment> {

        private final TransferStatus status;

        public ButtonTabListener(TransfersFragment f, TransferStatus status) {
            super(f);
            this.status = status;
        }

        @Override
        public void onClick(TransfersFragment f, View v) {
            f.selectedStatus = status;
            f.onTime();
        }
    }
    
    private static final class SDCardNotificationListener extends ClickAdapter<TransfersFragment> {

		public SDCardNotificationListener(TransfersFragment owner) {
			super(owner);
		}
    	
		@Override
		public void onClick(TransfersFragment owner, View v) {
//	        Intent i = new Intent(owner.getActivity(), SettingsActivity.class);
//	        i.setAction(Constants.ACTION_SETTINGS_SELECT_STORAGE);
//	        owner.getActivity().startActivity(i);
		}
    }
}
