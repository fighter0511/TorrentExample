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

package pt.torrentexample.core.provide;

import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.AudioColumns;
import android.provider.MediaStore.Files.FileColumns;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.Video.VideoColumns;

import pt.torrentexample.core.Constants;
import pt.torrentexample.core.FileDescriptor;


/**
 * Help yourself with TableFetchers.
 * <p/>
 * Note: if you need to fetch files by file path(s) see Librarian.instance().getFiles(filepath,exactMatch)
 *
 * @author gubatron
 * @author aldenml
 */
public final class TableFetchers {

    public final static TableFetcher AUDIO_TABLE_FETCHER = new AudioTableFetcher();
    public final static TableFetcher PICTURES_TABLE_FETCHER = new PicturesTableFetcher();
    public final static TableFetcher VIDEOS_TABLE_FETCHER = new VideosTableFetcher();
    public final static TableFetcher DOCUMENTS_TABLE_FETCHER = new DocumentsTableFetcher();
    public final static TableFetcher RINGTONES_TABLE_FETCHER = new RingtonesTableFetcher();
    public final static TableFetcher TORRENTS_TABLE_FETCHER = new TorrentsTableFetcher();

    public static abstract class AbstractTableFetcher implements TableFetcher {

        @Override
        public String where() {
            return null;
        }

        @Override
        public String[] whereArgs() {
            return new String[0];
        }
    }

    /**
     * Default Table Fetcher for Audio Files.
     */
    public final static class AudioTableFetcher extends AbstractTableFetcher {

        private int idCol;
        private int pathCol;
        private int mimeCol;
        private int artistCol;
        private int titleCol;
        private int albumCol;
        private int yearCol;
        private int sizeCol;
        private int dateAddedCol;
        private int dateModifiedCol;
        private int albumIdCol;

        public String[] getColumns() {
            return new String[]{AudioColumns._ID, AudioColumns.ARTIST, AudioColumns.TITLE, AudioColumns.ALBUM, AudioColumns.DATA, AudioColumns.YEAR, AudioColumns.MIME_TYPE, AudioColumns.SIZE, AudioColumns.DATE_ADDED, AudioColumns.DATE_MODIFIED, AudioColumns.ALBUM_ID};
        }

        public String getSortByExpression() {
            return AudioColumns.DATE_ADDED + " DESC";
        }

        public Uri getContentUri() {
            return MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }

        public void prepare(Cursor cur) {
            idCol = cur.getColumnIndex(AudioColumns._ID);
            pathCol = cur.getColumnIndex(AudioColumns.DATA);
            mimeCol = cur.getColumnIndex(AudioColumns.MIME_TYPE);
            artistCol = cur.getColumnIndex(AudioColumns.ARTIST);
            titleCol = cur.getColumnIndex(AudioColumns.TITLE);
            albumCol = cur.getColumnIndex(AudioColumns.ALBUM);
            yearCol = cur.getColumnIndex(AudioColumns.YEAR);
            sizeCol = cur.getColumnIndex(AudioColumns.SIZE);
            dateAddedCol = cur.getColumnIndex(AudioColumns.DATE_ADDED);
            dateModifiedCol = cur.getColumnIndex(AudioColumns.DATE_MODIFIED);
            albumIdCol = cur.getColumnIndex(AudioColumns.ALBUM_ID);
        }

        public FileDescriptor fetch(Cursor cur) {
            int id = cur.getInt(idCol);
            String path = cur.getString(pathCol);
            String mime = cur.getString(mimeCol);
            String artist = cur.getString(artistCol);
            String title = cur.getString(titleCol);
            String album = cur.getString(albumCol);
            String year = cur.getString(yearCol);
            int size = cur.getInt(sizeCol);
            long dateAdded = cur.getLong(dateAddedCol);
            long dateModified = cur.getLong(dateModifiedCol);
            long albumId = cur.getLong(albumIdCol);

            FileDescriptor fd = new FileDescriptor(Integer.valueOf(id), artist, title, album, year, path, Constants.FILE_TYPE_AUDIO, mime, size, dateAdded, dateModified, true);
            fd.albumId = albumId;

            return fd;
        }

        public byte getFileType() {
            return Constants.FILE_TYPE_AUDIO;
        }
    }

    public static class PicturesTableFetcher extends AbstractTableFetcher {

