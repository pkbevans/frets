package com.bondevans.frets.freteditor;

import android.content.Context;
import android.os.Bundle;
import androidx.fragment.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.bondevans.frets.R;
import com.bondevans.frets.fretview.FretSong;
import com.bondevans.frets.fretview.FretTrack;
import com.bondevans.frets.instruments.Instrument;
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
    private File mTrackTmpFile;
    private int mTrackBeingEdited;

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
        mSongName = contentView.findViewById(R.id.song_name);
        mKeywords = contentView.findViewById(R.id.song_keywords);
        return contentView;
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.ListFragment#onViewCreated(android.view.View, android.os.Bundle)
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        Log.d(TAG, "HELLO onViewCreated");
        final ListView lv = getListView();

        lv.setItemsCanFocus(false);
        lv.setLongClickable(true);
        lv.setFastScrollEnabled(true);
        Log.d(TAG, "HELLO onViewCreated2");
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onPause()
     */
    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onResume()
     */
    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        if (mFretSong != null) {
            setUpListView();
        } else Log.d(TAG, "mFretSong NULL!!");
    }

    @Override
    public void onStart() {
        Log.d(TAG, "onStart");
        super.onStart();
    }

    @Override
    public void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onDestroy()
     */
    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    /*
     */
    private class FretTrackAdapter extends ArrayAdapter<FretTrack> {
        private static final int TYPE_CLICK = 0;
        private static final int TYPE_INSTRUMENT = 1;
        private final Context context;
        private final ArrayList<FretTrack> fretTracks;
        ArrayAdapter<String> instrumentAdapter;
        ArrayAdapter<Instrument> fretInstrumentAdapter;
        int selectedPosition;

        public FretTrackAdapter(Context context, ArrayList<FretTrack> values) {
            super(context, R.layout.fretsongedit_item, values);
            this.context = context;
            this.fretTracks = values;
            instrumentAdapter = new ArrayAdapter<>
                    (FretSongEditFragment.this.getActivity(), simple_spinner_item, getActivity().getResources().getStringArray(R.array.midi_instrument_names));
            instrumentAdapter.setDropDownViewResource
                    (android.R.layout.simple_spinner_dropdown_item);
            fretInstrumentAdapter = new ArrayAdapter<>
                    (FretSongEditFragment.this.getActivity(), simple_spinner_item, Instrument.values());
            fretInstrumentAdapter.setDropDownViewResource
                    (android.R.layout.simple_spinner_dropdown_item);
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }
        @Override
        public int getItemViewType(int position){
            return fretTracks.get(position).isClickTrack()?TYPE_CLICK:TYPE_INSTRUMENT;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Log.d(TAG, "HELLO - getView: "+position+ " View type: "+ getItemViewType(position));
            ViewHolder holder;
            if( convertView == null) {
                holder = new ViewHolder();
                if(fretTracks.get(position).isClickTrack()) {
                    Log.d(TAG, "HELLO - getView CLICK :"+position);
                    return getClickTrackView(position, parent,holder);
                } else {
                    Log.d(TAG, "HELLO - getView NOT CLICK: "+position);
                    LayoutInflater inflater = (LayoutInflater) context
                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    convertView = inflater.inflate(R.layout.fretsongedit_item, parent, false);
                    // Creates a ViewHolder and store references to the child views
                    // we want to bind data to.
                    holder.trackName = convertView.findViewById(R.id.track_name);
                    holder.soloText = convertView.findViewById(R.id.solo_text);
                    holder.instrument = convertView.findViewById(R.id.instrument_spinner);
                    holder.fretInstrument = convertView.findViewById(R.id.fretinstrument_spinner);
                    holder.soloButton = convertView.findViewById(R.id.soloButton);
                    holder.drumTrack = convertView.findViewById(R.id.isDrumTrack);
                    holder.deleteButton = convertView.findViewById(R.id.deleteButton);
                    holder.editButton = convertView.findViewById(R.id.selectButton);
                    holder.isClick = false;
                    convertView.setTag(holder);
                }
            }
            else{
                // Get the ViewHolder back
                holder = (ViewHolder) convertView.getTag();
                if(fretTracks.get(position).isClickTrack()){
                    if(holder.isClick ) {
                        return convertView;
                    } else {
                        // This may be redundant.....
                        Log.d(TAG, "HELLO - NEED TO CONVERT!!!: "+position);
                        return getClickTrackView(position, parent,holder);
                    }
                }
            }
            holder.trackName.setText(fretTracks.get(position).getName());

            boolean isDrum = fretTracks.get(position).isDrumTrack();
            holder.drumTrack.setChecked(isDrum);
            holder.drumTrack.setTag(position);
            holder.drumTrack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedPosition = (Integer) v.getTag();
                    Log.d(TAG, "drumTrack onClick: "+selectedPosition);
                    fretTracks.get(selectedPosition).setDrumTrack(!fretTracks.get(selectedPosition).isDrumTrack());
                    mIsEdited = true;
                    notifyDataSetChanged();
                }
            });

            if(isDrum) {
                holder.instrument.setEnabled(false);
                holder.fretInstrument.setEnabled(false);
                holder.instrument.setEnabled(false);
            }
            else{
                holder.instrument.setEnabled(true);
                holder.instrument.setAdapter(instrumentAdapter);
                // Spinner item selection Listener
                holder.instrument.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        if(view != null) {
                            final int track = getListView().getPositionForView((View) view.getParent());
                            Log.d(TAG, "Instrument spinner: " + position + " id: " + id + " track:" + track);
                            if (fretTracks.get(track).getMidiInstrument() != position) {
                                fretTracks.get(track).setMidiInstrument(position);
                                Log.d(TAG, "Instrument updated");
                                mIsEdited = true;
                            }
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        Log.d(TAG, "Instrument spinner: nothing selected");
                    }
                });
                //
                holder.fretInstrument.setEnabled(true);
                holder.fretInstrument.setAdapter(fretInstrumentAdapter);
                // Spinner item selection Listener
                holder.fretInstrument.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        if(view != null) {
                            final int track = getListView().getPositionForView((View) view.getParent());
                            Log.d(TAG, "Fret Instrument spinner: " + position + " id: " + id + " track:" + track);
                            if (fretTracks.get(track).getFretInstrument() != position) {
                                fretTracks.get(track).setFretInstrument(position);
                                Log.d(TAG, "Instrument updated");
                                mIsEdited = true;
                            }
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        Log.d(TAG, "Fret Instrument spinner: nothing selected");
                    }
                });
            }
            holder.soloText.setText(position == mFretSong.getSoloTrack()?FretSongEditFragment.this.getActivity().getString(R.string.solo_track):FretSongEditFragment.this.getActivity().getString(R.string.backing_track));
            holder.soloButton.setEnabled(!isDrum);  // Dont allow drum track to be selected as SOlo Track.
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

            holder.deleteButton.setTag(position);
            holder.deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    selectedPosition = (Integer) view.getTag();
                    mFretSong.deleteTrack(selectedPosition);
                    mIsEdited = true;
                    notifyDataSetChanged();
                }
            });
            holder.editButton.setEnabled(position == mFretSong.getSoloTrack());
            holder.editButton.setTag(position);
            holder.editButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    selectedPosition = (Integer) view.getTag();
                    trackSelectedListener.onTrackSelected(selectedPosition);
                }
            });
            Log.d(TAG, "Setting Instrument: " + fretTracks.get(position).getMidiInstrument());
            holder.instrument.setSelection(fretTracks.get(position).getMidiInstrument());
            holder.fretInstrument.setSelection((fretTracks.get(position).getFretInstrument()==FretTrack.NO_FRET_INSTRUMENT?0:fretTracks.get(position).getFretInstrument()));

            return convertView;
        }

        class ViewHolder {
            TextView trackName;
            Spinner instrument;
            Spinner fretInstrument;
            RadioButton soloButton;
            TextView soloText;
            CheckBox drumTrack;
            ImageButton deleteButton;
            ImageButton editButton;
            boolean isClick;
        }
        View getClickTrackView(int position, ViewGroup parent, ViewHolder holder){
            Log.d(TAG, "HELLO - getView CLICK :"+position);
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View convertView = inflater.inflate(R.layout.fretsongedit_clickitem, parent, false);
            holder.isClick = true;
            convertView.setTag(holder);
            return convertView;
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

    public void updateFretSong(){
        isEdited();
    }
    public void setEdited(boolean b) {
        mIsEdited = b;
    }

    public FretSong getFretSong() {
        return mFretSong;
    }

    public int getTrackBeingEdited() {
        return mTrackBeingEdited;
    }

    public void setTrackBeingEdited(int trackBeingEdited) {
        this.mTrackBeingEdited = trackBeingEdited;
    }

    public File getTrackTmpFile() {
        return mTrackTmpFile;
    }

    public void setTrackTmpFile(File trackTmpFile) {
        this.mTrackTmpFile = trackTmpFile;
    }
}