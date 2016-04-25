package com.bondevans.fretboard.filebrowser;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.app.ListFragment;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.bondevans.fretboard.utils.Log;
import com.bondevans.fretboard.R;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;

public class FileBrowserFragment extends ListFragment {
    private final static String TAG = "FileBrowserFragment";
    private static final String KEY_CURDIR = "KEY7";
    private static final String PREF_KEY_FILEDIR = "fileDir";
    private static final String DEFAULT_FILEDIR = "/";
    private static final String MIDI_FILE_EXTN = ".mid";
    public File mCurrentDirectory;
    private OnFileSelectedListener fileSelectedListener;
    private TextView mCurrentFolder;
    private String mSdCardRoot;
    ArrayList<String> midiFiles;

    public interface OnFileSelectedListener {
        void onFileSelected(File songFile);
        void upOneLevel(View v);
        void enableUp(boolean enabled);
    }

    @Override
    public void onAttach(Activity activity) {
        Log.d(TAG, "HELLO onAttach (deprecated)");
        super.onAttach(activity);
        try {
            fileSelectedListener = (OnFileSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFileSelectedListener");
        }
    }

    @Override
    public void onAttach(Context context) {
        Log.d(TAG, "HELLO onAttach");
        super.onAttach(context);
        try {
            fileSelectedListener = (OnFileSelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnFileSelectedListener");
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "HELLO onCreate1");
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
        // See whether we've changed directory before changing orientation
        String curDir;
        if (savedInstanceState != null) {
            curDir = savedInstanceState.getString(KEY_CURDIR);
        } else {
            curDir = settings.getString(PREF_KEY_FILEDIR, DEFAULT_FILEDIR);
        }

        mCurrentDirectory = new File(curDir);
        Log.d(TAG, "HELLO onCreate3[" + mCurrentDirectory + "]");
        mSdCardRoot = Environment.getExternalStorageDirectory().getPath();
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.ListFragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.filebrowser_layout, container, false);
        mCurrentFolder = (TextView) contentView.findViewById(R.id.currentFolder);
        Log.d(TAG, "HELLO onCreatView");
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
        browseFolder(mCurrentDirectory);
    }

    /**
     * This function browses up one level
     * according to the field: currentDirectory
     */
    public void upOneLevel() {
        Log.d(TAG, "HELLO path5=[" + mCurrentDirectory.getParentFile().getPath() + "]");
        if (mCurrentDirectory.getParent() != null) {
            Log.d(TAG, "HELLO path6=[" + mCurrentDirectory.getParentFile().getPath() + "]");
            browseFolder(mCurrentDirectory.getParentFile());
        }
    }

    public void browseFolder(final File aFile) {
        Log.d(TAG, "HELLO browseFolder [" + aFile.getName() + "]");
        // On relative we display the full path in the title.
        if (!aFile.exists()) {
            // If file doesn't exist (for some bizarre reason) just default to /sdcard
            Log.d(TAG, "HELLO file doesn't exist");
            mCurrentDirectory = new File(mSdCardRoot);
            ListFilesTask task = new ListFilesTask(mCurrentDirectory);// Do this in background
            task.execute();
        } else if (aFile.isDirectory()) {
            mCurrentDirectory = aFile;// Remember current folder
            ListFilesTask task = new ListFilesTask(aFile);// Do this in background
            task.execute();
        } else {
            // Save file path in preferences so we come back here next time
            saveCurrentDir();
            // Start an intent to View the file, that was clicked...
            fileSelectedListener.onFileSelected(aFile);
        }
    }

    class ListFilesTask extends AsyncTask<Void, String, Void>{
        File folder;

        public ListFilesTask(File folder) {
            this.folder = folder;
            if (folder.getPath().equalsIgnoreCase("/")) {
                mCurrentFolder.setText("/");
                if(fileSelectedListener != null) {
                    fileSelectedListener.enableUp(false);
                }
            } else {
                mCurrentFolder.setText(folder.getPath());
                if(fileSelectedListener != null) {
                    fileSelectedListener.enableUp(true);
                }
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            midiFiles = new ArrayList<>();
            Log.d(TAG, "HELLO ASYNCTASK SDCARDROOT=[" + mSdCardRoot + "] CURRENT FOLDER=[" + folder.getPath() + "]");

            // Now add the midi files
            if (folder.listFiles(new MidiFileFilter()) != null) {
                for (File currentFile : folder.listFiles()) {
                    Log.d(TAG, "HELLO FILENAME: [" + currentFile.getName() + "]");
                    if (currentFile.getName().startsWith(".")) {
                        // Don't add anything starting with a . (i.e. hidden folders and files)
                        Log.d(TAG,"Ignoring DOT files");
                    } else if (currentFile.isDirectory()) {
                        // Add folder name - with slash on the end, so that the folder image is shown
                        midiFiles.add(currentFile.getName() + File.separator);
                    } else {
                        midiFiles.add(currentFile.getName());
                    }
                }
            } else {
                Log.d(TAG, "No files");
            }
            // Sort the files,
            Collections.sort(midiFiles, new FileNameComparator());
            return null;
        }

        class MidiFileFilter implements FileFilter{

            @Override
            public boolean accept(File pathname) {
                return(pathname.getName().endsWith(MIDI_FILE_EXTN));
            }
        }
        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            FileArrayAdapter fileArrayAdapter = new FileArrayAdapter(getActivity(), midiFiles);
            setListAdapter(fileArrayAdapter);
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // Get file+icon object from the list
        String selectedItem = (String) getListView().getItemAtPosition(position);
        browseFolder(new File(addDir(selectedItem)));
    }

    public void refresh() {
        browseFolder(mCurrentDirectory);
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onPause()
     */
    @Override
    public void onPause() {
        //		Log.d(TAG,"HELLO Pausing");
        super.onPause();
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onResume()
     */
    @Override
    public void onResume() {
        Log.d(TAG, "HELLO Resuming");
        super.onResume();
    }

    @Override
    public void onStop() {
        //		Log.d(TAG,"HELLO stopping");
        super.onStop();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onDestroy()
     */
    @Override
    public void onDestroy() {
        //		Log.d(TAG,"HELLO Being Destroyed");
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
            shareFile(addDir(selectedItem));
            return true;
        } else {
            return super.onContextItemSelected(item);
        }
    }

    /**
     * Adds current directory to an item selected from the list
     *
     * @param name selected item
     * @return returns file name with Path
     */
    private String addDir(String name) {
        return mCurrentDirectory.getPath() + File.separator + name;
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

    /* (non-Javadoc)
     * @see android.app.Activity#onConfigurationChanged(android.content.res.Configuration)
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public class FileArrayAdapter extends ArrayAdapter<String> {
        private final Context context;
        private final ArrayList<String> fileNames;

        public FileArrayAdapter(Context context, ArrayList<String> values) {
            super(context, R.layout.filebrowser_item, values);
            this.context = context;
            this.fileNames = values;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            //			Log.d(TAG, "HELLO - getView");
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.filebrowser_item, parent, false);
            TextView textView = (TextView) rowView.findViewById(R.id.file_name);
            ImageView imageView = (ImageView) rowView.findViewById(R.id.file_icon);
            // Change the icon for Folder, Up one level, song file, banned file, set list
            String fileNameText = fileNames.get(position);
            if (fileNameText.endsWith(File.separator)) {
                // Folder
                imageView.setImageResource(R.drawable.folder);
            } else {
                // Possible midi file
                imageView.setImageResource(R.drawable.midi_file_icon);
            }
            textView.setText(fileNameText);

            return rowView;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "HELLO PUTTING curDir=[" + mCurrentDirectory.getPath() + "]");
        outState.putString(KEY_CURDIR, mCurrentDirectory.getPath());
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.ListFragment#onDestroyView()
     */
    @Override
    public void onDestroyView() {
        // Save current Directory before leaving
        saveCurrentDir();
        super.onDestroyView();
    }

    void saveCurrentDir() {
        // Save file path in preferences so we come back here next time
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());

        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PREF_KEY_FILEDIR, this.mCurrentDirectory.getPath());
        // Don't forget to commit your edits!!!
        editor.apply();
    }
}