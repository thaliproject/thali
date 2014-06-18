var remoteCouch = 'http://localhost:58000/todos';
var db = new PouchDB(remoteCouch);


$(function() {

    // Try some pouchy stuff
    var todo = {
        _id: new Date().toISOString(),
        title: "some text",
        completed: false
    };
    db.put(todo, function callback(err, result) {
        if (!err) {
            $( "#output" ).html('Successfully posted a todo!');
            db.allDocs({include_docs: true, descending: true}, function(err, doc) {
                console.log(doc.rows.length + ", " + doc.rows[0]);
            });
        }
        else {
            $( "#output" ).html('Pouch Error: ' + err);
            console.log('Pouch Error: ' + err);
        }
    });



    //sync();

 });

function sync() {
    var opts = {live: false};
    db.replicate.to(remoteCouch, opts)
        .on('change', function (info) {
        // handle change
        }).on('complete', function (info) {
            $( "#output" ).html('Replicate Complete!');
        }).on('uptodate', function (info) {
            // handle up-to-date
        }).on('error', function (err) {
            $( "#output" ).html('Replicate Error!');
            console.log('Pouch Error on Replicate: ' + err);
        });

}