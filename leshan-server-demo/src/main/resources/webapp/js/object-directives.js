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

angular.module('objectDirectives', [])

.directive('object', function ($compile, $routeParams, $http, dialog, $filter, $modal, lwResources, helper) {
    return {
        restrict: "E",
        replace: true,
        scope: {
            object: '=',
            settings: '='
        },
        templateUrl: "partials/object.html",
        link: function (scope, element, attrs) {
            var parentPath = "";
            scope.status = {};
            scope.status.open = true;
            
            scope.object.path = parentPath + "/" + scope.object.id;
            scope.object.create  =  {tooltip : "Create <br/>"   + scope.object.path};
            
            scope.create = function () {
                var modalInstance = $modal.open({
                  templateUrl: 'partials/modal-instance.html',
                  controller: 'modalInstanceController',
                  resolve: {
                    object: function(){ return scope.object;},
                    instanceId: function(){ return null;},
                  }
                });
            
                modalInstance.result.then(function (result) {
                    var instance = result.instance;
                    promisedValues = instance.resources.map(r => r.getPromisedValue())
                    Promise.all(promisedValues).then(function(resourceValues) {
                        // Build payload
                        var payload = {};
                        if (instance.id)
                            payload["id"] = instance.id;
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
                        var instancepath  = scope.object.path;
                        $http({method: 'POST', url: "api/clients/" + $routeParams.clientId + instancepath, data: payload, headers:{'Content-Type': 'application/json'}, params:{format:format,timeout:timeout}})
                        .success(function(data, status, headers, config) {
                            helper.handleResponse(data, scope.object.create, function (formattedDate) {
                                if (data.success) {
                                    var instanceId = data.location ? data.location.split('/').pop() : instance.id;
                                    var newinstance = lwResources.addInstance(scope.object, instanceId, null);
                                    for (var i in payload.resources) {
                                        var tlvresource = payload.resources[i];
                                        resource = lwResources.addResource(scope.object, newinstance, tlvresource.id, null);
                                        resource.value = tlvresource.value;
                                        resource.valuesupposed = true;
                                        resource.tooltip = formattedDate;
                                    }
                                }
                            });
                        }).error(function(data, status, headers, config) {
                            if (status == 504){
                                helper.handleResponse(null, scope.object.create)
                            } else {
                                errormessage = "Unable to create instance " + instancepath + " for "+ $routeParams.clientId + " : " + status +" "+ data;
                                dialog.open(errormessage);
                            }
                            console.error(errormessage);
                        });
                    });
                });
            };
        }
    };
});
