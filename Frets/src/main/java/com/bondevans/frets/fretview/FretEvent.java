package com.bondevans.frets.fretview;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A FretEvent is an event that impacts on the Fretboard
 */
public class FretEvent extends FretBase {
//    private static final String TAG = FretEvent.class.getSimpleName();
    static final int MAX_BEND = 10;
    private static final String ELEMENT_EVENT = "ev";
    private static final String EVENT_ELEMENT_OPEN = "<"+ELEMENT_EVENT+">";
    private static final String EVENT_ELEMENT_CLOSE = "</"+ELEMENT_EVENT+">";
    // THESE ELEMENTS ARE WRITTEN OUT IN/READ IN FROM TOSTRING()
    public int deltaTicks;
    public int tempo;
    public int bend;
    public List<FretNote> fretNotes;
    public int track;
    public int totalTicks;
    private int clickEvent;
    private boolean hasOnNotes;
    // INTERNAL PROPERTIES - NOT WRITTEN OUT IN/READ IN FROM TOSTRING()

    /**
     * Constructor
     * @param deltaTicks time in ticks since previous event
     * @param fretNotes array of notes to play at the same time
     * @param tempo New tempo if > 0
     * @param bend Apply bend if > 0
     * @param totalTicks total ticks for this track as of this event
     */
    public FretEvent(int deltaTicks, List<FretNote> fretNotes, int tempo, int bend, int totalTicks) {
        this.deltaTicks = deltaTicks;
        this.tempo = tempo;
        this.bend = bend;
        this.fretNotes = fretNotes;
        this.totalTicks = totalTicks;

        this.hasOnNotes = false;
        for(FretNote fretNote: fretNotes){
            if(fretNote.on){
                this.hasOnNotes = true;
                break;
            }
        }
    }

    /**
     * Constructor - creates a click event - no notes
     * @param clickEvent - Click event number
     * @param deltaTicks - number of ticks delay
     * @param totalTicks - total ticks
     */
    public FretEvent(int clickEvent, int deltaTicks, int totalTicks){
        this.clickEvent = clickEvent;
        this.deltaTicks = deltaTicks;
        this.totalTicks = totalTicks;
        this.fretNotes =  new ArrayList<>();    // empty list of notes
        this.bend = 0;
        this.tempo = 0;
        this.hasOnNotes = false;
    }

    public FretEvent(String ev) {
//        Log.d(TAG, "ev");
        fretNotes = new ArrayList<>();
        this.deltaTicks = getTagInt(ev, ATTR_DELTATICKS);
        this.tempo = getTagInt(ev, ATTR_TEMPO);
        this.bend = getTagInt(ev, ATTR_BEND);
        this.track = getTagInt(ev, ATTR_EV_TRACK);
        this.totalTicks = getTagInt(ev, ATTR_EV_TOTALTICKS);
        this.clickEvent = getTagInt(ev, ATTR_EV_CLICKEVENT);
        this.hasOnNotes = getTagInt(ev, ATTR_EV_HASONNOTES)==1;

        fretNotes = getNotes(ev);
    }

    private static Pattern notePattern = Pattern.compile("<"+FretNote.ELEMENT_NOTE+"[^>]*>(.*?)</"+FretNote.ELEMENT_NOTE+">",
            Pattern.DOTALL|Pattern.CASE_INSENSITIVE);

    private List<FretNote> getNotes(String ev) {
        List<FretNote> fretNotes = new ArrayList<>();
        Matcher matcher = notePattern.matcher(ev);

        // look for contents of <nt></nt>
        while (matcher.find()) {
            String note = matcher.group(1);
//            Log.d(TAG, "HELLO found <"+FretNote.ELEMENT_NOTE+"> tag: [" + note + "]");
            fretNotes.add(new FretNote(note));
        }

        return fretNotes;
    }

    /**
     * Output contents of FretEvent to XML - used to serialize t disk
     * @return XML-like string of class contents
     */
    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder(EVENT_ELEMENT_OPEN+
                attr(ATTR_DELTATICKS, deltaTicks)+
                attr(ATTR_EV_TRACK, track) +
                attr(ATTR_TEMPO, tempo) +
                attr(ATTR_BEND, bend) +
                attr(ATTR_EV_TOTALTICKS, totalTicks) +
                attr(ATTR_EV_CLICKEVENT, clickEvent) +
                attr(ATTR_EV_HASONNOTES, hasOnNotes)
        );
        for(FretNote note: fretNotes){
            sb.append(note.toString());
        }
        sb.append(EVENT_ELEMENT_CLOSE);
        return sb.toString();
    }

    /**
     * Returns true if this event has ON notes (or false if it only has OFF notes)
     *
     * @return True if there are ON notes in this event, else False
     */
    public boolean hasOnNotes() {
        return hasOnNotes;
    }

    public int getTicks() {
        return deltaTicks;
    }

    public void setTicks(int ticks) {
//        Log.d(TAG, "track:"+track+" setTicks:"+ticks);
        this.deltaTicks = ticks;
    }
    public String dbg(){
        return "TRACK:"+track+" TICKS:"+ deltaTicks;
    }
    /*
     * Returns a List of notes that are turned ON on this event
     */
    public List<Integer> getOnNotes(){
        List<Integer> notes = new ArrayList<>();
        for (FretNote fretNote : fretNotes) {
            if (fretNote.on) {
                notes.add(fretNote.note);
            }
        }
        return notes;
    }

    public boolean isClickEvent() {
        return clickEvent>0;
    }

    public int getClickEvent() {
        return clickEvent;
    }
    public String toLogString(){
        return
        " ticks:" +deltaTicks+
        " tempo:" +tempo+
        " bend:" +bend+
        " notes:" +fretNotes.size()+
        " track:"  +track+
        " totalTicks:" +totalTicks+
        " clickEvent:" +clickEvent+
        " " + (hasOnNotes?" HAS-ONNOTES ": " NO-ONNOTES");
    }
}

