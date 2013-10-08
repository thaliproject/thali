package com.codeplex.peerly.browsertest;

import netscape.javascript.JSException;
import netscape.javascript.JSObject;

/**
 * Used to test the LiveConnectJsonXmlHTTPRequest object
 */
public class ClientJSObject extends JSObject {
    private String requestJsonString;

    public ClientJSObject(String requestJsonString) {
        super();
        this.requestJsonString = requestJsonString;
    }

    @Override
    public Object call(String s, Object[] objects) throws JSException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Object eval(String s) throws JSException {
        // For now we'll ignore the response
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
