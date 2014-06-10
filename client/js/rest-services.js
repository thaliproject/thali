'use strict';

angular.module('myApp.restServices', ['ngResource'])
    .factory('Contact', ['$resource',
        function ($resource) {
            return $resource('http://localhost:3000/contacts/:id', {});
        }]);