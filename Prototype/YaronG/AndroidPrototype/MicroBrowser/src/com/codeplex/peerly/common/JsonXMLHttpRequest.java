package com.codeplex.peerly.common;

import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * The underlying class that implements our JSON wrapper, SSL mutual auth enabled XMLHTTPRequest object. It only
 * implements the methods and functionality needed by PouchDB.
 *
 * This class only expects the readyState field to be accessed simultaneously from multiple threads. Otherwise
 * this class is not thread safe.
 */
public abstract class JsonXMLHttpRequest extends JsonXMLHttpRequestStateManager {
    private SynchronizedHttpURLConnection synchronizedHttpURLConnection = null;

    public int getStatus()
    {
        if (getReadyState().stateNumber() < 2)
        {
            throw new RuntimeException("status can only be found when ready state is 2 or higher.");
        }

        try {
            return synchronizedHttpURLConnection.getResponseCode();
        } catch (IOException e) {
            throw new RuntimeException(e.toString());
        }
    }

    // TODO: When we support binary we need to put in support for the response property

    public String getResponseText()
    {
        return getResponseBody();
    }

    public String GetResponseHeader(String header)
    {
        // We really don't support anything more sophisticated with
        if (getReadyState().stateNumber() < ReadyState.HEADERS_RECEIVED.stateNumber())
        {
            return null;
        }

        return synchronizedHttpURLConnection.getHeaderField(header);
    }

    public abstract void OnReadyStateChange(ReadyState readyState);

    public void open(String method, String url)
    {
        if (getReadyState() != ReadyState.UNSENT)
        {
            abort();
        }

        try {
            URL urlObject = new URL(url);
            // According to http://stackoverflow.com/questions/10116961/can-you-explain-the-httpurlconnection-connection-process creating the
            // httpURLConnection object causes a connection to be opened and so matches with ReadyState 1.
            synchronizedHttpURLConnection = new SynchronizedHttpURLConnection(urlObject);
            synchronizedHttpURLConnection.setRequestMethod(method);
            setReadyState(ReadyState.OPENED);
        } catch (MalformedURLException e) {
            abort();
            throw new RuntimeException(e.toString());
        } catch (IOException e) {
            abort();
            throw new RuntimeException(e.toString());
        }
    }

    public void setRequestHeader(String header, String value)
    {
        if (getReadyState() != ReadyState.OPENED)
        {
            throw new RuntimeException("setRequestHeader MUST be called after open and before send.");
        }

        String currentRequestHeaderValue = synchronizedHttpURLConnection.getRequestProperty(header);

        String newRequestHeaderValue = currentRequestHeaderValue == null ? value : currentRequestHeaderValue + "," + value;

        synchronizedHttpURLConnection.setRequestProperty(header, newRequestHeaderValue);
    }

    public void abort()
    {
        if (synchronizedHttpURLConnection != null)
        {
            synchronizedHttpURLConnection.disconnect();
        }

        synchronizedHttpURLConnection = null;
        setReadyState(ReadyState.UNSENT);
    }

    public void send(String data)
    {
        if (getReadyState() != ReadyState.OPENED)
        {
            throw new RuntimeException("You must have successfully called open before calling send.");
        }

        // TODO: We currently don't support transfer encoding but that is really dumb, we need to fix it. This
        // setting keeps the server from using a transfer encoding.
        synchronizedHttpURLConnection.setRequestProperty("Accept-Encoding", "identity");

        final String finalData = data;

        new Thread(new Runnable()
        {
            @Override
            public void run() {
                if (finalData != null)
                {
                    synchronizedHttpURLConnection.setDoOutput(true);
                    try {
                        OutputStream out = synchronizedHttpURLConnection.getOutputStream();
                        out.write(finalData.getBytes("UTF-8"));
                    } catch (IOException e) {
                        // TODO: There is an eventListener interface once can subscribe to on the XMLHttpRequest object
                        // to hear about errors. But PouchDB doesn't use it. So I'm not quite sure what Pouch will do
                        // if there is an error while sending/receiving a request. But for now we'll just abort.
                        // TODO: We really need a logging framework so these errors at least get logged
                    }
                }

                // The following is a sleezy and possibly just plain wrong trick. I want to see if the status and
                // response headers are available so I just ask for all the headers. But if the server is using
                // chunked headers then heck if I know how httpURLConnection models that. But whatever. The following
                // call is supposed to block until the headers are available.
                synchronizedHttpURLConnection.getHeaderFields();
                setReadyState(ReadyState.HEADERS_RECEIVED);

                // TODO: At some point we should support chunking and readyState 3 but not today.

                // TODO: If the server goes bad this call could stall forever. Not good.

                // TODO: This only works because we don't support servers that support transfer encoding
                int contentLength = Integer.parseInt(synchronizedHttpURLConnection.getHeaderField("content-length"));
                String contentType = synchronizedHttpURLConnection.getHeaderField("content-type");

                // TODO: Obviously we need to support more than just JSON!
                // MIME types are case insensitive
                if (contentLength > 0 && contentType.equalsIgnoreCase("Application/JSON") == false)
                {
                    throw new RuntimeException("For now we only support Application/JSON but we received " + contentType);
                }

                try {
                     setReadyStateToDone(Utilities.StringifyInputStream(contentLength, synchronizedHttpURLConnection.getInputStream()));
                } catch (IOException e) {
                    throw new RuntimeException(e.toString());
                }
            }
        });
    }
}
