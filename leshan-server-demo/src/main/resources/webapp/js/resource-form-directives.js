/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/

angular.module('resourceFormDirectives', [])

.directive('resourceform', function ($compile, $routeParams, $http, dialog,$filter) {
    return {
        restrict: "E",
        replace: true,
        scope: {
            resource: '=',
            parent: '='
        },
        templateUrl: "partials/resource-form.html",
        link: function (scope, element, attrs) {
            // define place holder
            if (scope.resource.def.type == "opaque") {
                scope.resource.placeholder = "byte value in Hexadecimal"; 
            } else if (scope.resource.def.type == "string") {
                scope.resource.placeholder = "string value";
            } else if (scope.resource.def.type == "float" || scope.resource.def.type == "integer") {
                scope.resource.placeholder = "number value";
            } else if (scope.resource.def.type == "time") {
                scope.resource.placeholder = "ISO 8601 time value (eg:2013-02-09T13:20+01:00)";
            } else if (scope.resource.def.type == "boolean") {
                scope.resource.placeholder = "true or false";
            }

            // define writable function
            scope.writable = function() {
                if(scope.resource.def.instancetype != "multiple") {
                    if(scope.resource.def.hasOwnProperty("operations")) {
                        return scope.resource.def.operations.indexOf("W") != -1;
                    }
                }
                return false;
            };

            // utility to get hex value from file
            function toByteArray(file, resolve) {
                var reader = new FileReader();
                reader.onload = function() {
                    var u = new Uint8Array(this.result),
                    a = new Array(u.length),
                    i = u.length;
                    while (i--) // map to hex
                        a[i] = (u[i] < 16 ? '0' : '') + u[i].toString(16);
                    u = null; // free memory
                    resolve(a.join(''));
                }
                reader.readAsArrayBuffer(file);
            }

            // Add promisedValue to get resource value
            scope.resource.getPromisedValue = function() {
                return new Promise(function(resolve,reject) {
                    if (scope.resource.fileValue){
                        toByteArray(scope.resource.fileValue, resolve);
                    } else {
                        resolve(scope.resource.stringValue);
                    }
                });
            }
        }
    };
});