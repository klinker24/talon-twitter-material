package com.klinker.android.twitter_l.services;
/*
 * Copyright 2014 Luke Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.klinker.android.twitter_l.data.sq_lite.MentionsDataSource;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.NotificationUtils;
import com.klinker.android.twitter_l.utils.Utils;

import java.util.List;

import twitter4j.Paging;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

public class MentionsRefreshService extends IntentService {

    SharedPreferences sharedPrefs;

    public MentionsRefreshService() {
        super("MentionsRefreshService");
    }

    @Override
    public void onHandleIntent(Intent intent) {
        sharedPrefs = AppSettings.getSharedPreferences(this);


        Context context = getApplicationContext();
        AppSettings settings = AppSettings.getInstance(context);

        // if they have mobile data on and don't want to sync over mobile data
        if (Utils.getConnectionStatus(context) && !settings.syncMobile) {
            return;
        }

        try {
            Twitter twitter = Utils.getTwitter(context, settings);

            int currentAccount = sharedPrefs.getInt("current_account", 1);

            MentionsDataSource dataSource = MentionsDataSource.getInstance(context);

            long[] lastId = dataSource.getLastIds(currentAccount);
            Paging paging;
            paging = new Paging(1, 200);
            if (lastId[0] > 0) {
                paging.sinceId(lastId[0]);
            }

            List<twitter4j.Status> statuses = twitter.getMentionsTimeline(paging);

            int inserted = MentionsDataSource.getInstance(context).insertTweets(statuses, currentAccount);

            sharedPrefs.edit().putBoolean("refresh_me", true).commit();
            sharedPrefs.edit().putBoolean("refresh_me_mentions", true).commit();

            if (!intent.getBooleanExtra("no_notify", false) && settings.notifications && settings.mentionsNot && inserted > 0) {
                if (intent.getBooleanExtra("from_launcher", false)) {
                    NotificationUtils.refreshNotification(context, true);
                } else {
                    NotificationUtils.refreshNotification(context);
                }
            }

            if (settings.syncSecondMentions) {
                Intent second = new Intent(context, SecondMentionsRefreshService.class);
                second.putExtra("no_notify", intent.getBooleanExtra("no_notify", false));
                startService(second);
            }

        } catch (TwitterException e) {
            // Error in updating status
            Log.d("Twitter Update Error", e.getMessage());
        }
    }
}