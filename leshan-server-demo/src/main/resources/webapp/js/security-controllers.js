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

angular.module('securityControllers', [])

.controller('SecurityCtrl', [
    '$scope',
    '$http',
    'dialog',
    function SecurityCtrl($scope, $http, dialog) {

        function toHex(byteArray){
            var hex = [];
            for (var i in byteArray){
                hex[i] = byteArray[i].toString(16).toUpperCase();
                if (hex[i].length === 1){
                    hex[i] = '0' + hex[i];
                }
            }
            return hex.join('');
        };
        function base64ToBytes(base64){
            var byteKey = atob(base64);
            var byteKeyLength = byteKey.length;
            var array = new Uint8Array(new ArrayBuffer(byteKeyLength));
            for(i = 0; i < byteKeyLength; i++) {
              array[i] = byteKey.charCodeAt(i);
            }
            return array;
        }
        $scope.wrap = (s) => s.replace(
                /([^\n]{1,32})/g, '$1\n'
        );

        // update navbar
        angular.element("#navbar").children().removeClass('active');
        angular.element("#security-navlink").addClass('active');

        // get the list of security info by end-point
        $http.get('api/security/clients'). error(function(data, status, headers, config){
            $scope.error = "Unable to get the clients security info list: " + status + " " + data;
            console.error($scope.error);
        }).success(function(data, status, headers, config) {
            $scope.securityInfos = {};
            for (var i = 0; i < data.length; i++) {
                $scope.securityInfos[data[i].endpoint] = data[i];
            }
        });

        $http.get('api/security/server'). error(function(data, status, headers, config){
            $scope.error = "Unable to get the server security info list: " + status + " " + data;
            console.error($scope.error);
        }).success(function(data, status, headers, config) {
            if (data.certificate){
                $scope.certificate = data.certificate
                $scope.certificate.bytesDer = base64ToBytes($scope.certificate.b64Der);
                $scope.certificate.hexDer = toHex($scope.certificate.bytesDer);

                $scope.pubkey = data.certificate.pubkey;
                $scope.pubkey.bytesDer = base64ToBytes($scope.pubkey.b64Der);
                $scope.pubkey.hexDer = toHex($scope.pubkey.bytesDer);
            } else if (data.pubkey) {
                $scope.pubkey = data.pubkey;
                $scope.pubkey.bytesDer = base64ToBytes($scope.pubkey.b64Der);
                $scope.pubkey.hexDer = toHex($scope.pubkey.bytesDer);
            }
        });

        $scope.remove = function(endpoint) {
            $http({method: 'DELETE', url: "api/security/clients/" + endpoint, headers:{'Content-Type': 'text/plain'}})
            .success(function(data, status, headers, config) {
                delete $scope.securityInfos[endpoint];
           }).error(function(data, status, headers, config) {
               errormessage = "Unable to remove security info for endpoint " + endpoint + ": " + status + " - " + data;
               dialog.open(errormessage);
               console.error(errormessage);
            });
        };

        $scope.saveFile = function(filename, bytes) {
            var blob = new Blob([bytes], {type: "application/octet-stream"});
            saveAs(blob, filename);
        };

        $scope.save = function() {
            $scope.$broadcast('show-errors-check-validity');
            if ($scope.form.$valid) {
                if($scope.securityMode == "psk") {
                    var security = {endpoint: $scope.endpoint, psk : { identity : $scope.pskIdentity , key : $scope.pskValue}};
                } else if($scope.securityMode == "rpk") {
                    var security = {endpoint: $scope.endpoint, rpk : { key : $scope.rpkValue }};
                } else {
                    var security = {endpoint: $scope.endpoint, x509 : true};
                }
                if(security) {
                    $http({method: 'PUT', url: "api/security/clients/", data: security, headers:{'Content-Type': 'text/plain'}})
                    .success(function(data, status, headers, config) {
                        $scope.securityInfos[$scope.endpoint] = security;
                        $('#newSecurityModal').modal('hide');
                    }).error(function(data, status, headers, config) {
                        errormessage = "Unable to add security info for endpoint " + $scope.endpoint + ": " + status + " - " + data;
                        dialog.open(errormessage);
                        console.error(errormessage);
                    });
                }
            }
        };

        $scope.showModal = function() {
            $('#newSecurityModal').modal('show');
            $scope.$broadcast('show-errors-reset');
            $scope.endpoint = '';
            $scope.securityMode = 'psk';
            $scope.pskIdentity = '';
            $scope.pskValue = '';
            $scope.rpkXValue = '';
            $scope.rpkYValue = '';
            $scope.defaultParams = 'secp256r1';
       };
}])


/* directive to toggle error class on input fields */
.directive('showErrors', function($timeout) {
    return {
        restrict : 'A',
        require : '^form',
        link : function(scope, el, attrs, formCtrl) {
            // find the text box element, which has the 'name' attribute
            var inputEl = el[0].querySelector("[name]");
            // convert the native text box element to an angular element
            var inputNgEl = angular.element(inputEl);
            // get the name on the text box
            var inputName = inputNgEl.attr('name');

            // only apply the has-error class after the user leaves the text box
            inputNgEl.bind('blur', function() {
                el.toggleClass('has-error', formCtrl[inputName].$invalid);
            });

            scope.$on('show-errors-check-validity', function() {
                el.toggleClass('has-error', formCtrl[inputName].$invalid);
            });

            scope.$on('show-errors-reset', function() {
                $timeout(function() {
                    el.removeClass('has-error');
                }, 0, false);
            });
        }
    };
});
