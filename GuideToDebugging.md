---
title: Guide to Debugging
layout: default
---

# Apache HTTPClient 

Because we use SSL everywhere Apache's logging infrastructure is critical for debugging. [http://hc.apache.org/httpclient-3.x/logging.html](http://hc.apache.org/httpclient-3.x/logging.html) provides an overview. I'll usually just use simple logging. 

<pre>
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
        System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire.header", "debug");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.commons.httpclient", "debug");
        System.setProperty("org.apache.commons.logging.simplelog.defaultlog", "all");
</pre>

The last line is one I added and it's a very blunt weapon but it has proven useful for finding things. Obviously none of this code should ever see production.

I wrote a quick function to wrap this all up, see ThaliTestUtilities.configuringLoggingApacheClient.

# SSL Connections in Java 

[http://docs.oracle.com/javase/1.5.0/docs/guide/security/jsse/ReadDebug.html](http://docs.oracle.com/javase/1.5.0/docs/guide/security/jsse/ReadDebug.html) - The magic is passing in -Djavax.net.debug=all

# Couchbase Logging 

Couchbase created their own logging abstraction (I never could figure out why they didn't just use something like SLF4J, I suggested it to them) to abstract their logging dependencies in Android and Java.

Their logging mechanism works on a combination of tags and levels. The idea is that one submits a tag and a level and if a log statement with that tag and at least that level is submitted then it will be output. I also submitted a PR so that they will output any unregistered tag by default that is at least at level INFO. But if one wants verbose logging then one has to submit a specific request. I wrote a function to cover all of their tags I know of and all of our tags. See ThaliTestUtilities.turnLoggingTo11 and the associated CblLogTags.turnto11() function.

But there's more work! Because Couchbase's logging doesn't actually bottom out on its own. Instead it speaks to the Android logging system on Android and the default Java logging system in the JVM. In Android there isn't much to do, you can just set the logging level via IntelliJ's console and it's all good. But in Java one has to actually tell the system what to output. Thankfully this is pretty easy. I just use the code:

<pre>
        Logger log = Logger.getLogger("com.couchbase.lite");
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        log.addHandler(handler);
        log.setLevel(Level.ALL);
</pre>

# TJWS Logging 

TJWS does not use the same logging as Couchbase (of course). Instead it's currently hard coded by Couchbase to output all of its logging content to system.err. There doesn't appear to be any kind of leveling system in TJWS.

# Thali Logging 

Most of our logic sits on top of Couchbase and so we use Couchbase's logging. See their section and CblLogTags.turnto11() for more details.

But for some of our more generic crypto libraries and for non-Couchbase related code we use SLF4J. We didn't want these libraries, which didn't need any Couchbase code, to take a dependency on Couchbase's custom logging system. To turn on SLF4J logging in the case of Java just use:

<pre>
        System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "trace");
        System.setProperty(SimpleLogger.LOG_FILE_KEY, "System.out");
</pre>

# Tor Logging 

Like everything else this is handled via the torrc file. Just add something like:

<pre>
   Log debug file c:\Users\yarong\Desktop\Tor Browser\Data\Tor\debug.log
</pre>
