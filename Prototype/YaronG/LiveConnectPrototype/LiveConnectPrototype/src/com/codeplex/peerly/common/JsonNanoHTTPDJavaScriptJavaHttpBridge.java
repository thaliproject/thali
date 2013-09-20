/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codeplex.peerly.common;

import java.io.IOException;

/**
 *
 * @author yarong
 * This defines the methods that Javascript will expect to find on the HTTP
 * server object.
 */
public interface JsonNanoHTTPDJavaScriptJavaHttpBridge {
    public boolean isHttpServerRunning();
    public void startHttpServer(int port, String requestHandlerCallBack);
    public void stopHttpServer();
    public void setResponse(String responseJsonString);
}
