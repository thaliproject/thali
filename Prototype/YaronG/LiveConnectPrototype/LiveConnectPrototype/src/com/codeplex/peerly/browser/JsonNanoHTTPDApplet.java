/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codeplex.peerly.browser;

import com.codeplex.peerly.common.JsonNanoHTTPDJavaScriptJavaHttpBridge;
import java.applet.Applet;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import netscape.javascript.*;
import org.json.JSONObject;

/**
 *
 * @author yarong
 */
public class JsonNanoHTTPDApplet extends Applet implements JsonNanoHTTPDJavaScriptJavaHttpBridge {
    private LiveConnectJsonNanoHTTPD server;
    
    @Override
    public boolean isHttpServerRunning()
    {
        if (server == null)
        {
            return false;
        }
        
        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return server.isAlive();
            }
        });
    }
    
    @Override
    public void startHttpServer(int port, String requestHandlerCallBack)
    {
        if (server != null)
        {
            throw new RuntimeException("The server is already running.");
        }
        
        final int finalPort = port;
        final String finalRequestHandlerCallBack = requestHandlerCallBack;
        final JSObject finalWindow = JSObject.getWindow(this);
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                server = new LiveConnectJsonNanoHTTPD(finalPort, finalRequestHandlerCallBack, finalWindow);
                try
                {
                    server.start();
                }
                catch (IOException ioe)
                {
                    throw new RuntimeException(ioe.toString());
                }     
                
                return null;
            }
        });
    }
    
    @Override
    public void stopHttpServer()
    {
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                if (server != null)
                {
                    server.stop();
                }
                return null;
            }
        });
    }
    
    @Override
    public void setResponse(String responseJsonString)
    {
        server.SetResponse(new JSONObject(responseJsonString));
    }
}