        private int idCol;
        private int titleCol;
        private int pathCol;
        private int mimeCol;
        private int sizeCol;
        private int dateAddedCol;
        private int dateModifiedCol;

        public FileDescriptor fetch(Cursor cur) {
            int id = cur.getInt(idCol);
            String path = cur.getString(pathCol);
            String mime = cur.getString(mimeCol);
            String title = cur.getString(titleCol);
            int size = cur.getInt(sizeCol);
            long dateAdded = cur.getLong(dateAddedCol);
            long dateModified = cur.getLong(dateModifiedCol);

            return new FileDescriptor(Integer.valueOf(id), null, title, null, null, path, Constants.FILE_TYPE_PICTURES, mime, size, dateAdded, dateModified, true);
        }

        public String[] getColumns() {
            return new String[]{ImageColumns._ID, ImageColumns.TITLE, ImageColumns.DATA, ImageColumns.MIME_TYPE, ImageColumns.MINI_THUMB_MAGIC, ImageColumns.SIZE, ImageColumns.DATE_ADDED, ImageColumns.DATE_MODIFIED};
        }

        public Uri getContentUri() {
            return MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        }

        public byte getFileType() {
            return Constants.FILE_TYPE_PICTURES;
        }

        public String getSortByExpression() {
            return ImageColumns.DATE_ADDED + " DESC";
        }

        public void prepare(Cursor cur) {
            idCol = cur.getColumnIndex(ImageColumns._ID);
            titleCol = cur.getColumnIndex(ImageColumns.TITLE);
            pathCol = cur.getColumnIndex(ImageColumns.DATA);
            mimeCol = cur.getColumnIndex(ImageColumns.MIME_TYPE);
            sizeCol = cur.getColumnIndex(ImageColumns.SIZE);
            dateAddedCol = cur.getColumnIndex(ImageColumns.DATE_ADDED);
            dateModifiedCol = cur.getColumnIndex(ImageColumns.DATE_MODIFIED);
        }
    }

    public static final class VideosTableFetcher extends AbstractTableFetcher {

        private int idCol;
        private int pathCol;
        private int mimeCol;
        private int artistCol;
        private int titleCol;
        private int albumCol;
        private int sizeCol;
        private int dateAddedCol;
        private int dateModifiedCol;

        public FileDescriptor fetch(Cursor cur) {
            int id = cur.getInt(idCol);
            String path = cur.getString(pathCol);
            String mime = cur.getString(mimeCol);
            String artist = cur.getString(artistCol);
            String title = cur.getString(titleCol);
            String album = cur.getString(albumCol);
            int size = cur.getInt(sizeCol);
            long dateAdded = cur.getLong(dateAddedCol);
            long dateModified = cur.getLong(dateModifiedCol);

            return new FileDescriptor(id, artist, title, album, null, path, Constants.FILE_TYPE_VIDEOS, mime, size, dateAdded, dateModified, true);
        }

        public String[] getColumns() {
            return new String[]{VideoColumns._ID, VideoColumns.ARTIST, VideoColumns.TITLE, VideoColumns.ALBUM, VideoColumns.DATA, VideoColumns.MIME_TYPE, VideoColumns.MINI_THUMB_MAGIC, VideoColumns.SIZE, VideoColumns.DATE_ADDED, VideoColumns.DATE_MODIFIED};
        }

        public Uri getContentUri() {
            return MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        }

        public byte getFileType() {
            return Constants.FILE_TYPE_VIDEOS;
        }

        public String getSortByExpression() {
            return VideoColumns.DATE_ADDED + " DESC";
        }

        public void prepare(Cursor cur) {
            idCol = cur.getColumnIndex(VideoColumns._ID);
            pathCol = cur.getColumnIndex(VideoColumns.DATA);
            mimeCol = cur.getColumnIndex(VideoColumns.MIME_TYPE);
            artistCol = cur.getColumnIndex(VideoColumns.ARTIST);
            titleCol = cur.getColumnIndex(VideoColumns.TITLE);
            albumCol = cur.getColumnIndex(VideoColumns.ALBUM);
            sizeCol = cur.getColumnIndex(VideoColumns.SIZE);
            dateAddedCol = cur.getColumnIndex(VideoColumns.DATE_ADDED);
            dateModifiedCol = cur.getColumnIndex(VideoColumns.DATE_MODIFIED);
        }
    }

