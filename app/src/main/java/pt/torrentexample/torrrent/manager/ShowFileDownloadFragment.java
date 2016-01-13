package pt.torrentexample.torrrent.manager;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Toast;

import java.io.File;
import java.sql.Date;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import pt.torrentexample.R;
import pt.torrentexample.torrrent.manager.showfile.FileArrayAdapter;
import pt.torrentexample.torrrent.manager.showfile.Item;

/**
 * Created by PhucThanh on 1/13/2016.
 */
public class ShowFileDownloadFragment extends Fragment {

    private File currentDir;
    private FileArrayAdapter adapter;
    private ListView listFile;

    public ShowFileDownloadFragment newInstance() {
        ShowFileDownloadFragment fragment = new ShowFileDownloadFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentDir = new File("/sdcard/TorrentExample/TorrentsData");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_file_downloaded, null);
        listFile = (ListView) view.findViewById(R.id.list_file);
        fill(currentDir);
        return view;
    }

    private void fill(File f) {
        File[] dirs = f.listFiles();
        List<Item> dir = new ArrayList<Item>();
        List<Item> fls = new ArrayList<Item>();
        try {
            for (File ff : dirs) {
                Date lastModDate = new Date(ff.lastModified());
                DateFormat formater = DateFormat.getDateTimeInstance();
                String date_modify = formater.format(lastModDate);
                if (ff.isDirectory()) {
                    File[] fbuf = ff.listFiles();
                    int buf = 0;
                    if (fbuf != null) {
                        buf = fbuf.length;
                    } else buf = 0;
                    String num_item = String.valueOf(buf);
                    if (buf == 0) num_item = num_item + " item";
                    else num_item = num_item + " items";

                    dir.add(new Item(ff.getName(), num_item, date_modify, ff.getAbsolutePath(), "directory_icon"));
                } else {
                    fls.add(new Item(ff.getName(), ff.length() + " Byte", date_modify, ff.getAbsolutePath(), "file_icon"));
                }
            }
        } catch (Exception e) {

        }
        Collections.sort(dir);
        Collections.sort(fls);
        dir.addAll(fls);
        if (!f.getName().equalsIgnoreCase("TorrentsData"))
            dir.add(0, new Item("..", "Quay lại", "", f.getParent(), "directory_up"));
        adapter = new FileArrayAdapter(getContext(), dir);
        listFile.setAdapter(adapter);

        listFile.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, final long id) {
                if (position == 0) {
                    openFile((Item) adapter.getItem(0));
                } else {
                    final Item itemFile = (Item) adapter.getItem(position);
                    PopupMenu popupMenu = new PopupMenu(getContext(), view);
                    popupMenu.inflate(R.menu.menu_file_downloaded);
                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()) {
                                case R.id.open:
                                    openFile(itemFile);
                                    return true;
                                case R.id.delete:
                                    deleteFile(itemFile);
                                    return true;
                            }
                            return true;
                        }
                    });
                    popupMenu.show();
                }
            }
        });
    }

    private void openFile(Item item){
        if (item.getImage().equalsIgnoreCase("directory_icon") || item.getImage().equalsIgnoreCase("directory_up")) {
            currentDir = new File(item.getPath());
            fill(currentDir);
        } else {
            runFile(item);
        }
    }

    private void deleteFile(Item item){
        String name = item.getName();
        String path = item.getPath();
        File file = new File(path);
        file.delete();
        path = path.replace(name, "");
        currentDir = new File(path);
        fill(currentDir);
    }

    private void runFile(Item item){
        MimeTypeMap myMime = MimeTypeMap.getSingleton();
        Intent intent = new Intent(Intent.ACTION_VIEW);
        String name = item.getName();
        String path = item.getPath();
        File file = new File(path);
        if(fileExt(name).substring(1) != null){
            String mimeType = myMime.getMimeTypeFromExtension(fileExt(name).substring(1));
            intent.setDataAndType(Uri.fromFile(file), mimeType);
            getContext().startActivity(intent);
        } else {
            getContext().startActivity(intent);
        }
    }

    private String fileExt(String url) {
        if (url.indexOf("?") > -1) {
            url = url.substring(0, url.indexOf("?"));
        }
        if (url.lastIndexOf(".") == -1) {
            return null;
        } else {
            String ext = url.substring(url.lastIndexOf("."));
            if (ext.indexOf("%") > -1) {
                ext = ext.substring(0, ext.indexOf("%"));
            }
            if (ext.indexOf("/") > -1) {
                ext = ext.substring(0, ext.indexOf("/"));
            }
            return ext.toLowerCase();

        }
    }
}
