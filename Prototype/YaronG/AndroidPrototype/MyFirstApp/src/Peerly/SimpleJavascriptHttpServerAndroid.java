/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Peerly;

import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import java.io.IOException;

/**
 *
 * @author yarong
 */
public class SimpleJavascriptHttpServerAndroid {
    private SimpleHTTPServer server;
    private WebView webView;
    private AndroidSimpleHTTPServerRequestHandler androidSimpleHTTPServerRequestHandler;

    public SimpleJavascriptHttpServerAndroid(WebView webView)
    {
        this.webView = webView;
    }

    @JavascriptInterface
    public String helloWorldMethod()
    {
        return "I am a method!";
    }

    @JavascriptInterface
    public boolean isHttpServerRunning()
    {
        return server.isAlive();
    }

    @JavascriptInterface
    public void startHttpServer(int port, String requestHandlerCallBack)
    {
        if (server != null)
        {
            throw new RuntimeException();
        }

        androidSimpleHTTPServerRequestHandler = new AndroidSimpleHTTPServerRequestHandler(requestHandlerCallBack, webView);

        server = new SimpleHTTPServer(port, androidSimpleHTTPServerRequestHandler);
        try
        {
            server.start();
        }
        catch (IOException ioe)
        {
            throw new RuntimeException();
        }
    }

    @JavascriptInterface
    public void stopHttpServer()
    {
        if (server == null)
        {
            throw new RuntimeException();
        }

        server.stop();
    }

    @JavascriptInterface
    public void setResponse(Object responseObject)
    {
        androidSimpleHTTPServerRequestHandler.SetResponse(responseObject);
    }
}

