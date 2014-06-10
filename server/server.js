var express = require('express'),
    path = require('path'),
    contacts = require('./routes/contact');

var app = express();
app.use(express.bodyParser());
app.use(express.static(path.join(__dirname, '../client')));

app.get('/contacts/:id', contacts.findById);
app.get('/contacts', contacts.findAll);
// app.post('/contacts/', contacts.createNew);

app.listen(3000);
console.log('Listening on port 3000...');