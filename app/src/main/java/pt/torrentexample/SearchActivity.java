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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ProgressBar;


import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import frostwire.logging.Logger;
import frostwire.search.FileSearchResult;
import frostwire.search.HttpSearchResult;
import frostwire.search.SearchManagerSignal;
import frostwire.search.SearchResult;
import frostwire.search.torrent.TorrentCrawledSearchResult;
import frostwire.search.torrent.TorrentSearchResult;
import frostwire.util.HttpClientFactory;
import frostwire.util.JsonUtils;
import frostwire.util.Ref;
import frostwire.util.http.HttpClient;
import frostwire.uxstats.UXAction;
import frostwire.uxstats.UXStats;
import pt.torrentexample.core.ConfigurationManager;
import pt.torrentexample.core.Constants;
import pt.torrentexample.gui.LocalSearchEngine;
import pt.torrentexample.gui.Util.Slide;
import pt.torrentexample.gui.Util.SlideList;
import pt.torrentexample.gui.Util.TorrentPromotionSearchResult;
import pt.torrentexample.gui.Util.UIUtils;
import pt.torrentexample.gui.adapter.SearchResultListAdapter;
import pt.torrentexample.gui.adnetwork.Offers;
import pt.torrentexample.gui.dialog.NewTransferDialog;
import pt.torrentexample.gui.services.Engine;
import pt.torrentexample.gui.task.StartDownloadTask;
import pt.torrentexample.gui.transfers.HttpSlideSearchResult;
import pt.torrentexample.gui.transfers.TransferManager;
import pt.torrentexample.gui.view.AbstractDialog;
import pt.torrentexample.gui.view.ClickAdapter;
import pt.torrentexample.gui.view.PromotionsView;
import pt.torrentexample.gui.view.RichNotification;
import pt.torrentexample.gui.view.RichNotificationActionLink;
import pt.torrentexample.gui.view.SearchInputView;
import pt.torrentexample.gui.view.SearchProgressView;
import rx.Observer;
import rx.Subscription;

/**
 * @author gubatron
 * @author aldenml
 */
public final class SearchActivity extends Activity implements AbstractDialog.OnDialogClickListener, SearchProgressView.CurrentQueryReporter {
    private static final Logger LOG = Logger.getLogger(SearchActivity.class);

    private static int startedTransfers = 0;
    private static long lastInterstitialShownTimestamp = -1;

    private SearchResultListAdapter adapter;
    private List<Slide> slides;

    private SearchInputView searchInput;
    private ProgressBar deepSearchProgress;
    private PromotionsView promotions;
    private SearchProgressView searchProgress;
    private pt.torrentexample.gui.view.ListView list;

    private String currentQuery;

