package com.codeplex.peerly.common;

/**
 * Created with IntelliJ IDEA.
 * User: yarong
 * Date: 9/30/13
 * Time: 3:55 PM
 * To change this template use File | Settings | File Templates.
 */
public interface JsonXmlHTTPRequestJavascriptBridge {
    public void sendJsonXmlHTTPRequest(String peerlyXMLHttpRequestManagerObjectName, int key, String requestJsonString, String proxyIPorDNS, int proxyPort);
}
