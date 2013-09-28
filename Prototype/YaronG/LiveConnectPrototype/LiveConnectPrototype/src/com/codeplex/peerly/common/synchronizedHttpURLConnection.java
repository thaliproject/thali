package com.codeplex.peerly.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * Near as I can tell there is no guarantee that a httpURLConnection object is thread safe. But I need to call it
 * across multiple threads. My solution then is to wrap it in a bunch of synchronization statements. Not the most
 * performant solution but in practice I don't think it will matter.
 */
public class SynchronizedHttpURLConnection {
    private HttpURLConnection httpURLConnection = null;

    public SynchronizedHttpURLConnection(URL url) throws IOException {
        httpURLConnection = (HttpURLConnection) url.openConnection();
    }

    public synchronized void setRequestMethod(String method) throws ProtocolException {
        httpURLConnection.setRequestMethod(method);
    }

    public synchronized String getRequestProperty(String header)
    {
        return httpURLConnection.getRequestProperty(header);
    }

    public synchronized void setRequestProperty(String header, String value)
    {
        httpURLConnection.setRequestProperty(header, value);
    }

    public synchronized void disconnect()
    {
        httpURLConnection.disconnect();
    }

    public synchronized int getResponseCode() throws IOException {
        return httpURLConnection.getResponseCode();
    }

    public synchronized InputStream getInputStream() throws IOException {
        return httpURLConnection.getInputStream();
    }

    public synchronized String getHeaderField(String header)
    {
        return httpURLConnection.getHeaderField(header);
    }

    public synchronized void setDoOutput(Boolean newValue)
    {
        httpURLConnection.setDoOutput(newValue);
    }

    public synchronized void setFixedLengthStreamingMode(int contentLength)
    {
        httpURLConnection.setFixedLengthStreamingMode(contentLength);
    }

    /**
     * Synchronized is no protection from stupid. If two threads both call getOutputStream and
     * start trying to write to the output stream I have no idea what will happen. So don't do that.
     */
    public synchronized OutputStream getOutputStream() throws IOException {
        return httpURLConnection.getOutputStream();
    }

    public synchronized Map<String, List<String>> getHeaderFields()
    {
        return httpURLConnection.getHeaderFields();
    }
}
