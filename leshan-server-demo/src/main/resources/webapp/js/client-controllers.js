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
 *     Achim Kraus (Bosch Software Innovations GmbH) - fix typo in notificationCallback
 *                                                     processing multiple resources
 *******************************************************************************/

var lwClientControllers = angular.module('clientControllers', []);

// Update client in a list of clients (replaces the client with the same endpoint))
function updateClient(updated, clients) {
    return clients.reduce(function(accu, client) {
        if (updated.endpoint === client.endpoint) {
            accu.push(updated);
        } else {
            accu.push(client);
        }
        return accu;
    }, []);
}

lwClientControllers.controller('ClientListCtrl', [
    '$scope',
    '$http',
    '$location',
    function ClientListCtrl($scope, $http,$location) {

        // update navbar
        angular.element("#navbar").children().removeClass('active');
        angular.element("#client-navlink").addClass('active');

        // free resource when controller is destroyed
        $scope.$on('$destroy', function(){
            if ($scope.eventsource){
                $scope.eventsource.close();
            }
        });

        // add function to show client
        $scope.showClient = function(client) {
            $location.path('/clients/' + client.endpoint);
        };

        // the tooltip message to display for a client (all standard attributes, plus additional ones)
        $scope.clientTooltip = function(client) {
            var standard = ["Lifetime: " + client.lifetime + "s",
                            "Binding mode: " + client.bindingMode,
                            "Protocol version: " + client.lwM2mVersion,
                            "Address: " + client.address];

            var tooltip = standard.join("<br/>");
            if (client.additionalRegistrationAttributes) {
                var attributes = client.additionalRegistrationAttributes;
                var additionals = [];
                for (key in attributes) {
                    var value = attributes[key];
                    additionals.push(key + " : " + value);
                }
                if (additionals.length>0){
                    tooltip = tooltip + "<hr/>" + additionals.join("<br/>");
                }
            }
            return tooltip;
        };

        // get the list of connected clients
        $http.get('api/clients'). error(function(data, status, headers, config){
            $scope.error = "Unable to get client list: " + status + " " + data;
            console.error($scope.error);
        }).success(function(data, status, headers, config) {
            $scope.clients = data;

            // HACK : we can not use ng-if="clients"
            // because of https://github.com/angular/angular.js/issues/3969
            $scope.clientslist = true;

            // listen for clients registration/deregistration
            $scope.eventsource = new EventSource('event');

            var registerCallback = function(msg) {
                $scope.$apply(function() {
                    var client = JSON.parse(msg.data);
                    $scope.clients.push(client);
                });
            };

            var updateCallback =  function(msg) {
                $scope.$apply(function() {
                    var client = JSON.parse(msg.data);
                    $scope.clients = updateClient(client, $scope.clients);
                });
            };

            var sleepingCallback =  function(msg) {
                $scope.$apply(function() {
                    var data = JSON.parse(msg.data);
                    for (var i = 0; i < $scope.clients.length; i++) {
                        if ($scope.clients[i].endpoint === data.ep) {
                            $scope.clients[i].sleeping = true;
                        }
                    }
                });
            };

            var awakeCallback =  function(msg) {
                $scope.$apply(function() {
                    var data = JSON.parse(msg.data);
                    for (var i = 0; i < $scope.clients.length; i++) {
                        if ($scope.clients[i].endpoint === data.ep) {
                            $scope.clients[i].sleeping = false;
                        }
                    }
                });
            };

            $scope.eventsource.addEventListener('REGISTRATION', registerCallback, false);

            $scope.eventsource.addEventListener('UPDATED', updateCallback, false);

            $scope.eventsource.addEventListener('SLEEPING', sleepingCallback, false);

            $scope.eventsource.addEventListener('AWAKE', awakeCallback, false);

            var getClientIdx = function(client) {
                for (var i = 0; i < $scope.clients.length; i++) {
                    if ($scope.clients[i].registrationId == client.registrationId) {
                        return i;
                    }
                }
                return -1;
            };
            var deregisterCallback = function(msg) {
                $scope.$apply(function() {
                    var clientIdx = getClientIdx(JSON.parse(msg.data));
                    if(clientIdx >= 0) {
                        $scope.clients.splice(clientIdx, 1);
                    }
                });
            };
            $scope.eventsource.addEventListener('DEREGISTRATION', deregisterCallback, false);
        });
}]);

