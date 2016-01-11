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

package pt.torrentexample.gui.fragments;

import android.app.Activity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.AsyncTaskLoader;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RadioButton;
import android.widget.TextView;

import frostwire.logging.Logger;
import frostwire.util.StringUtils;
import frostwire.uxstats.UXAction;
import frostwire.uxstats.UXStats;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import pt.torrentexample.R;
import pt.torrentexample.core.ConfigurationManager;
import pt.torrentexample.core.Constants;
import pt.torrentexample.core.FileDescriptor;
import pt.torrentexample.gui.Finger;
import pt.torrentexample.gui.Peer;
import pt.torrentexample.gui.Util.UIUtils;
import pt.torrentexample.gui.adapter.FileListAdapter;
import pt.torrentexample.gui.view.AbstractFragment;
import pt.torrentexample.gui.view.ListView;
import pt.torrentexample.gui.view.MainFragment;
import pt.torrentexample.gui.view.OverScrollListener;

/**
 * @author gubatron
 * @author aldenml
 */
public class BrowsePeerFragment extends AbstractFragment implements LoaderCallbacks<Object>, MainFragment {
    private static final Logger LOG = Logger.getLogger(BrowsePeerFragment.class);
    private static final int LOADER_FILES_ID = 0;
    private ListView list;
    private FileListAdapter adapter;
    private Peer peer;
    private Finger finger;
    private View header;
    private long lastAdapterRefresh;
    private String previousFilter;
    private Set<FileListAdapter.FileDescriptorItem> previouslyChecked;

    // given the byte:fileType as the index, this array will match the corresponding UXAction code.
    // no if's necessary, random access -> O(1)
    private final int[] browseUXActions = {
            UXAction.LIBRARY_BROWSE_FILE_TYPE_AUDIO,
            UXAction.LIBRARY_BROWSE_FILE_TYPE_PICTURES,
            UXAction.LIBRARY_BROWSE_FILE_TYPE_VIDEOS,
            UXAction.LIBRARY_BROWSE_FILE_TYPE_DOCUMENTS,
            UXAction.LIBRARY_BROWSE_FILE_TYPE_APPLICATIONS,
            UXAction.LIBRARY_BROWSE_FILE_TYPE_RINGTONES,
            UXAction.LIBRARY_BROWSE_FILE_TYPE_TORRENTS
    };

    public BrowsePeerFragment() {
        super(R.layout.fragment_browse_peer);
        this.peer = new Peer();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setRetainInstance(true);
    }

    @Override
    public Loader<Object> onCreateLoader(int id, Bundle args) {
        if (id == LOADER_FILES_ID) {
            return createLoaderFiles(args.getByte("fileType"));
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Object> loader, Object data) {
        if (data == null) {
            LOG.warn("Something wrong, data is null");
            return;
        }

        if (loader.getId() == LOADER_FILES_ID) {
            updateFiles((Object[]) data);
        }

        updateHeader();
    }

    @Override
    public void onLoaderReset(Loader<Object> loader) {
    }

    @Override
    public void onResume() {
        super.onResume();
        initBroadcastReceiver();

        if (adapter != null) {
            restorePreviouslyChecked();
//            restorePreviousFilter();
            browseFilesButtonClick(adapter.getFileType());
        }
    }

    private void restorePreviouslyChecked() {
        if (previouslyChecked != null && !previouslyChecked.isEmpty()) {
            adapter.setChecked(previouslyChecked);
        }
    }

//    private void restorePreviousFilter() {
//        if (previousFilter != null && filesBar != null) {
//           filesBar.setText(previousFilter);
//        }
//    }

    private void savePreviouslyCheckedFileDescriptors() {
        if (adapter != null) {
            final Set<FileListAdapter.FileDescriptorItem> checked = adapter.getChecked();
            if (checked != null && !checked.isEmpty()) {
                previouslyChecked = new HashSet<>(checked);
            } else {
                previouslyChecked = null;
            }
        }
    }

    private void savePreviousFilter() {
//        if (!StringUtils.isNullOrEmpty(filesBar.getText())) {
//            previousFilter = filesBar.getText();
//        }
    }

    private void clearPreviousFilter() {
        previousFilter = null;
    }

    private void initBroadcastReceiver() {
        final IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_REFRESH_FINGER);
        filter.addAction(Constants.ACTION_MEDIA_PLAYER_PLAY);
        filter.addAction(Constants.ACTION_MEDIA_PLAYER_PAUSED);
        filter.addAction(Constants.ACTION_MEDIA_PLAYER_STOPPED);
    }

    @Override
    public void onPause() {
        super.onPause();
        savePreviouslyCheckedFileDescriptors();
        savePreviousFilter();
    }

    @Override
    public View getHeader(Activity activity) {
        LayoutInflater inflater = LayoutInflater.from(activity);
        header = inflater.inflate(R.layout.view_browse_peer_header, null);

        updateHeader();

        return header;
    }

