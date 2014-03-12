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

var testDbUrl;

// Clean up state a little
function startTest() {
    ThaliXMLHttpRequest.ProvisionClientToHub(host, port,
        handleRespThenDoNext(function(resp) {
            testDbUrl = resp + "test";
        }, destroyRemote));
}

function destroyRemote() {
    PouchDB.destroy(testDbUrl, errorOrDoNext(destroyLocal));
}

function destroyLocal() {
    PouchDB.destroy('local', errorOrDoNext(destroyLocalCopy));
}

function destroyLocalCopy() {
    PouchDB.destroy('localcopy', errorOrDoNext(createLocalAndPutDoc1));
}

var localCouch;

function createLocalAndPutDoc1() {
    localCouch = new PouchDB('local');
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
    localCouch.replicate.to(testDbUrl , { create_target: false }, errorOrDoNext(replicateFrom));
}

var localFromThali;

function replicateFrom() {
    //var server = testDbUrl;
    //var server = "http://127.0.0.1:2693/test5";

    localFromThali = new PouchDB('localcopy');
    localFromThali.replicate.from(testDbUrl, errorOrDoNext(getAllLocalDocs));
}

var allLocalDocs;

function getAllLocalDocs() {
    localCouch.allDocs({include_docs: true}, handleRespThenDoNext(function(docs) { allLocalDocs = docs; }, seeIfItWorked));
}

function seeIfItWorked() {
    for(var localDoc in allLocalDocs.rows) {
        localFromThali.get(allLocalDocs.rows[localDoc].id, handleRespThenDoNext(function(doc) {
            var div = document.createElement("div");
            div.innerHTML = "A Doc! ";
            doubleLog("A doc has completed.");
        }));
    }
}

var host = "127.0.0.1";
var port = 9898;
window.onload = startTest();
