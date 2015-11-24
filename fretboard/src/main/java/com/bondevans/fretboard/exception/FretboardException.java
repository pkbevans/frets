package com.bondevans.fretboard.exception;

public class FretboardException extends Exception{
    /**
     *
     */
    private static final long serialVersionUID = -875551211184945633L;
    String errMsg="";
    public FretboardException(String message){
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
