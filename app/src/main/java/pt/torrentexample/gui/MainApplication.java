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

package pt.torrentexample.gui;

import android.app.Application;
import android.text.TextUtils;
import android.view.ViewConfiguration;


import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.frostwire.jlibtorrent.DHT;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.StandardExceptionParser;
import com.google.android.gms.analytics.Tracker;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

import frostwire.bittorrent.BTContext;
import frostwire.bittorrent.BTEngine;
import frostwire.logging.Logger;
import frostwire.search.CrawlPagedWebSearchPerformer;
import frostwire.util.DirectoryUtils;
import pt.torrentexample.AnalyticsTrackers;
import pt.torrentexample.R;
import pt.torrentexample.core.ConfigurationManager;
import pt.torrentexample.core.Constants;
import pt.torrentexample.core.SystemPaths;
import pt.torrentexample.gui.Util.HttpResponseCache;
import pt.torrentexample.gui.Util.ImageLoader;
import pt.torrentexample.gui.services.Engine;

/**
 * @author gubatron
 * @author aldenml
 */
public class MainApplication extends Application {

    private static final Logger LOG = Logger.getLogger(MainApplication.class);
    public static final String TAG = MainApplication.class.getSimpleName();
    private RequestQueue mRequestQueue;

    private static MainApplication mInstance;
    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        AnalyticsTrackers.initialize(this);
        AnalyticsTrackers.getInstance().get(AnalyticsTrackers.Target.APP);
        try {
            ignoreHardwareMenu();
            installHttpCache();
            ConfigurationManager.create(this);
            setupBTEngine();
            NetworkManager.create(this);
            Librarian.create(this);
            Engine.create(this);
            ImageLoader.getInstance(this);
            CrawlPagedWebSearchPerformer.setMagnetDownloader(null); // this effectively turn off magnet downloads
            LocalSearchEngine.create();
            cleanTemp();
        } catch (Throwable e) {
            throw new RuntimeException("Unable to initialized main components", e);
        }
    }

    @Override
    public void onLowMemory() {
//        ImageCache.getInstance(this).evictAll();
//        ImageLoader.getInstance(this).clear();
        super.onLowMemory();
    }

    private void ignoreHardwareMenu() {
        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field f = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if (f != null) {
                f.setAccessible(true);
                f.setBoolean(config, false);
            }
        } catch (Throwable e) {
            // ignore
        }
    }

    private void installHttpCache() {
        try {
            HttpResponseCache.install(this);
        } catch (IOException e) {
            LOG.error("Unable to install global http cache", e);
        }
    }

    private void setupBTEngine() {
        BTEngine.ctx = new BTContext();
        BTEngine.getInstance().reloadBTContext(SystemPaths.getTorrents(),
                SystemPaths.getTorrentData(),
                SystemPaths.getLibTorrent(this),
                0,0,"0.0.0.0",false,false);
        BTEngine.ctx.optimizeMemory = true;
        BTEngine.getInstance().start();

        boolean enable_dht = ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_NETWORK_ENABLE_DHT);
        DHT dht = new DHT(BTEngine.getInstance().getSession());
        if (!enable_dht) {
            dht.stop();
        } else {
            // just make sure it's started otherwise.
            // (we could be coming back from a crash on an unstable state)
            dht.start();
        }
    }

    private void cleanTemp() {
        try {
            File tmp = SystemPaths.getTemp();
            DirectoryUtils.deleteFolderRecursively(tmp);

            if (tmp.mkdirs()) {
                new File(tmp, ".nomedia").createNewFile();
            }
        } catch (Throwable e) {
            LOG.error("Error during setup of temp directory", e);
        }
    }

//analytics
    /**
     * Gets the default {@link Tracker} for this {@link Application}.
     * @return tracker
     */
    public static synchronized MainApplication getInstance() {
        return mInstance;
    }

    public synchronized Tracker getGoogleAnalyticsTracker() {
        AnalyticsTrackers analyticsTrackers = AnalyticsTrackers.getInstance();
        return analyticsTrackers.get(AnalyticsTrackers.Target.APP);
    }

    /***
     * Tracking screen view
     *
     * @param screenName screen name to be displayed on GA dashboard
     */
    public void trackScreenView(String screenName) {
        Tracker t = getGoogleAnalyticsTracker();

        // Set screen name.
        t.setScreenName(screenName);

        // Send a screen view.
        t.send(new HitBuilders.ScreenViewBuilder().build());

        GoogleAnalytics.getInstance(this).dispatchLocalHits();
    }

    /***
     * Tracking exception
     *
     * @param e exception to be tracked
     */
    public void trackException(Exception e) {
        if (e != null) {
            Tracker t = getGoogleAnalyticsTracker();

            t.send(new HitBuilders.ExceptionBuilder()
                            .setDescription(
                                    new StandardExceptionParser(this, null)
                                            .getDescription(Thread.currentThread().getName(), e))
                            .setFatal(false)
                            .build()
            );
        }
    }

    /***
     * Tracking event
     *
     * @param category event category
     * @param action   action of the event
     * @param label    label
     */
    public void trackEvent(String category, String action, String label) {
        Tracker t = getGoogleAnalyticsTracker();

        // Build and send an Event.
        t.send(new HitBuilders.EventBuilder().setCategory(category).setAction(action).setLabel(label).build());
    }

    //voley config
    public RequestQueue getRequestQueue(){
        if (mRequestQueue == null){
            mRequestQueue = Volley.newRequestQueue(getApplicationContext());
        }

        return mRequestQueue;
    }


    public <T> void addToRequestQueue(Request<T> req, String tag){
        req.setTag(TextUtils.isEmpty(tag) ? TAG : tag);
        getRequestQueue().add(req);
    }

    public <T> void addToRequestQueue(Request<T> req){
        req.setTag(TAG);
        getRequestQueue().add(req);
    }

    public void cancelPendingRequests(Object tag){
        if (mRequestQueue == null){
            mRequestQueue.cancelAll(tag);
        }
    }
}
