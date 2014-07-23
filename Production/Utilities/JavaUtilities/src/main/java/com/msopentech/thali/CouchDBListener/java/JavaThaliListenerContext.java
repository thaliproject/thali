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

package com.msopentech.thali.CouchDBListener.java;

import com.couchbase.lite.JavaContext;

import java.io.File;

/**
 * Work around for https://github.com/couchbase/couchbase-lite-java/issues/4 and
 * https://github.com/couchbase/couchbase-lite-java-core/issues/117
 */
public class JavaThaliListenerContext extends JavaContext {
    private final File rootDirectory;

    public JavaThaliListenerContext(File rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    @Override
    public File getRootDirectory() {
        return rootDirectory;
    }

    @Override
    public File getFilesDir() {
        // Unfortunately subdir is private, not public so I can't reference it directly
        File filesDir = new File(getRootDirectory(), "cblite");
        if (filesDir.exists() == false && filesDir.mkdirs() == false) {
            throw new RuntimeException("Could create filesDir: " + filesDir.getAbsolutePath());
        }
        return filesDir;
    }
}
