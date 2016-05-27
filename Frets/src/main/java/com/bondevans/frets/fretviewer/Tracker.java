package com.bondevans.frets.fretviewer;

import com.bondevans.frets.fretview.FretEvent;

import java.util.List;

public class Tracker {
    List<FretEvent> events;
    int ticksTotal;
    boolean end;
    int currentPos;

    public Tracker (List<FretEvent> events){
        this.events = events;
        ticksTotal=0;
        end=false;
        currentPos=0;
    }
    public void first(){
        currentPos=0;
        ticksTotal=0;
        if((currentPos+1)>= events.size()) {
            end = true;
        }
        else {
            ticksTotal+=events.get(currentPos).getTicks();
            end = false;
        }
    }
    public void next(){
        if((currentPos+1)>= events.size()) {
            end = true;
        }
        else {
            ++currentPos;
            ticksTotal+=events.get(currentPos).getTicks();
            end = false;
        }
    }
    public boolean lessThan(Tracker tracker){
        return(ticksTotal<tracker.getTicksTotal());
    }

    private int getTicksTotal() {
        return ticksTotal;
    }

    public FretEvent getCurrent() {
        return events.get(currentPos);
    }
}
