function addBlogEntry(blogText, dateTimeStamp) {
    var blogEntry = {
        _id: window.PouchDB.uuid(),
        body: blogText,
        dateTimePost: dateTimeStamp
    };
    window.db.put(blogEntry, function callback(err, result) {
        if (err !== null) {
            //TODO: DO SOMETHING USEFUL HERE!
            throw err;
        }
    });
}

function updateBlogEntriesTable() {
    window.db.allDocs({ include_docs: true }, function (err, response) {
        if (err !== null) {
            // TODO: DO SOMETHING USEFUL HERE!
            throw err;
        }
        var blogEntriesTable = "<table><tr><th>Date</th><th>Value</th></tr>";
        response.rows.forEach(function callback(element, index, array) {
            blogEntriesTable = blogEntriesTable + "<tr><td>" + element.doc.dateTimePost + "</td><td>" + element.doc.body + "</td></tr>";
        });
        blogEntriesTable = blogEntriesTable + "</table>";
        document.getElementById("drawMicroBlogContent").innerHTML = blogEntriesTable;
    });
}

function deleteContentsOfTable(callBack) {
    window.db.allDocs({ include_docs: true }, function (err, response) {
        if (err !== null) {
            // TODO: DO SOMETHING USEFUL HERE!
            throw err;
        }
        response.rows.forEach(function callback(element, index, array) {
            window.db.remove(element.doc, function (err, response) {
                if (err !== null) {
                    // TODO: DO SOMETHING USEFUL HERE!
                    throw err;
                }
            }
                );
        });
        callBack();
    });
}

function microBlogSubmitButtonHandler(obj) {
    var postElement = document.getElementById("microBlogContentPostText"),
        postElementvalue = postElement.value;
    addBlogEntry(postElementvalue, new Date().toISOString());
    postElement.value = "";
}

function microBlogStartSynchHandler(obj) {
    var remoteURLElement = document.getElementById("microBlogRemoteMicroBlogUrl"),
        remoteURLValue = remoteURLElement.value,
        options = {
            "onChange": updateBlogEntriesTable,
            "continuous": true
        };
    window.db.replicate.to(remoteURLValue, options);
    window.db.replicate.from(remoteURLValue, options);
}

function microBlogDeleteContentsOfMicroBlogHandler(obj) {
    deleteContentsOfTable(function () {
        updateBlogEntriesTable();
    });
}

function SetUp(obj) {

    var peerlyExpress = new window.PeerlyExpress(8090, window.PeerlyHttpServer),
        pouchDBExpress = new window.PouchDBExpress(peerlyExpress, window.PouchDB);
    //var peerlyHttpServer = new window.PeerlyHttpServer(8090, window.Express.PeerlyHttpServerCallback);

    document.getElementById("microBlogStartSynch").onclick = microBlogStartSynchHandler;
    document.getElementById("microBlogSubmitButton").onclick = microBlogSubmitButtonHandler;
    document.getElementById("deleteContentsOfMicroBlog").onclick = microBlogDeleteContentsOfMicroBlogHandler;

    window.db = new window.PouchDB('microblog');
    window.db.remoteCouch = false;
    window.db.info(function (err, info) {
        window.db.changes({
            since: info.update_seq,
            continuous: true,
            onChange: updateBlogEntriesTable
        });
    });
    updateBlogEntriesTable();
}

window.onload = SetUp;
