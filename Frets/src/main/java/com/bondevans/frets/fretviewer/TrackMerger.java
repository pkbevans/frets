package com.bondevans.frets.fretviewer;

import com.bondevans.frets.fretview.FretEvent;
import com.bondevans.frets.utils.Log;

import java.util.List;

public class TrackMerger {
    private static final String TAG = TrackMerger.class.getSimpleName();
    private List<FretEvent> events;
    private Tracker t1;
    private Tracker t2;

    public TrackMerger(List<FretEvent> events, int track){
        this.events = events;
        t1 = new Tracker(events);
        // Set all events to track ZERO
        for(FretEvent ev: events)ev.track=track;
    }
    public void mergeTrack(List<FretEvent> events, int track){
        t2 = new Tracker(events);
        // Set track number for each event
        Log.d(TAG, "mergeTrack:"+track);
        for(FretEvent ev: events)ev.track=track;
        // merge t2 into t1
        doMerge();
    }

    /**
     * Merges track 2 into track 1
     */
    private void doMerge() {
        t1.first();
        t2.first();
        //Loop until no more on track 2
        while(!t2.end){
            Log.d(TAG, (t1.end ?"END": "MORE")+
                    ","+t1.currentPos+
                    ","+t1.getCurrent().getTicks()+
                    ","+t1.ticksTotal+
                    ","+(t2.end ?"END": "MORE")+
                    ","+t2.currentPos+
                    ","+t2.getCurrent().getTicks()+
                    ","+t2.ticksTotal
            );
            if(t1.lessThan(t2)){
                Log.d(TAG, "t1<t2");
                // t1 current position is correct
                if(t1.end) {
                    // Add t2 on the end
                    // Amend t2 ticks to reflect time since last t1 event
                    t2.getCurrent().setTicks(t2.ticksTotal-t1.ticksTotal);
                    // Add on the end. currentPos will point to new entry
                    events.add(++t1.currentPos,t2.getCurrent());
                    // Update the total ticks for the entry just added
                    t1.ticksTotal= t2.ticksTotal;
                    t2.next();
                }
                else{
                    t1.next();
                }
            }
            else if(t2.lessThan(t1)){
                //  Stick t2 in before t1 and adjust ticks for both
                Log.d(TAG, "t2<t1");
                t2.getCurrent().setTicks(t1.getCurrent().getTicks()-(t1.ticksTotal-t2.ticksTotal));
                t1.getCurrent().setTicks(t1.ticksTotal-t2.ticksTotal);
                events.add(t1.currentPos,t2.getCurrent());
                // t1.currentPos now points to new one added, so need to update total ticks
                t1.ticksTotal=t2.ticksTotal;
                t2.next();
            }
            else{
                Log.d(TAG, "t2==t1");
                // t1 and t2 are the same. Add t2 after t1 and set t2 ticks to zero
                t2.getCurrent().setTicks(0);
                events.add(++t1.currentPos,t2.getCurrent());
                t2.next();
            }
        }
    }

    public void log() {
        int i=0;
        for(FretEvent ev: events){
            Log.d(TAG, "EV:" + (i++)+" "+ev.dbg());
        }
    }
}
