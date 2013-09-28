package com.codeplex.peerly.common;

/**
 * These are the readystate values for XMLHTTPRequest
 */
public enum ReadyState {
    UNSENT(0),
    OPENED(1),
    HEADERS_RECEIVED(2),
    LOADING(3),
    DONE(4);

    private final int stateNumberValue;
    ReadyState(int stateNumber)
    {
        this.stateNumberValue = stateNumber;
    }

    public int stateNumber()
    {
        return stateNumberValue;
    }
}
