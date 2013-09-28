package com.codeplex.peerly.common;

import java.util.concurrent.Callable;

/**
 * Manages state for a XMLHTTPRequest
 * The existence of this class indicates that I probably screwed up the design of the class. This is where I manager
 * various state transitions in order to prevent myself from accidentally screwing up a state transition by
 * manipulating one of the state variables directly inside of JsonXMLHttpRequest.
 */
public abstract class JsonXMLHttpRequestStateManager {
    private ReadyState readyState = ReadyState.UNSENT; // NEVER SET THIS DIRECTLY! CALL setReadyState INSTEAD!
    private String responseBody = null;

    public abstract void OnReadyStateChange(ReadyState readyState);

    public void setReadyState(ReadyState readyState)
    {
        if (readyState == ReadyState.DONE)
        {
            throw new RuntimeException("readyState should only be set to DONE vis the setReadStateToDone method.");
        }
        this.readyState = readyState;
        responseBody = null;
        OnReadyStateChange(readyState);
    }

    public void setReadyStateToDone(String responseBody)
    {
        readyState = ReadyState.DONE;
        this.responseBody = responseBody;
        OnReadyStateChange(readyState);
    }

    public String getResponseBody()
    {
        return responseBody;
    }

    public ReadyState getReadyState()
    {
        return readyState;
    }
}
