/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2013, FrostWire(R). All rights reserved.
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

package pt.torrentexample.gui.adapter.menu;

import android.content.Context;

import frostwire.uxstats.UXAction;
import frostwire.uxstats.UXStats;
import pt.torrentexample.R;
import pt.torrentexample.gui.NetworkManager;
import pt.torrentexample.gui.Util.UIUtils;
import pt.torrentexample.gui.adapter.MenuAction;
import pt.torrentexample.gui.transfers.BittorrentDownload;
import pt.torrentexample.gui.transfers.TransferManager;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public final class ResumeDownloadMenuAction extends MenuAction {
    //private static final Logger LOG = Logger.getLogger(ResumeDownloadMenuAction.class);
    private final BittorrentDownload download;

    public ResumeDownloadMenuAction(Context context, BittorrentDownload download, int stringId) {
        super(context, R.drawable.contextmenu_icon_play_transfer, stringId);
        this.download = download;
    }
    
    @Override
    protected void onClick(Context context) {
        boolean bittorrentDisconnected = TransferManager.instance().isBittorrentDisconnected();
        if (bittorrentDisconnected){
            UIUtils.showLongMessage(context, R.string.cant_resume_torrent_transfers);
        } else {
            if (NetworkManager.instance().isDataUp()) {
                if (download.isResumable()) {
                    download.resume();
                    UXStats.instance().log(UXAction.DOWNLOAD_RESUME);
                }
            } else {
                UIUtils.showShortMessage(context, R.string.please_check_connection_status_before_resuming_download);
            }
        }
    }
}