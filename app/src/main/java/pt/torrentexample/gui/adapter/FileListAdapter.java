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

package pt.torrentexample.gui.adapter;

import android.content.Context;
import android.view.View;
import android.widget.TextView;


import org.apache.commons.io.FilenameUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import pt.torrentexample.R;
import pt.torrentexample.core.Constants;
import pt.torrentexample.core.FileDescriptor;
import pt.torrentexample.gui.Util.UIUtils;
import pt.torrentexample.gui.adapter.menu.DeleteFileMenuAction;

/**
 * Adapter in control of the List View shown when we're browsing the files of
 * one peer.
 *
 * @author gubatron
 * @author aldenml
 */
public class FileListAdapter extends AbstractListAdapter<FileListAdapter.FileDescriptorItem> {


    private final byte fileType;
    private final FileListFilter fileListFilter;

    public FileListAdapter(Context context, List<FileDescriptor> files, byte fileType) {
        super(context, getViewItemId(fileType), convertFiles(files));

        setShowMenuOnClick(true);

        fileListFilter = new FileListFilter();
        setAdapterFilter(fileListFilter);

        this.fileType = fileType;

    }

    public byte getFileType() {
        return fileType;
    }

    @Override
    protected final void populateView(View view, FileDescriptorItem item) {
        if (getViewItemId() == R.layout.view_browse_thumbnail_peer_list_item) {
            populateViewThumbnail(view, item);
        } else {
            populateViewPlain(view, item);
        }
    }

    @Override
    protected MenuAdapter getMenuAdapter(View view) {
        Context context = getContext();

        List<MenuAction> items = new ArrayList<MenuAction>();

        // due to long click generic handle
        FileDescriptor fd = null;

        if (view.getTag() instanceof FileDescriptorItem) {
            FileDescriptorItem item = (FileDescriptorItem) view.getTag();
            fd = item.fd;
        } else if (view.getTag() instanceof FileDescriptor) {
            fd = (FileDescriptor) view.getTag();
        }

        List<FileDescriptor> checked = convertItems(getChecked());
        ensureCorrectMimeType(fd);
        boolean canOpenFile = fd.mime != null && (fd.mime.contains("audio") || fd.mime.contains("bittorrent") || fd.filePath != null);

        boolean showSingleOptions = showSingleOptions(checked, fd);

        if (showSingleOptions) {
            if (canOpenFile) {
                items.add(new OpenMenuAction(context, fd.filePath, fd.mime));
            }

        }
        List<FileDescriptor> list = checked;
        if(list.size() == 0){
            list = Arrays.asList(fd);
        }
        items.add(new DeleteFileMenuAction(context, this, list));

        return new MenuAdapter(context, fd.title, items);
    }

    public void deleteItem(FileDescriptor fd) {
        FileDescriptorItem item = new FileDescriptorItem();
        item.fd = fd;
        super.deleteItem(item);
    }

    protected void onLocalPlay() {
    }

    private void ensureCorrectMimeType(FileDescriptor fd) {
        if (fd.filePath.endsWith(".apk")) {
            fd.mime = Constants.MIME_TYPE_ANDROID_PACKAGE_ARCHIVE;
        }
        if (fd.filePath.endsWith(".torrent")) {
            fd.mime = Constants.MIME_TYPE_BITTORRENT;
        }
    }

    private void populateViewThumbnail(View view, FileDescriptorItem item) {
        FileDescriptor fd = item.fd;

        TextView title = findView(view, R.id.view_browse_peer_list_item_file_title);
        title.setText(fd.title);

        if (fd.fileType == Constants.FILE_TYPE_AUDIO || fd.fileType == Constants.FILE_TYPE_APPLICATIONS) {
            TextView fileExtra = findView(view, R.id.view_browse_peer_list_item_extra_text);
            fileExtra.setText(fd.artist);
        } else {
            TextView fileExtra = findView(view, R.id.view_browse_peer_list_item_extra_text);
            fileExtra.setText(R.string.empty_string);
        }

        TextView fileSize = findView(view, R.id.view_browse_peer_list_item_file_size);
        fileSize.setText(UIUtils.getBytesInHuman(fd.fileSize));

    }

