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

angular.module('instanceDirectives', [])

.directive('instance', function ($compile, $routeParams, $http, dialog,$filter, lwResources, $modal, helper) {
    return {
        restrict: "E",
        replace: true,
        scope: {
            instance: '=',
            parent: '=',
            settings: '='
        },
        templateUrl: "partials/instance.html",
        link: function (scope, element, attrs) {
            var parentPath = "";
            scope.instance.path = scope.parent.path + "/" + scope.instance.id;

            scope.instance.read  =  {tooltip : "Read <br/>"   + scope.instance.path};
            scope.instance.write =  {tooltip : "Write <br/>"  + scope.instance.path};
            scope.instance.del  =  {tooltip : "Delete <br/>"   + scope.instance.path};
            scope.instance.observe = {tooltip : "Observe <br/>" + scope.instance.path};

            scope.writable = function() {
                return scope.instance.resources.find(resource => resource.def.operations === "W" || resource.def.operations === "RW");
            };

            scope.read = function() {
                var format = scope.settings.multi.format;
                var timeout = scope.settings.timeout.value;
                var uri = "api/clients/" + $routeParams.clientId + scope.instance.path;
                $http.get(uri, {params:{format:format, timeout:timeout}})
                .success(function(data, status, headers, config) {
                    helper.handleResponse(data, scope.instance.read, function (formattedDate){
                        if (data.success && data.content) {
                            for(var i in data.content.resources) {
                                var tlvresource = data.content.resources[i];
                                resource = lwResources.addResource(scope.parent, scope.instance, tlvresource.id, null);
                                if("value" in tlvresource) {
                                    // single value
                                    resource.value = tlvresource.value;
                                }
                                else if("values" in tlvresource) {
                                    // multiple instances
                                    var tab = new Array();
                                    for (var j in tlvresource.values) {
                                        tab.push(j+"="+tlvresource.values[j]);
                                    }
                                    resource.value = tab.join(", ");
                                }
                                resource.valuesupposed = false;
                                resource.tooltip = formattedDate;
                            }
                        }
                    });
                }).error(function(data, status, headers, config) {
                    if (status == 504){
                        helper.handleResponse(null, scope.instance.read);
                    } else {
                        errormessage = "Unable to read instance " + scope.instance.path + " for "+ $routeParams.clientId + " : " + status +" "+ data;
                        dialog.open(errormessage);
                    }
                    console.error(errormessage);
                });
            };

            scope.del = function() {
                var timeout = scope.settings.timeout.value;
                var uri = "api/clients/" + $routeParams.clientId + scope.instance.path;
                $http.delete(uri, {params:{timeout:timeout}})
                .success(function(data, status, headers, config) {
                    helper.handleResponse(data, scope.instance.del, function (formattedDate){
                        // manage delete instance in resource tree.
                        if (data.success) {
                            var i = scope.parent.instances.indexOf(scope.instance);
                            if(i != -1) {
                                scope.parent.instances.splice(i, 1);
                            }
                        }
                    });
                }).error(function(data, status, headers, config) {
                    if (status == 504){
                        helper.handleResponse(null, scope.instance.del);
                    } else {
                        errormessage = "Unable to delete instance " + scope.instance.path + " for "+ $routeParams.clientId + " : " + status +" "+ data;
                        dialog.open(errormessage);
                    }
                    console.error(errormessage);
                });
            };

            scope.write = function () {
                var modalInstance = $modal.open({
                  templateUrl: 'partials/modal-instance.html',
                  controller: 'modalInstanceController',
                  resolve: {
                    object: function(){ return scope.parent;},
                    instanceId: function(){return scope.instance.id;}
                  }
                });

                modalInstance.result.then(function (result) {
                    var instance = result.instance
                    promisedValues = instance.resources.map(r => r.getPromisedValue())
                    Promise.all(promisedValues).then(function(resourceValues) {
                        // Build payload
                        var payload = {};
                        payload["id"] = scope.instance.id;
                        payload["resources"] = [];

                        for(i in instance.resources){
                            var resource = instance.resources[i];
                            var resourceValue = resourceValues[i];
                            if (resourceValue != undefined){
                                payload.resources.push({
                                    id:resource.id,
                                    value:lwResources.getTypedValue(resourceValue, resource.def.type)
                                });
                            }
                        }
                        // Send request
                        var format = scope.settings.multi.format;
                        var timeout = scope.settings.timeout.value;
                        $http({method: 'PUT', url: "api/clients/" + $routeParams.clientId + scope.instance.path, data: payload, headers:{'Content-Type': 'application/json'}, params:{format:format, replace:result.replace, timeout:timeout}})
                        .success(function(data, status, headers, config) {
                            helper.handleResponse(data, scope.instance.write, function (formattedDate) {
                                if (data.success) {
                                    for (var i in payload.resources) {
                                        var tlvresource = payload.resources[i];
                                        resource = lwResources.addResource(scope.parent, scope.instance, tlvresource.id, null);
                                        resource.value = tlvresource.value;
                                        resource.valuesupposed = true;
                                        resource.tooltip = formattedDate;
                                    }
                                }
                            });
                        }).error(function(data, status, headers, config) {
                            if (status == 504){
                                helper.handleResponse(null, scope.instance.write);
                            } else {
                                errormessage = "Unable to write resource " + scope.instance.path + " for "+ $routeParams.clientId + " : " + status +" "+ data;
                                dialog.open(errormessage);
                            }
                            console.error(errormessage);
                        });
                    });
                });
            };

            scope.startObserve = function() {
                var format = scope.settings.multi.format;
                var timeout = scope.settings.timeout.value;
                var uri = "api/clients/" + $routeParams.clientId + scope.instance.path+"/observe";
                $http.post(uri, null, {params:{format:format, timeout:timeout}})
                .success(function(data, status, headers, config) {
                    helper.handleResponse(data, scope.instance.observe, function (formattedDate) {
                        if (data.success) {
                            scope.instance.observed = true;

                            for(var i in data.content.resources) {
                                var tlvresource = data.content.resources[i];
                                resource = lwResources.addResource(scope.parent, scope.instance, tlvresource.id, null);
                                if("value" in tlvresource) {
                                    // single value
                                    resource.value = tlvresource.value;
                                }
                                else if("values" in tlvresource) {
                                    // multiple instances
                                    var tab = new Array();
                                    for (var j in tlvresource.values) {
                                        tab.push(j+"="+tlvresource.values[j]);
                                    }
                                    resource.value = tab.join(", ");
                                }
                                resource.valuesupposed = false;
                                resource.tooltip = formattedDate;
                            }

                            scope.instance.valuesupposed = false;
                            scope.instance.tooltip = formattedDate;
                        }
                    });
                }).error(function(data, status, headers, config) {
                    if (status == 504){
                        helper.handleResponse(null, scope.instance.observe);
                    } else {
                        errormessage = "Unable to start observation on instance " + scope.instance.path + " for "+ $routeParams.clientId + " : " + status +" "+ data;
                        dialog.open(errormessage);
                    }
                    console.error(errormessage);
                });
                
            };

            scope.stopObserve = function() {
                var uri = "api/clients/" + $routeParams.clientId + scope.instance.path + "/observe";
                $http.delete(uri)
                .success(function(data, status, headers, config) {
                    scope.instance.observed = false;
                    scope.instance.observe.stop = "success";
                }).error(function(data, status, headers, config) {
                    errormessage = "Unable to stop observation on instance " + scope.instance.path + " for "+ $routeParams.clientId + " : " + status +" "+ data;
                    dialog.open(errormessage);
                    console.error(errormessage);
                });
            };
        }
    };
});
