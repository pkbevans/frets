package com.bondevans.frets.fretlist;

import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.media.midi.MidiManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.bondevans.frets.R;
import com.bondevans.frets.app.FretApplication;
import com.bondevans.frets.filebrowser.FileBrowserActivity;
import com.bondevans.frets.selectSynth.SelectSynthActivity;
import com.bondevans.frets.user.UserProfileActivity;
import com.bondevans.frets.user.UserProfileFragment;
import com.bondevans.frets.utils.Log;
import com.bondevans.frets.utils.Pref;
import com.bondevans.frets.utils.Synth;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

public class FretListActivity extends AppCompatActivity {
    private static final String TAG = FretListActivity.class.getSimpleName();
    static final String ARG_FRETLIST_NUMBER = "fretlist_number";
    static final String ARG_FRETLIST_TYPE = "fretlist_type";
    static final int FRETLIST_TYPE_PUBLIC = 1;
    static final int FRETLIST_TYPE_PRIVATE = 2;
    private List<QueryUpdateListener> mListeners = new ArrayList<>();
    private FretApplication mApp;

    public interface QueryUpdateListener {
        void onQueryUpdate(String search);
    }

    public synchronized void registerDataUpdateListener(QueryUpdateListener listener) {
        mListeners.add(listener);
    }

    public synchronized void unregisterDataUpdateListener(QueryUpdateListener listener) {
        mListeners.remove(listener);
    }

   @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.fretlist_activity);
        FretListPagerAdapter fretListPagerAdapter = new FretListPagerAdapter(this, getSupportFragmentManager());
        ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(fretListPagerAdapter);
        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);
        handleIntent(getIntent());
        // Connect to Synth
        mApp = (FretApplication)getApplicationContext();
        connectSynth();
        Toolbar toolbar = findViewById(R.id.tool_bar); // Attaching the layout to the toolbar object
        setSupportActionBar(toolbar);                   // Setting toolbar as the ActionBar with setSupportActionBar() call
        getSupportActionBar().setTitle("");             // Empty string - since we are using the logo image
    }

    private void connectSynth() {
        // Is synth set in Application
        if(null == mApp.getMidiDeviceInfo()){
            Log.d(TAG, "HELLO connectSynth 1");
            MidiManager midiManager = (MidiManager) getSystemService(MIDI_SERVICE);
            if (midiManager == null) {
                Toast.makeText(this, "MidiManager is null!", Toast.LENGTH_LONG)
                        .show();
                return;
            }
            // See if we've previously connected to a synth
            String synthDescription = Pref.getPreference(this, Pref.SYNTH);
            if(synthDescription.isEmpty()){
                Log.d(TAG, "HELLO connectSynth 2");
                launchSelectSynthActivity();
            } else {
                Log.d(TAG, "HELLO connectSynth 3");
                ArrayList<Synth.SynthDetails> synths = Synth.getSynths(midiManager);
                for (Synth.SynthDetails synth: synths) {
                    Log.d(TAG, "HELLO connectSynth 4");
                    if(synthDescription.equalsIgnoreCase(synth.getDescription())){
                        Log.d(TAG, "HELLO connectSynth 5");
                        Synth.openSynth(mApp, midiManager,synth);
                    }
                }
            }
        }else{
            Log.d(TAG, "HELLO Already got synth");
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }
    private void handleIntent(Intent intent) {
        Log.d(TAG, "HELLO - handle intent");

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String search = intent.getStringExtra(SearchManager.QUERY);
            Log.d(TAG, "HELLO - DO A SEARCH: "+ search);
            updateQuery(search);
        }
    }
    private void updateQuery(String search){
        for (QueryUpdateListener listener : mListeners) {
            listener.onQueryUpdate(search);
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.fretlist_menu, menu);
        // Associate searchable configuration with the SearchView
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView =
                (SearchView) menu.findItem(R.id.action_search).getActionView();
        MenuItem searchMenuItem = menu.findItem(R.id.action_search);
        searchMenuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                Log.d(TAG, "HELLO - onMenuItemActionExpand");
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                Log.d(TAG, "HELLO - onMenuItemActionCollapse");
                updateQuery("");
                return true;
            }
        });
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected 1");
        int id = item.getItemId();
        switch (id) {
            case R.id.action_import:
                launchBrowser();
                return true;
            case R.id.action_view_profile:
                launchProfileActivity();
                return true;
            case R.id.action_select_synth:
                launchSelectSynthActivity();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void launchProfileActivity() {
        Intent intent = new Intent(this, UserProfileActivity.class);
        intent.putExtra(UserProfileFragment.INTENT_UID, FretApplication.getUID());
        intent.putExtra(UserProfileFragment.INTENT_EDITABLE, true);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "NO ACTIVITY FOUND: "+UserProfileActivity.class.getSimpleName());
        }
    }

    private void launchSelectSynthActivity() {
        Intent intent = new Intent(this, SelectSynthActivity.class);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "NO ACTIVITY FOUND: "+SelectSynthActivity.class.getSimpleName());
        }
    }

    private void launchMidiTest() {
        Intent intent = new Intent(this, MidiTest.class);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "NO ACTIVITY FOUND: "+MidiTest.class.getSimpleName());
        }
    }

    private void launchBrowser() {
        Intent intent = new Intent(this, FileBrowserActivity.class);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "NO ACTIVITY FOUND: "+FileBrowserActivity.class.getSimpleName());
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }
}
