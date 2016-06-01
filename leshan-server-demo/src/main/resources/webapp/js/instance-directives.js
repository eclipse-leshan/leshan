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

angular.module('instanceDirectives', [])

.directive('instance', function ($compile, $routeParams, $http, dialog,$filter, lwResources, $modal) {
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

            scope.read = function() {
                var format = scope.settings.multi.format;
                var uri = "api/clients/" + $routeParams.clientId + scope.instance.path;
                $http.get(uri, {params:{format:format}})
                .success(function(data, status, headers, config) {
                    // manage request information
                    var read = scope.instance.read;
                    read.date = new Date();
                    var formattedDate = $filter('date')(read.date, 'HH:mm:ss.sss');
                    read.status = data.status;
                    read.tooltip = formattedDate + "<br/>" + read.status;

                    // manage read data
                    if (data.status == "CONTENT" && data.content) {
                        for(var i in data.content.resources) {
                            var tlvresource = data.content.resources[i];
                            resource = lwResources.addResource(scope.parent, scope.instance, tlvresource.id, null)
                            if("value" in tlvresource) {
                                // single value
                                resource.value = tlvresource.value
                            }
                            else if("values" in tlvresource) {
                                // multiple instances
                                var tab = new Array();
                                for (var j in tlvresource.values) {
                                    tab.push(j+"="+tlvresource.values[j])
                                }
                                resource.value = tab.join(", ");
                            }
                            resource.valuesupposed = false;
                            resource.tooltip = formattedDate;
                        }
                    }
                }).error(function(data, status, headers, config) {
                    errormessage = "Unable to read instance " + scope.instance.path + " for "+ $routeParams.clientId + " : " + status +" "+ data
                    dialog.open(errormessage);
                    console.error(errormessage)
                });;
            };


            scope.del = function() {
                var uri = "api/clients/" + $routeParams.clientId + scope.instance.path;
                $http.delete(uri)
                .success(function(data, status, headers, config) {
                    // manage request information
                    var del = scope.instance.del;
                    del.date = new Date();
                    var formattedDate = $filter('date')(del.date, 'HH:mm:ss.sss');
                    del.status = data.status;
                    del.tooltip = formattedDate + "<br/>" + del.status;

                    // manage delete instance in resource tree.
                    if (data.status == "DELETED") {
                        scope.parent.instances.splice(scope.instance,1);
                    }
                }).error(function(data, status, headers, config) {
                    errormessage = "Unable to delete instance " + scope.instance.path + " for "+ $routeParams.clientId + " : " + status +" "+ data
                    dialog.open(errormessage);
                    console.error(errormessage)
                });;
            };

            scope.write = function () {
                var modalInstance = $modal.open({
                  templateUrl: 'partials/modal-instance.html',
                  controller: 'modalInstanceController',
                  resolve: {
                    object: function(){ return scope.parent},
                    instanceId: function(){return scope.instance.id}
                  }
                });

                modalInstance.result.then(function (instance) {
                    // Build payload
                    var payload = {};
                    payload["id"] = scope.instance.id;
                    payload["resources"] = [];

                    for(i in instance.resources){
                        var resource = instance.resources[i];
                        if (resource.value != undefined){
                            payload.resources.push({
                                id:resource.id,
                                value:lwResources.getTypedValue(resource.value, resource.def.type)
                            })
                        }
                    }
                    // Send request
                    var format = scope.settings.multi.format;
                    $http({method: 'PUT', url: "api/clients/" + $routeParams.clientId + scope.instance.path, data: payload, headers:{'Content-Type': 'application/json'}, params:{format:format}})
                    .success(function(data, status, headers, config) {
                        write = scope.instance.write;
                        write.date = new Date();
                        var formattedDate = $filter('date')(write.date, 'HH:mm:ss.sss');
                        write.status = data.status;
                        write.tooltip = formattedDate + "<br/>" + write.status;

                        if (data.status == "CHANGED") {
                            for (var i in payload.resources) {
                                var tlvresource = payload.resources[i];
                                resource = lwResources.addResource(scope.parent, scope.instance, tlvresource.id, null)
                                resource.value = tlvresource.value;
                                resource.valuesupposed = true;
                                resource.tooltip = formattedDate;
                            }
                        }
                    }).error(function(data, status, headers, config) {
                        errormessage = "Unable to write resource " + scope.instance.path + " for "+ $routeParams.clientId + " : " + status +" "+ data
                        dialog.open(errormessage);
                        console.error(errormessage)
                    });;

                });
            };

            scope.startObserve = function() {
                var format = scope.settings.multi.format;
                var uri = "api/clients/" + $routeParams.clientId + scope.instance.path+"/observe";
                $http.post(uri, null, {params:{format:format}})
                .success(function(data, status, headers, config) {
                    var observe = scope.instance.observe;
                    observe.date = new Date();
                    var formattedDate = $filter('date')(observe.date, 'HH:mm:ss.sss');
                    observe.status = data.status;
                    observe.tooltip = formattedDate + "<br/>" + observe.status;

                    if (data.status == "CONTENT") {
                        scope.instance.observed = true;

                        for(var i in data.content.resources) {
                            var tlvresource = data.content.resources[i];
                            resource = lwResources.addResource(scope.parent, scope.instance, tlvresource.id, null)
                            if("value" in tlvresource) {
                                // single value
                                resource.value = tlvresource.value
                            }
                            else if("values" in tlvresource) {
                                // multiple instances
                                var tab = new Array();
                                for (var j in tlvresource.values) {
                                    tab.push(j+"="+tlvresource.values[j])
                                }
                                resource.value = tab.join(", ");
                            }
                            resource.valuesupposed = false;
                            resource.tooltip = formattedDate;
                        }


                        scope.instance.valuesupposed = false;
                        scope.instance.tooltip = formattedDate;
                    }
                }).error(function(data, status, headers, config) {
                    errormessage = "Unable to start observation on instance " + scope.instance.path + " for "+ $routeParams.clientId + " : " + status +" "+ data
                    dialog.open(errormessage);
                    console.error(errormessage)
                });;
                
            };

            scope.stopObserve = function() {
                var uri = "api/clients/" + $routeParams.clientId + scope.instance.path + "/observe";
                $http.delete(uri)
                .success(function(data, status, headers, config) {
                    scope.instance.observed = false;
                    scope.instance.observe.stop = "success";
                }).error(function(data, status, headers, config) {
                    errormessage = "Unable to stop observation on instance " + scope.instance.path + " for "+ $routeParams.clientId + " : " + status +" "+ data
                    dialog.open(errormessage);
                    console.error(errormessage)
                });;
            };
        }
    }
});
