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

import com.couchbase.lite.Manager;
import com.couchbase.lite.util.Log;

import static com.couchbase.lite.util.Log.VERBOSE;

/**
 * Couchbase's logging system uses tags, this class is used to
 * track all the tags we have added so that when we are debugging
 * we can set their output level.
 */
public class CblLogTags {
    public static final String TAG_THALI_LISTENER = "ThaliListener";
    public static final String TAG_THALI_BOGUSREQUESTAUTHORIZATION = "ThaliBogusRequestAuthorization";
    public static final String TAG_THALI_BRIDGETESTMANAGER = "ThaliBridgeTestManager";
    public static final String TAG_THALI_REPLICATION_MANAGER = "ReplicationManager";

    public static void turnTo11() {
        Log.enableLogging(TAG_THALI_LISTENER, VERBOSE);
        Log.enableLogging(TAG_THALI_BOGUSREQUESTAUTHORIZATION, VERBOSE);
        Log.enableLogging(TAG_THALI_BRIDGETESTMANAGER, VERBOSE);
        Log.enableLogging(TAG_THALI_REPLICATION_MANAGER, VERBOSE);
    }
}
