package com.bondevans.frets.freteditor;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.bondevans.frets.R;
import com.bondevans.frets.fretview.FretSong;
import com.bondevans.frets.fretview.FretTrack;
import com.bondevans.frets.utils.Log;

import java.io.File;
import java.util.ArrayList;

import static android.R.layout.simple_spinner_item;

public class FretSongEditFragment extends ListFragment {
    private final static String TAG = FretSongEditFragment.class.getSimpleName();
    private OnTrackSelectedListener trackSelectedListener;
    private FretSong mFretSong;
    private boolean mIsEdited = false;
    private EditText mSongName;
    private EditText mKeywords;

    public interface OnTrackSelectedListener {
        void onTrackSelected(int track);
    }

    @Override
    public void onAttach(Context context) {
        Log.d(TAG, "HELLO onAttach");
        super.onAttach(context);
        try {
            trackSelectedListener = (OnTrackSelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement " + OnTrackSelectedListener.class.getSimpleName());
        }
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        Log.d(TAG, "HELLO onCreate");
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.ListFragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.fretsongedit_layout, container, false);
        Log.d(TAG, "HELLO onCreatView");
        mSongName = (EditText) contentView.findViewById(R.id.song_name);
        mKeywords = (EditText) contentView.findViewById(R.id.song_keywords);
        return contentView;
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.ListFragment#onViewCreated(android.view.View, android.os.Bundle)
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        Log.d(TAG, "HELLO onViewCreated");
        final ListView lv = getListView();
        registerForContextMenu(lv);

        lv.setTextFilterEnabled(true);
        lv.setItemsCanFocus(false);
        lv.setLongClickable(true);
        lv.setFastScrollEnabled(true);
        Log.d(TAG, "HELLO onViewCreated2");
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Log.d(TAG, "itemClicked: " + position);
        trackSelectedListener.onTrackSelected(position);
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onPause()
     */
    @Override
    public void onPause() {
        Log.d(TAG, "HELLO Pausing");
        super.onPause();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onResume()
     */
    @Override
    public void onResume() {
        Log.d(TAG, "HELLO Resuming");
        super.onResume();
        if (mFretSong != null) {
            setUpListView();
        } else Log.d(TAG, "mFretSong NULL!!");
    }

    @Override
    public void onStart() {
        Log.d(TAG, "HELLO onStart");
        super.onStart();
    }

    @Override
    public void onStop() {
        Log.d(TAG, "HELLO stopping");
        super.onStop();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onDestroy()
     */
    @Override
    public void onDestroy() {
        Log.d(TAG, "HELLO Being Destroyed");
        super.onDestroy();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu, android.view.View, android.view.ContextMenu.ContextMenuInfo)
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        String selectedItem = (String) getListView().getItemAtPosition(info.position);
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.filebrowser_menu, menu);
        menu.setHeaderTitle("Options");
        if (selectedItem.endsWith(File.separator) || selectedItem.equals("..")) {
            // Directory, so can't edit or delete or convert or save_as or add to set
            menu.removeItem(R.id.share);
        }
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getGroupId() != R.id.context_group_browser) {
            Log.d(TAG, "HELLO onContextItemSelected - sonvView Context Menu");

            return super.onContextItemSelected(item);
        }
        Log.d(TAG, "HELLO onContextItemSelected");
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        String selectedItem = (String) getListView().getItemAtPosition(info.position);
        Log.d(TAG, "HELLO selectedItem=[" + selectedItem + "]");
        if (item.getItemId() == R.id.share) {
            shareFile(selectedItem);
            return true;
        } else {
            return super.onContextItemSelected(item);
        }
    }

