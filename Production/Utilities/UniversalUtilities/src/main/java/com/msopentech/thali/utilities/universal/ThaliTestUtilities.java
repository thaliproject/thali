/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0

THIS CODE IS PROVIDED ON AN *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, EITHER EXPRESS OR IMPLIED,
INCLUDING WITHOUT LIMITATION ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE, FITNESS FOR A PARTICULAR PURPOSE,
MERCHANTABLITY OR NON-INFRINGEMENT.

See the Apache 2 License for the specific language governing permissions and limitations under the License.
*/


package com.msopentech.thali.utilities.universal;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * These are utilities used to run tests in different projects.
 */
public class ThaliTestUtilities {
    /**
     * Turns on various logging interfaces for Apache HTTP Client including the use of SimpleLog
     */
    public static void configuringLoggingApacheClient() {
        // According to http://stackoverflow.com/questions/3246792/how-to-enable-logging-for-apache-commons-httpclient-on-android
        // the following two lines are needed on Android. They aren't needed in general Java.
        Logger.getLogger("org.apache.http.wire").setLevel(Level.FINEST);
        Logger.getLogger("org.apache.http/headers").setLevel(Level.FINEST);

        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");

        System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");

        System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire.header", "debug");

        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.commons.httpclient", "debug");

        System.setProperty("org.apache.commons.logging.simplelog.defaultlog", "all");
    }
}
