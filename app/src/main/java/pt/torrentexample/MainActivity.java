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

package pt.torrentexample;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout.SimpleDrawerListener;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.RelativeLayout;



import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import frostwire.logging.Logger;
import frostwire.util.Ref;
import frostwire.util.StringUtils;
import frostwire.uxstats.UXAction;
import frostwire.uxstats.UXStats;
import pt.torrentexample.core.ConfigurationManager;
import pt.torrentexample.core.Constants;
import pt.torrentexample.gui.Util.DangerousPermissionsChecker;
import pt.torrentexample.gui.Util.UIUtils;
import pt.torrentexample.gui.adnetwork.Offers;
import pt.torrentexample.gui.dialog.YesNoDialog;
import pt.torrentexample.gui.fragments.BrowsePeerFragment;
import pt.torrentexample.gui.fragments.TransfersFragment;
import pt.torrentexample.gui.services.Engine;
import pt.torrentexample.gui.transfers.TransferManager;
import pt.torrentexample.gui.view.AbstractActivity;
import pt.torrentexample.gui.view.AbstractDialog;
import pt.torrentexample.gui.view.MainController;
import pt.torrentexample.gui.view.MainFragment;
import pt.torrentexample.gui.view.MainMenuAdapter;
import pt.torrentexample.gui.view.TimerObserver;
import pt.torrentexample.gui.view.TimerService;
import pt.torrentexample.gui.view.TimerSubscription;


/**
 *
 * @author gubatron
 * @author aldenml
 *
 */
