angular.module('myApp.services', [])
    .factory('Contact', function($db, $q, $rootScope) {
        return {
            create: function (contact) {
                var deferred = $q.defer();
                $db.post(contact, function(err, response) {
                    $rootScope.$apply(function() {
                        if(response.ok) {
                            // db.replicate.to(tdhdb);
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
                $db.allDocs({include_docs:true}, function(err,response) {
                    $rootScope.$apply(function() {
                        if (response) {
                            var map = Array.prototype.map;
                            var returnValue = map.call(response.rows, function(x) { return x.doc;});
                            if (contactId) {
                                returnValue = returnValue.filter(function(contact) { return contact._id == contactId})[0];
                            }
                            // db.replicate.to(tdhdb);
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
                $db.post(contact, function(err, response) {
                    $rootScope.$apply(function() {
                        if(response.ok) {
                            // db.replicate.to(tdhdb);
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
                $db.remove(contact, function(err, response) {
                    $rootScope.$apply(function() {
                        if (response) {
                            // db.replicate.to(tdhdb);
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
    });