    public static abstract class AbstractFilesTableFetcher extends AbstractTableFetcher {

        private int idCol;
        private int pathCol;
        private int mimeCol;
        private int titleCol;
        private int sizeCol;
        private int dateAddedCol;
        private int dateModifiedCol;

        public FileDescriptor fetch(Cursor cur) {
            int id = cur.getInt(idCol);
            String path = cur.getString(pathCol);
            String mime = cur.getString(mimeCol);
            String title = cur.getString(titleCol);
            int size = cur.getInt(sizeCol);
            long dateAdded = cur.getLong(dateAddedCol);
            long dateModified = cur.getLong(dateModifiedCol);
            return new FileDescriptor(Integer.valueOf(id), null, title, null, null, path, Constants.FILE_TYPE_DOCUMENTS, mime, size, dateAdded, dateModified, true);
        }

        public String[] getColumns() {
            return new String[]{FileColumns._ID, FileColumns.DATA, FileColumns.SIZE, FileColumns.TITLE, FileColumns.MIME_TYPE, FileColumns.DATE_ADDED, FileColumns.DATE_MODIFIED};
        }

        public Uri getContentUri() {
            return MediaStore.Files.getContentUri("external");
        }

        public byte getFileType() {
            return Constants.FILE_TYPE_DOCUMENTS;
        }

        public String getSortByExpression() {
            return FileColumns.DATE_ADDED + " DESC";
        }

        public void prepare(Cursor cur) {
            idCol = cur.getColumnIndex(FileColumns._ID);
            pathCol = cur.getColumnIndex(FileColumns.DATA);
            mimeCol = cur.getColumnIndex(FileColumns.MIME_TYPE);
            titleCol = cur.getColumnIndex(FileColumns.TITLE);
            sizeCol = cur.getColumnIndex(FileColumns.SIZE);
            dateAddedCol = cur.getColumnIndex(FileColumns.DATE_ADDED);
            dateModifiedCol = cur.getColumnIndex(FileColumns.DATE_MODIFIED);
        }
    }

    public static final class DocumentsTableFetcher extends AbstractFilesTableFetcher {

        final static String extensionsWhereSubClause = getExtsWhereSubClause();

        private static String getExtsWhereSubClause() {
            final String[] exts = new String[]{"jpg", "jpeg", "gif", "backup", "ticr", "aac", "mp3" , "mp4", "js", "dat", "bak", "png", "mrpr", "webm", "idx", "apnx", "db", "phl", "asc", "torrent", "xlog", "3gp", "alrbackup", "bin", "gz", "db-journal"};
            StringBuilder sb = new StringBuilder();
            for (String ext : exts) {
                sb.append(FileColumns.DATA + " NOT LIKE '%."+ext+"' AND ");
            }
            return sb.toString();
        }

        @Override
        public String where() {
            String where = FileColumns.DATA + " NOT LIKE ? AND " +
                    FileColumns.DATA + " NOT LIKE ? AND " +
                    FileColumns.DATA + " NOT LIKE ? AND " +
                    FileColumns.DATA + " NOT LIKE ? AND " +
                    extensionsWhereSubClause +
                    FileColumns.MEDIA_TYPE + " = " + FileColumns.MEDIA_TYPE_NONE + " AND " +
                    FileColumns.SIZE + " > 0 AND " + FileColumns.SIZE + " != 4096";
            return where;
        }

        @Override
        public String[] whereArgs() {
            return new String[]{"%cache%", "%/.%", "%/libtorrent/%", "%com.google.%"};
        }
    }

    public static final class TorrentsTableFetcher extends AbstractFilesTableFetcher {

        @Override
        public String where() {
            return FileColumns.DATA + " NOT LIKE ? AND " +
                    FileColumns.DATA + " NOT LIKE ? AND " +
                    FileColumns.DATA + " NOT LIKE ? AND " +
                    FileColumns.DATA + " LIKE ? AND " +
                    FileColumns.MEDIA_TYPE + " = " + FileColumns.MEDIA_TYPE_NONE + " AND " +
                    FileColumns.SIZE + " > 0";
        }

        @Override
        public String[] whereArgs() {
            return new String[]{"%/cache/%", "%/.%", "%/libtorrent/%", "%.torrent"};
        }
    }

    public static final class RingtonesTableFetcher extends AbstractTableFetcher {