public class MainActivity extends AbstractActivity implements
        AbstractDialog.OnDialogClickListener,
        ServiceConnection,
        ActivityCompat.OnRequestPermissionsResultCallback,
        DangerousPermissionsChecker.PermissionsCheckerHolder {

    private static final Logger LOG = Logger.getLogger(MainActivity.class);
    private static final String FRAGMENTS_STACK_KEY = "fragments_stack";
    private static final String CURRENT_FRAGMENT_KEY = "current_fragment";
    private static final String LAST_BACK_DIALOG_ID = "last_back_dialog";
    private static final String SHUTDOWN_DIALOG_ID = "shutdown_dialog";
    private static boolean firstTime = true;
    private final Map<Integer, DangerousPermissionsChecker> permissionsCheckers;
    private MainController controller;
    private DrawerLayout drawerLayout;
    @SuppressWarnings("deprecation")
    private ActionBarDrawerToggle drawerToggle;
    private View leftDrawer;
    private ListView listMenu;
    private BrowsePeerFragment library;
    private TransfersFragment transfers;
    private Fragment currentFragment;
    private final Stack<Integer> fragmentsStack;
    private TimerSubscription playerSubscription;
    private BroadcastReceiver mainBroadcastReceiver;
    private boolean externalStoragePermissionsRequested = false;

    public MainActivity() {
        super(R.layout.activity_main);
        this.controller = new MainController(this);
        this.fragmentsStack = new Stack<>();
        this.permissionsCheckers = initPermissionsCheckers();
    }

    public void switchFragment(int itemId) {
        controller.switchFragment(itemId);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_SEARCH) {
            if (!(getCurrentFragment() instanceof BrowsePeerFragment)) {
                controller.switchFragment(R.id.menu_main_library);
            }
        } else if (keyCode == KeyEvent.KEYCODE_MENU) {
            toggleDrawer();
        } else {
            return super.onKeyDown(keyCode, event);
        }

        return true;
    }

    @Override
    public void onBackPressed() {
        if (fragmentsStack.size() > 1) {
            try {
                fragmentsStack.pop();
                int id = fragmentsStack.peek();
                Fragment fragment = getFragmentManager().findFragmentById(id);
                switchContent(fragment, false);
            } catch (Throwable e) {
                // don't break the app
                showLastBackDialog();
            }
        } else {
            showLastBackDialog();
        }

        syncSlideMenu();
        updateHeader(getCurrentFragment());
    }

    public void onConfigurationUpdate() {
        setupMenuItems();
    }

    public void shutdown() {
        Offers.stopAdNetworks(this);
        UXStats.instance().flush(true); // sends data and ends 3rd party APIs sessions.
        finish();
        Engine.instance().shutdown();
    }

    private boolean isShutdown() {
        Intent intent = getIntent();
        boolean result = intent != null && intent.getBooleanExtra("shutdown-" + ConfigurationManager.instance().getUUIDString(), false);
        if (result) {
            shutdown();
        }
        return result;
    }

    @Override
    protected void initComponents(Bundle savedInstanceState) {
        if (isShutdown()) {
            return;
        }
        initDrawerListener();
        leftDrawer = findView(R.id.activity_main_left_drawer);
        listMenu = findView(R.id.left_drawer);
        initPlayerItemListener();
        setupFragments();
        setupMenuItems();
        setupInitialFragment(savedInstanceState);
        onNewIntent(getIntent());
        setupActionBar();
        setupDrawer();
    }

    private void initPlayerItemListener() {
//        playerItem = findView(R.id.slidemenu_player_menuitem);
//        playerItem.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                controller.launchPlayerActivity();
//            }
//        });
    }

    private void initDrawerListener() {
        drawerLayout = findView(R.id.drawer_layout);
        drawerLayout.setDrawerListener(new SimpleDrawerListener() {
            @Override
            public void onDrawerStateChanged(int newState) {
                refreshPlayerItem();
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                syncSlideMenu();
            }

            @Override
            public void onDrawerClosed(View drawerView) {

            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (intent == null) {
            return;
        }

        String action = intent.getAction();

        if (action != null) {
            if (action.equals(Constants.ACTION_SHOW_TRANSFERS)) {
                intent.setAction(null);
                controller.showTransfers(TransfersFragment.TransferStatus.ALL);
            } else if (action.equals(Constants.ACTION_OPEN_TORRENT_URL)) {
                openTorrentUrl(intent);
            }
            // When another application wants to "Share" a file and has chosen FrostWire to do so.
            // We make the file "Shared" so it's visible for other FrostWire devices on the local network.
            else if (action.equals(Intent.ACTION_SEND) ||
                     action.equals(Intent.ACTION_SEND_MULTIPLE)) {
                controller.handleSendAction(intent);
                intent.setAction(null);
            } else if (action.equals(Constants.ACTION_START_TRANSFER_FROM_PREVIEW)) {
//                if (Ref.alive(NewTransferDialog.srRef)) {
//                    SearchFragment.startDownload(this, NewTransferDialog.srRef.get(), getString(R.string.download_added_to_queue));
//                    UXStats.instance().log(UXAction.DOWNLOAD_CLOUD_FILE_FROM_PREVIEW);
//                }
            } else if (action.equals(Constants.ACTION_REQUEST_SHUTDOWN)) {
                UXStats.instance().log(UXAction.MISC_NOTIFICATION_EXIT);
                showShutdownDialog();
            }
        }

        if (intent.hasExtra(Constants.EXTRA_DOWNLOAD_COMPLETE_NOTIFICATION)) {
            controller.showTransfers(TransfersFragment.TransferStatus.COMPLETED);
            TransferManager.instance().clearDownloadsToReview();
            try {
                ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(Constants.NOTIFICATION_DOWNLOAD_TRANSFER_FINISHED);
                Bundle extras = intent.getExtras();
                if (extras.containsKey(Constants.EXTRA_DOWNLOAD_COMPLETE_PATH)) {
                    File file = new File(extras.getString(Constants.EXTRA_DOWNLOAD_COMPLETE_PATH));
                    if (file.isFile()) {
                        UIUtils.openFile(this, file.getAbsoluteFile());
                    }
                }
            } catch (Throwable e) {
                LOG.warn("Error handling download complete notification", e);
            }
        }
    }

    private void openTorrentUrl(Intent intent) {
        //Open a Torrent from a URL or from a local file :), say from Astro File Manager.
        /**
         * TODO: Ask @aldenml the best way to plug in NewTransferDialog.
         * I've refactored this dialog so that it is forced (no matter if the setting
         * to not show it again has been used) and when that happens the checkbox is hidden.
         *
         * However that dialog requires some data about the download, data which is not
         * obtained until we have instantiated the Torrent object.
         *
         * I'm thinking that we can either:
         * a) Pass a parameter to the transfer manager, but this would probably
         * not be cool since the transfer manager (I think) should work independently from
         * the UI thread.
         *
         * b) Pass a "listener" to the transfer manager, once the transfer manager has the torrent
         * it can notify us and wait for the user to decide whether or not to continue with the transfer
         *
         * c) Forget about showing that dialog, and just start the download, the user can cancel it.
         */

        //Show me the transfer tab
        Intent i = new Intent(this, MainActivity.class);
        i.setAction(Constants.ACTION_SHOW_TRANSFERS);
        i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(i);

        //go!
        final String uri = intent.getDataString();
        if (uri != null) {
            TransferManager.instance().downloadTorrent(uri);
        } else {
            LOG.warn("MainActivity.onNewIntent(): Couldn't start torrent download from Intent's URI, intent.getDataString() -> null");
            LOG.warn("(maybe URI is coming in another property of the intent object - #fragmentation)");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        initDrawerListener();
        setupDrawer();
        initPlayerItemListener();

        refreshPlayerItem();

        if (ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_GUI_TOS_ACCEPTED)) {
            if (ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_GUI_INITIAL_SETTINGS_COMPLETE)) {
                mainResume();
                Offers.initAdNetworks(this);
            }
        }
        ConfigurationManager.instance().setBoolean(Constants.PREF_KEY_GUI_TOS_ACCEPTED, true);
        checkLastSeenVersion();
        registerMainBroadcastReceiver();
        syncSlideMenu();

        //uncomment to test social links dialog
        //UIUtils.showSocialLinksDialog(this, true, null, "");

        if (ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_GUI_TOS_ACCEPTED)) {
            checkExternalStoragePermissionsOrBindMusicService();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mainBroadcastReceiver != null) {
            try {
                unregisterReceiver(mainBroadcastReceiver);
            } catch (Throwable ignored) {
                //oh well (the api doesn't provide a way to know if it's been registered before,
                //seems like overkill keeping track of these ourselves.)
            }
        }
    }

    private Map<Integer,DangerousPermissionsChecker> initPermissionsCheckers() {
        Map<Integer, DangerousPermissionsChecker> checkers = new HashMap<>();

        // EXTERNAL STORAGE ACCESS CHECKER.
        final DangerousPermissionsChecker externalStorageChecker =
                new DangerousPermissionsChecker(this, DangerousPermissionsChecker.EXTERNAL_STORAGE_PERMISSIONS_REQUEST_CODE);
        externalStorageChecker.setPermissionsGrantedCallback(new DangerousPermissionsChecker.OnPermissionsGrantedCallback() {
            @Override
            public void onPermissionsGranted() {
                UIUtils.showInformationDialog(MainActivity.this,
                        R.string.restarting_summary,
                        R.string.restarting,
                        false,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                externalStorageChecker.restartFrostWire(1000);
                            }
                        });
            }
        });
        checkers.put(DangerousPermissionsChecker.EXTERNAL_STORAGE_PERMISSIONS_REQUEST_CODE, externalStorageChecker);

        // add more permissions checkers if needed...

        return checkers;
    }

    private void registerMainBroadcastReceiver() {
        mainBroadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                if (Constants.ACTION_NOTIFY_SDCARD_MOUNTED.equals(intent.getAction())) {
                    onNotifySdCardMounted();
                }
            }
        };

        IntentFilter bf = new IntentFilter(Constants.ACTION_NOTIFY_SDCARD_MOUNTED);
        registerReceiver(mainBroadcastReceiver, bf);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (outState != null) {
            super.onSaveInstanceState(outState);
            saveLastFragment(outState);
            saveFragmentsStack(outState);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_GUI_TOS_ACCEPTED)) {
            // we are still in the wizard.
            return;
        }

        if (isShutdown()) {
            return;
        }

        checkExternalStoragePermissionsOrBindMusicService();
    }

    private void checkExternalStoragePermissionsOrBindMusicService() {
        DangerousPermissionsChecker checker = permissionsCheckers.get(DangerousPermissionsChecker.EXTERNAL_STORAGE_PERMISSIONS_REQUEST_CODE);
        if (!externalStoragePermissionsRequested && checker!=null && checker.noAccess()) {
            checker.requestPermissions();
            externalStoragePermissionsRequested = true;
        }
    }

    private void onNotifySdCardMounted() {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (playerSubscription != null) {
            playerSubscription.unsubscribe();
        }

        //avoid memory leaks when the device is tilted and the menu gets recreated.
    }

    private void saveLastFragment(Bundle outState) {
        Fragment fragment = getCurrentFragment();
        if (fragment != null) {
            getFragmentManager().putFragment(outState, CURRENT_FRAGMENT_KEY, fragment);
        }
    }

    private void mainResume() {
        syncSlideMenu();
        if (firstTime) {
            firstTime = false;
            Engine.instance().startServices(); // it's necessary for the first time after wizard
        }
    }

    private void checkLastSeenVersion() {
        final String lastSeenVersion = ConfigurationManager.instance().getString(Constants.PREF_KEY_CORE_LAST_SEEN_VERSION);
        if (StringUtils.isNullOrEmpty(lastSeenVersion)) {
            //fresh install
            ConfigurationManager.instance().setString(Constants.PREF_KEY_CORE_LAST_SEEN_VERSION, Constants.FROSTWIRE_VERSION_STRING);
            UXStats.instance().log(UXAction.CONFIGURATION_WIZARD_FIRST_TIME);
        } else if (!Constants.FROSTWIRE_VERSION_STRING.equals(lastSeenVersion)) {
            //just updated.
            ConfigurationManager.instance().setString(Constants.PREF_KEY_CORE_LAST_SEEN_VERSION, Constants.FROSTWIRE_VERSION_STRING);
            UXStats.instance().log(UXAction.CONFIGURATION_WIZARD_AFTER_UPDATE);
        }
    }

    private void toggleDrawer() {
        if (drawerLayout.isDrawerOpen(leftDrawer)) {
            drawerLayout.closeDrawer(leftDrawer);
        } else {
            drawerLayout.openDrawer(leftDrawer);
            syncSlideMenu();
        }

        updateHeader(getCurrentFragment());
    }

    private void showLastBackDialog() {
        YesNoDialog dlg = YesNoDialog.newInstance(LAST_BACK_DIALOG_ID, R.string.minimize_frostwire, R.string.are_you_sure_you_wanna_leave);
        dlg.show(getFragmentManager()); //see onDialogClick
    }

    private void showShutdownDialog() {
        YesNoDialog dlg = YesNoDialog.newInstance(SHUTDOWN_DIALOG_ID, R.string.app_shutdown_dlg_title, R.string.app_shutdown_dlg_message);
        dlg.show(getFragmentManager()); //see onDialogClick
    }

    public void onDialogClick(String tag, int which) {
        if (tag.equals(LAST_BACK_DIALOG_ID) && which == AbstractDialog.BUTTON_POSITIVE) {
            onLastDialogButtonPositive();
        } else if (tag.equals(SHUTDOWN_DIALOG_ID) && which == AbstractDialog.BUTTON_POSITIVE) {
            onShutdownDialogButtonPositive();
        }
    }

    private void onLastDialogButtonPositive() {
        Offers.showInterstitial(this, false, true);
    }

    private void onShutdownDialogButtonPositive() {
        Offers.showInterstitial(this, true, false);
    }

    private void syncSlideMenu() {
        listMenu.clearChoices();
        invalidateOptionsMenu();

        Fragment fragment = getCurrentFragment();
        int menuId=R.id.menu_main_library;
        if (fragment instanceof BrowsePeerFragment) {
            menuId=R.id.menu_main_library;
        } else if (fragment instanceof TransfersFragment) {
            menuId=R.id.menu_main_transfers;
        }
        setCheckedItem(menuId);
        updateHeader(getCurrentFragment());
    }

    private void setCheckedItem(int id) {
        try {
            listMenu.clearChoices();
            ((MainMenuAdapter) listMenu.getAdapter()).notifyDataSetChanged();

            int position = 0;
            MainMenuAdapter adapter = (MainMenuAdapter) listMenu.getAdapter();
            for (int i = 0; i < adapter.getCount(); i++) {
                listMenu.setItemChecked(i, false);
                if (adapter.getItemId(i) == id) {
                    position = i;
                    break;
                }
            }

            if (id != -1) {
                listMenu.setItemChecked(position, true);
            }

            invalidateOptionsMenu();

            if (drawerToggle != null) {
                drawerToggle.syncState();
            }
        } catch (Throwable e) { // protecting from weird android UI engine issues
            LOG.warn("Error setting slide menu item selected", e);
        }
    }

    private void refreshPlayerItem() {
    }

    private void setupMenuItems() {
        listMenu.setAdapter(new MainMenuAdapter(this));
        listMenu.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        listMenu.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                syncSlideMenu();
                controller.closeSlideMenu();
                try {
                    if (id == R.id.menu_main_settings) {
                        controller.showPreferences();
                    } else if (id == R.id.menu_main_shutdown) {
                        showShutdownDialog();
                    }
                    else {
                        listMenu.setItemChecked(position, true);
                        controller.switchFragment((int) id);
                    }
                } catch (Throwable e) { // protecting from weird android UI engine issues
                    LOG.error("Error clicking slide menu item", e);
                }
            }
        });
    }

    private void setupFragments() {
        library = (BrowsePeerFragment) getFragmentManager().findFragmentById(R.id.activity_main_fragment_browse_peer);
        transfers = (TransfersFragment) getFragmentManager().findFragmentById(R.id.activity_main_fragment_transfers);

        hideFragments(getFragmentManager().beginTransaction()).commit();
    }

    private FragmentTransaction hideFragments(FragmentTransaction ts) {
        return ts.hide(library).hide(transfers);
    }

    private void setupInitialFragment(Bundle savedInstanceState) {
        Fragment fragment = null;

        if (savedInstanceState != null) {
            fragment = getFragmentManager().getFragment(savedInstanceState, CURRENT_FRAGMENT_KEY);
            restoreFragmentsStack(savedInstanceState);
        }
        if (fragment == null) {
            fragment = library;
            setCheckedItem(R.id.menu_main_library);
        }

        switchContent(fragment);
    }

    private void saveFragmentsStack(Bundle outState) {
        int[] stack = new int[fragmentsStack.size()];
        for (int i = 0; i < stack.length; i++) {
            stack[i] = fragmentsStack.get(i);
        }
        outState.putIntArray(FRAGMENTS_STACK_KEY, stack);
    }

    private void restoreFragmentsStack(Bundle savedInstanceState) {
        try {
            int[] stack = savedInstanceState.getIntArray(FRAGMENTS_STACK_KEY);
            for (int id : stack) {
                fragmentsStack.push(id);
            }
        } catch (Throwable ignored) {
        }
    }

    private void updateHeader(Fragment fragment) {
        try {
            RelativeLayout placeholder = (RelativeLayout) getActionBar().getCustomView();
            if (placeholder != null && placeholder.getChildCount() > 0) {
                placeholder.removeAllViews();
            }

            if (fragment instanceof MainFragment) {
                View header = ((MainFragment) fragment).getHeader(this);
                if (placeholder != null && header != null) {
                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                    params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
                    placeholder.addView(header, params);
                }
            }
        } catch (Throwable e) {
            LOG.error("Error updating main header", e);
        }
    }

    private void switchContent(Fragment fragment, boolean addToStack) {
        hideFragments(getFragmentManager().beginTransaction()).show(fragment).commitAllowingStateLoss();
        if (addToStack && (fragmentsStack.isEmpty() || fragmentsStack.peek() != fragment.getId())) {
            fragmentsStack.push(fragment.getId());
        }
        currentFragment = fragment;
        updateHeader(fragment);
    }

    /*
     * The following methods are only public to be able to use them from another package(internal).
     */

    public Fragment getFragmentByMenuId(int id) {
        switch (id) {

        case R.id.menu_main_library:
            return library;
        case R.id.menu_main_transfers:
            return transfers;
        default:
            return null;
        }
    }

    public void switchContent(Fragment fragment) {
        switchContent(fragment, true);
    }

    public Fragment getCurrentFragment() {
        return currentFragment;
    }

    public void closeSlideMenu() {
        drawerLayout.closeDrawer(leftDrawer);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    private void setupActionBar() {
        ActionBar bar = getActionBar();
        if (bar != null) {
            bar.setCustomView(R.layout.view_custom_actionbar);
            bar.setDisplayShowCustomEnabled(true);
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setHomeButtonEnabled(true);
        }
    }

    private void setupDrawer() {
        drawerToggle = new MenuDrawerToggle(this, drawerLayout);
        drawerLayout.setDrawerListener(drawerToggle);
    }

    public void onServiceConnected(final ComponentName name, final IBinder service) {
//        mService = IApolloService.Stub.asInterface(service);
    }

    /**
     * {@inheritDoc}
     */
    public void onServiceDisconnected(final ComponentName name) {

    }

    public DangerousPermissionsChecker getPermissionsChecker(int requestCode) {
        return permissionsCheckers.get(requestCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        DangerousPermissionsChecker checker = permissionsCheckers.get(requestCode);
        if (checker != null) {
            checker.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private static final class MenuDrawerToggle extends ActionBarDrawerToggle {
        private final WeakReference<MainActivity> activityRef;

        public MenuDrawerToggle(MainActivity activity, DrawerLayout drawerLayout) {
            super(activity, drawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close);

            // aldenml: even if the parent class holds a strong reference, I decided to keep a weak one
            this.activityRef = Ref.weak(activity);
        }

        @Override
        public void onDrawerClosed(View view) {
            if (Ref.alive(activityRef)) {
                activityRef.get().invalidateOptionsMenu();
                activityRef.get().syncSlideMenu();
            }
        }

        @Override
        public void onDrawerOpened(View drawerView) {
            if (Ref.alive(activityRef)) {
                UIUtils.hideKeyboardFromActivity(activityRef.get());
                activityRef.get().invalidateOptionsMenu();
                activityRef.get().syncSlideMenu();
            }
        }

        @Override
        public void onDrawerStateChanged(int newState) {
            if (Ref.alive(activityRef)) {
                MainActivity activity = activityRef.get();
                activity.refreshPlayerItem();
                activity.syncSlideMenu();
            }
        }
    }

    public void performYTSearch(String ytUrl) {
//        SearchFragment searchFragment = (SearchFragment) getFragmentByMenuId(R.id.menu_main_search);
//        searchFragment.performYTSearch(ytUrl);
//        switchContent(searchFragment);
    }
}
