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
public interface JsonNanonHTTPDJavascriptBridge {
    public boolean isHttpServerRunning(int port);
    public void startHttpServer(int port, String requestHandlerCallBack);
    public void stopHttpServer(int port);
    public void setResponse(int port, String responseJsonString);
}
