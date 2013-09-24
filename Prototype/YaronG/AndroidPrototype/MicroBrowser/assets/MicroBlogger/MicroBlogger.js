function AddBlogEntry(blogText, dateTimeStamp) {
    var blogEntry = {
        _id: PouchDB.uuid(),
        body: blogText,
        dateTimePost: dateTimeStamp
    };
    db.put(blogEntry, function callback(err, result) {
        //TODO: DO SOMETHING USEFUL HERE!
    });
}

function UpdateBlogEntriesTable() {
    db.allDocs({ include_docs: true }, function(err, response) {
        if (!err) {
            // TODO: DO SOMETHING USEFUL HERE!
        }
        var blogEntriesTable = "<table><tr><th>Date</th><th>Value</th></tr>";
        response.rows.forEach(function callback(element, index, array) {
            blogEntriesTable = blogEntriesTable + "<tr><td>" + element.doc.dateTimePost + "</td><td>" + element.doc.body + "</td></tr>";
        });
        blogEntriesTable = blogEntriesTable + "</table>";
        document.getElementById("drawMicroBlogContent").innerHTML = blogEntriesTable;
    });
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

function RequestCallBack(method, uri, jsonQueryParams, jsonHeaders, requestBody, responseContext)
{
    var queryParams = JSON.parse(jsonQueryParams);
    var headers = JSON.parse(jsonHeaders);
    var response = new Object();
    response.responseCode = 200;
    response.responseMIMEType = "text/html";
    response.responseBody = "method: " + method + ", uri: " + uri;
    responseContext(response);
}

function SetUp(obj) {
    PeerlyHttpServer.startHttpServer(8090, Express.PeerlyHttpServerCallback);

    document.getElementById("microBlogStartSynch").onclick = MicroBlogStartSynchHandler;
    document.getElementById("microBlogSubmitButton").onclick = MicroBlogSubmitButtonHandler;

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
