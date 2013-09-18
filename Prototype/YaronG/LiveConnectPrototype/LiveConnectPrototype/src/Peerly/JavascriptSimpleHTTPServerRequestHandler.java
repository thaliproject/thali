/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Peerly;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import netscape.javascript.JSException;
import netscape.javascript.JSObject;

/**
 *
 * @author yarong
 */
public class JavascriptSimpleHTTPServerRequestHandler implements SimpleHTTPServer.SimpleRequestHandler
{
    private String requestCallBackName;
    private JSObject window;
    private JSObject responseObject;

    public JavascriptSimpleHTTPServerRequestHandler(String requestCallBackName, JSObject window)
    {
        this.requestCallBackName = requestCallBackName;
        this.window = window;
    }

    @Override
    public SimpleResponse handler(String method, String requestUriPath, Map<String, String> queryParams, Map<String, String> headers, String requestBody) {
        responseObject = null;
        window.call(requestCallBackName, new Object[] { (Object)method, (Object)requestUriPath, (Object)StringMapToJson(queryParams), (Object)StringMapToJson(headers), (Object)requestBody } );
        while(responseObject == null)
        {
            // TODO: Put some reasonable time out here so we don't get stuck in this loop for infinity
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                Logger.getLogger(JavascriptSimpleHTTPServerRequestHandler.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException();
            }            
        }
        
        // Unfortunately Chrome returns a Double while FireFox return an int, oh joy
        Object responseCodeObject = responseObject.getMember("responseCode");
        int responseCode;
        if (responseCodeObject instanceof Integer)
        {
            responseCode = (int)responseCodeObject;
        } else
        if (responseCodeObject instanceof Double)
        {
            responseCode = ((Double)responseObject.getMember("responseCode")).intValue();
        } else
        {
            throw new RuntimeException();
        }
        
        String mimeType = PropertyToString(responseObject, "responseMIMEType");
        String responseBody = PropertyToString(responseObject, "responseBody");
        SimpleResponse simpleResponse = new SimpleResponse(responseCode, mimeType, responseBody);

        //BUBUG: This is beyond dorky, obviously we should have a proper map to list any headers that are to be set but I don't need
        // that right now.
        String locationHeaderValue = PropertyToString(responseObject, "LocationHeader");
        if (locationHeaderValue != null)
        {
            simpleResponse.addHeader("Location", locationHeaderValue);
        }
        
        return simpleResponse;
    }
    
    public void SetResponse(JSObject responseObject)
    {
        this.responseObject = responseObject;
    }
    
    private String PropertyToString(JSObject responseObject, String propertyName)
    {
        try
        {
            return (String)responseObject.getMember(propertyName);
        }
        catch (JSException jse)
        {
            return null;
        }
    }
        
    private String StringMapToJson(Map<String, String> map)
    {
        boolean first = true;
        StringBuilder json = new StringBuilder("{");
        for (Map.Entry<String, String> entry : map.entrySet())
        {
            if (first == true)
            {
                first = false;
            } 
            else
            {
                json.append(",");
            }
            json.append("\"");
            json.append(entry.getKey());
            json.append("\":\"");
            json.append(entry.getValue());
            json.append("\"");            
        }
        json.append("}");
        return json.toString();
    }
}
