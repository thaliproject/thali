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

var host = "127.0.0.1";
var port = 9898;
var secondPort = 9899;

/**
 * Runs a test between two DB hosts, first and second. At least one of the two have to be remote, although it's fine for
 * both to be remote. They both just can't be local. The test will be data into the first host in a DB named 'test' and then
 * replicate it to the second host in a DB named 'test' and then replicate it from the second host back to the first host into
 * a DB called 'copytest' and then validate that 'test' and 'copytest' on the first host contain the same contents.
 * @param baseNameForFirstDb If this is local then make it blank, if it's remote then put in the base of the URL without the DB name
 * @param baseNameForSecondDb If this is local then make it blank, if it's remote then put in the base of the URL without the DB name
 */
function twoPartyTest(baseNameForFirstDb, baseNameForSecondDb) {
    //TODO: We hard code replicate on server to false but that isn't the right choice in general, we need better logic
    // but we can worry about this one we start trying to test hub to hub replication again.
    var firstTestDb, secondTestDb, firstCopyOfTestDb;
    var firstDbName = baseNameForFirstDb + "test";
    var secondDbName = baseNameForSecondDb + "test";
    var firstCopyDbName = baseNameForFirstDb + "copytest";
    PouchDB.destroy(firstDbName)
        .then(function() { return PouchDB.destroy(firstCopyDbName)})
        .then(function() { return PouchDB.destroy(secondDbName)})
        .then(function() {
            firstTestDb = new PouchDB(firstDbName);
            secondTestDb = new PouchDB(secondDbName);
            firstCopyOfTestDb = new PouchDB(firstCopyDbName);
            return firstTestDb.put({_id: 'bar', foo: "bar"});
        })
        .then(function() { return firstTestDb.put({_id: 'blah', foo: "blah"}) })
        .then(function() { return createATonOfDocs(firstTestDb) })
        .then(function() {
            // I've never found a reliable notification for replication so we use a timer
            firstTestDb.replicate.to(secondTestDb, { create_target: true, server: false});
            return promiseSetTimeout(30000)})
        .then(function() {
            firstCopyOfTestDb.replicate.from(secondTestDb, { server: false });
            return promiseSetTimeout(30000)})
        .then(function() { return allDocsTheSame(firstTestDb, firstCopyOfTestDb)})
        .then(function() { window.ThaliBridgeCallOnce("Test","Good", failCallBack, failCallBack) })
        .catch(failCallBack);
}

function testLocalToRemote() {
    var httpKeyBaseForThaliHub;
    ThaliXMLHttpRequest.ProvisionClientToHub(host, port, handleRespThenDoNext(
        function (resp) { httpKeyBaseForThaliHub = resp },
        function () {
            twoPartyTest("", httpKeyBaseForThaliHub);
        }
    ))
}

function testEverythingRemote() {
    var httpKeyBaseForFirstThaliHub, httpKeyBaseForSecondThaliHub;
    ThaliXMLHttpRequest.ProvisionClientToHub(host, port,
        handleRespThenDoNext(
            function (resp) { httpKeyBaseForFirstThaliHub = resp },
            function () {
                ThaliXMLHttpRequest.ProvisionClientToHub(host, secondPort, handleRespThenDoNext(
                    function () {
                        // We ignore the response, this call just lets us successfully issue a destroy
                        // for the test db, in a real world scenario this call wouldn't happen.
                        return;
                    },
                    function () {
                        ThaliXMLHttpRequest.ProvisionHubToHub( httpKeyBaseForFirstThaliHub, host, secondPort,
                            handleRespThenDoNext(
                                function (resp) { httpKeyBaseForSecondThaliHub = resp; },
                                function() {
                                    twoPartyTest(httpKeyBaseForFirstThaliHub, httpKeyBaseForSecondThaliHub);
                                }));
                    }
                ));
            }));
}

function promiseSetTimeout(timeOut) {
    return new Promise(function(resolve, reject) {
        setTimeout(function() { resolve() }, timeOut);
    });
}

function createATonOfDocs(db) {
    var docBag = [];
    var baseGuid = guid();
    for(var i = 0; i < 10; ++i) {
        docBag = docBag.concat({silly: baseGuid + 1});
    }
    return db.bulkDocs({docs: docBag});
}

function allDocsTheSame(db1, db2) {
    var db1Rows, db2Rows;
    return db1.allDocs({include_docs: true})
        .then(function(allDb1DocsObject) {
            db1Rows = allDb1DocsObject.rows;
            return db2.allDocs({include_docs: true});
        })
        .then(function(allDb2DocsObject) {
            db2Rows = allDb2DocsObject.rows;
            if (db1Rows.length != db2Rows.length) {
                return Promise.reject("Length of rows is not the same! db1Rows = " + db1Rows.length + ", db2Rows = " +
                    db2Rows.length);
            }
            var allResults = [];
            db1Rows.forEach(function(row) { allResults.push(db2.get(row.id)) });
            return Promise.all(allResults);
        });
}

var failCallBack = function(responseString) {
    window.doubleLog("oy!" + responseString);
    window.ThaliBridgeCallOnce("Test", "Bad", failCallBack, failCallBack);
};

/**
 * Logs through whatever mechanisms we have available
 * @param message
 */
window.doubleLog = function(message) {
    //Timestamp is useful when debugging with Chrome
    //console.timeStamp(message);
    window.ThaliBridgeCallOnce("log", message, failCallBack, failCallBack);
    console.log(message);
};

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
            window.doubleLog("oy!" + err);
            window.ThaliBridgeCallOnce("Test", "Bad", failCallBack, failCallBack);
        } else {
            window.doubleLog("About to start function " + handleResponse.name);
            handleResponse(resp);
            if (doNext != null) {
                window.doubleLog("About to start function " + doNext.name);
                doNext();
            }
        }
    }
}

window.onload = testLocalToRemote();
