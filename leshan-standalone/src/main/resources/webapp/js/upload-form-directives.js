/**
 * Created by Jyotsna.Bhonde on 12/2/2015.
 */

angular.module('uploadFormDirectives', [])

    .directive('uploadform', function ($compile, $routeParams, $http, dialog,$filter) {
        return {
            restrict: "E",
            replace: true,
            scope: {
                resource: '=',
                parent: '='
            },
            templateUrl: "partials/upload-form.html",
            link: function (scope, element, attrs) {
                scope.writable = function() {
                    if(scope.resource.def.instancetype != "multiple") {
                        if(scope.resource.def.hasOwnProperty("operations")) {
                            return scope.resource.def.operations.indexOf("U") != -1;
                        }
                    }
                    return false;
                }
            }
        }
    });