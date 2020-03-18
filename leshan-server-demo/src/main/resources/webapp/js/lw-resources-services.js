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

var myModule = angular.module('lwResourcesServices', []);

var objectDefs; // cache for objectDefs

myModule.factory('lwResources',["$http", function($http) {
    var serviceInstance = {};

    /**
     * Get array from url string (e.g. : "/3/0/1" => [3,0,1])
     */
    var url2array = function(url) {
        if (url.length > 0 && url.charAt(0) === '/') {
            url = url.substr(1);
        }
        var array = url.split("/");
        // check if all element in the array is a number
        for (i in array) {
          if (isNaN(parseInt(array[i], 10))){
              return [];
          }
        }
        return array;
    };

    /**
     * Search an element in an array by id
     */
    var searchById = function(array, id) {
        for (i in array) {
            var elem = array[i];
            if (elem.id == id) {
                return elem;
            }
        }
        return null;
    };

    /**
     * Search a resource in the given tree
     */
    var findResource = function(tree, url) {
        var resourcepath = url2array(url);

        if (resourcepath.length == 3) {
            var object = searchById(tree, resourcepath[0]);
            if (object != undefined) {
                var instance = searchById(object.instances, resourcepath[1]);
                if (instance != undefined) {
                    return searchById(instance.resources, resourcepath[2]);
                }
            }
        }
        return null;
    };

    /**
     * Search an instance in the given tree
     */
    var findInstance = function(tree, url) {
        var instancepath = url2array(url);

        if (instancepath.length == 2) {
            var object = searchById(tree, instancepath[0]);
            if (object != undefined) {
                return searchById(object.instances, instancepath[1]);
            }
        }
        return null;
    };

    /**
     * Build Resource Tree for the given rootPath and objectLinks
     */
    var buildResourceTree = function(endpoint, rootPath, objectLinks, callback) {
        if (objectLinks.length == 0)
            callback([]);

        getObjectDefinitions(endpoint, function(objectDefs){
            var tree = [];

            for (var i = 0; i < objectLinks.length; i++) {

                // remove root path from link
                var link = objectLinks[i].url;
                if(link.indexOf(rootPath) == 0) {
                    link.slice(rootPath.length);
                }

                // get list of resources (e.g. : [3] or [1,123]
                var resourcepath = url2array(link);
                var attributes = objectLinks[i].attributes;

                switch (resourcepath.length) {
                case 0:
                    // ignore empty path
                    break;
                case 1:
                    // object
                    var object = addObject(tree, objectDefs, resourcepath[0],
                            attributes);

                    // manage single instance
                    if (object.instancetype != "multiple") {
                        addInstance(object, 0, null);
                    }

                    break;
                case 2:
                    // instance
                    var object = addObject(tree, objectDefs, resourcepath[0], null);
                    addInstance(object, resourcepath[1], attributes);

                    break;
                case 3:
                default:
                    // resource
                    var object = addObject(tree, objectDefs, resourcepath[0], null);
                    var instance = addInstance(object, resourcepath[1], null);
                    addResource(object, instance, resourcepath[2], attributes);

                    break;
                }
            }
            callback(tree);
        });
    };

    /**
     * Update Resource Tree for the given rootPath and objectLinks
     */
    var updateResourceTree = function(endpoint, tree, rootPath, objectLinks, callback) {
        if (objectLinks.length == 0)
            callback([]);

        getObjectDefinitions(endpoint, function(objectDefs){

            // add missing
            for (var i = 0; i < objectLinks.length; i++) {

                // remove root path from link
                var link = objectLinks[i].url;
                if(link.indexOf(rootPath) == 0) {
                    link.slice(rootPath.length);
                }

                // get list of resources (e.g. : [3] or [1,123]
                var resourcepath = url2array(link);
                var attributes = objectLinks[i].attributes;

                switch (resourcepath.length) {
                case 0:
                    // ignore empty path
                    break;
                case 1:
                    // object
                    var object = addObject(tree, objectDefs, resourcepath[0],
                            attributes);
                    break;
                case 2:
                    // instance
                    var object = addObject(tree, objectDefs, resourcepath[0], null);
                    addInstance(object, resourcepath[1], attributes);

                    break;
                }
            }

            // remove extra object instances
            result = tree.filter(function (object) {
                // remove extra instances
                object.instances = object.instances.filter(instance => objectLinks.find(link => link.url.startsWith(rootPath+object.id+"/"+instance.id)));
                // filter object
                return objectLinks.find(link => link.url.startsWith(rootPath+object.id));
            });

            // sort object
            result.sort(function(o1,o2){return o1.id - o2.id});

            callback(result);
        });
    };

    /**
     * add object with the given ID to resource tree if necessary and return it
     */
    var addObject = function(tree, objectDefs, objectId, attributes) {
        var object = searchById(tree, objectId);

        // if object is not already in the tree
        if (object == undefined) {
            // search object definition for this id
            object = searchById(objectDefs, objectId);

            // manage unknown object
            if (object == undefined) {
                object = {
                    name : "Object " + objectId,
                    unknown : true,
                    id : objectId,
                    instancetype : "multiple",
                    resourcedefs : []
                };
            }

            // add instances field to this object
            object.instances = [];

            // add object to tree
            tree.push(object);
        }
        if (attributes != undefined) {
            if (attributes.title != undefined) {
                object.name = attributes.title;
            } else if (attributes.rt != undefined) {
                object.name = attributes.rt;
            }
        }
        return object;
    };

    /**
     * add instance with the given ID to resource tree if necessary and return it
     */
    var addInstance = function(object, instanceId, attributes) {
        var instance = searchById(object.instances, instanceId);

        // create instance if necessary
        if (instance == undefined) {
            instance = {
                name : "Instance " + instanceId,
                id : instanceId,
                resources : []
            };

            for (j in object.resourcedefs) {
                var resourcedef = object.resourcedefs[j];
                instance.resources.push({
                    def : resourcedef,
                    id : resourcedef.id
                });
            }
            object.instances.push(instance);
        }
        if (attributes != undefined) {
            if (attributes.title != undefined) {
                instance.name = attributes.title;
            } else if (attributes.rt != undefined) {
                instance.name = attributes.rt;
            }
        }
        return instance;
    };

    /**
     * add resource with the given ID to resource tree if necessary and return it
     */
    var addResource = function(object, instance, resourceId, attributes) {
        var resource = searchById(instance.resources, resourceId);

        // create resource if necessary
        if (resource == undefined) {
            // create resource definition if necessary
            var resourcedef = searchById(object.resourcedefs, resourceId);
            if (resourcedef == undefined){
                var resourcedef = {
                    name : "Resource " + resourceId,
                    id : resourceId,
                    unknown : true,
                    operations : "RW",
                    type : "opaque"
                };
                object.resourcedefs.push(resourcedef);
            }

            // create resource
            resource = {
                def : resourcedef,
                id : resourceId,
            };
            instance.resources.push(resource);
        }
        if (attributes != undefined) {
            if (attributes.title != undefined) {
                resource.def.name = attributes.title;
            } else if (attributes.rt != undefined) {
                resource.def.name = attributes.rt;
            }
        }
        return resource;
    };

    var getTypedValue = function(strValue, type) {
        var val = strValue;
        if(type != undefined) {
            switch(type) {
                case "integer":
                    val = parseInt(strValue);
                    break;
                case "float":
                    val = parseFloat(strValue);
                    break;
                default:
                    val = strValue;
            }
        }
        return val;
    };

    /**
     * Load all the Object Definition known by the server.
     */
    var loadObjectDefinitions = function(endpoint, callback) {
        $http.get("api/objectspecs/"+endpoint)
        .success(function(data, status, headers, config) {
            if (data) {
                objectDefs = data;
                callback(objectDefs);
            }else{
                callback([]);
            }
        }).error(function(data, status, headers, config) {
            errormessage = "Unable to load object specfication : " + status +" "+ data;
            console.error(errormessage);
            callback([]);
        });
    };

    /**
     * Return a copy of model describing the LWM2M Objects defined by OMA
     */
    var getObjectDefinitions = function(endpoint,callback) {
        loadObjectDefinitions(endpoint, function(objectDefs){
            callback($.extend(true,[],objectDefs)); // make a deep copy of the cache
        });
    };

    serviceInstance.buildResourceTree = buildResourceTree;
    serviceInstance.updateResourceTree = updateResourceTree;
    serviceInstance.findResource = findResource;
    serviceInstance.findInstance = findInstance;
    serviceInstance.addInstance = addInstance;
    serviceInstance.addResource = addResource;
    serviceInstance.getTypedValue = getTypedValue;

    return serviceInstance;
    }]);
