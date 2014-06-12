'use strict';

angular.module('myApp.controllers', [])
    .controller('MainCtrl', ['$scope', '$rootScope', '$window', '$location', function ($scope, $rootScope, $window, $location) {
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
    .controller('ContactListCtrl', ['$scope', 'Contact', function ($scope, contact, $q) {
        $scope.editContact = function(contact) {
            $scope.opts = ['on', 'off'];
            if (event === 'new') {
                $scope.newContact = true;
                $scope.contact = {name: '', uniqueId: ''};
            } else {
                $scope.newContact = false;
                $scope.contact = contact;
            }
        };
        contact.retrieve().then(function(contacts) {
            console.log("Getting contacts.");
            $scope.contacts = contacts;
        }, function(reason) {
            console.log("Failed: " + reason);
        }, function(update) {
            console.log("Got update: " + update);
        });
    }])
    .controller('ContactDetailCtrl', ['$scope', '$routeParams', 'Contact', function ($scope, $routeParams, contact, $q) {
        $scope.contact = contact.get($routeParams.contactId);
        // contact.get({id: $routeParams.contactId}).then(function(contact) {
        //     console.log("Getting contacts.");
        //     $scope.contact = contact;
        // }, function(reason) {
        //     console.log("Failed: " + reason);
        // }, function(update) {
        //     console.log("Got update: " + update);
        // });
    }]);
    // .controller('ContactEditCtrl', ['$scope', '$routeParams', 'Contact', function ($scope, $routeParams, contact) {
    //     $scope.editEvent = function(event) {
    //         $scope.opts = ['on', 'off'];
         
    //         if (event === 'new') {
    //           $scope.newEvent = true;
    //           $scope.event = {name: '', uniqueId: ''};
    //         }
    //         else {
    //           $scope.newEvent = false;
    //           $scope.event = event;
    //         }
    //       };
    // }]);
