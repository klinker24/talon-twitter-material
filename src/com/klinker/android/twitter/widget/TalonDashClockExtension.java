package com.klinker.android.twitter.widget;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;
import com.klinker.android.twitter.R;
import com.klinker.android.twitter.data.sq_lite.HomeContentProvider;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.ui.MainActivity;
import com.klinker.android.twitter.utils.TweetLinkUtils;
import com.klinker.android.twitter.utils.NotificationUtils;


public class TalonDashClockExtension extends DashClockExtension {

    @Override
    protected void onInitialize(boolean isReconnect) {
        super.onInitialize(isReconnect);

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.klinker.android.twitter.NEW_TWEET");
        registerReceiver(pullReceiver, filter);

        String[] watcher = {"content://" + HomeContentProvider.AUTHORITY};
        this.addWatchContentUris(watcher);
        this.setUpdateWhenScreenOn(true);
    }

    @Override
    protected void onUpdateData(int arg0) {

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        int currentAccount = sharedPrefs.getInt("current_account", 1);

        int[] unreads;
        try {
            unreads = NotificationUtils.getUnreads(this);
        }  catch (Exception e) {
            unreads = new int[] {0, 0, 0};
        }
        int homeTweets = unreads[0];
        int mentionsTweets = unreads[1];
        int dmTweets = unreads[2];

        if (!sharedPrefs.getBoolean("dashclock_timeline", true)) {
            homeTweets = 0;
        }
        if (!sharedPrefs.getBoolean("dashclock_mentions", true)) {
            mentionsTweets = 0;
        }
        if (!sharedPrefs.getBoolean("dashclock_dms", true)) {
            dmTweets = 0;
        }

        if (homeTweets > 0 || mentionsTweets > 0 || dmTweets > 0) {

            Intent intent = new Intent(this, MainActivity.class);

            Bundle b = new Bundle();
            b.putBoolean("dashclock", true);
            intent.putExtras(b);

            publishUpdate(new ExtensionData()
                    .visible(true)
                    .icon(R.drawable.ic_stat_icon)
                    .status(homeTweets + mentionsTweets + dmTweets + "")
                    .expandedTitle(NotificationUtils.getTitle(unreads, this, currentAccount)[0])
                    .expandedBody(TweetLinkUtils.removeColorHtml(NotificationUtils.getLongTextNoHtml(unreads, this, currentAccount), AppSettings.getInstance(this)))
                    .clickIntent(intent));
        } else {
            publishUpdate(new ExtensionData()
                    .visible(false));
        }
    }
}