    private void populateViewPlain(View view, FileDescriptorItem item) {
        FileDescriptor fd = item.fd;

        TextView title = findView(view, R.id.view_browse_peer_list_item_file_title);
        title.setText(fd.title);

        TextView fileExtra = findView(view, R.id.view_browse_peer_list_item_extra_text);
        if (fd.fileType == Constants.FILE_TYPE_AUDIO || fd.fileType == Constants.FILE_TYPE_APPLICATIONS) {
            fileExtra.setText(fd.artist);
        } else if (fd.fileType == Constants.FILE_TYPE_DOCUMENTS) {
            fileExtra.setText(FilenameUtils.getExtension(fd.filePath));
        } else {
            fileExtra.setText(R.string.empty_string);
        }

        TextView fileSize = findView(view, R.id.view_browse_peer_list_item_file_size);
        fileSize.setText(UIUtils.getBytesInHuman(fd.fileSize));


    }

    private boolean showSingleOptions(List<FileDescriptor> checked, FileDescriptor fd) {
        if (checked.size() > 1) {
            return false;
        }
        return checked.size() != 1 || checked.get(0).equals(fd);
    }

    private static int getViewItemId(byte fileType) {
        if (fileType == Constants.FILE_TYPE_PICTURES || fileType == Constants.FILE_TYPE_VIDEOS || fileType == Constants.FILE_TYPE_APPLICATIONS || fileType == Constants.FILE_TYPE_AUDIO) {
            return R.layout.view_browse_thumbnail_peer_list_item;
        } else {
            return R.layout.view_browse_peer_list_item;
        }
    }

    private static ArrayList<FileDescriptor> convertItems(Collection<FileDescriptorItem> items) {
        if (items == null) {
            return new ArrayList<>();
        }

        ArrayList<FileDescriptor> list = new ArrayList<>(items.size());

        for (FileDescriptorItem item : items) {
            list.add(item.fd);
        }

        return list;
    }

    private static ArrayList<FileDescriptorItem> convertFiles(Collection<FileDescriptor> fds) {
        if (fds == null) {
            return new ArrayList<>();
        }

        ArrayList<FileDescriptorItem> list = new ArrayList<>(fds.size());

        for (FileDescriptor fd : fds) {
            FileDescriptorItem item = new FileDescriptorItem();
            item.fd = fd;
            list.add(item);
        }

        return list;
    }


    private static class FileListFilter implements ListAdapterFilter<FileDescriptorItem> {
        public boolean accept(FileDescriptorItem obj, CharSequence constraint) {
            String keywords = constraint.toString();

            if (keywords == null || keywords.length() == 0) {
                return true;
            }

            keywords = keywords.toLowerCase(Locale.US);

            FileDescriptor fd = obj.fd;

            if (fd.fileType == Constants.FILE_TYPE_AUDIO) {
                return fd.album.trim().toLowerCase(Locale.US).contains(keywords) || fd.artist.trim().toLowerCase(Locale.US).contains(keywords) || fd.title.trim().toLowerCase(Locale.US).contains(keywords) || fd.filePath.trim().toLowerCase(Locale.US).contains(keywords);
            } else {
                return fd.title.trim().toLowerCase(Locale.US).contains(keywords) || fd.filePath.trim().toLowerCase(Locale.US).contains(keywords);
            }
        }
    }


    public static class FileDescriptorItem {

        public FileDescriptor fd;
        public boolean inSD;
        public boolean mounted;
        public boolean exists;

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof FileDescriptorItem)) {
                return false;
            }

            return fd.equals(((FileDescriptorItem) o).fd);
        }

        @Override
        public int hashCode() {
            return fd.id;
        }
    }
}
