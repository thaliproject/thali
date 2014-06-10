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
    .controller('ContactListCtrl', ['$scope', 'Contact', function ($scope, contact) {
        $scope.contacts = contact.query();
    }])
    .controller('ContactDetailCtrl', ['$scope', '$routeParams', 'Contact', function ($scope, $routeParams, contact) {
        $scope.contact = contact.get({contactId: $routeParams.contactId});
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
