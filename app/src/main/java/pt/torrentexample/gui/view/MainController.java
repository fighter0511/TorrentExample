/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2015, FrostWire(R). All rights reserved.
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

package pt.torrentexample.gui.view;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import pt.torrentexample.MainActivity;
import pt.torrentexample.R;
import pt.torrentexample.SettingsActivity;
import pt.torrentexample.core.FileDescriptor;
import pt.torrentexample.gui.Librarian;
import pt.torrentexample.gui.adnetwork.Offers;
import pt.torrentexample.gui.fragments.TransfersFragment;
import pt.torrentexample.gui.services.Engine;
import pt.torrentexample.gui.transfers.TransferManager;

/**
 * 
 * @author gubatron
 * @author aldenml
 *
 */
public final class MainController {

    private final MainActivity activity;

    public MainController(MainActivity activity) {
        this.activity = activity;
    }

    public MainActivity getActivity() {
        return activity;
    }

    public void closeSlideMenu() {
        activity.closeSlideMenu();
    }

    public void switchFragment(int itemId) {
        Fragment fragment = activity.getFragmentByMenuId(itemId);
        if (fragment != null) {
            activity.switchContent(fragment);
        }
    }



    public void showFreeApps(Context context) {
        Offers.onFreeAppsClick(context);
    }


    public void showTransfers(TransfersFragment.TransferStatus status) {
        if (!(activity.getCurrentFragment() instanceof TransfersFragment)) {
            TransfersFragment fragment = (TransfersFragment) activity.getFragmentByMenuId(R.id.menu_main_transfers);
            fragment.selectStatusTab(status);
            switchFragment(R.id.menu_main_transfers);
        }
    }

    public void showPreferences() {
        Intent i = new Intent(activity, SettingsActivity.class);
        activity.startActivity(i);
    }

    public void handleSendAction(Intent intent) {
        String action = intent.getAction();
        if (action.equals(Intent.ACTION_SEND)) {
            handleSendSingleFile(intent);
        }
    }

    private void handleSendSingleFile(Intent intent) {
        Uri uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (uri == null) {
            return;
        }

        FileDescriptor fileDescriptor = Librarian.instance().getFileDescriptor(uri);

        if (fileDescriptor != null) {
            // Until we don't show .torrents on file manager, the most logical thing to do if user wants to
            // "Share" a `.torrent` from a third party app with FrostWire, that is starting the `.torrent` transfer.
            if (fileDescriptor.filePath != null && fileDescriptor.filePath.endsWith(".torrent")) {
                TransferManager.instance().downloadTorrent(uri.toString());
                activity.switchFragment(R.id.menu_main_transfers);
            }
        }
    }
}
