package com.codeplex.peerly.browser;

import com.codeplex.peerly.common.JsonNanonHTTPDJavascriptBridge;
import com.codeplex.peerly.org.json.JSONObject;
import netscape.javascript.JSObject;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class managed all the instances of a HTTP server created by an applet. The class is not thread safe as it assumes
 * it will only be called from the main UX thread inside the browser's Javascript engine.
 */
public class JsonNanoHTTPDJavascriptInterface implements JsonNanonHTTPDJavascriptBridge {
    private Map<Integer, LiveConnectJsonNanoHTTPD> servers; // Indexed by port # they are listening on
    private JSObject window;

    public JsonNanoHTTPDJavascriptInterface(JSObject window) {
        this.window = window;
        servers = new HashMap<Integer, LiveConnectJsonNanoHTTPD>();
    }

    @Override
    public boolean isHttpServerRunning(int port)
    {

        if (servers.containsKey(port) == false)
        {
            return false;
        }

        final int finalPort = port;
        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return servers.get(finalPort).isAlive();
            }
        });
    }

    @Override
    public void startHttpServer(int port, String requestHandlerCallBack)
    {
        if (servers.containsKey(port))
        {
            throw new RuntimeException("The server is already running.");
        }

        final int finalPort = port;
        final String finalRequestHandlerCallBack = requestHandlerCallBack;
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                try
                {
                    LiveConnectJsonNanoHTTPD liveConnectJsonNanoHTTPD = new LiveConnectJsonNanoHTTPD(finalPort, finalRequestHandlerCallBack, window);
                    servers.put(finalPort, liveConnectJsonNanoHTTPD);
                    liveConnectJsonNanoHTTPD.start();
                }
                catch (Exception e) {
                    Logger.getLogger(PeerlyApplet.class.getName()).log(Level.SEVERE, null, e);
                    throw new RuntimeException(e.toString()); // At this point the exception will be caught in Javascript and
                    // depending on environment the string is visible but not an internal
                    // exception.
                }

                return null;
            }
        });
    }

    @Override
    public void stopHttpServer(int port)
    {
        if (servers.containsKey(port) == false)
        {
            return;
        }

        final int finalPort = port;

        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                LiveConnectJsonNanoHTTPD liveConnectJsonNanoHTTPD = servers.get(finalPort);
                liveConnectJsonNanoHTTPD.stop();
                servers.remove(finalPort);
                return null;
            }
        });
    }

    @Override
    public void setResponse(int port, String responseJsonString)
    {
        servers.get(port).SetResponse(new JSONObject(responseJsonString));
    }
}
