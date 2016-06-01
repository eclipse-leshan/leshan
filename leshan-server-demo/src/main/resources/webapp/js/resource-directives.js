/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/

angular.module('resourceDirectives', [])

.directive('resource', function ($compile, $routeParams, $http, dialog, $filter, lwResources) {
    return {
        restrict: "E",
        replace: true,
        scope: {
            resource: '=',
            parent: '=',
            settings: '='
        },
        templateUrl: "partials/resource.html",
        link: function (scope, element, attrs) {
            scope.resource.path = scope.parent.path + "/" + scope.resource.def.id;
            scope.resource.read  =  {tooltip : "Read <br/>"   + scope.resource.path};
            scope.resource.write =  {tooltip : "Write <br/>"  + scope.resource.path};
            scope.resource.exec  =  {tooltip : "Execute <br/>"+ scope.resource.path};
            scope.resource.execwithparams = {tooltip : "Execute with paramaters<br/>"+ scope.resource.path};
            scope.resource.observe  =  {tooltip : "Observe <br/>"+ scope.resource.path};
            
            scope.readable = function() {
                if(scope.resource.def.hasOwnProperty("operations")) {
                    return scope.resource.def.operations.indexOf("R") != -1;
                }
                return false;
            }
           
            scope.writable = function() {
                if(scope.resource.def.instancetype != "multiple") {
                    if(scope.resource.def.hasOwnProperty("operations")) {
                        return scope.resource.def.operations.indexOf("W") != -1;
                    }
                }
                return false;
            }

            scope.executable = function() {
                if(scope.resource.def.instancetype != "multiple") {
                    if(scope.resource.def.hasOwnProperty("operations")) {
                        return scope.resource.def.operations.indexOf("E") != -1;
                    }
                }
                return false;
            }

            scope.startObserve = function() {
                var format = scope.settings.single.format;
                var uri = "api/clients/" + $routeParams.clientId + scope.resource.path+"/observe";
                $http.post(uri, null,{params:{format:format}})
                .success(function(data, status, headers, config) {
                    var observe = scope.resource.observe;
                    observe.date = new Date();
                    var formattedDate = $filter('date')(observe.date, 'HH:mm:ss.sss');
                    observe.status = data.status;
                    observe.tooltip = formattedDate + "<br/>" + observe.status;
                    
                    if (data.status == "CONTENT") {
                        scope.resource.observed = true;
                        if("value" in data.content) {
                            // single value
                            scope.resource.value = data.content.value
                        }
                        else if("values" in data.content) {
                            // multiple instances
                            var tab = new Array();
                            for (var i in data.content.values) {
                                tab.push(i+"="+data.content.values[i])
                            }
                            scope.resource.value = tab.join(", ");
                        }
                        scope.resource.valuesupposed = false;
                        scope.resource.tooltip = formattedDate;
                    }
                }).error(function(data, status, headers, config) {
                    errormessage = "Unable to start observation on resource " + scope.resource.path + " for "+ $routeParams.clientId + " : " + status +" "+ data
                    dialog.open(errormessage);
                    console.error(errormessage)
                });;
            };
            
            scope.stopObserve = function() {
                var uri = "api/clients/" + $routeParams.clientId + scope.resource.path + "/observe";
                $http.delete(uri)
                .success(function(data, status, headers, config) {
                    scope.resource.observed = false;
                    scope.resource.observe.stop = "success";
                }).error(function(data, status, headers, config) {
                    errormessage = "Unable to stop observation on resource " + scope.resource.path + " for "+ $routeParams.clientId + " : " + status +" "+ data
                    dialog.open(errormessage);
                    console.error(errormessage)
                });;
            };
            
            
            scope.read = function() {
                var format = scope.settings.single.format;
                var uri = "api/clients/" + $routeParams.clientId + scope.resource.path;
                $http.get(uri, {params:{format:format}})
                .success(function(data, status, headers, config) {
                    // manage request information
                    var read = scope.resource.read;
                    read.date = new Date();
                    var formattedDate = $filter('date')(read.date, 'HH:mm:ss.sss');
                    read.status = data.status;
                    read.tooltip = formattedDate + "<br/>" + read.status;
                    
                    // manage read data
                    if (data.status == "CONTENT" && data.content) {
                        if("value" in data.content) {
                            // single value
                            scope.resource.value = data.content.value
                        }
                    else if("values" in data.content) {
                            // multiple instances
                            var tab = new Array();
                            for (var i in data.content.values) {
                                tab.push(i+"="+data.content.values[i])
                            }
                            scope.resource.value = tab.join(", ");
                        }
                        scope.resource.valuesupposed = false;
                        scope.resource.tooltip = formattedDate;
                    }
                }).error(function(data, status, headers, config) {
                    errormessage = "Unable to read resource " + scope.resource.path + " for "+ $routeParams.clientId + " : " + status +" "+ data
                    dialog.open(errormessage);
                    console.error(errormessage)
                });;
            };

            scope.write = function() {
                $('#writeModalLabel').text(scope.resource.def.name);
                $('#writeInputValue').val(scope.resource.value);
                $('#writeSubmit').unbind();
                $('#writeSubmit').click(function(e){
                    e.preventDefault();
                    var value = $('#writeInputValue').val();

                    if(value != undefined) {
                        $('#writeModal').modal('hide');

                        var rsc = {};
                        rsc["id"] = scope.resource.def.id;
                        value = lwResources.getTypedValue(value, scope.resource.def.type);
                        rsc["value"] = value;

                        var format = scope.settings.single.format;
                        $http({method: 'PUT', url: "api/clients/" + $routeParams.clientId + scope.resource.path, data: rsc, headers:{'Content-Type': 'application/json'},params:{format:format}})
                        .success(function(data, status, headers, config) {
                            write = scope.resource.write;
                            write.date = new Date();
                            var formattedDate = $filter('date')(write.date, 'HH:mm:ss.sss');
                            write.status = data.status;
                            write.tooltip = formattedDate + "<br/>" + write.status;
                            
                            if (data.status == "CHANGED") {
                                scope.resource.value = value;
                                scope.resource.valuesupposed = true;
                                scope.resource.tooltip = formattedDate;
                            }
                        }).error(function(data, status, headers, config) {
                            errormessage = "Unable to write resource " + scope.resource.path + " for "+ $routeParams.clientId + " : " + status +" "+ data
                            dialog.open(errormessage);
                            console.error(errormessage)
                        });;
                    }
                });

                $('#writeModal').modal('show');
            };

            scope.exec = function() {
                $http.post("api/clients/" + $routeParams.clientId+ scope.resource.path)
                .success(function(data, status, headers, config) {
                    var exec = scope.resource.exec;
                    exec.date = new Date();
                    var formattedDate = $filter('date')(exec.date, 'HH:mm:ss.sss');
                    exec.status = data.status;
                    exec.tooltip = formattedDate + "<br/>" + exec.status;
                    scope.resource.execwithparams.tooltip = exec.tooltip;
                }).error(function(data, status, headers, config) {
                    errormessage = "Unable to execute resource " + scope.resource.path + " for "+ $routeParams.clientId + " : " + status +" "+ data
                    dialog.open(errormessage);
                    console.error(errormessage)
                });;
            };

            scope.execWithParams = function() {
                $('#writeModalLabel').text(scope.resource.def.name);
                $('#writeInputValue').val(scope.resource.value);
                $('#writeSubmit').unbind();
                $('#writeSubmit').click(function(e){
                    e.preventDefault();
                    var value = $('#writeInputValue').val();

                    if(value) {
                        $('#writeModal').modal('hide');

                        $http({method: 'POST', url: "api/clients/" + $routeParams.clientId + scope.resource.path, data: value})
                        .success(function(data, status, headers, config) {
                            exec = scope.resource.exec;
                            exec.date = new Date();
                            var formattedDate = $filter('date')(exec.date, 'HH:mm:ss.sss');
                            exec.status = data.status;
                            exec.tooltip = formattedDate + "<br/>" + exec.status;
                            scope.resource.execwithparams.tooltip = exec.tooltip;
                        }).error(function(data, status, headers, config) {
                            errormessage = "Unable to execute resource " + scope.resource.path + " for "+ $routeParams.clientId + " : " + status +" "+ data
                            dialog.open(errormessage);
                            console.error(errormessage);
                        });
                    }
                });
                $('#writeModal').modal('show');
            };
        }
    }
});
