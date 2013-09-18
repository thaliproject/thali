/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Peerly;

import java.applet.Applet;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import netscape.javascript.*;

/**
 *
 * @author yarong
 */
public class SimpleJavascriptHttpServerApplet extends Applet {
    private SimpleHTTPServer server;
    private JavascriptSimpleHTTPServerRequestHandler javascriptSimpleHTTPServerRequestHandler;
    
    
    public String helloWorldField = "I am a field!";
    public String helloWorldMethod()
    {
        return "I am a method!";
    }
    
    public boolean isHttpServerRunning()
    {
        return server.isAlive();
    }
    
    public void startHttpServer(int port, String requestHandlerCallBack)
    {
        if (server != null)
        {
            throw new RuntimeException();
        }
        
        final int finalPort = port;
        javascriptSimpleHTTPServerRequestHandler = new JavascriptSimpleHTTPServerRequestHandler(requestHandlerCallBack, JSObject.getWindow(this));
        final JavascriptSimpleHTTPServerRequestHandler finalJavascriptSimpleHTTPServerRequestHandler = javascriptSimpleHTTPServerRequestHandler;
        
        AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                server = new SimpleHTTPServer(finalPort, finalJavascriptSimpleHTTPServerRequestHandler);
                try
                {
                    server.start();
                }
                catch (IOException ioe)
                {
                    throw new RuntimeException();
                }     
                
                return null;
            }
        });
    }
    
    public void stopHttpServer()
    {
        if (server == null)
        {
            throw new RuntimeException();
        }
        
        AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                server.stop();
                return null;
            }
        });
    }
    
    public void setResponse(JSObject responseObject)
    {
        javascriptSimpleHTTPServerRequestHandler.SetResponse(responseObject);
    }
}

