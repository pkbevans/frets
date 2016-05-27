package com.bondevans.frets.fretview;

import com.bondevans.frets.utils.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A FretEvent is an event that impacts on the Fretboard
 */
public class FretEvent extends FretBase {
    private static final String TAG = FretEvent.class.getSimpleName();
    public static final int MAX_BEND = 10;
    public static final String ELEMENT_EVENT = "ev";
    public static final String EVENT_ELEMENT_OPEN = "<"+ELEMENT_EVENT+">";
    public static final String EVENT_ELEMENT_CLOSE = "</"+ELEMENT_EVENT+">";
    // THESE ELEMENTS ARE WRITTEN OUT IN/READ IN FROM TOSTRING()
    int deltaTime;
    int tempo;
    public int bend;
    public List<FretNote> fretNotes;
    // INTERNAL PROPERTIES - NOT WRITTEN OUT IN/READ IN FROM TOSTRING()
    public int track;

    /**
     * Constructor
     * @param deltaTime time in ticks since previous event
     * @param fretNotes array of notes to play at the same time
     * @param tempo New tempo if > 0
     * @param bend Apply bend if > 0
     */
    public FretEvent(int deltaTime, List<FretNote> fretNotes, int tempo, int bend) {
        this.deltaTime = deltaTime;
        this.tempo = tempo;
        this.bend = bend;
        this.fretNotes = fretNotes;
    }

    public FretEvent(String ev) {
//        Log.d(TAG, "ev=[" + ev + "]");
        fretNotes = new ArrayList<>();
        this.deltaTime = getTagInt(ev, ATTR_DELTATIME);
        this.tempo = getTagInt(ev, ATTR_TEMPO);
        this.bend = getTagInt(ev, ATTR_BEND);
        fretNotes = getNotes(ev);
    }

    private static Pattern notePattern = Pattern.compile("<"+FretNote.ELEMENT_NOTE+"[^>]*>(.*?)</"+FretNote.ELEMENT_NOTE+">",
            Pattern.DOTALL|Pattern.CASE_INSENSITIVE);

    private List<FretNote> getNotes(String ev) {
        List<FretNote> fretNotes = new ArrayList<>();
        Matcher matcher = notePattern.matcher(ev);

        // look for contents of <ev></ev>
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
                attr(ATTR_DELTATIME,deltaTime)+
                attr(ATTR_TEMPO, tempo) +
                attr(ATTR_BEND, bend));
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
        for (FretNote fretNote : fretNotes) {
            if (fretNote.on) {
                return true;
            }
        }

        return false;
    }

    public int getTicks() {
        return deltaTime;
    }

    public void setTicks(int ticks) {
        Log.d(TAG, "track:"+track+" setTicks:"+ticks);
        this.deltaTime = ticks;
    }
    public String dbg(){
        return "TRACK:"+track+" TICKS:"+deltaTime;
    }
}

