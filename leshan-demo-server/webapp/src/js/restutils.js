/*******************************************************************************
 * Copyright (c) 2021 Sierra Wireless and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 *******************************************************************************/

/**
 * Helper function to convert data from UI Component format
 * To REST API format
 *
 * -----------------------------------------------------------------
 * /!\ maybe we should modify the REST API to limit this conversion ?
 * -----------------------------------------------------------------
 */

/**
 * @param {Object} model model of the object.
 * @param {Number} id the instance id.
 * @param {*} values the values of the resource from UI Component.
 * @returns a LWM2M object instance usable for REST API.
 */
function instanceToREST(model, id, value) {
  let data = { kind: "instance", resources: [] };

  // set ID
  if (id != null) {
    data.id = id;
  }

  // set resources
  for (let id in value) {
    let resmodel = model.resourcedefs.find(function(m) {
      return m.id == id;
    });
    data.resources.push(resourceToREST(resmodel, value[id]));
  }

  return data;
}

/**
 * @param {Object} model model of the resource.
 * @param {*} value the value of the resource from UI Component.
 * @returns a single instance OR multi instance resource usable for REST API.
 */
function resourceToREST(model, value) {
  if (model.instancetype == "single") {
    return singleInstanceResourceToREST(model, value);
  } else {
    return multiInstanceResourceToREST(model, value);
  }
}

/**
 * @param {Object} model model of the resource.
 * @param {*} value the value of the resource from UI Component.
 * @returns a single instance resource usable for REST API.
 */
function singleInstanceResourceToREST(model, value) {
  let res = {};
  res.id = model.id;
  res.kind = "singleResource";
  res.value = value;
  res.type = model.type;
  return res;
}

/**
 * @param {Object} model model of the resource.
 * @param {*} value the value of the resource from UI Component.
 * @returns multi instance resource usable for REST API.
 */
function multiInstanceResourceToREST(model, value) {
  let res = {};
  res.id = model.id;
  res.kind = "multiResource";
  res.type = model.type;
  res.values = value.reduce(function(resource, instance) {
    resource[instance.id] = instance.val;
    return resource;
  }, {});
  return res;
}

/**
 * @param {Object} model model of the resource.
 * @param {Number} instanceId the id of the resource instance
 * @param {*} value the value of the resource instance from UI Component.
 * @returns a resource instance usable for REST API.
 */
function resourceInstanceToREST(model, instanceId, value) {
  let res = {};
  res.id = instanceId;
  res.kind = "resourceInstance";
  res.value = value;
  res.type = model.type;
  return res;
}

/**
 * @param {Object} model model of the resource.
 * @param {object} path the lwm2m path object
 * @param {*} value the value of the resource or resource instance from UI Component.
 * @returns a resource instance or single resource usable for REST API.
 */
function singleValueToREST(model, path, value) {
  if (path.type == "resource" && model.instancetype == "single") {
    return singleInstanceResourceToREST(model, value);
  } else if (path.type == "resourceinstance") {
    return resourceInstanceToREST(model, path.resourceinstanceid, value);
  }
  throw new Error(`Unsupported kind of node ${path.type}`);
}

export {
  resourceToREST,
  instanceToREST,
  resourceInstanceToREST,
  singleValueToREST,
};
