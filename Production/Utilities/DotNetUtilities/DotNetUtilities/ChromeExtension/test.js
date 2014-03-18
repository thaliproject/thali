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

"use strict";

/**
 * Logs both from the Timeline function and in the general log
 * @param message
 */
function doubleLog(message) {
    console.timeStamp(message);
    console.log(message);
}

/**
 *
 * @param {Function} doNext
 * @returns {Function}
 */
function errorOrDoNext(doNext) {
    return function(err, resp)
    {
        if (err != null) {
            window.alert("oy!");
        } else {
            doubleLog("About to start function " + doNext.name);
            doNext();
        }
    }
}

/**
 *
 * @param {Function} handleResponse If the associated called succeeded then the resp object will be passed to this function.
 * @param {Function} [doNext]
 * @returns {Function}
 */
function handleRespThenDoNext(handleResponse, doNext) {
    return function(err, resp)
    {
        if (err != null) {
            window.alert("oy!");
        } else {
            doubleLog("About to start function " + handleResponse.name);
            handleResponse(resp);
            if (doNext != null) {
                doubleLog("About to start function " + doNext.name);
                doNext();
            }
        }
    }
}


var host = "127.0.0.1";
var port = 9898;
var testDbUrl;
var localDbBaseUrl;
var localCouch;
var localFromThali;

// Clean up state a little
function startTest(localIsOnThali) {
    localDbBaseUrl = "";
    ThaliXMLHttpRequest.ProvisionClientToHub(host, port,
        handleRespThenDoNext(function(resp) {
            testDbUrl = resp + "test";
        }, localIsOnThali ? provisionHubToHub : destroyRemote));
}

function provisionHubToHub() {
    ThaliXMLHttpRequest.ProvisionHubToHub(testDbUrl, host, port,
        handleRespThenDoNext(function(resp) {
            localDbBaseUrl = resp;
        }, destroyRemote));
}

function destroyRemote() {
    PouchDB.destroy(testDbUrl, errorOrDoNext(destroyLocal));
}

function destroyLocal() {
    PouchDB.destroy(localDbBaseUrl + 'local', errorOrDoNext(destroyLocalCopy));
}

function destroyLocalCopy() {
    PouchDB.destroy(localDbBaseUrl + 'localcopy', errorOrDoNext(createLocalAndPutDoc1));
}

function createLocalAndPutDoc1() {
    localCouch = new PouchDB(localDbBaseUrl + 'local');
    localCouch.put({_id: 'bar', foo: "bar"}, errorOrDoNext(putDoc2));
}

function putDoc2() {
    localCouch.put({_id: 'blah', foo: "blah"}, errorOrDoNext(postATonOfDocs));
}

function postATonOfDocs() {
    var docBag = [];
    var baseGuid = guid();
    for(var i = 0; i < 10; ++i) {
        docBag = docBag.concat({silly: baseGuid + 1});
    }
    localCouch.bulkDocs({docs: docBag}, errorOrDoNext(replicateTo));
}

function replicateTo() {
    var options = { create_target: true };
    // Disabled due to a bug in CouchBase, it doesn't support local to local replication
//    if (testWithThali) {
//        options.server = true;
//    }
    localCouch.replicate.to(testDbUrl , options, errorOrDoNext(replicateFrom));
}

function replicateFrom() {
    // Disabled setting server; true because of a bug in CouchBase, it doesn't support local to local replication
    var options = {};
//    var options = testWithThali ? { server: true } : {};
    localFromThali = new PouchDB(localDbBaseUrl + 'localcopy');
    localFromThali.replicate.from(testDbUrl, options, errorOrDoNext(getAllLocalDocs));
}

var allLocalDocs;

function getAllLocalDocs() {
    localCouch.allDocs({include_docs: true}, handleRespThenDoNext(function(docs) { allLocalDocs = docs.rows; }, seeIfItWorked));
}

function seeIfItWorked() {
    if (allLocalDocs.length == 0) {
        testSuccessful();
        return;
    }

    localFromThali.get(allLocalDocs[0].id, handleRespThenDoNext(function(doc) {
        doubleLog("A doc has completed.");
        allLocalDocs = allLocalDocs.slice(1);
    }, seeIfItWorked));
}

function testSuccessful() {
    doubleLog("Test successfully completed! testWithThatli = " + testWithThali);
    if (testWithThali == false) {
        testWithThali = true;
        startTest(testWithThali);
    }
}

// The value below SHOULD be set to false in order to run both versions of the test
window.testWithThali = false;
window.onload = startTest(testWithThali);
