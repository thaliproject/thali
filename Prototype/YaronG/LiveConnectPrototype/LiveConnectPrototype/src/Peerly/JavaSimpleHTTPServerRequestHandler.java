/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Peerly;

import java.util.Map;

/**
 *
 * @author yarong
 * This is just used for local testing
 */
public class JavaSimpleHTTPServerRequestHandler implements SimpleHTTPServer.SimpleRequestHandler
{
    public JavaSimpleHTTPServerRequestHandler()
    {
        super();
    }
    
    @Override
    public SimpleResponse handler(String method, String uri,  Map<String, String> queryParams, Map<String, String> headers, String requestBody) {
        String htmlResponse = "<html><body><p>Method: " + method + "</p><p>uri: " + uri + "</p>";
        htmlResponse += StringMapToHtml(queryParams, "Request URI Query Params") + StringMapToHtml(headers, "Request Headers");
        htmlResponse += "<h2>Request Body</h2><p>" + requestBody + "</p></body></html>";
        return new SimpleResponse(200, "text/html", htmlResponse);
    }
    
    private String StringMapToHtml(Map<String, String> map, String title)
    {
        String htmlResponse = "<h2>" + title + "</h2>";
        for(Map.Entry<String, String> entry : map.entrySet())
        {
            htmlResponse += "<p>" + entry.getKey() + " = " + entry.getValue() + "</p>";
        }
        return htmlResponse;
    }
}
