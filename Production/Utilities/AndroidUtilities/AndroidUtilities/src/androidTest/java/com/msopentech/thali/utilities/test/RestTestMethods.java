package com.msopentech.thali.utilities.test;

import com.jayway.jsonassert.JsonAssert;
import com.jayway.jsonassert.JsonAsserter;
import com.msopentech.thali.local.utilities.UtilitiesTestCase;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.hamcrest.Matchers.equalTo;

/**
 * In our happy place we would just use rest-assured but unfortunately it doesn't work in Android due to a conflict
 * with their Apache Client so here is our hacked up version for now.
 */
public class RestTestMethods extends UtilitiesTestCase {
    public static void testOptions(String url, Map<String, String> headersToAdd, int statusCode,
                                   Map<String, String> headersToFind) throws IOException {
        HttpOptions httpOptionsRequest = new HttpOptions(url);
        testMethod(httpOptionsRequest, headersToAdd, statusCode, headersToFind);
    }

    public static void testGet(String url, Map<String, String> headersToAdd, int statusCode,
                               Map<String, String> headersToFind) throws IOException {
        HttpGet httpGet = new HttpGet(url);
        testMethod(httpGet, headersToAdd, statusCode, headersToFind);
    }

    public static void testGet(String url, Map<String, String> headersToAdd, int statusCode,
                               Map<String, String> headersToFind, Map<String, Object> jsonPathAndValue)
            throws IOException {
        HttpGet httpGet = new HttpGet(url);
        HttpResponse httpResponse = testMethod(httpGet, headersToAdd, statusCode, headersToFind);
        testJson(httpResponse, jsonPathAndValue);
    }

    public static void testDelete(String url, Map<String, String> headersToAdd, int statusCode,
                                  Map<String, String> headersToFind) throws IOException {
        HttpDelete httpDelete = new HttpDelete(url);
        testMethod(httpDelete, headersToAdd, statusCode, headersToFind);
    }

    public static void testPut(String url, Map<String, String> headersToAdd, int statusCode,
                               Map<String, String> headersToFind) throws IOException {
        HttpPut httpPut = new HttpPut(url);
        testMethod(httpPut, headersToAdd, statusCode, headersToFind);
    }

    public static void testPost(String url, Map<String, String> headersToAdd, String body, int statusCode,
                                Map<String, String> headersToFind) throws IOException {
        HttpPost httpPost = new HttpPost(url);
        if (body != null) {
            httpPost.setEntity(new StringEntity(body));
        }
        testMethod(httpPost, headersToAdd, statusCode, headersToFind);
    }

    public static HttpResponse testMethod(HttpUriRequest httpUriRequest, Map<String, String> headersToAdd, int statusCode,
                              Map<String, String> headersToFind) throws IOException {
        HttpClient httpClient = new DefaultHttpClient();
        if (headersToAdd != null) {
            for(String headerName : headersToAdd.keySet()) {
                httpUriRequest.setHeader(headerName, headersToAdd.get(headerName));
            }
        }
        HttpResponse httpResponse = httpClient.execute(httpUriRequest);
        assertEquals(statusCode, httpResponse.getStatusLine().getStatusCode());
        testHeaders(httpResponse, headersToFind);
        return httpResponse;
    }

    public static void testHeaders(HttpResponse httpResponse, Map<String,String> headersToFind) {
        if (headersToFind == null) {
            return;
        }

        SortedMap<String, String> headersInResponse = new TreeMap<String, String>();
        for(Header header : httpResponse.getAllHeaders()) {
            headersInResponse.put(header.getName(), header.getValue());
        }

        for(String headerName : headersToFind.keySet()) {
            assertTrue(headersInResponse.containsKey(headerName));
            assertTrue(headersInResponse.get(headerName).compareTo(headersToFind.get(headerName)) == 0);
        }
    }

    public static void testJson(HttpResponse httpResponse, Map<String, Object> jsonPathAndValue) throws IOException {
        JsonAsserter jsonAsserter = JsonAssert.with(httpResponse.getEntity().getContent());
        for(String path : jsonPathAndValue.keySet()) {
            jsonAsserter.assertThat(path, equalTo(jsonPathAndValue.get(path)));
        }
    }
}