    public void shareFile(String fileName) {
        File aFile = new File(fileName);
        Intent theIntent = new Intent(Intent.ACTION_SEND);
        theIntent.setType("text/plain");
        // the formatted text.
        theIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(aFile));
        //next line specific to email attachments
        theIntent.putExtra(Intent.EXTRA_SUBJECT, "Sending " + aFile.getName());
        try {
            startActivity(Intent.createChooser(theIntent, "Share With...."));
        } catch (Exception e) {
            Log.d(TAG, "OOPS - Sharing failure");
        }
    }

    private class FretTrackAdapter extends ArrayAdapter<FretTrack> {
        private final Context context;
        private final ArrayList<FretTrack> fretTracks;
        ArrayAdapter instrumentAdapter;
        int selectedPosition;

        public FretTrackAdapter(Context context, ArrayList<FretTrack> values) {
            super(context, R.layout.fretsongedit_item, values);
            this.context = context;
            this.fretTracks = values;
            instrumentAdapter = new ArrayAdapter<>
                    (FretSongEditFragment.this.getActivity(), simple_spinner_item, getActivity().getResources().getStringArray(R.array.midi_instrument_names));

            instrumentAdapter.setDropDownViewResource
                    (android.R.layout.simple_spinner_dropdown_item);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Log.d(TAG, "HELLO - getView: "+position);
            ViewHolder holder;
            if( convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.fretsongedit_item, parent, false);
                // Creates a ViewHolder and store references to the child views
                // we want to bind data to.
                holder = new ViewHolder();

                holder.trackName = (TextView) convertView.findViewById(R.id.track_name);
                holder.events = (TextView) convertView.findViewById(R.id.events);
                holder.soloText = (TextView) convertView.findViewById(R.id.solo_text);
                holder.instrument = (Spinner) convertView.findViewById(R.id.instrument_spinner);
                holder.soloButton = (RadioButton) convertView.findViewById(R.id.soloButton);
                convertView.setTag(holder);
            }
            else{
                // Get the ViewHolder back to get fast access to the TextView
                // and the ImageView.
                holder = (ViewHolder) convertView.getTag();
            }
            holder.trackName.setText(fretTracks.get(position).getName());
            holder.events.setText(String.format(getString(R.string.fret_events), fretTracks.get(position).fretEvents.size()));

            holder.instrument.setAdapter(instrumentAdapter);
            // Spinner item selection Listener
            holder.instrument.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    final int track = getListView().getPositionForView((View) view.getParent());
                    Log.d(TAG, "Instrument spinner: " + position + " id: " + id + " track:" + track);
                    if (fretTracks.get(track).getMidiInstrument() != position) {
                        fretTracks.get(track).setMidiInstrument(position);
                        Log.d(TAG, "Instrument updated");
                        mIsEdited = true;
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    Log.d(TAG, "Instrument spinner: nothing selected");
                }
            });
            Log.d(TAG, "Setting Instrument: " + fretTracks.get(position).getMidiInstrument());
            holder.instrument.setSelection(fretTracks.get(position).getMidiInstrument());
            holder.soloText.setText(position == mFretSong.getSoloTrack()?FretSongEditFragment.this.getActivity().getString(R.string.solo_track):FretSongEditFragment.this.getActivity().getString(R.string.rhythm_track));
            holder.soloButton.setChecked(position == mFretSong.getSoloTrack());
            holder.soloButton.setTag(position);
            holder.soloButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    selectedPosition = (Integer) view.getTag();
                    mFretSong.setSoloTrack(selectedPosition);
                    mIsEdited = true;
                    notifyDataSetChanged();
                }
            });
            return convertView;
        }

        class ViewHolder {
            TextView trackName;
            TextView events;
            Spinner instrument;
            RadioButton soloButton;
            TextView soloText;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState");
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.ListFragment#onDestroyView()
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    public void setFretSong(FretSong fretSong) {
        Log.d(TAG, "setFretSong fretsong");
        mFretSong = fretSong;
        mSongName.setText(mFretSong.getName());
        mKeywords.setText(mFretSong.getKeywords());
        setUpListView();
    }

    private void setUpListView() {
        Log.d(TAG, "setUpListView");
        FretTrackAdapter fretTrackAdapter = new FretTrackAdapter(getActivity(), (ArrayList<FretTrack>) mFretSong.getFretTracks());
        final ListView listView = getListView();
        listView.setAdapter(fretTrackAdapter);
    }

    public boolean isEdited() {
        if (mSongName.getText().toString().compareTo(mFretSong.getName()) != 0) {
            mFretSong.setName(mSongName.getText().toString());
            mIsEdited = true;
        }
        if (mKeywords.getText().toString().compareTo(mFretSong.getKeywords()) > 0) {
            mFretSong.setKeywords(mKeywords.getText().toString());
            mIsEdited = true;
        }
        return mIsEdited;
    }

    public void setEdited(boolean b) {
        mIsEdited = b;
    }

    public FretSong getFretSong() {
        return mFretSong;
    }
}