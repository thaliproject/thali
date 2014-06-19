angular.module('myApp.services', [])
    .factory('Contact', function($q, $rootScope) {
        var dbname = 'thali-contacts';
        var tdhdb = 'http://127.0.0.1:58000/'+dbname;
        var db = new PouchDB(dbname);
        // Create the tdh db if we need to
        PouchDB.replicate(dbname, tdhdb, {live:true, create_target:true});
        return {
            create: function (contact) {
                var deferred = $q.defer();
                db.post(contact, function(err, response) {
                    $rootScope.$apply(function() {
                        if(response.ok) {
                            db.replicate.to(tdhdb);
                            deferred.resolve(response);
                        } else {
                            console.log("Failed to save new contact: " + err);
                            deferred.reject(err);
                        }
                    });
                });
                return deferred.promise;
            },
            retrieve: function (contactId) {
                var deferred = $q.defer();
                db.allDocs({include_docs:true}, function(err,response) {
                    $rootScope.$apply(function() {
                        if (response) {
                            var map = Array.prototype.map;
                            var returnValue = map.call(response.rows, function(x) { return x.doc;});
                            if (contactId) {
                                returnValue = returnValue.filter(function(contact) { return contact._id == contactId})[0];
                            }
                            db.replicate.to(tdhdb);
                            deferred.resolve(returnValue);
                        } else {
                            console.log("Error getting all contacts: " + err);
                            deferred.reject(err);
                        }                   
                    });
                });
                return deferred.promise;
            }, 
            update: function (contact) {
                var deferred = $q.defer();
                db.post(contact, function(err, response) {
                    $rootScope.$apply(function() {
                        if(response.ok) {
                            db.replicate.to(tdhdb);
                            deferred.resolve(response);
                        } else {
                            console.log("Failed to update contact: " + err);
                            deferred.reject(err);
                        }
                    });
                });
                return deferred.promise;
            },
            delete: function (contact) {
                console.log("In delete!");
                var deferred = $q.defer();
                db.remove(contact, function(err, response) {
                    $rootScope.$apply(function() {
                        if (response) {
                            db.replicate.to(tdhdb);
                            deferred.resolve(response);
                        } else {
                            console.log("Failed to delete contact: " + err);
                            deferred.reject(err);
                        }
                    });
                });
                return deferred.promise;
            }
        }
    })
    .run(function (){
        var dbname = 'thali-contacts';
        var tdhdb = 'http://127.0.0.1:58000/'+dbname;
        var db = new PouchDB(dbname);

        // Pull content from the TDH database first
        db.replicate.from(tdhdb);

        // Check, if there are no contacts in the TDH, load up demo contacts.
        db.info().then(function(info) {
            // Initialize demo data
            if (info.doc_count < 1) {
                db.bulkDocs({docs : demoContacts }, function(err, response) {
                    if (response) {
                        console.log("Making demo contacts.");
                        console.log(response);  
                    } else {
                        console.log("Error making demo contacts.");
                        console.log(err);
                    }
                });
            }
        });
    });