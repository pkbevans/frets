package com.bondevans.fretboard.fretview;

import com.bondevans.fretboard.utils.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A FretEvent is an event that impacts on the Fretboard
 */
public class FretEvent extends FretBase {
    private static final String TAG = FretEvent.class.getSimpleName();
    public static final String ELEMENT_EVENT = "ev";
    public static final String EVENT_ELEMENT_OPEN = "<"+ELEMENT_EVENT+">";
    public static final String EVENT_ELEMENT_CLOSE = "</"+ELEMENT_EVENT+">";
    private static final String ATTR_DELTATIME = "dt";
    private static final String ATTR_TEMPO = "te";
    int deltaTime;
    int tempo;
    List<FretNote> fretNotes;

    /**
     * Constructor
     * @param deltaTime time in ticks since previous event
     * @param fretNotes array of notes to play at the same time
     * @param tempo New tempo if > 0
     */
    public FretEvent(int deltaTime, List<FretNote> fretNotes, int tempo){
        this.deltaTime = deltaTime;
        this.tempo = tempo;
        this.fretNotes = fretNotes;
    }

    public FretEvent(String ev) {
//        Log.d(TAG, "ev=[" + ev + "]");
        fretNotes = new ArrayList<>();
        this.deltaTime = getTagInt(ev, ATTR_DELTATIME);
        this.tempo = getTagInt(ev, ATTR_TEMPO);
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
                attr(ATTR_TEMPO,tempo));
        for(FretNote note: fretNotes){
            sb.append(note.toString());
        }
        sb.append(EVENT_ELEMENT_CLOSE);
        return sb.toString();
    }
}