    private FileTypeCounter fileTypeCounter;
    private Subscription localSearchSubscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_search);
        fileTypeCounter = new FileTypeCounter();
        currentQuery = null;
        initComponents();
        setupAdapter();

        if (slides != null) {
            promotions.setSlides(slides);
        } else {
            new LoadSlidesTask(this).execute();
        }
    }


    @Override
    public void onResume() {
        super.onResume();

        if (adapter != null && (adapter.getCount() > 0 || adapter.getTotalCount() > 0)) {
            refreshFileTypeCounters(true);
        }
    }

    @Override
    public void onDestroy() {
        if (this.localSearchSubscription != null) {
            try {
                this.localSearchSubscription.unsubscribe();
            } catch (Throwable ignored) {

            }
        }
        super.onDestroy();
    }

    protected void initComponents() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Tìm kiếm");
        toolbar.setTitleTextColor(Color.WHITE);
        searchInput = (SearchInputView) findViewById(R.id.fragment_search_input);
        searchInput.setShowKeyboardOnPaste(true);
        searchInput.setOnSearchListener(new SearchInputView.OnSearchListener() {
            public void onSearch(View v, String query, int mediaTypeId) {
                if (query.contains("://m.soundcloud.com/") || query.contains("://soundcloud.com/")) {
//                    cancelSearch();
//                    new DownloadSoundcloudFromUrlTask(getActivity(), query).execute();
//                    searchInput.setText("");
                } else if (query.contains("youtube.com/")) {
                    performYTSearch(query);
                } else if (query.startsWith("magnet:?xt=urn:btih:")) {
                    startMagnetDownload(query);
                    currentQuery = null;
                    searchInput.setText("");
                } else {
                    performSearch(query, mediaTypeId);
                }
            }

            public void onMediaTypeSelected(View v, int mediaTypeId) {
                adapter.setFileType(mediaTypeId);
                showSearchView(getWindow().getDecorView().getRootView());
            }

            public void onClear(View v) {
                cancelSearch();
            }
        });

        deepSearchProgress = (ProgressBar) findViewById(R.id.fragment_search_deepsearch_progress);
        deepSearchProgress.setVisibility(View.GONE);

        promotions = (PromotionsView) findViewById(R.id.fragment_search_promos);
        promotions.setOnPromotionClickListener(new PromotionsView.OnPromotionClickListener() {
            @Override
            public void onPromotionClick(PromotionsView v, Slide slide) {
                startPromotionDownload(slide);
            }
        });


        searchProgress = (SearchProgressView) findViewById(R.id.fragment_search_search_progress);
        searchProgress.setCurrentQueryReporter(this);

        searchProgress.setCancelOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (LocalSearchEngine.instance().isSearchFinished()) {
                    performSearch(searchInput.getText(), adapter.getFileType()); // retry
                } else {
                    cancelSearch();
                }
            }
        });

        list = (pt.torrentexample.gui.view.ListView) findViewById(R.id.fragment_search_list);

        showSearchView(getWindow().getDecorView().getRootView());
        showRatingsReminder(getWindow().getDecorView().getRootView());
    }

    private void startMagnetDownload(String magnet) {
        //go!
        TransferManager.instance().downloadTorrent(magnet);

        //Show me the transfer tab
        Intent i = new Intent(this, TorrentDownloadActivity.class);
        i.setAction(Constants.ACTION_SHOW_TRANSFERS);
        i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(i);
    }

    private static String extractYTId(String ytUrl) {
        String vId = null;
        Pattern pattern = Pattern.compile(".*(?:youtu.be\\/|v\\/|u\\/\\w\\/|embed\\/|watch\\?v=)([^#\\&\\?]*).*");
        Matcher matcher = pattern.matcher(ytUrl);
        if (matcher.matches()) {
            vId = matcher.group(1);
        }
        return vId;
    }

    private void setupAdapter() {
        if (adapter == null) {
            adapter = new SearchResultListAdapter(this) {
                @Override
                protected void searchResultClicked(SearchResult sr) {
                    startTransfer(sr, getString(R.string.download_added_to_queue));
                }
            };

            this.localSearchSubscription = LocalSearchEngine.instance().observable().subscribe(new Observer<SearchManagerSignal>() {
                @Override
                public void onCompleted() {
                    // IMPORTANT: This method is never called by the PublishSubject (aka "Publisher"), as the publisher
                    // is reused by the observers and we don't want it to end. Instead the publisher sends SearchManagerSignals
                    // and when the signal is a SearchManagerSignal.End, it's captured by the onNext() method below, which
                    // will then invoke this one.
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            searchProgress.setProgressEnabled(false);
                            deepSearchProgress.setVisibility(View.GONE);
                        }
                    });
                }

                @Override
                public void onError(Throwable e) {
                    LOG.error("Some error in the rx stream", e);
                }

                @Override
                public void onNext(final SearchManagerSignal signal) {
                    if (signal instanceof SearchManagerSignal.Results){
                        @SuppressWarnings("unchecked")
                        final SearchManagerSignal.Results resultsSignal = (SearchManagerSignal.Results) signal;
                        SearchResultListAdapter.FilteredSearchResults fsr = adapter.filter((List<SearchResult>) resultsSignal.elements);
                        final List<SearchResult> filteredList = fsr.filtered;

                        fileTypeCounter.add(fsr);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                adapter.addResults(resultsSignal.elements, filteredList);
                                showSearchView(getWindow().getDecorView().getRootView());
                                refreshFileTypeCounters(true);
                            }
                        });
                    } else if (signal instanceof SearchManagerSignal.End) {
                        onCompleted();
                    }
                }
            });
        }

        list.setAdapter(adapter);
    }

    private void refreshFileTypeCounters(boolean fileTypeCountersVisible) {
//        searchInput.updateFileTypeCounter(Constants.FILE_TYPE_APPLICATIONS, fileTypeCounter.fsr.numApplications);
//        searchInput.updateFileTypeCounter(Constants.FILE_TYPE_AUDIO, fileTypeCounter.fsr.numAudio);
//        searchInput.updateFileTypeCounter(Constants.FILE_TYPE_DOCUMENTS, fileTypeCounter.fsr.numDocuments);
//        searchInput.updateFileTypeCounter(Constants.FILE_TYPE_PICTURES, fileTypeCounter.fsr.numPictures);
//        searchInput.updateFileTypeCounter(Constants.FILE_TYPE_TORRENTS, fileTypeCounter.fsr.numTorrents);
//        searchInput.updateFileTypeCounter(Constants.FILE_TYPE_VIDEOS, fileTypeCounter.fsr.numVideo);

//        searchInput.setFileTypeCountersVisible(fileTypeCountersVisible);
    }

    public void performYTSearch(String query) {
        String ytId = extractYTId(query);
        if (ytId != null) {
            searchInput.setText("");
            searchInput.performClickOnRadioButton(Constants.FILE_TYPE_VIDEOS);
            performSearch(ytId, Constants.FILE_TYPE_VIDEOS);
            searchInput.setHint(getResources().getString(R.string.searching_for) + " youtube:" + ytId);
        }
    }

    private void performSearch(String query, int mediaTypeId) {
        adapter.clear();
        adapter.setFileType(mediaTypeId);
        fileTypeCounter.clear();
        refreshFileTypeCounters(false);
        currentQuery = query;
        LocalSearchEngine.instance().performSearch(query);
        searchProgress.setProgressEnabled(true);
        showSearchView(getWindow().getDecorView().getRootView());
        UXStats.instance().log(UXAction.SEARCH_STARTED_ENTER_KEY);
    }

    private void cancelSearch() {
        adapter.clear();
        fileTypeCounter.clear();
        refreshFileTypeCounters(false);
        currentQuery = null;
        LocalSearchEngine.instance().cancelSearch();
        searchProgress.setProgressEnabled(false);
        showSearchView(getWindow().getDecorView().getRootView());
    }

    private void showSearchView(View view) {
        if (LocalSearchEngine.instance().isSearchStopped()) {
            switchView(view, R.id.fragment_search_promos);
            deepSearchProgress.setVisibility(View.GONE);
        } else {
            if (adapter != null && adapter.getCount() > 0) {
                switchView(view, R.id.fragment_search_list);
                deepSearchProgress.setVisibility(LocalSearchEngine.instance().isSearchFinished() ? View.GONE : View.VISIBLE);
            } else {
                switchView(view, R.id.fragment_search_search_progress);
                deepSearchProgress.setVisibility(View.GONE);
            }
        }

        boolean searchFinished = LocalSearchEngine.instance().isSearchFinished();
        searchProgress.setProgressEnabled(!searchFinished);
    }

    private void switchView(View v, int id) {
        if (v != null) {
            FrameLayout frameLayout = (FrameLayout) findViewById(R.id.fragment_search_framelayout);

            int childCount = frameLayout.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View childAt = frameLayout.getChildAt(i);
                childAt.setVisibility((childAt.getId() == id) ? View.VISIBLE : View.INVISIBLE);
            }
        }
    }

    @Override
    public void onDialogClick(String tag, int which) {
        if (tag.equals(NewTransferDialog.TAG) && which == AbstractDialog.BUTTON_POSITIVE) {
            if (Ref.alive(NewTransferDialog.srRef)) {
                startDownload(this, NewTransferDialog.srRef.get(), getString(R.string.download_added_to_queue));
                LocalSearchEngine.instance().markOpened(NewTransferDialog.srRef.get(), adapter);
            }
        }
    }

    private void startTransfer(final SearchResult sr, final String toastMessage) {
        if (ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_GUI_SHOW_NEW_TRANSFER_DIALOG)) {
            if (sr instanceof FileSearchResult) {
                try {
                    NewTransferDialog dlg = NewTransferDialog.newInstance((FileSearchResult) sr, false);
                    dlg.show(getFragmentManager());
                } catch (IllegalStateException e) {
                    // android.app.FragmentManagerImpl.checkStateLoss:1323 -> java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState
                    // just start the download then if the dialog crapped out.
                    onDialogClick(NewTransferDialog.TAG, AbstractDialog.BUTTON_POSITIVE);
                }
            }
        } else {

                startDownload(this, sr, toastMessage);

        }
        uxLogAction(sr);
    }

    public static void startDownload(Context ctx, SearchResult sr, String message) {
        StartDownloadTask task = new StartDownloadTask(ctx, sr, message);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            task.execute();
        }
        UIUtils.showTransfersOnDownloadStart(ctx);

        if (ctx instanceof Activity) {
            showInterstitialOfferIfNecessary((Activity) ctx);
        }
    }

    public static void showInterstitialOfferIfNecessary(Activity ctx) {
        startedTransfers++;
        ConfigurationManager CM = ConfigurationManager.instance();
        final int INTERSTITIAL_OFFERS_TRANSFER_STARTS = CM.getInt(Constants.PREF_KEY_GUI_INTERSTITIAL_OFFERS_TRANSFER_STARTS);
        final int INTERSTITIAL_TRANSFER_OFFERS_TIMEOUT_IN_MINUTES = CM.getInt(Constants.PREF_KEY_GUI_INTERSTITIAL_TRANSFER_OFFERS_TIMEOUT_IN_MINUTES);
        final long INTERSTITIAL_TRANSFER_OFFERS_TIMEOUT_IN_MS = TimeUnit.MINUTES.toMillis(INTERSTITIAL_TRANSFER_OFFERS_TIMEOUT_IN_MINUTES);

        long timeSinceLastOffer = System.currentTimeMillis() - lastInterstitialShownTimestamp;
        boolean itsBeenLongEnough = timeSinceLastOffer >= INTERSTITIAL_TRANSFER_OFFERS_TIMEOUT_IN_MS;
        boolean startedEnoughTransfers = startedTransfers >= INTERSTITIAL_OFFERS_TRANSFER_STARTS;
        boolean shouldDisplayFirstOne = (lastInterstitialShownTimestamp == -1 && startedEnoughTransfers);

        if (shouldDisplayFirstOne || (itsBeenLongEnough && startedEnoughTransfers)) {
            Offers.showInterstitial(ctx, false, false);
            startedTransfers = 0;
            lastInterstitialShownTimestamp = System.currentTimeMillis();
        }
    }

    private void showRatingsReminder(View v) {
        final RichNotification ratingReminder = (RichNotification) findViewById(R.id.fragment_search_rating_reminder_notification);
        ratingReminder.setVisibility(View.GONE);
        final ConfigurationManager CM = ConfigurationManager.instance();
        boolean alreadyRated = CM.getBoolean(Constants.PREF_KEY_GUI_ALREADY_RATED_US_IN_MARKET);

        if (alreadyRated || ratingReminder.wasDismissed()) {
            return;
        }

        final int finishedDownloads = Engine.instance().getNotifiedDownloadsBloomFilter().count();
        final int intervalFactor = Constants.IS_GOOGLE_PLAY_DISTRIBUTION ? 4 : 1;
        final int REMINDER_INTERVAL = intervalFactor * CM.getInt(Constants.PREF_KEY_GUI_FINISHED_DOWNLOADS_BETWEEN_RATINGS_REMINDER);

        //LOG.info("successful finishedDownloads: " + finishedDownloads);

        if (finishedDownloads < REMINDER_INTERVAL) {
            return;
        }

        ClickAdapter<SearchActivity> onRateAdapter = createOnRateClickAdapter(ratingReminder, CM);
        ratingReminder.setOnClickListener(onRateAdapter);

        RichNotificationActionLink rateFrostWireActionLink =
                new RichNotificationActionLink(ratingReminder.getContext(),
                        getString(R.string.love_frostwire),
                        onRateAdapter);

        RichNotificationActionLink sendFeedbackActionLink =
                new RichNotificationActionLink(ratingReminder.getContext(),
                        getString(R.string.send_feedback),
                        createOnFeedbackClickAdapter(ratingReminder, CM));

        ratingReminder.updateActionLinks(rateFrostWireActionLink, sendFeedbackActionLink);
        ratingReminder.setVisibility(View.VISIBLE);
    }

    // takes user to Google Play store so it can rate the app.
    private ClickAdapter<SearchActivity> createOnRateClickAdapter(final RichNotification ratingReminder, final ConfigurationManager CM) {
        return new OnRateClickAdapter(SearchActivity.this, ratingReminder, CM);
    }

    // opens default email client and pre-fills email to support@frostwire.com
    // with some information about the app and environment.
    private ClickAdapter<SearchActivity> createOnFeedbackClickAdapter(final RichNotification ratingReminder, final ConfigurationManager CM) {
        return new OnFeedbackClickAdapter(SearchActivity.this, ratingReminder, CM);
    }

    private void startPromotionDownload(Slide slide) {
        SearchResult sr;

        switch (slide.method) {
            case Slide.DOWNLOAD_METHOD_TORRENT:
                sr = new TorrentPromotionSearchResult(slide);
                break;
            case Slide.DOWNLOAD_METHOD_HTTP:
                sr = new HttpSlideSearchResult(slide);
                break;
            default:
                sr = null;
                break;
        }
        if (sr == null) {

            //check if there is a URL available to open a web browser.
            if (slide.url != null) {
                Intent i = new Intent("android.intent.action.VIEW", Uri.parse(slide.url));
                try {
                    startActivity(i);
                } catch (Throwable t) {
                    // some devices incredibly may have no apps to handle this intent.
                }
            }

            return;
        }

        String stringDownloadingPromo;

        try {
            stringDownloadingPromo = getString(R.string.downloading_promotion, sr.getDisplayName());
        } catch (Throwable e) {
            stringDownloadingPromo = getString(R.string.azureus_manager_item_downloading);
        }

        startTransfer(sr, stringDownloadingPromo);
    }

    private void uxLogAction(SearchResult sr) {
        UXStats.instance().log(UXAction.SEARCH_RESULT_CLICKED);

        if (sr instanceof HttpSearchResult) {
            UXStats.instance().log(UXAction.DOWNLOAD_CLOUD_FILE);
        } else if (sr instanceof TorrentSearchResult) {
            if (sr instanceof TorrentCrawledSearchResult) {
                UXStats.instance().log(UXAction.DOWNLOAD_PARTIAL_TORRENT_FILE);
            } else {
                UXStats.instance().log(UXAction.DOWNLOAD_FULL_TORRENT_FILE);
            }
        }
    }

    @Override
    public String getCurrentQuery() {
        return currentQuery;
    }

    private static class LoadSlidesTask extends AsyncTask<Void, Void, List<Slide>> {

        private final WeakReference<SearchActivity> fragment;

        public LoadSlidesTask(SearchActivity fragment) {
            this.fragment = new WeakReference<SearchActivity>(fragment);
        }

        @Override
        protected List<Slide> doInBackground(Void... params) {
            try {
                HttpClient http = HttpClientFactory.getInstance(HttpClientFactory.HttpContext.SEARCH);
                String url = String.format("%s?from=android&fw=%s&sdk=%s", Constants.SERVER_PROMOTIONS_URL, Constants.FROSTWIRE_VERSION_STRING, Build.VERSION.SDK_INT);
                String json = http.get(url);
                SlideList slides = JsonUtils.toObject(json, SlideList.class);
                // yes, these requests are done only once per session.
                //LOG.info("SearchActivity.LoadSlidesTask performed http request to " + url);
                slides.slides.remove(0);
                return slides.slides;
            } catch (Throwable e) {
                LOG.error("Error loading slides from url", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<Slide> result) {
            SearchActivity f = fragment.get();
            if (f != null && result != null && !result.isEmpty()) {
                f.slides = result;
                f.promotions.setSlides(result);
            }
        }
    }

    private static final class FileTypeCounter {

        private final SearchResultListAdapter.FilteredSearchResults fsr = new SearchResultListAdapter.FilteredSearchResults();

        public void add(SearchResultListAdapter.FilteredSearchResults fsr) {
            this.fsr.numAudio += fsr.numAudio;
            this.fsr.numApplications += fsr.numApplications;
            this.fsr.numDocuments += fsr.numDocuments;
            this.fsr.numPictures += fsr.numPictures;
            this.fsr.numTorrents += fsr.numTorrents;
            this.fsr.numVideo += fsr.numVideo;
        }

        public void clear() {
            this.fsr.numAudio = 0;
            this.fsr.numApplications = 0;
            this.fsr.numDocuments = 0;
            this.fsr.numPictures = 0;
            this.fsr.numTorrents = 0;
            this.fsr.numVideo = 0;
        }


    }

    private static class OnRateClickAdapter extends ClickAdapter<SearchActivity> {
        private final WeakReference<RichNotification> ratingReminderRef;
        private final ConfigurationManager CM;

        public OnRateClickAdapter(final SearchActivity owner, final RichNotification ratingReminder, final ConfigurationManager CM) {
            super(owner);
            ratingReminderRef = Ref.weak(ratingReminder);
            this.CM = CM;
        }

        @Override
        public void onClick(SearchActivity owner, View v) {
            if (Ref.alive(ratingReminderRef)) {
                ratingReminderRef.get().setVisibility(View.GONE);
            }
            CM.setBoolean(Constants.PREF_KEY_GUI_ALREADY_RATED_US_IN_MARKET, true);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("market://details?id=" + Constants.APP_PACKAGE_NAME));
            try {
                owner.startActivity(intent);
            } catch (Throwable ignored) {
            }
        }
    }

    private static class OnFeedbackClickAdapter extends ClickAdapter<SearchActivity> {
        private final WeakReference<RichNotification> ratingReminderRef;
        private final ConfigurationManager CM;

        public OnFeedbackClickAdapter(SearchActivity owner, final RichNotification ratingReminder, final ConfigurationManager CM) {
            super(owner);
            ratingReminderRef = Ref.weak(ratingReminder);
            this.CM = CM;
        }
        @Override
        public void onClick(SearchActivity owner, View v) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"support@frostwire.com"});
            String plusOrBasic = (Constants.IS_GOOGLE_PLAY_DISTRIBUTION) ? "basic" : "plus";
            intent.putExtra(Intent.EXTRA_SUBJECT, String.format("[Feedback - frostwire-android (%s) - v%s b%s]", plusOrBasic, Constants.FROSTWIRE_VERSION_STRING, Constants.FROSTWIRE_BUILD));

            String body = String.format("\n\nAndroid SDK: %d\nAndroid RELEASE: %s (%s)\nManufacturer-Model: %s - %s\nDevice: %s\nBoard: %s\nCPU ABI: %s\nCPU ABI2: %s\n\n",
                    Build.VERSION.SDK_INT,
                    Build.VERSION.RELEASE,
                    Build.VERSION.CODENAME,
                    Build.MANUFACTURER,
                    Build.MODEL,
                    Build.DEVICE,
                    Build.BOARD,
                    Build.CPU_ABI,
                    Build.CPU_ABI2);

            intent.putExtra(Intent.EXTRA_TEXT, body);
            owner.startActivity(Intent.createChooser(intent, owner.getString(R.string.choose_email_app)));

            if (Ref.alive(ratingReminderRef)) {
                ratingReminderRef.get().setVisibility(View.GONE);
            }
            CM.setBoolean(Constants.PREF_KEY_GUI_ALREADY_RATED_US_IN_MARKET, true);
        }
    }
}
