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

package pt.torrentexample.gui.task;

import android.content.Context;

import frostwire.logging.Logger;
import frostwire.search.SearchResult;
import pt.torrentexample.R;
import pt.torrentexample.gui.Util.UIUtils;
import pt.torrentexample.gui.transfers.BittorrentDownload;
import pt.torrentexample.gui.transfers.DownloadTransfer;
import pt.torrentexample.gui.transfers.ExistingDownload;
import pt.torrentexample.gui.transfers.InvalidTransfer;
import pt.torrentexample.gui.transfers.TransferManager;
import pt.torrentexample.gui.view.ContextTask;


/**
 * 
 * @author gubatron
 * @author aldenml
 * 
 */
public class StartDownloadTask extends ContextTask<DownloadTransfer> {

    private static final Logger LOG = Logger.getLogger(StartDownloadTask.class);

    private final SearchResult sr;
    private final String message;

    public StartDownloadTask(Context ctx, SearchResult sr, String message) {
        super(ctx);
        this.sr = sr;
        this.message = message;
    }
    
    public StartDownloadTask(Context ctx, SearchResult sr){
        this(ctx,sr,null);
    }

    @Override
    protected DownloadTransfer doInBackground() {
        DownloadTransfer transfer = null;
        try {
            transfer = TransferManager.instance().download(sr);
        } catch (Throwable e) {
            LOG.warn("Error adding new download from result: " + sr, e);
            e.printStackTrace();
        }
        return transfer;
    }

    @Override
    protected void onPostExecute(Context ctx, DownloadTransfer transfer) {
        if (transfer != null) {
            if (!(transfer instanceof InvalidTransfer)) {
                TransferManager tm = TransferManager.instance();
                if (tm.isBittorrentDownloadAndMobileDataSavingsOn(transfer)) {
                    UIUtils.showLongMessage(ctx, R.string.torrent_transfer_enqueued_on_mobile_data);
                    ((BittorrentDownload) transfer).pause();
                } else {
                    if (tm.isBittorrentDownloadAndMobileDataSavingsOff(transfer)) {
                        UIUtils.showLongMessage(ctx, R.string.torrent_transfer_consuming_mobile_data);
                    }
                    
                    if (message != null){
                        UIUtils.showShortMessage(ctx, message);
                    }
                }
            } else {
                if (transfer instanceof ExistingDownload) {
                    //nothing happens here, the user should just see the transfer
                    //manager and we avoid adding the same transfer twice.
                } else {
                    UIUtils.showLongMessage(ctx, ((InvalidTransfer) transfer).getReasonResId());
                }
            }
        }
    }
}