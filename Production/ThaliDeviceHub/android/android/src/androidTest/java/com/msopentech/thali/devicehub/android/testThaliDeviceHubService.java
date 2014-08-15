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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.test.ServiceTestCase;

import java.io.IOException;
import java.net.UnknownHostException;

public class testThaliDeviceHubService extends ServiceTestCase<ThaliDeviceHubService> {
    private volatile String localMachineIPHttpKeyURLName;
    private final static long timeToWaitMillis = 30*1000;

    public testThaliDeviceHubService() {
        super(ThaliDeviceHubService.class);
    }

    public void setUp() {
        localMachineIPHttpKeyURLName = null;
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            localMachineIPHttpKeyURLName = bundle.getString(ThaliDeviceHubService.LocalMachineIPHttpKeyURLName);
        }
    };

    public void testLifeCycle() throws InterruptedException, IOException {
        getContext().registerReceiver(broadcastReceiver, new IntentFilter(ThaliDeviceHubService.HttpKeysNotification));

        // We test startService twice to make sure we keep getting the broadcast intent.
        exerciseStartService();
        localMachineIPHttpKeyURLName = null;
        exerciseStartService();
    }

    private void exerciseStartService() throws InterruptedException, IOException {
        //Intent startServiceIntent = new Intent(getContext(), ThaliDeviceHubService.class);
        // This represents how most Thali apps would send the intent since they won't have access
        // to the ThaliDeviceHubService.class value
        Intent startServiceIntent = new Intent("com.msopentech.thali.devicehub.android.ThaliDeviceHubService");
        startService(startServiceIntent);
        ThaliDeviceHubService thaliDeviceHubService = getService();
        Thread.sleep(100, 0);
        assertTrue(thaliDeviceHubService.thaliListenerRunning);

        long endWait = System.currentTimeMillis() + timeToWaitMillis;
        while(localMachineIPHttpKeyURLName == null && System.currentTimeMillis() <= endWait) {
            Thread.sleep(100, 0);
        }
        assertTrue(thaliDeviceHubService.httpKeysSentByBroadcast);
        assertEquals(
                localMachineIPHttpKeyURLName,
                thaliDeviceHubService.thaliListener.getHttpKeys().getLocalMachineIPHttpKeyURL());
    }
}
