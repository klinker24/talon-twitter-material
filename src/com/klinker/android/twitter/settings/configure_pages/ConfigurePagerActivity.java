package com.klinker.android.twitter.settings.configure_pages;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.settings.configure_pages.fragments.PageOneFragment;
import com.klinker.android.twitter.settings.configure_pages.fragments.PageTwoFragment;
import com.klinker.android.twitter.ui.LoginActivity;
import com.klinker.android.twitter.utils.Utils;


public class ConfigurePagerActivity extends Activity {

    private ConfigurationPagerAdapter mSectionsPagerAdapter;
    private Context context;
    private SharedPreferences sharedPrefs;
    private AppSettings settings;
    private ActionBar actionBar;
    private ViewPager mViewPager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        settings = new AppSettings(this);

        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

        Utils.setUpTheme(context, settings);
        setContentView(R.layout.trends_activity);

        setUpDoneDiscard();

        actionBar = getActionBar();

        if (!settings.isTwitterLoggedIn) {
            Intent login = new Intent(context, LoginActivity.class);
            startActivity(login);
            finish();
        }

        mSectionsPagerAdapter = new ConfigurationPagerAdapter(getFragmentManager(), context);

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOverScrollMode(ViewPager.OVER_SCROLL_NEVER);

        mViewPager.setOffscreenPageLimit(3);
    }

    public void setUpDoneDiscard() {
        LayoutInflater inflater = (LayoutInflater) getActionBar().getThemedContext()
                .getSystemService(LAYOUT_INFLATER_SERVICE);
        final View customActionBarView = inflater.inflate(
                R.layout.actionbar_done_discard, null);
        TextView doneButton = (TextView) customActionBarView.findViewById(R.id.done);
        doneButton.setText(getResources().getString(R.string.done_label));
        customActionBarView.findViewById(R.id.actionbar_done).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int currentAccount = sharedPrefs.getInt("current_account", 1);

                        SharedPreferences.Editor editor = sharedPrefs.edit();

                        editor.putInt("account_" + currentAccount + "_page_1", PageOneFragment.type);
                        editor.putInt("account_" + currentAccount + "_page_2", PageTwoFragment.type);

                        editor.putInt("account_" + currentAccount + "_list_1", PageOneFragment.listId);
                        editor.putInt("account_" + currentAccount + "_list_2", PageTwoFragment.listId);

                        editor.putString("account_" + currentAccount + "_name_1", PageOneFragment.listName);
                        editor.putString("account_" + currentAccount + "_name_2", PageTwoFragment.listName);

                        editor.commit();

                        onBackPressed();
                    }
                });
        customActionBarView.findViewById(R.id.actionbar_discard).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onBackPressed();
                    }
                });

        // Show the custom action bar view and hide the normal Home icon and title.
        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayOptions(
                ActionBar.DISPLAY_SHOW_CUSTOM,
                ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME
                        | ActionBar.DISPLAY_SHOW_TITLE);
        actionBar.setCustomView(customActionBarView, new ActionBar.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
    }


}
