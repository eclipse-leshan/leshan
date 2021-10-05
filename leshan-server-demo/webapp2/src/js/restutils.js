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
  let data = { type: "instance", resources: [] };

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
  res.type = "singleResource";
  res.value = value;
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
  res.type = "multiResource";
  res.values = value.reduce(function(resource, instance) {
    resource[instance.id] = instance.val;
    return resource;
  }, {});
  return res;
}

export { resourceToREST, instanceToREST };
