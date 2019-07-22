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

function getBase64(file) {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.readAsDataURL(file);
        reader.onload = () => resolve(reader.result);
        reader.onerror = error => reject(error);
    });
}

angular.module('resourceDirectives', [])

.directive('resource', function ($compile, $routeParams, $http, dialog, $filter, lwResources, helper) {
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
            scope.resource.execwithparams = {tooltip : "Execute with parameters<br/>"+ scope.resource.path};
            scope.resource.observe  =  {tooltip : "Observe <br/>"+ scope.resource.path};
            
            scope.readable = function() {
                if(scope.resource.def.hasOwnProperty("operations")) {
                    return scope.resource.def.operations.indexOf("R") != -1;
                }
                return false;
            };
           
            scope.writable = function() {
                if(scope.resource.def.instancetype != "multiple") {
                    if(scope.resource.def.hasOwnProperty("operations")) {
                        return scope.resource.def.operations.indexOf("W") != -1;
                    }
                }
                return false;
            };

            scope.executable = function() {
                if(scope.resource.def.instancetype != "multiple") {
                    if(scope.resource.def.hasOwnProperty("operations")) {
                        return scope.resource.def.operations.indexOf("E") != -1;
                    }
                }
                return false;
            };

            scope.startObserve = function() {
                var format = scope.settings.single.format;
                var uri = "api/clients/" + $routeParams.clientId + scope.resource.path+"/observe";
                $http.post(uri, null,{params:{format:format}})
                .success(function(data, status, headers, config) {
                	helper.handleResponse(data, scope.resource.observe, function (formattedDate){
                		if (data.success) {
                            scope.resource.observed = true;
                            if("value" in data.content) {
                                // single value
                                scope.resource.value = data.content.value;
                            }
                            else if("values" in data.content) {
                                // multiple instances
                                var tab = new Array();
                                for (var i in data.content.values) {
                                    tab.push(i+"="+data.content.values[i]);
                                }
                                scope.resource.value = tab.join(", ");
                            }
                            scope.resource.valuesupposed = false;
                            scope.resource.tooltip = formattedDate;
                        }	
                	});
                }).error(function(data, status, headers, config) {
                    errormessage = "Unable to start observation on resource " + scope.resource.path + " for "+ $routeParams.clientId + " : " + status +" "+ data;
                    dialog.open(errormessage);
                    console.error(errormessage);
                });
            };
            
            scope.stopObserve = function() {
                var uri = "api/clients/" + $routeParams.clientId + scope.resource.path + "/observe";
                $http.delete(uri)
                .success(function(data, status, headers, config) {
                    scope.resource.observed = false;
                    scope.resource.observe.stop = "success";
                }).error(function(data, status, headers, config) {
                    errormessage = "Unable to stop observation on resource " + scope.resource.path + " for "+ $routeParams.clientId + " : " + status +" "+ data;
                    dialog.open(errormessage);
                    console.error(errormessage);
                });
            };
            
            
            scope.read = function() {
                var format = scope.settings.single.format;
                var uri = "api/clients/" + $routeParams.clientId + scope.resource.path;
                $http.get(uri, {params:{format:format}})
                .success(function(data, status, headers, config) {
                    // manage request information
                	helper.handleResponse(data, scope.resource.read, function (formattedDate){
                		if (data.success && data.content) {
                            if("value" in data.content) {
                                // single value
                                scope.resource.value = data.content.value;
                            }
                            else if("values" in data.content) {
                                // multiple instances
                                var tab = new Array();
                                for (var i in data.content.values) {
                                    tab.push(i+"="+data.content.values[i]);
                                }
                                scope.resource.value = tab.join(", ");
                            }
                            scope.resource.valuesupposed = false;
                            scope.resource.tooltip = formattedDate;
                        }                		
                	});
                }).error(function(data, status, headers, config) {
                    errormessage = "Unable to read resource " + scope.resource.path + " for "+ $routeParams.clientId + " : " + status +" "+ data;
                    dialog.open(errormessage);
                    console.error(errormessage);
                });
            };

            scope.write = function() {
                $('#writeSubmit').unbind();
                $('#writeModalLabel').text(scope.resource.def.name);

                if(scope.resource.def.type === 'opaque') {
                    $('#writeModalTextInput').hide();
                    $('#writeModalFileInput').show();

                    $('#writeInputFile').val(null);
                    $('#writeSubmit').click(function(e){
                        e.preventDefault();
                        var file = $('#writeInputFile').prop('files')[0];

                        getBase64(file).then(function (value) {
                            if (value != undefined) {
                                $('#writeModal').modal('hide');

                                var rsc = {};
                                rsc["id"] = scope.resource.def.id;
                                value = lwResources.getTypedValue(value, scope.resource.def.type);
                                rsc["base64Value"] = value;

                                var format = scope.settings.single.format;
                                $http({ method: 'PUT', url: "api/clients/" + $routeParams.clientId + scope.resource.path, data: rsc, headers: { 'Content-Type': 'application/json' }, params: { format: format } })
                                    .success(function (data, status, headers, config) {
                                        helper.handleResponse(data, scope.resource.write, function (formattedDate) {
                                            if (data.success) {
                                                scope.resource.value = "";
                                                scope.resource.valuesupposed = true;
                                                scope.resource.tooltip = formattedDate;
                                            }
                                        });
                                    }).error(function (data, status, headers, config) {
                                        errormessage = "Unable to write resource " + scope.resource.path + " for " + $routeParams.clientId + " : " + status + " " + data;
                                        dialog.open(errormessage);
                                        console.error(errormessage);
                                    });
                            }
                        });
                    });
                } else {
                    $('#writeModalTextInput').show();
                    $('#writeModalFileInput').hide();

                    $('#writeInputValue').val(scope.resource.value);
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
                                helper.handleResponse(data, scope.resource.write, function (formattedDate){
                                    if (data.success) {
                                        scope.resource.value = value;
                                        scope.resource.valuesupposed = true;
                                        scope.resource.tooltip = formattedDate;
                                    }
                                });
                            }).error(function(data, status, headers, config) {
                                errormessage = "Unable to write resource " + scope.resource.path + " for "+ $routeParams.clientId + " : " + status +" "+ data;
                                dialog.open(errormessage);
                                console.error(errormessage);
                            });
                        }
                    });
                }

                $('#writeModal').modal('show');
            };

            scope.exec = function() {
                $http.post("api/clients/" + $routeParams.clientId+ scope.resource.path)
                .success(function(data, status, headers, config) {
                	helper.handleResponse(data, scope.resource.exec);
                }).error(function(data, status, headers, config) {
                    errormessage = "Unable to execute resource " + scope.resource.path + " for "+ $routeParams.clientId + " : " + status +" "+ data;
                    dialog.open(errormessage);
                    console.error(errormessage);
                });
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
                        	helper.handleResponse(data, scope.resource.exec);                            
                        }).error(function(data, status, headers, config) {
                            errormessage = "Unable to execute resource " + scope.resource.path + " for "+ $routeParams.clientId + " : " + status +" "+ data;
                            dialog.open(errormessage);
                            console.error(errormessage);
                        });
                    }
                });
                $('#writeModal').modal('show');
            };
        }
    };
});
