function AddBlogEntry(blogText, dateTimeStamp) {
    var blogEntry = {
        _id: PouchDB.uuid(),
        body: blogText,
        dateTimePost: dateTimeStamp
    };
    db.put(blogEntry, function callback(err, result) {
        if (err !== null) {
            //TODO: DO SOMETHING USEFUL HERE!
            throw err;
        }
    });
}

function UpdateBlogEntriesTable() {
    db.allDocs({ include_docs: true }, function(err, response) {
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

function DeleteContentsOfTable(callBack) {
    db.allDocs({ include_docs: true }, function(err, response) {
        if (err !== null) {
            // TODO: DO SOMETHING USEFUL HERE!
            throw err;
        }
        response.rows.forEach(function callback(element, index, array) {
            db.remove(element.doc, function(err, response){
                    if (err !== null) {
                        // TODO: DO SOMETHING USEFUL HERE!
                        throw err;
                    }
                }
            )
        });
        callBack();
    })
}

function MicroBlogSubmitButtonHandler(obj) {
    var postElement = document.getElementById("microBlogContentPostText");
    var postElementvalue = postElement.value;
    AddBlogEntry(postElementvalue, new Date().toISOString());
    postElement.value = "";
}

function MicroBlogStartSynchHandler(obj) {
    var remoteURLElement = document.getElementById("microBlogRemoteMicroBlogUrl");
    var remoteURLValue = remoteURLElement.value;
    var options = {
        "onChange": UpdateBlogEntriesTable,
        "continuous": true
    };
    window.db.replicate.to(remoteURLValue, options);
    window.db.replicate.from(remoteURLValue, options);
}

function MicroBlogDeleteContentsOfMicroBlogHandler(obj){
    DeleteContentsOfTable(function() {
       UpdateBlogEntriesTable();
    });
}

function SetUp(obj) {

    PeerlyHttpServer.startHttpServer(8090, Express.PeerlyHttpServerCallback);

    document.getElementById("microBlogStartSynch").onclick = MicroBlogStartSynchHandler;
    document.getElementById("microBlogSubmitButton").onclick = MicroBlogSubmitButtonHandler;
    document.getElementById("deleteContentsOfMicroBlog").onclick = MicroBlogDeleteContentsOfMicroBlogHandler;

    window.db = new PouchDB('microblog');
    window.remoteCouch = false;
    db.info(function(err, info) {
        db.changes({
            since: info.update_seq,
            continuous: true,
            onChange: UpdateBlogEntriesTable
        });
    });
    UpdateBlogEntriesTable();
}

window.onload = SetUp;
