/*******************************************************************************
 * Copyright (c) 2017 Sierra Wireless and others.
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

var myModule = angular.module('helperServices', []);

myModule.factory('helper', ["$filter", function($filter) {
  var serviceInstance = {};
  
  serviceInstance.handleResponse = function (response, lwm2mNode, successCallback) {
      lwm2mNode.date = new Date();
      var formattedDate = $filter('date')(lwm2mNode.date, 'HH:mm:ss.sss');
      if (response != null){
          if (!response.valid){
              lwm2mNode.status = "INVALID";  
          }else if (response.success){
              lwm2mNode.status = "SUCCESS";
          }else {
              lwm2mNode.status = "ERROR";
          }
    
          if (response.valid)
              lwm2mNode.tooltip = formattedDate + "<br/>" + response.status ;
          else
              lwm2mNode.tooltip = formattedDate + "<br/> Not LWM2M Code <br/>" + response.status;
          
          if (response.errormessage)
              lwm2mNode.tooltip = lwm2mNode.tooltip + "<br/>" + response.errormessage;

          if (successCallback && response.success) {
              successCallback(formattedDate);
          }
      } else {
          lwm2mNode.status = "TIMEOUT";
          lwm2mNode.tooltip = formattedDate + "<br/>" + lwm2mNode.status ;
      }
  };
  return serviceInstance;
}]);