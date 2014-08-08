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

package com.msopentech.thali.local.utilities;

import android.content.Context;
import com.couchbase.lite.android.AndroidContext;
import com.msopentech.thali.toronionproxy.FileUtilities;

import java.io.File;

/**
 * Overrides the default filesDir to let us run two Thali Listeners side by side
 */
public class AndroidContextChangeFilesDir extends AndroidContext {
    private final String subFolderName;
    private File filesDir = null;

    public AndroidContextChangeFilesDir(Context wrappedContext, String subfolderName) {
        super(wrappedContext);
        this.subFolderName = subfolderName;
    }

    @Override
    public File getFilesDir() {
        if (filesDir == null) {
            filesDir =  new File(super.getFilesDir(), subFolderName);
            if (filesDir.exists()) {
                FileUtilities.recursiveFileDelete(filesDir);
            }
            if (filesDir.mkdirs() == false) {
                throw new RuntimeException("Could not create working directory - " + filesDir.getAbsolutePath());
            }
        }

        return filesDir;
    }
}
