package com.klinker.android.twitter_l.adapters;
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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.data.App;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.ui.profile_viewer.ProfilePager;
import com.klinker.android.twitter_l.utils.SDK11;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import twitter4j.User;
import uk.co.senab.bitmapcache.BitmapLruCache;
import uk.co.senab.bitmapcache.CacheableBitmapDrawable;

public class PeopleArrayAdapter extends ArrayAdapter<User> {

    public Context context;

    public boolean openFirst = false;

    public List<User> users;

    public LayoutInflater inflater;
    public AppSettings settings;

    public int layout;
    public Resources res;
    public BitmapLruCache mCache;

    public Handler mHandler;

    public static class ViewHolder {
        public TextView name;
        public TextView screenName;
        public TextView following;
        public ImageView picture;
        public LinearLayout background;
        public long userId;
    }

    public PeopleArrayAdapter(Context context, ArrayList<User> users, boolean openFirst) {
        super(context, R.layout.tweet);

        this.context = context;
        this.users = users;

        settings = AppSettings.getInstance(context);
        inflater = LayoutInflater.from(context);

        this.openFirst = openFirst;

        setUpLayout();

        mHandler = new Handler();
    }

    public PeopleArrayAdapter(Context context, List<User> users) {
        super(context, R.layout.tweet);

        this.context = context;
        this.users = users;

        settings = AppSettings.getInstance(context);
        inflater = LayoutInflater.from(context);

        setUpLayout();

        mHandler = new Handler();
    }

    public void setUpLayout() {
        layout = R.layout.person;
        mCache = App.getInstance(context).getBitmapCache();
    }

    @Override
    public int getCount() {
        return users.size();
    }

    @Override
    public User getItem(int position) {
        return users.get(position);
    }

    public View newView(ViewGroup viewGroup) {
        View v = null;
        final ViewHolder holder = new ViewHolder();

        v = inflater.inflate(layout, viewGroup, false);

        holder.name = (TextView) v.findViewById(R.id.name);
        holder.screenName = (TextView) v.findViewById(R.id.screen_name);
        holder.background = (LinearLayout) v.findViewById(R.id.background);
        holder.picture = (ImageView) v.findViewById(R.id.profile_pic);
        holder.following = (TextView) v.findViewById(R.id.following);

        v.setTag(holder);
        return v;
    }

    public void setFollowingStatus(ViewHolder holder, Long id) {

    }

