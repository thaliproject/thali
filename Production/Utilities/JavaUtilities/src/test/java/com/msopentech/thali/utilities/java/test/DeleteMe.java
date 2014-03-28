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

package com.msopentech.thali.utilities.java.test;

import com.couchbase.lite.JavaContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

// Work around for https://github.com/couchbase/couchbase-lite-java/issues/4 and
// https://github.com/couchbase/couchbase-lite-java-core/issues/117
class DeleteMe extends JavaContext {
    private final File tempDirectory;
    public DeleteMe() {
        try {
            tempDirectory = Files.createTempDirectory("javacoretest").toFile();
            tempDirectory.deleteOnExit();
            File filesDir = getFilesDir();
            if (filesDir.exists() == false && filesDir.mkdirs() == false) {
                throw new RuntimeException();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public File getRootDirectory() {
        return tempDirectory;
    }
}
