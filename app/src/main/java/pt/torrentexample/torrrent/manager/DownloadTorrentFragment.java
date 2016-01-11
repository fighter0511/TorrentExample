package pt.torrentexample.torrrent.manager;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import frostwire.logging.Logger;
import pt.torrentexample.R;
import pt.torrentexample.TorrentDownloadActivity;
import pt.torrentexample.gui.Util.UIUtils;
import pt.torrentexample.gui.adapter.TransferListAdapter;
import pt.torrentexample.gui.services.EngineService;
import pt.torrentexample.gui.transfers.Transfer;
import pt.torrentexample.gui.transfers.TransferManager;
import pt.torrentexample.gui.view.TimerObserver;
import pt.torrentexample.gui.view.TimerService;
import pt.torrentexample.gui.view.TimerSubscription;

/**
 * Created by PhucThanh on 1/11/2016.
 */
public class DownloadTorrentFragment extends Fragment implements TimerObserver{

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
    private EngineService.CheckDHTUICallback onDHTCheckCallback;
    private String uri;

    public enum TransferStatus {
        ALL, DOWNLOADING, COMPLETED
    }


    public DownloadTorrentFragment(){

    }

    public static DownloadTorrentFragment newInstance(){
        DownloadTorrentFragment fragment = new DownloadTorrentFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        transferComparator = new TransferCompator();
        onDHTCheckCallback = new OnCheckDHTCallback();

    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        subscription = TimerService.subscribe(this, 2);
    }

    private TextView textView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_download_torrent, container, false);
//        textView = (TextView) view.findViewById(R.id.textView);
        String myTag = getTag();
        ((TorrentManagerActivity)getActivity()).setString(myTag);
        initComponent(view);
        return view;
    }

    public void getUri(String string){
        uri = string;
    }

    public void initComponent(View view){
        list = (ExpandableListView) view.findViewById(R.id.fragment_transfers_list);
        textDHTPeers = (TextView) view.findViewById(R.id.fragment_transfers_dht_peers);
        textDHTPeers.setVisibility(View.GONE);
        textDownloads = (TextView) view.findViewById(R.id.fragment_transfers_text_downloads);
        textUploads = (TextView) view.findViewById(R.id.fragment_transfers_text_uploads);
    }


    @Override
    public void onResume() {
        super.onResume();
        if(uri != null){
            TransferManager.instance().downloadTorrent(uri);
        }
        onTime();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        subscription.unsubscribe();
    }

    @Override
    public void onTime() {
        if(adapter != null){
            List<Transfer> transfers = filter(TransferManager.instance().getTransfers());
            Collections.sort(transfers, transferComparator);
            adapter.updateList(transfers);
        } else if(this != null){
            setUpAdapter();
        }
        String sDown = UIUtils.rate2speed(TransferManager.instance().getDownloadsBandwidth());
        String sUp = UIUtils.rate2speed(TransferManager.instance().getUploadsBandwidth());

        int downloads = TransferManager.instance().getActiveDownloads();
        int uploads = TransferManager.instance().getActiveUploads();

        updatePermanentStatusNotification(sDown, sUp, downloads, uploads);
        updateStatusBar(sDown, sUp, downloads, uploads);

        adapter.notifyDataSetChanged();
    }

    private void setUpAdapter(){
        List<Transfer> transfers = filter(TransferManager.instance().getTransfers());
        Collections.sort(transfers, transferComparator);
        adapter = new TransferListAdapter(getActivity(), transfers);
        list.setAdapter(adapter);
    }

    private List<Transfer> filter(List<Transfer> transfers){
        Iterator<Transfer> it;
        it = transfers.iterator();
        while (it.hasNext()){
            if(it.next().isComplete()){
                it.remove();
            }
        }
        return transfers;
    }
    private void updateStatusBar(String sDown, String sUp, int downloads, int uploads){
        textDownloads.setText(downloads + " @ " + sDown);
        textUploads.setText(uploads + " @ " + sUp);
        EngineService.asyncCheckDHTPeers(getView(), onDHTCheckCallback);
    }

    private void updatePermanentStatusNotification(String sDown, String sUp, int downloads, int uploads){
        if(++androidNotificationUpdateTick >= FROSTWIRE_STATUS_NOTIFICATION_UPDATE_INTERVAL_IN_SECS){
            androidNotificationUpdateTick = 0;
            EngineService.updatePermanentStatusNotification(
                    new WeakReference<Context>(getActivity()),
                    new WeakReference<>(getView()),
                    downloads, sDown, uploads, sUp
            );
        }
    }

    private class OnCheckDHTCallback implements EngineService.CheckDHTUICallback{

        @Override
        public void onCheckDHT(boolean dhtEnabled, int dhtPeers) {
            textDHTPeers.setVisibility(dhtEnabled ? View.VISIBLE : View.GONE);
            if(!dhtEnabled){
                return;
            }
            textDHTPeers.setText(dhtPeers + " " + DownloadTorrentFragment.this.getString(R.string.dht_contacts));
        }
    }

    private static final class TransferCompator implements Comparator<Transfer>{

        @Override
        public int compare(Transfer lhs, Transfer rhs) {
            try{
                return -lhs.getDateCreated().compareTo(rhs.getDateCreated());
            } catch (Throwable e){

            }
            return 0;
        }
    }
}
