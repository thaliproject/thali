/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Peerly;

import fi.iki.elonen.NanoHTTPD;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author yarong
 */
public class SimpleHTTPServer extends NanoHTTPD {
    private final SimpleRequestHandler simpleRequestHandler;

    public interface SimpleRequestHandler {
        public SimpleResponse handler(String method, String uri, Map<String, String> queryParams, Map<String, String> headers, String requestBody);
    }
    
    public SimpleHTTPServer(int port, SimpleRequestHandler handler)
    {
        super(port);
        this.simpleRequestHandler = handler;
    }
    
    @Override
    public Response serve(HTTPSession session) {
        InputStream inputStream = session.getInputStream();
        Map<String, String> headers = session.getHeaders();
        int size;
        String requestBody = null;
        
        // This is wrong on so many levels. First, we should validate that
        // we want to deal with the content we have given. Second, this
        // doesn't deal with chunked content. Third, we don't check that
        // the content is actually UTF-8. Fourth, we don't check the MIME
        // type. Fifth, we actually store everything in memory rather than
        // processing it as a stream so we can use RAM better. Etc.
        if (headers.containsKey("content-length"))
        {
            size = Integer.parseInt(headers.get("content-length"));
            byte[] requestBodyByteArray = new byte[size];
            int bytesRead;
            try {
                bytesRead = inputStream.read(requestBodyByteArray);
                if (bytesRead != size)
                {
                    throw new RuntimeException();
                }
            } catch (IOException ex) {
                Logger.getLogger(SimpleHTTPServer.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException();
            }

            try {
                requestBody = new String(requestBodyByteArray,"UTF-8");
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(SimpleHTTPServer.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException();
            }
        }
        
        return simpleRequestHandler.handler(MethodEnumToMethodString(session.getMethod()), session.getUri(), session.getParms(), session.getHeaders(), requestBody).response;
    }
    
    private String MethodEnumToMethodString(Method methodEnum)
    {
        switch(methodEnum)
        {
            case GET:
                return "GET";
            case PUT:
                return "PUT";
            case POST:
                return "POST";
            case DELETE:
                return "DELETE";
            case HEAD:
                return "HEAD";
            default:
                throw new RuntimeException();
        }
    }
}
