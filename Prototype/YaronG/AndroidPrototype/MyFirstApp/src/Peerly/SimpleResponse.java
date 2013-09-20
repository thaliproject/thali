/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Peerly;

import fi.iki.elonen.NanoHTTPD;
import static fi.iki.elonen.NanoHTTPD.MIME_HTML;

/**
 *
 * @author yarong
 */
    public class SimpleResponse extends NanoHTTPD.Response {
        
        public SimpleResponse(int responseCode, String mimeType, String responseBody)
        {         
            super(SighReallyDoIHaveToDoThis(responseCode), mimeType == null ? MIME_HTML : mimeType, responseBody);
        }
        
        private static NanoHTTPD.Response.Status SighReallyDoIHaveToDoThis(int responseCode)
        {
            switch(responseCode)
            {
                case 200:
                    return NanoHTTPD.Response.Status.OK;
                case 201:
                    return NanoHTTPD.Response.Status.CREATED;
                case 202:
                    return NanoHTTPD.Response.Status.ACCEPTED;
                case 204:
                    return NanoHTTPD.Response.Status.NO_CONTENT;
                case 206:
                    return NanoHTTPD.Response.Status.PARTIAL_CONTENT;
                case 301:
                    return NanoHTTPD.Response.Status.REDIRECT;
                case 304:
                    return NanoHTTPD.Response.Status.NOT_MODIFIED;
                case 400:
                    return NanoHTTPD.Response.Status.BAD_REQUEST;
                case 401:
                    return NanoHTTPD.Response.Status.UNAUTHORIZED;
                case 403:
                    return NanoHTTPD.Response.Status.FORBIDDEN;
                case 404:
                    return NanoHTTPD.Response.Status.NOT_FOUND;
                case 416:
                    return NanoHTTPD.Response.Status.RANGE_NOT_SATISFIABLE;
                case 500:
                    return NanoHTTPD.Response.Status.INTERNAL_ERROR;
                default:
                    throw new RuntimeException();
            }
        }
    }
    