    @Override
    protected void initComponents(View v) {

        list = findView(v, R.id.fragment_browse_peer_list);
        list.setOverScrollListener(new OverScrollListener() {
            @Override
            public void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
                long now = SystemClock.elapsedRealtime();
                if (scrollY < 0 && clampedY && (now - lastAdapterRefresh) > 5000) {
                    refreshSelection();
                }
            }
        });

    }


    private void browseFilesButtonClick(byte fileType) {
        if (adapter != null) {
            savePreviouslyCheckedFileDescriptors();
            savePreviousFilter();
            saveListViewVisiblePosition(adapter.getFileType());
            adapter.clear();
        }
        reloadFiles(fileType);
        logBrowseAction(fileType);
    }

    private void logBrowseAction(byte fileType) {
        try {
            UXStats.instance().log(browseUXActions[fileType]);
        } catch (Throwable ignored) {}
    }

    private void reloadFiles(byte fileType) {
        getLoaderManager().destroyLoader(LOADER_FILES_ID);
        Bundle bundle = new Bundle();
        bundle.putByte("fileType", fileType);
        getLoaderManager().restartLoader(LOADER_FILES_ID, bundle, this);
    }

    private Loader<Object> createLoaderFiles(final byte fileType) {
        AsyncTaskLoader<Object> loader = new AsyncTaskLoader<Object>(getActivity()) {
            @Override
            public Object loadInBackground() {
                try {
                    return new Object[]{fileType, peer.browse(fileType)};
                } catch (Throwable e) {
                    LOG.error("Error performing finger", e);
                }
                return null;
            }
        };
        loader.forceLoad();
        return loader;
    }

    private void updateHeader() {
        if (finger == null) {
            if (peer == null) {
                LOG.warn("Something wrong, finger  and peer are null");
                return;
            } else {

                finger = peer.finger();
            }
        }

        if (header != null) {

            byte fileType = adapter != null ? adapter.getFileType() : Constants.FILE_TYPE_TORRENTS;

            int numTotal = 0;

            switch (fileType) {
                case Constants.FILE_TYPE_TORRENTS:
                    numTotal = finger.numTotalTorrentFiles;
                    break;
            }

            String fileTypeStr = getString(R.string.my_filetype, UIUtils.getFileTypeAsString(getResources(), fileType));

            TextView title = (TextView) header.findViewById(R.id.view_browse_peer_header_text_title);
            TextView total = (TextView) header.findViewById(R.id.view_browse_peer_header_text_total);

            title.setText(fileTypeStr);
            total.setText("(" + String.valueOf(numTotal) + ")");
        }

        if (adapter == null) {
            browseFilesButtonClick(Constants.FILE_TYPE_TORRENTS);
        }

        restoreListViewScrollPosition();
    }

    private void restoreListViewScrollPosition() {
        if (adapter != null) {
            int savedListViewVisiblePosition = getSavedListViewVisiblePosition(adapter.getFileType());
            savedListViewVisiblePosition = (savedListViewVisiblePosition > 0) ? savedListViewVisiblePosition + 1 : 0;
            list.setSelection(savedListViewVisiblePosition);
        }
    }

    private void updateFiles(Object[] data) {
        if (data == null) {
            LOG.warn("Something wrong, data is null");
            return;
        }

        try {
            byte fileType = (Byte) data[0];

            @SuppressWarnings("unchecked")
            List<FileDescriptor> items = (List<FileDescriptor>) data[1];
            adapter = new FileListAdapter(getActivity(), items, fileType) {

                @Override
                protected void onItemChecked(View v, boolean isChecked) {
                    if (!isChecked) {
//                        filesBar.clearCheckAll();
                    }
                }

                @Override
                protected void onLocalPlay() {
                    if (adapter != null) {
                        saveListViewVisiblePosition(adapter.getFileType());
                    }
                }
            };
            adapter.setCheckboxesVisibility(true);
            restorePreviouslyChecked();
//            restorePreviousFilter();
            list.setAdapter(adapter);

        } catch (Throwable e) {
            LOG.error("Error updating files in list", e);
        }
    }

    private void saveListViewVisiblePosition(byte fileType) {
        int firstVisiblePosition = list.getFirstVisiblePosition();
        ConfigurationManager.instance().setInt(Constants.BROWSE_PEER_FRAGMENT_LISTVIEW_FIRST_VISIBLE_POSITION + fileType, firstVisiblePosition);
    }

    private int getSavedListViewVisiblePosition(byte fileType) {
        //will return 0 if not found.
        return ConfigurationManager.instance().getInt(Constants.BROWSE_PEER_FRAGMENT_LISTVIEW_FIRST_VISIBLE_POSITION + fileType);
    }



    public void refreshSelection() {
        if (adapter != null) {
            lastAdapterRefresh = SystemClock.elapsedRealtime();
            browseFilesButtonClick(adapter.getFileType());
        }
    }
}
