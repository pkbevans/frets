package com.bondevans.frets.fretlist;

import android.content.Context;

import com.bondevans.frets.R;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
public class FretListPagerAdapter extends FragmentPagerAdapter {

    @StringRes
    private static final int[] TAB_TITLES = new int[]{R.string.public_frets_tab_text, R.string.my_frets_tab_text };
    private static final String TAG = FretListPagerAdapter.class.getSimpleName();
    private final Context mContext;

    FretListPagerAdapter(Context context, FragmentManager fm) {
        super(fm, FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        mContext = context;
    }

    @Override
    public Fragment getItem(int position) {
        // getItem is called to instantiate the fragment for the given page.
        // Return a FretListFragment (defined as a static inner class below).
        int type;
        switch(position){
            case 0:
                type = FretListActivity.FRETLIST_TYPE_PUBLIC;
                return FretListFragment.newInstance(position + 1, type);
            case 1:
                type = FretListActivity.FRETLIST_TYPE_PRIVATE;
                return FretListFragment.newInstance(position + 1, type);
        }
        return null;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return mContext.getResources().getString(TAB_TITLES[position]);
    }

    @Override
    public int getCount() {
        // Show 2 total pages.
        return 2;
    }
}