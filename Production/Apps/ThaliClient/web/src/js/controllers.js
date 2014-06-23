'use strict';

angular.module('addressBook.controllers', [])
    .controller('MainCtrl', ['$scope', '$rootScope', '$window', '$location', '$db', function ($scope, $rootScope, $window, $location, $db) {
        $scope.slide = '';
        $rootScope.back = function() {
          $scope.slide = 'slide-right';
          $window.history.back();
        }
        $rootScope.go = function(path){
          $scope.slide = 'slide-left';
          $location.url(path);
        }
    }])
    .controller('ContactListCtrl', ['$scope', 'Contact', '$location', function ($scope, contact, $location) {
        contact.retrieve().then(function(contacts) {
            $scope.contacts = contacts;
        }, function(reason) {
            console.log("Failed: " + reason);
        });
        $scope.delete = function(contactToDelete) {
            console.log("Got delete: " + contactToDelete);
            contact.delete(contactToDelete).then(function() {
                $scope.contacts.remove($scope.contacts.indexOf(contactToDelete));
            }, function(reason) {
                console.log("Delete failed.");
            });
        }
    }])
    .controller('ContactDetailCtrl', ['$scope', '$routeParams', 'Contact', function ($scope, $routeParams, contact) {
        contact.retrieve($routeParams.contactId).then(function(contact) {
            $scope.contact = contact;
        }, function(reason) {
            console.log("Failed: " + reason);
        });
    }])
    .controller('ContactNewCtrl', ['$scope', 'Contact', '$location', function($scope, contact, $location) {
        $scope.contactId = '';
        $scope.contactName = '';
        $scope.videoStream = '';
        $scope.onSuccess = function(data) {
            $scope.contactId = data;
            console.log(data);
        };
        $scope.onError = function(error) {
            console.log(error);
        };
        $scope.onVideoError = function(error) {
            console.log(error);
        };
        $scope.save = function(contactId, contactName) {
            contact.create({name: contactName, uniqueId: contactId}).then(function() {
                $location.path('/contacts');
            }, function(reason) {
                console.log("Failed to save contact: " + reason);
            });
        }
    }]);