    public void bindView(final View view, int position, final User user) {
        final ViewHolder holder = (ViewHolder) view.getTag();

        final long id = user.getId();
        holder.userId = id;

        setFollowingStatus(holder, id);

        holder.name.setText(user.getName());
        holder.screenName.setText("@" + user.getScreenName());

        final String url = user.getBiggerProfileImageURL();

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (holder.userId == id) {
                    loadImage(context, holder, url, mCache, id);
                }
            }
        }, 500);

        holder.background.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent viewProfile = new Intent(context, ProfilePager.class);
                viewProfile.putExtra("name", user.getName());
                viewProfile.putExtra("screenname", user.getScreenName());
                viewProfile.putExtra("proPic", user.getOriginalProfileImageURL());
                //viewProfile.putExtra("tweetid", holder.tweetId);
                viewProfile.putExtra("retweet", false);

                context.startActivity(viewProfile);
            }
        });

        if (openFirst && position == 0) {
            holder.background.performClick();
            ((Activity) context).finish();
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v;
        if (convertView == null) {

            v = newView(parent);

        } else {
            v = convertView;

            final ViewHolder holder = (ViewHolder) v.getTag();

            holder.picture.setImageDrawable(null);
        }

        bindView(v, position, users.get(position));

        return v;
    }

    // used to place images on the timeline
    public static ImageUrlAsyncTask mCurrentTask;

    public void loadImage(Context context, final ViewHolder holder, final String url, BitmapLruCache mCache, final long tweetId) {
        // First check whether there's already a task running, if so cancel it
        /*if (null != mCurrentTask) {
            mCurrentTask.cancel(true);
        }*/

        if (url == null) {
            return;
        }

        BitmapDrawable wrapper = mCache.getFromMemoryCache(url);

        if (null != wrapper && holder.picture.getVisibility() != View.GONE) {
            // The cache has it, so just display it
            holder.picture.setImageDrawable(wrapper);
            Animation fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in);

            holder.picture.startAnimation(fadeInAnimation);
        } else {
            // Memory Cache doesn't have the URL, do threaded request...
            holder.picture.setImageDrawable(null);

            mCurrentTask = new ImageUrlAsyncTask(context, holder, mCache, tweetId);

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    SDK11.executeOnThreadPool(mCurrentTask, url);
                } else {
                    mCurrentTask.execute(url);
                }
            } catch (RejectedExecutionException e) {
                // This shouldn't happen, but might.
            }

        }
    }

    private static class ImageUrlAsyncTask
            extends AsyncTask<String, Void, CacheableBitmapDrawable> {

        private BitmapLruCache mCache;
        private Context context;
        private ViewHolder holder;
        private long id;

        ImageUrlAsyncTask(Context context, ViewHolder holder, BitmapLruCache cache, long tweetId) {
            this.context = context;
            mCache = cache;
            this.holder = holder;
            this.id = tweetId;
        }

        @Override
        protected CacheableBitmapDrawable doInBackground(String... params) {
            try {
                if (holder.userId != id) {
                    return null;
                }
                final String url = params[0];

                // Now we're not on the main thread we can check all caches
                CacheableBitmapDrawable result;

                try {
                    result = mCache.get(url, null);
                } catch (Exception e) {
                    result = null;
                }

                if (null == result) {

                    // The bitmap isn't cached so download from the web
                    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                    InputStream is = new BufferedInputStream(conn.getInputStream());

                    Bitmap b = decodeSampledBitmapFromResourceMemOpt(is, 500, 500);

                    try {
                        is.close();
                    } catch (Exception e) {

                    }
                    try {
                        conn.disconnect();
                    } catch (Exception e) {

                    }

                    // Add to cache
                    try {
                        result = mCache.put(url, b);
                    } catch (Exception e) {
                        result = null;
                    }

                }

                return result;

            } catch (IOException e) {
                Log.e("ImageUrlAsyncTask", e.toString());
            } catch (OutOfMemoryError e) {
                Log.v("ImageUrlAsyncTask", "Out of memory error here");
            }

            return null;
        }

        public Bitmap decodeSampledBitmapFromResourceMemOpt(
                InputStream inputStream, int reqWidth, int reqHeight) {

            byte[] byteArr = new byte[0];
            byte[] buffer = new byte[1024];
            int len;
            int count = 0;

            try {
                while ((len = inputStream.read(buffer)) > -1) {
                    if (len != 0) {
                        if (count + len > byteArr.length) {
                            byte[] newbuf = new byte[(count + len) * 2];
                            System.arraycopy(byteArr, 0, newbuf, 0, count);
                            byteArr = newbuf;
                        }

                        System.arraycopy(buffer, 0, byteArr, count, len);
                        count += len;
                    }
                }

                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(byteArr, 0, count, options);

                options.inSampleSize = calculateInSampleSize(options, reqWidth,
                        reqHeight);
                options.inPurgeable = true;
                options.inInputShareable = true;
                options.inJustDecodeBounds = false;
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;

                return BitmapFactory.decodeByteArray(byteArr, 0, count, options);

            } catch (Exception e) {
                e.printStackTrace();

                return null;
            }
        }

        public static int calculateInSampleSize(BitmapFactory.Options opt, int reqWidth, int reqHeight) {
            // Raw height and width of image
            final int height = opt.outHeight;
            final int width = opt.outWidth;
            int inSampleSize = 1;

            if (height > reqHeight || width > reqWidth) {

                final int halfHeight = height / 2;
                final int halfWidth = width / 2;

                // Calculate the largest inSampleSize value that is a power of 2 and keeps both
                // height and width larger than the requested height and width.
                while ((halfHeight / inSampleSize) > reqHeight
                        && (halfWidth / inSampleSize) > reqWidth) {
                    inSampleSize *= 2;
                }
            }

            return inSampleSize;
        }

        @Override
        protected void onPostExecute(CacheableBitmapDrawable result) {
            super.onPostExecute(result);

            try {
                if (result != null && holder.userId == id) {
                    holder.picture.setImageDrawable(result);
                    Animation fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in);

                    holder.picture.startAnimation(fadeInAnimation);
                }

            } catch (Exception e) {

            }
        }
    }
}
