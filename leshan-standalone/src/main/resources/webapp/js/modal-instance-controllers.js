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

angular.module('modalInstanceControllers', [])

.controller('modalInstanceController',[
    '$scope',
    '$modalInstance',
    'object',
    'instanceId',
    function($scope, $modalInstance, object, instanceId) {
        $scope.object = object;

        // Set dialog
        if (instanceId != undefined) {
            // Update mode
            $scope.title = "Update Instance  " + instanceId + " of " + object.name;
            $scope.oklabel = "Update";
            $scope.showinstanceid = false;
        } else {
            // Create mode
            $scope.title = "Create New Instance of " + object.name;
            $scope.oklabel = "Create";
            $scope.showinstanceid = true;
        }

        // Create a working object
        var instance = {
            name : "Instance " + instanceId,
            id : instanceId,
            resources : []
        };
        for (j in object.resourcedefs) {
            var resourcedef = object.resourcedefs[j]
            instance.resources.push({
                def : resourcedef,
                id : resourcedef.id
            });
        }
        $scope.instance = instance

        
        // Define button function 
        $scope.submit = function() {
            $scope.$broadcast('show-errors-check-validity');
            if ($scope.form.$valid){
                $modalInstance.close($scope.instance);
            }
        };
        $scope.cancel = function() {
            $modalInstance.dismiss();
        };
    }
]);
