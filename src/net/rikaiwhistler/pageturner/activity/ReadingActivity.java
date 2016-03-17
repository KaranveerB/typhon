/*
 * Copyright (C) 2012 Alex Kuiper
 * 
 * This file is part of PageTurner
 *
 * PageTurner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PageTurner is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PageTurner.  If not, see <http://www.gnu.org/licenses/>.*
 */
package net.rikaiwhistler.pageturner.activity;


import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ExpandableListView;

import com.google.inject.Inject;

import net.rikaiwhistler.pageturner.Configuration;
import net.rikaiwhistler.pageturner.fragment.ReadingFragment;
import net.rikaiwhistler.pageturner.view.NavigationCallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import roboguice.inject.InjectFragment;

import java.util.ArrayList;
import java.util.List;

import static jedi.functional.FunctionalPrimitives.forEach;

public class ReadingActivity extends PageTurnerActivity {

    @InjectFragment(net.rikaiwhistler.pageturner.R.id.fragment_reading)
    private ReadingFragment readingFragment;

    @Inject
    private Configuration config;

    private static final Logger LOG = LoggerFactory
            .getLogger("ReadingActivity");

    private int searchIndex = -1;

    @Override
    protected int getMainLayoutResource() {
        return net.rikaiwhistler.pageturner.R.layout.activity_reading;
    }

    @Override
    public void onDrawerClosed(View view) {
        getSupportActionBar().setTitle(net.rikaiwhistler.pageturner.R.string.app_name);
        super.onDrawerClosed(view);
    }


    @Override
    protected void initDrawerItems( ExpandableListView expandableListView ) {
        super.initDrawerItems( expandableListView );

        if ( expandableListView == null ) {
            return;
        }

        if ( this.readingFragment != null ) {

            if ( readingFragment.hasSearchResults() ) {
                List<NavigationCallback> searchCallbacks =
                        this.readingFragment.getSearchResults();

                getAdapter().findGroup(this.searchIndex).match(
                        s -> forEach(searchCallbacks, s::addChild),
                        () -> LOG.error("Could not find Search drawer item!"));

            }
        }
    }

    protected List<NavigationCallback> getMenuItems( Configuration config ) {

        List<NavigationCallback> menuItems = new ArrayList<>();

        //Add in a blank item to get the spacing right
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && config.isFullScreenEnabled() ) {
            menuItems.add( new NavigationCallback("") );
            menuItems.add( new NavigationCallback("") );
        }

        String nowReading = getString(net.rikaiwhistler.pageturner.R.string.now_reading, config.getLastReadTitle());
        NavigationCallback readingCallback = new NavigationCallback(nowReading);
        menuItems.add(readingCallback);

        if (this.readingFragment != null) {

            if (this.readingFragment.hasTableOfContents()) {

                NavigationCallback tocCallback = new NavigationCallback(getString(net.rikaiwhistler.pageturner.R.string.toc_label));
                readingCallback.addChild(tocCallback);
                tocCallback.addChildren(readingFragment.getTableOfContents());
            }

            if (this.readingFragment.hasHighlights()) {
                NavigationCallback highlightsCallback = new NavigationCallback(getString(net.rikaiwhistler.pageturner.R.string.highlights));
                readingCallback.addChild(highlightsCallback);
                highlightsCallback.addChildren(readingFragment.getHighlights());
            }

            if (this.readingFragment.hasSearchResults()) {
                menuItems.add(new NavigationCallback(getString(net.rikaiwhistler.pageturner.R.string.search_results)));
                this.searchIndex = menuItems.size() - 1;
            }

            if (this.readingFragment.hasBookmarks()) {
                NavigationCallback bookmarksCallback = new NavigationCallback(getString(net.rikaiwhistler.pageturner.R.string.bookmarks));
                readingCallback.addChild(bookmarksCallback);

                bookmarksCallback.addChildren(readingFragment.getBookmarks());
            }
        }

        menuItems.add(new NavigationCallback(getString(net.rikaiwhistler.pageturner.R.string.open_library))
                .setOnClick(() -> launchActivity(LibraryActivity.class)));

        menuItems.add(new NavigationCallback(getString(net.rikaiwhistler.pageturner.R.string.download))
                .setOnClick(() -> launchActivity(CatalogActivity.class)));

        menuItems.add(new NavigationCallback(getString(net.rikaiwhistler.pageturner.R.string.prefs)).setOnClick(this::startPreferences));

        return menuItems;
    }

    @Override
    protected void startPreferences() {
        if (readingFragment != null) {
            this.readingFragment.saveConfigState();
        }

        Intent intent = new Intent(this, PageTurnerPrefsActivity.class);
        startActivity(intent);
    }

    @Override
    protected int getTheme(Configuration config) {
        int theme = config.getTheme();

        if (config.isFullScreenEnabled()) {
            if (config.getColourProfile() == Configuration.ColourProfile.NIGHT) {
                theme = net.rikaiwhistler.pageturner.R.style.DarkFullScreen;
            } else {
                theme = net.rikaiwhistler.pageturner.R.style.LightFullScreen;
            }
        }

        return theme;
    }

    @Override
    protected void onCreatePageTurnerActivity(Bundle savedInstanceState) {

        Class<? extends PageTurnerActivity> lastActivityClass = config.getLastActivity();

        if (!config.isAlwaysOpenLastBook() && lastActivityClass != null
                && lastActivityClass != ReadingActivity.class
                && getIntent().getData() == null) {
            Intent intent = new Intent(this, lastActivityClass);

            startActivity(intent);
            finish();
        }

    }

    @Override
    public boolean onSearchRequested() {
        readingFragment.onSearchRequested();
        return true;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        readingFragment.onWindowFocusChanged(hasFocus);
    }

    public void onMediaButtonEvent(View view) {
        this.readingFragment.onMediaButtonEvent(view.getId());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return readingFragment.onTouchEvent(event);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {

        int action = event.getAction();
        int keyCode = event.getKeyCode();

        if (action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK && isDrawerOpen()) {
            closeNavigationDrawer();
            return true;
        }

        if (readingFragment.dispatchKeyEvent(event)) {
            return true;
        }

        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void beforeLaunchActivity() {
        readingFragment.saveReadingPosition();
        readingFragment.getBookView().releaseResources();
    }

}