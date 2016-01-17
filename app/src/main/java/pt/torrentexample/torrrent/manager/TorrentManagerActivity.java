package pt.torrentexample.torrrent.manager;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import frostwire.uxstats.UXAction;
import frostwire.uxstats.UXStats;
import pt.torrentexample.R;
import pt.torrentexample.core.ConfigurationManager;
import pt.torrentexample.core.Constants;
import pt.torrentexample.gui.MainApplication;
import pt.torrentexample.gui.dialog.YesNoDialog;

public class TorrentManagerActivity extends FragmentActivity {

    private ViewPager viewPager;
    private TabLayout tabLayout;
    private String string;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_torrent_manager);
        ConfigurationManager.instance().setBoolean(Constants.PREF_KEY_GUI_TOS_ACCEPTED, true);
        initComponent();
        onNewIntent(getIntent());
    }

    public void initComponent(){
        tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        viewPager = (ViewPager) findViewById(R.id.viewPager);

        tabLayout.addTab(tabLayout.newTab().setText("Torrent"));
        tabLayout.addTab(tabLayout.newTab().setText("Downloaded"));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        PagerAdapter pagerAdapter = new PagerAdapter(getSupportFragmentManager(), tabLayout.getTabCount());
        viewPager.setAdapter(pagerAdapter);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    public class PagerAdapter extends FragmentStatePagerAdapter{
        private int numOfTabs;

        public PagerAdapter(FragmentManager fm, int numOfTabs) {
            super(fm);
            this.numOfTabs = numOfTabs;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position){
                case 0:
                    ShowTorrentFragment tab1 = new ShowTorrentFragment();
                    return tab1;
                case 1:
                    ShowFileDownloadFragment tab2 = new ShowFileDownloadFragment();
                    return tab2;
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return numOfTabs;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkUpdate();
        MainApplication.getInstance().trackScreenView("Torrent manager activity");
    }

    public void checkUpdate(){
        String urlUpdate = "http://5play.me:8892/WebDirectory-0.0.1-SNAPSHOT/browser/torrent_config";
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, urlUpdate,
                null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    String currentVersion = response.getString("current_version");
                    String forceUpdate = response.getString("force_update");
                    String updateUrl = response.getString("update_url");
                    if(!currentVersion.equalsIgnoreCase("1") || !forceUpdate.equalsIgnoreCase("false") ){
                        showUpdateDialog(updateUrl);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {

            }
        });
        MainApplication.getInstance().addToRequestQueue(request, "req");
    }

    public void showUpdateDialog(final String updateUrl){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Cập nhật");
        builder.setMessage("Đã có phiên bản mới của ứng dụng, cập nhật để có những tính năng mới nhất");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl));
                startActivity(intent);
            }
        });
        builder.setNegativeButton("Cancal", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if(intent == null){
            return;
        }
        String action = intent.getAction();
        if(action != null){
            if(action.equals(Constants.ACTION_REQUEST_SHUTDOWN)){
                UXStats.instance().log(UXAction.MISC_NOTIFICATION_EXIT);
                showShutdownDialog();
            }
        }
    }

    private static final String SHUTDOWN_DIALOG_ID = "shutdown_dialog";

    private void showShutdownDialog() {
        YesNoDialog dlg = YesNoDialog.newInstance(SHUTDOWN_DIALOG_ID, R.string.app_shutdown_dlg_title, R.string.app_shutdown_dlg_message);
        dlg.show(getFragmentManager()); //see onDialogClick
    }
}
