package com.bondevans.fretboard.midi.event.meta;

import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;

import com.bondevans.fretboard.midi.event.MidiEvent;

public class GenericMetaEvent extends MetaEvent
{
    private static final String TAG = "GenericMetaEvent";
    private byte[] mData;

    protected GenericMetaEvent(long tick, long delta, MetaEventData info)
    {
        super(tick, delta, info.type, info.length);

        mData = info.data;

        Log.d(TAG, "Warning: GenericMetaEvent used because type (" + info.type + ") wasn't recognized or unexpected data length (" + info.length.getValue() + ") for type.");
    }

    @Override
    protected int getEventSize()
    {
        return 1 + 1 + mLength.getByteCount() + mLength.getValue();
    }

    @Override
    protected void writeToFile(OutputStream out) throws IOException
    {
        super.writeToFile(out);
        out.write(mLength.getBytes());
        out.write(mData);
    }

    @Override
    public int compareTo(MidiEvent other)
    {
        if(mTick != other.getTick())
        {
            return mTick < other.getTick() ? -1 : 1;
        }
        if(mDelta.getValue() != other.getDelta())
        {
            return mDelta.getValue() < other.getDelta() ? 1 : -1;
        }

        return 1;
    }

}
