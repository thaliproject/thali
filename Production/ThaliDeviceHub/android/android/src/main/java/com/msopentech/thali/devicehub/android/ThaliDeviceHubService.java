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

package com.msopentech.thali.devicehub.android;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import com.couchbase.lite.android.AndroidContext;
import com.couchbase.lite.util.Log;
import com.msopentech.thali.CouchDBListener.ThaliListener;
import com.msopentech.thali.utilities.universal.CblLogTags;

import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

/**
 * The service is marked false for exported and has no intents so it has to be called directly by class. In theory
 * because of these two facts the exported value is not needed since the lack of intents sets it to false by default
 * but whatever, I'll be paranoid. In any case the service is only to be started and invoked (in an Android sense)
 * by the hub application. Anyone else who wants to communicate to the service should do it over the wire, including
 * local apps.
 *
 * The service, once started, runs forever. It also ignores whatever intent is sent in, it just starts. Which is
 * why it returns START_STICKY which says that if the system needs to kill the server to retrieve memory then
 * the service should be restarted with a null intent. We don't care what the actual intent was that started the
 * service since the only action we take is to run the CouchDB server.
 *
 * We are explicitly using a start service and not a bound service under the potentially false impression that
 * we want the service to run even if the user should turn off the Thali Device Hub application. The service is
 * core to Thali and all apps need it. In addition it needs to run all the time (or some reasonably facsimile there
 * of depending on battery) so that incoming requests from off device can be handled. So a bound service doesn't
 * make sense because then we would only run while some local app wants us to.
 *
 * It's tempting to argue this should be a foreground service since it will eat battery and users should know its
 * there and be able to kill it. But our goal is that the service 'just works' so the user shouldn't need to be
 * aware of it. To the extent there are battery issues it is our job to manage them for the user by running
 * less frequently. We will support turning the service off via the TDH Application UX but otherwise we shouldn't
 * claim the foreground service role.
 */
public class ThaliDeviceHubService extends Service {
    protected ThaliListener thaliListener = null;

    @Override
    public void onCreate() {
        thaliListener = new ThaliListener();
        // TODO: We will replace with the proxy with a non-null object once we have deployment under control
        // Embarrassing enough I'm not sure if getApplicationContext is the right context to get. :(
        try {
            thaliListener.startServer(new AndroidContext(getApplicationContext()), ThaliListener.DefaultThaliDeviceHubPort,
                    null);
        } catch (UnrecoverableKeyException e) {
            Log.e(CblLogTags.TAG_THALI_LISTENER, "Couldn't start", e);
        } catch (NoSuchAlgorithmException e) {
            Log.e(CblLogTags.TAG_THALI_LISTENER, "Couldn't start", e);
        } catch (KeyStoreException e) {
            Log.e(CblLogTags.TAG_THALI_LISTENER, "Couldn't start", e);
        }
    }

    @Override
    public void onDestroy() {
        if (thaliListener != null) {
            thaliListener.stopServer();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