lwClientControllers.controller('ClientDetailCtrl', [
    '$scope',
    '$location',
    '$routeParams',
    '$http',
    'lwResources',
    '$filter',
    function($scope, $location, $routeParams, $http, lwResources,$filter) {
        // update navbar
        angular.element("#navbar").children().removeClass('active');
        angular.element("#client-navlink").addClass('active');

        // free resource when controller is destroyed
        $scope.$on('$destroy', function(){
            if ($scope.eventsource){
                $scope.eventsource.close();
            }
        });

        // default format
        $scope.settings={};
        $scope.settings.multi = {format:"TLV"};
        $scope.settings.single = {format:"TLV"};

        $scope.clientId = $routeParams.clientId;

        // get client details
        $http.get('api/clients/' + $routeParams.clientId)
        .error(function(data, status, headers, config) {
            $scope.error = "Unable to get client " + $routeParams.clientId+" : "+ status + " " + data;
            console.error($scope.error);
        })
        .success(function(data, status, headers, config) {
            $scope.client = data;

            // update resource tree with client details
            lwResources.buildResourceTree($scope.client.rootPath, $scope.client.objectLinks, function (objects){
                $scope.objects = objects;
            });

            // listen for clients registration/deregistration/observe
            $scope.eventsource = new EventSource('event?ep=' + $routeParams.clientId);

            var registerCallback = function(msg) {
                $scope.$apply(function() {
                    $scope.deregistered = false;
                    $scope.client = JSON.parse(msg.data);
                    lwResources.buildResourceTree($scope.client.rootPath, $scope.client.objectLinks, function (objects){
                        $scope.objects = objects;
                    });
                });
            };
            $scope.eventsource.addEventListener('REGISTRATION', registerCallback, false);

            var deregisterCallback = function(msg) {
                $scope.$apply(function() {
                    $scope.deregistered = true;
                    $scope.client = null;
                });
            };
            $scope.eventsource.addEventListener('DEREGISTRATION', deregisterCallback, false);

            var notificationCallback = function(msg) {
                $scope.$apply(function() {
                    var content = JSON.parse(msg.data);
                    var resource = lwResources.findResource($scope.objects, content.res);
                    if (resource) {
                        if("value" in content.val) {
                            // single value
                            resource.value = content.val.value;
                        }
                        else if("values" in content.val) {
                            // multiple instances
                            var tab = new Array();
                            for (var i in content.val.values) {
                                tab.push(i+"="+content.val.values[i]);
                            }
                            resource.value = tab.join(", ");
                        }
                        resource.valuesupposed = false;
                        resource.observed = true;

                        var formattedDate = $filter('date')(new Date(), 'HH:mm:ss.sss');
                        resource.tooltip = formattedDate;
                    } else {
                        // instance?
                        var instance = lwResources.findInstance($scope.objects, content.res);
                        if (instance) {
                            instance.observed = true;
                            for(var i in content.val.resources) {
                                var tlvresource = content.val.resources[i];
                                resource = lwResources.addResource(instance.parent, instance, tlvresource.id, null);
                                if("value" in tlvresource) {
                                    // single value
                                    resource.value = tlvresource.value;
                                } else if("values" in tlvresource) {
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
                    } // TODO object level
                });
            };
            $scope.eventsource.addEventListener('NOTIFICATION', notificationCallback, false);

            $scope.coaplogs = [];
            var coapLogCallback = function(msg) {
                $scope.$apply(function() {
                    var log = JSON.parse(msg.data);
                    log.date = $filter('date')(new Date(log.timestamp), 'HH:mm:ss.sss');
                    if (256 < $scope.coaplogs.length) $scope.coaplogs.shift();
                    $scope.coaplogs.push(log);
                });
            };
            $scope.eventsource.addEventListener('COAPLOG', coapLogCallback, false);

            // coap logs hidden by default
            $scope.coapLogsCollapsed = true;
            $scope.toggleCoapLogs = function() {
                $scope.coapLogsCollapsed = !$scope.coapLogsCollapsed;
            };
        });
    }]);
