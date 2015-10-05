package com.bondevans.fretboard.fretboardplayer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
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

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class FileBrowserFragment extends ListFragment
{
    private final static String TAG = "FileBrowserFragment";
    private static final String KEY_CURDIR = "KEY7";
    private static final String PREF_KEY_FILEDIR = "fileDir";
    private static final String DEFAULT_FILEDIR = "/";
    private List<String> directoryEntries = new ArrayList<>();
    public File mCurrentDirectory;
    // Song Conversion
    private OnFileSelectedListener fileSelectedListener;
    private TextView mCurrentFolder;
    private SongArrayAdapter songArrayAdapter;
    private String mSdCardRoot;
    private String mFilter="";
    private SdCardFactory mCards;

    public interface OnFileSelectedListener{
        void onFileSelected(boolean inSet, File songFile);
        void upOneLevel(View v);
        void enableUp(boolean enabled);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            fileSelectedListener = (OnFileSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFileSelectedListener");
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    public static FileBrowserFragment newInstance() {
        FileBrowserFragment f = new FileBrowserFragment();
        Bundle b = new Bundle();
        f.setArguments(b);
        Log.d(TAG, "HELLO newInstance");
        return f;
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "HELLO onCreate1");
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
        // Default is /sdcard/chordinator
        Log.d(TAG, "HELLO onCreate2");

        // See whether we've changed directory before changing orientation
        String curDir="";
        if( savedInstanceState != null ){
            curDir=savedInstanceState.getString(KEY_CURDIR);
        }
        else{
            curDir = settings.getString(PREF_KEY_FILEDIR, DEFAULT_FILEDIR);
        }

        mCurrentDirectory = new File (curDir);
        Log.d(TAG, "HELLO onCreate3["+mCurrentDirectory+"]");
        // Only all access to sdcard on Chordinator Aug
        mSdCardRoot = Environment.getExternalStorageDirectory().getPath();

        Log.d(TAG, "HELLO onCreate4");
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.ListFragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.filebrowser_layout, container, false);
        mCurrentFolder = (TextView)contentView.findViewById(R.id.currentFolder);
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
        mCards = new SdCardFactory(getActivity());
        mCards.log();
        browseFolder(mCurrentDirectory);
    }

    /**
     * This function browses up one level
     * according to the field: currentDirectory
     */
    public void upOneLevel(){
        Log.d(TAG, "HELLO path5=["+mCurrentDirectory.getParentFile().getPath()+"]");
        if(mCurrentDirectory.getParent() != null){
            Log.d(TAG, "HELLO path6=["+mCurrentDirectory.getParentFile().getPath()+"]");
            browseFolder(mCurrentDirectory.getParentFile());
        }
    }

    public void browseFolder(final File aFile){
        Log.d(TAG, "HELLO browseFolder [" + aFile.getName() + "] filter [" + mFilter + "]");
        // On relative we display the full path in the title.
        if(!aFile.exists()){
            // If file doesn't exist (for some bizarre reason) just default to /sdcard
            Log.d(TAG, "HELLO file doesn't exist");
            mCurrentDirectory = new File(mSdCardRoot);
            listFilesinFolder(mCurrentDirectory, mFilter);
        }
        else if (aFile.isDirectory()){
            mCurrentDirectory = aFile;// Remember current folder
            listFilesinFolder(aFile, mFilter);
        }
        else{
            // Save file path in preferences so we come back here next time
            saveCurrentDir();
            // Start an intent to View the file, that was clicked...
            openFile(aFile);
        }
    }

    private void openFile(File theFile){
        fileSelectedListener.onFileSelected(false, theFile);
    }

    /**
     * fill
     * lists all files and folders in a given directory
     * @param folder
     * @param nameFilter
     */
    private void listFilesinFolder(File folder, String nameFilter) {
        directoryEntries.clear();
        ArrayList<String> songFiles = new ArrayList<String>();
        Log.d(TAG, "HELLO SDCARDROOT=["+mSdCardRoot+"] CURRENT FOLDER=["+folder.getPath()+"]");
        // If we have gone UP from an sdcard root then simply list the sdcard and any other external sdcards - not the actual folders.
        // Also disable the up button at this point
        boolean root=false;
        if(mCards.isPathUpFromRoot(folder)){
            fileSelectedListener.enableUp(false);
            root=true;
            for( SdCardFactory.SdCard sdcard: mCards.getSdCards()){
                directoryEntries.add(sdcard.name+File.separator);
            }
            mCurrentFolder.setText("/");
        }
        else{
            fileSelectedListener.enableUp(true);
            mCurrentFolder.setText(doPath(folder.getPath()));
        }

        if(!root){
            // Now add the midi files
            FilenameFilter fnf = new MyFilenameFilter(nameFilter);
            if(folder.listFiles(fnf)!=null){
                for (File currentFile : folder.listFiles(fnf)){
                    if (currentFile.getName().startsWith(".")){
                        // Don't add anything starting with a . (i.e. hidden folders and files)
                    }
                    else if (currentFile.isDirectory()) {
                        // Add folder name - with slash on the end, so that the folder image is shown
                        songFiles.add(currentFile.getName()+File.separator);
                    }
                    else{
                        //				Log.d(TAG, "HELLO Adding FILENAME: ["+fileName+"]");
                        songFiles.add(songName(currentFile));
                    }
                }
            }
        }
        // Sort the song files,
        Collections.sort(songFiles, new FileNameComparator());
        //then add them to the directory listing
        Iterator<String> e;
        e = songFiles.iterator();
        while (e.hasNext()) {
            directoryEntries.add(e.next());
        }
        Log.d(TAG, "HELLO B4 setListAdapter2");
        e = directoryEntries.iterator();
        String [] myfiles = new String [directoryEntries.size()];

        int x=0;
        while(e.hasNext()){
            myfiles[x++]=e.next();
        }
        songArrayAdapter = new SongArrayAdapter(getActivity(), myfiles);
        setListAdapter(songArrayAdapter);
        final ListView lv = getListView();
        registerForContextMenu(lv);

        lv.setTextFilterEnabled(true);
        lv.setItemsCanFocus(false);
        lv.setLongClickable(true);
    }

    /**
     * Returns song name given a song file
     * @param file
     * @return
     */
    private String songName( File file ){
        return file.getName();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        //		Log.d(TAG, "HELLO onListItemClick");
        // Get file+icon object from the list
        String selectedFile = (String) getListView().getItemAtPosition(position);
        openItem(selectedFile);
    }

    /**
     * openItem - Opens the selected file
     * @param selectedItem
     */
    private void openItem(String selectedItem){
        Log.L(TAG, "Item", selectedItem);
        if(mCards.isRoot(selectedItem)){
            browseFolder(new File(mCards.toPath(selectedItem)));
        }
        else{
            browseFolder(new File(addDir(mCards.toPath(selectedItem))));
        }
    }

    public void refresh(){
        browseFolder(mCurrentDirectory);
    }

    public void refresh(String filter){
        mFilter = filter;
        if( mCurrentDirectory != null){
            browseFolder(mCurrentDirectory);
        }
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onPause()
     */
    @Override
    public void onPause() {
        //		Log.d(TAG,"HELLO Pausing");
        //
        super.onPause();
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onResume()
     */
    @Override
    public void onResume() {
        Log.d(TAG,"HELLO Resuming");
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
        // CHORDINATOR - AUGMENTED
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        String selectedItem = (String) getListView().getItemAtPosition(info.position);
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.file_browser_context, menu);
        menu.setHeaderTitle("Options");
        if(selectedItem.endsWith(File.separator) || selectedItem.equals("..")){
            // Directory, so can't edit or delete or convert or save_as or add to set
            menu.removeItem(R.id.share);
        }
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if(item.getGroupId()!= R.id.context_group_browser){
            Log.d(TAG,"HELLO onContextItemSelected - sonvView Context Menu");

            return super.onContextItemSelected(item);
        }
        Log.d(TAG,"HELLO onContextItemSelected");
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        String selectedItem = (String) getListView().getItemAtPosition(info.position);
        Log.d(TAG, "HELLO selectedItem=["+selectedItem+"]");
        if (item.getItemId() == R.id.share) {
            shareSong(addDir(selectedItem));
            return true;
        } else {
            return super.onContextItemSelected(item);
        }
    }

    /**
     * Adds current directory to an item selected from the list
     * @param name
     * @return
     */
    private String addDir(String name){
        return mCurrentDirectory.getPath() + File.separator + name;
    }

    public void shareSong(String fileName){
        File aFile = new File(fileName);
        Intent theIntent = new Intent(Intent.ACTION_SEND);
        theIntent.setType("text/plain");
        // the formatted text.
        theIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(aFile));
//		theIntent.putExtra(Intent.EXTRA_TEXT, mSf.getSong().getSongText());
        //next line specific to email attachments
        theIntent.putExtra(Intent.EXTRA_SUBJECT, "Sending " + aFile.getName());
        try {
            startActivity(Intent.createChooser(theIntent, "Share With...."));
        }
        catch (Exception e) {
        }
    }

    @SuppressLint("DefaultLocale")
    public class MyFilenameFilter implements FilenameFilter{
        private String filter="";
        public MyFilenameFilter(String filter){
            this.filter = filter.toLowerCase();
        }
        @Override
        public boolean accept(File dir, String filename) {
            if(filename.toLowerCase().contains(filter))
                return true;
            return false;
        }

    }
    /* (non-Javadoc)
     * @see android.app.Activity#onConfigurationChanged(android.content.res.Configuration)
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public class SongArrayAdapter extends ArrayAdapter<String> {
        private final Context context;
        private final String[] fileNames;

        public SongArrayAdapter(Context context, String[] values) {
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
            String fileNameText = fileNames[position];
            if (fileNameText.endsWith(File.separator)) {
                // Folder
                imageView.setImageResource(R.drawable.folder);
            }
            else{
                // Possible song file
                imageView.setImageResource(R.drawable.song_file_icon);
            }
            textView.setText(fileNameText);

            return rowView;
        }
    }
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "HELLO PUTTING curDir=["+mCurrentDirectory.getPath()+"]");
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
    void saveCurrentDir(){
        // Save file path in preferences so we come back here next time
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());

        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PREF_KEY_FILEDIR, this.mCurrentDirectory.getPath());
        // Don't forget to commit your edits!!!
        editor.commit();
    }

    /**
     * doPath pretties up the folder path for display
     * @param path
     * @return
     */
    private String doPath(String path){
        // remove any slashes on the end of the path
        Log.d(TAG, "HELLO doPath in["+path+"]");
        if( path.endsWith("/")){
            path = path.substring(0, path.length()-1);
        }
        // Replace Environment.getExternalStorageDirectory() with /sdcard for display purposes only
        path = mCards.toDisplay(path);

        Log.d(TAG, "HELLO dopath out["+path+"]");
        return path;
    }
}