/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Peerly;

import java.util.Map;
import netscape.javascript.JSException;
import netscape.javascript.JSObject;

/**
 *
 * @author yarong
 */
public class JavascriptSimpleHTTPServerRequestHandler implements SimpleHTTPServer.SimpleRequestHandler
{
    private String callBackName;
    private JSObject window;

    public JavascriptSimpleHTTPServerRequestHandler(String callBackName, JSObject window)
    {
        this.callBackName = callBackName;
        this.window = window;
    }

    @Override
    public SimpleResponse handler(String method, String uri, Map<String, String> queryParams, Map<String, String> headers, String requestBody) {
        JSObject responseObject = (JSObject) window.call(callBackName, new Object[] { (Object)method, (Object)uri, (Object)StringMapToJson(queryParams), (Object)StringMapToJson(headers), (Object)requestBody } );
        int responseCode = ((Double)responseObject.getMember("responseCode")).intValue();
        String mimeType = PropertyToString(responseObject, "responseMIMEType");
        String responseBody = PropertyToString(responseObject, "responseBody");
        return new SimpleResponse(responseCode, mimeType, responseBody);
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
