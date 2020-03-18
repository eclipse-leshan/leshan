/*******************************************************************************
 * Copyright (c) 2013-2019 Sierra Wireless and others.
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

angular.module('modalResourceControllers', [])

.controller('modalResourceController',[
    '$scope',
    '$modalInstance',
    'instance',
    'resource',
    function($scope, $modalInstance, instance, resource) {
        // configure modal
        $scope.title = "Update resource " + resource.def.name;
        $scope.instance = instance;
        // create resource working copy
        $scope.resource = {
            def : resource.def,
            id : resource.def.id,
        };

        // Define button function 
        $scope.submit = function() {
            $scope.$broadcast('show-errors-check-validity');
            if ($scope.form.$valid){
                $modalInstance.close($scope.resource);
            }
        };

        $scope.cancel = function() {
            $modalInstance.dismiss();
        };
    }
]);
