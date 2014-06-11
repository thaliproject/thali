'use strict';

angular.module('myApp', [
	'ja.qr',
    'ngTouch',
    'ngRoute',
    'ngAnimate',
    'myApp.controllers',
    'myApp.memoryServices'
]).
config(['$routeProvider', function ($routeProvider) {
    $routeProvider.when('/contacts', {templateUrl: 'partials/contact-list.html', controller: 'ContactListCtrl'});
    $routeProvider.when('/contacts/:contactId', {templateUrl: 'partials/contact-detail.html', controller: 'ContactDetailCtrl'});
    $routeProvider.otherwise({redirectTo: '/contacts'});
}]);