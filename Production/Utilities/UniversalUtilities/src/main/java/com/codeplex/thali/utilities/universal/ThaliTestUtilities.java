package com.codeplex.thali.utilities.universal;

/**
 * These are utilities used to run tests in different projects.
 */
public class ThaliTestUtilities {
    /**
     * Turns on various logging interfaces for Apache HTTP Client including the use of SimpleLog
     */
    public static void configuringLoggingApacheClient() {
            System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");

            System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");

            System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire.header", "debug");

            System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.commons.httpclient", "debug");

            System.setProperty("org.apache.commons.logging.simplelog.defaultlog", "all");
    }
}
