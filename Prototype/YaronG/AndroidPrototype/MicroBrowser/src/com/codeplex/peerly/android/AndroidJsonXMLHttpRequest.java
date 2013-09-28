package com.codeplex.peerly.android;

/**
 * Sigh... so the deal as far as I can tell is that if you add a Java object to WebView via addJavascriptInterface
 * the object only shows up inside the WebView if the WebView is fully reloaded. This means that there isn't a way
 * (again, near as I can tell) to dynamically create new Java objects and make those new Java objects show up
 * in the WebView. In Applets you can handle this scenario by returning Java objects directly into JavaScript but
 * (once again, near as I can tell since the docs suck) WebView only exposes simple types and Strings. So I had to
 * come up with this horrific hack where I replicate all the JsonXMLHttpRequest APIs but add an index number to
 * them in order
 */
public class AndroidJsonXMLHttpRequest {
}
