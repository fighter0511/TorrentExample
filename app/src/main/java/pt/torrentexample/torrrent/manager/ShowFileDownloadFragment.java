package pt.torrentexample.torrrent.manager;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
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
import pt.torrentexample.SearchActivity;
import pt.torrentexample.gui.MainApplication;
import pt.torrentexample.torrrent.manager.showfile.FileArrayAdapter;
import pt.torrentexample.torrrent.manager.showfile.Item;

/**
 * Created by PhucThanh on 1/13/2016.
 */
public class ShowFileDownloadFragment extends Fragment {

    private File currentDir;
    private FileArrayAdapter adapter;
    private ListView listFile;
    private FloatingActionButton fab;

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
        fab = (FloatingActionButton) view.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainApplication.getInstance().trackEvent("Downloaded Screen", "Search", "Track Event");
                Intent intent = new Intent(getContext(), SearchActivity.class);
                getContext().startActivity(intent);
            }
        });
        return view;
    }

    public void showDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Tìm kiếm");
        builder.setMessage("Kết quả tìm kiếm sẽ được hiện thị trên trình duyệt mặc định");
        final EditText editText = new EditText(getContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        editText.setLayoutParams(lp);
        editText.setGravity(Gravity.CENTER);
        builder.setView(editText);
        builder.setPositiveButton("Tìm kiếm", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String edtString = editText.getText().toString();
                String uri = "http://www.google.com/search?q=" + edtString + " torrent";
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                getContext().startActivity(intent);
            }
        });
        builder.setNegativeButton("Bỏ qua", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
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
                    String name = ff.getName();
                    String image = "";
                    if (name.contains("torrent")) {
                        image = "torrent_icon";
                    } else if (name.contains("mp3") || name.contains("3gp") || name.contains("mp4")
                            || name.contains("m4a") || name.contains("aac") || name.contains("wav")) {
                        image = "icon_media_play";
                    } else if (name.contains("jpg") || name.contains("gif") || name.contains("png")
                            || name.contains("bmp") || name.contains("webp")) {
                        image = "icon_photo";
                    } else image = "file_icon";

                    fls.add(new Item(ff.getName(), ff.length() + " Byte", date_modify, ff.getAbsolutePath(), image));
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
                Item item = (Item) adapter.getItem(position);
                openFile(item);
            }
        });
        listFile.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final Item item = (Item) adapter.getItem(position);
                if (!item.getImage().equalsIgnoreCase("directory_up")) {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle("Xóa file");
                    builder.setMessage("Bạn có muốn xóa file này không?");
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            deleteFile(item);
                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    builder.show();
                }
                return true;
            }
        });
    }

    private void openFile(Item item) {
        if (item.getImage().equalsIgnoreCase("directory_icon") || item.getImage().equalsIgnoreCase("directory_up")) {
            currentDir = new File(item.getPath());
            fill(currentDir);
        } else {
            runFile(item);
        }
    }

    private void deleteFile(Item item) {
        String name = item.getName();
        String path = item.getPath();
        File file = new File(path);
        file.delete();
        path = path.replace(name, "");
        currentDir = new File(path);
        fill(currentDir);
    }

    private void runFile(Item item) {
        MimeTypeMap myMime = MimeTypeMap.getSingleton();
        Intent intent = new Intent(Intent.ACTION_VIEW);
        String name = item.getName();
        String path = item.getPath();
        File file = new File(path);
        if (fileExt(name).substring(1) != null) {
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