        private int idCol;
        private int pathCol;
        private int mimeCol;
        private int artistCol;
        private int titleCol;
        private int albumCol;
        private int yearCol;
        private int sizeCol;
        private int dateAddedCol;
        private int dateModifiedCol;

        public FileDescriptor fetch(Cursor cur) {
            int id = cur.getInt(idCol);
            String path = cur.getString(pathCol);
            String mime = cur.getString(mimeCol);
            String artist = cur.getString(artistCol);
            String title = cur.getString(titleCol);
            String album = cur.getString(albumCol);
            String year = cur.getString(yearCol);
            int size = cur.getInt(sizeCol);
            long dateAdded = cur.getLong(dateAddedCol);
            long dateModified = cur.getLong(dateModifiedCol);

            return new FileDescriptor(Integer.valueOf(id), artist, title, album, year, path, Constants.FILE_TYPE_RINGTONES, mime, size, dateAdded, dateModified, true);
        }

        public String[] getColumns() {
            return new String[]{AudioColumns._ID, AudioColumns.ARTIST, AudioColumns.TITLE, AudioColumns.ALBUM, AudioColumns.DATA, AudioColumns.YEAR, AudioColumns.MIME_TYPE, AudioColumns.SIZE, AudioColumns.DATE_ADDED, AudioColumns.DATE_MODIFIED};
        }

        public Uri getContentUri() {
            return MediaStore.Audio.Media.INTERNAL_CONTENT_URI;
        }

        public byte getFileType() {
            return Constants.FILE_TYPE_RINGTONES;
        }

        public String getSortByExpression() {
            return AudioColumns.DATE_ADDED + " DESC";
        }

        public void prepare(Cursor cur) {
            idCol = cur.getColumnIndex(AudioColumns._ID);
            pathCol = cur.getColumnIndex(AudioColumns.DATA);
            mimeCol = cur.getColumnIndex(AudioColumns.MIME_TYPE);
            artistCol = cur.getColumnIndex(AudioColumns.ARTIST);
            titleCol = cur.getColumnIndex(AudioColumns.TITLE);
            albumCol = cur.getColumnIndex(AudioColumns.ALBUM);
            yearCol = cur.getColumnIndex(AudioColumns.YEAR);
            sizeCol = cur.getColumnIndex(AudioColumns.SIZE);
            dateAddedCol = cur.getColumnIndex(AudioColumns.DATE_ADDED);
            dateModifiedCol = cur.getColumnIndex(AudioColumns.DATE_MODIFIED);
        }
    }

    public static TableFetcher getFetcher(byte fileType) {
        switch (fileType) {
            case Constants.FILE_TYPE_AUDIO:
                return AUDIO_TABLE_FETCHER;
            case Constants.FILE_TYPE_PICTURES:
                return PICTURES_TABLE_FETCHER;
            case Constants.FILE_TYPE_VIDEOS:
                return VIDEOS_TABLE_FETCHER;
            case Constants.FILE_TYPE_DOCUMENTS:
                return DOCUMENTS_TABLE_FETCHER;
            case Constants.FILE_TYPE_RINGTONES:
                return RINGTONES_TABLE_FETCHER;
            case Constants.FILE_TYPE_TORRENTS:
                return TORRENTS_TABLE_FETCHER;
            default:
                return null;
        }
    }

    public static TableFetcher getFetcher(Uri uri) {
        String str = uri.toString();
        if (str.startsWith(AUDIO_TABLE_FETCHER.getContentUri().toString())) {
            return AUDIO_TABLE_FETCHER;
        } else if (str.startsWith(PICTURES_TABLE_FETCHER.getContentUri().toString())) {
            return PICTURES_TABLE_FETCHER;
        } else if (str.startsWith(VIDEOS_TABLE_FETCHER.getContentUri().toString())) {
            return VIDEOS_TABLE_FETCHER;
        } else if (str.startsWith(DOCUMENTS_TABLE_FETCHER.getContentUri().toString())) {
            return DOCUMENTS_TABLE_FETCHER;
        } else if (str.startsWith(TORRENTS_TABLE_FETCHER.getContentUri().toString())) {
            return TORRENTS_TABLE_FETCHER;
        } else if (str.startsWith(RINGTONES_TABLE_FETCHER.getContentUri().toString())) {
            return RINGTONES_TABLE_FETCHER;
        } else {
            return DOCUMENTS_TABLE_FETCHER;
        }
    }
}
