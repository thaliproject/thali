var express = require('express'),
    path = require('path');

var app = express();
app.use(express.bodyParser());
app.use(express.static(path.join(__dirname, '.')));

app.listen(3000);
console.log('Listening on port 3000...');