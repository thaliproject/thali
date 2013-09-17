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
public class Testing extends Applet {
    private SimpleHTTPServer server;
    
    
    public String helloWorldField = "I am a field!";
    public String helloWorldMethod()
    {
        return "I am a method!";
    }
    public void callMe(String callBackName)
    {
        JSObject window = JSObject.getWindow(this);
        int count = 0;
        window.call(callBackName, new Object[] { (Object)count });
        count = 10;
        window.call(callBackName, new Object[] { (Object)count});
    }
    
    public void startHttpServer(int port, String requestHandlerCallBack)
    {
        if (server != null)
        {
            throw new RuntimeException();
        }
        
        final int finalPort = port;
        final String finalRequestHandlerCallBack = requestHandlerCallBack;
        final JSObject finalWindow = JSObject.getWindow(this);
        
        AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                server = new SimpleHTTPServer(finalPort, new JavascriptSimpleHTTPServerRequestHandler(finalRequestHandlerCallBack, finalWindow));
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
}
