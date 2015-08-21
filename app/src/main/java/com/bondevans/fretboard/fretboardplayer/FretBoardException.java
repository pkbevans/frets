package com.bondevans.fretboard.fretboardplayer;

public class FretBoardException extends Exception{
    /**
     *
     */
    private static final long serialVersionUID = -875551211184945633L;
    String errMsg="";
    public FretBoardException(String message){
        errMsg = message;
    }
    /* (non-Javadoc)
     * @see java.lang.Throwable#getMessage()
     */
    @Override
    public String getMessage() {
        return errMsg;
    }

}
