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

function RequestCallBack(method, uri, jsonQueryParams, jsonHeaders, requestBody)
{
    var queryParams = JSON.parse(jsonQueryParams);
    var headers = JSON.parse(jsonHeaders);
    var response = new Object();
    response.responseCode = 200;
    response.responseMIMEType = "text/html";
    response.responseBody = "method: " + method + ", uri: " + uri;
    return response;
}

function SetUp(obj) {
    // I have run into issues where if I don't do something simple with the applet like load a field then more
    // complex operations like running the server don't work at all, the methods pretend they aren't there. Odd.
    var field = app.helloWorldField;
    app.startHttpServer(8090, "RequestCallBack");

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
