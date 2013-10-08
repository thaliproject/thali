package com.codeplex.peerly.browsertest;

import com.codeplex.peerly.browser.JsonNanoHTTPDJavascriptInterface;
import com.codeplex.peerly.org.json.JSONObject;
import netscape.javascript.JSException;
import netscape.javascript.JSObject;

/**
 * Accepts a request call intended for Javascript from the server and sends the request body back as a response.
 */
public class ServerJSObject extends JSObject {
    private JsonNanoHTTPDJavascriptInterface jsonNanoHTTPDJavascriptInterface;
    private String callbackName;
    private int port;

    public void SetServer(JsonNanoHTTPDJavascriptInterface jsonNanoHTTPDJavascriptInterface1, int port, String callbackName) {
        this.jsonNanoHTTPDJavascriptInterface = jsonNanoHTTPDJavascriptInterface1;
        this.port = port;
        this.callbackName = callbackName;
    }

    @Override
    public Object call(String s, Object[] objects) throws JSException {
        if (s.equals(callbackName) == false) {
            throw new RuntimeException("Wow. Huh?");
        }

        if (objects.length != 1) {
            throw new RuntimeException("Too many objects.");
        }

        String requestObject = (String)objects[0];
        JSONObject request = new JSONObject(requestObject);
        JSONObject response = new JSONObject();
        response.put("responseCode", 200);
        response.put("responseMIMEType", "Application/JSON");
        response.put("responseBody", requestObject);
        jsonNanoHTTPDJavascriptInterface.setResponse(port, response.toString());
        return null;
    }

    @Override
    public Object eval(String s) throws JSException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Object getMember(String s) throws JSException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setMember(String s, Object o) throws JSException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeMember(String s) throws JSException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Object getSlot(int i) throws JSException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setSlot(int i, Object o) throws JSException {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
