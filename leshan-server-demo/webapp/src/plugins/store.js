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

import Vue from "vue";

class Store {
  /*
    State STRUCTURE : 
    ================
    
    state { endpoint : { 
      data : { "singleInstanceResourcepath":   {  isSingle:true, val: "a resource value", supposed: true },
               "multipleInstanceResourcepath": {  isSingle:false, vals: {
                                                    id1: { val: "a resource value", supposed: true },
                                                    id1: { val: "a resource value", supposed: true }
                                                  },
                                                  supposed: true
                                                },    
             },
      observed : { path(object/instance/resource/resourceinstance) : boolean }
      compositeObserved : { 'path1,path2,...' : boolean}
      }
    };  
   */

  state = {};
  constructor() {
    // HACK to make state reactive
    // see https://forum.vuejs.org/t/how-to-make-a-simple-reactive-object-that-can-be-shared-between-components/13199/3
    // maybe we should use Vuex instead of this home made store ?
    var tmpVm = new Vue({ data: { state: {} } });
    this.state = tmpVm.state;
  }

  initState(endpoint) {
    Vue.set(this.state, endpoint, {
      data: {},
      observed: {},
      compositeObserved: {},
    });
  }

  data(endpoint, path) {
    return this.state[endpoint].data[path];
  }

  /**
   * @param {String} endpoint endpoint of the client
   * @param {String} path path to a resource e.g. /3/0/1
   * @param {*} resource the resource
   *                    (format for single : {id:3, value:"value"}  )
   *                    (format for multi : {id:4, values: {3:"value",4:"anothervalue"}}  )
   * @param {Boolean} supposed true means the value is supposed (not really send by the client)
   */
  newResourceValue(endpoint, path, resource, supposed = false) {
    if (resource.value !== undefined) {
      this.newSingleResourceValue(endpoint, path, resource.value, supposed);
    } else if (resource.values !== undefined) {
      this.newMultiResourceValue(endpoint, path, resource.values, supposed);
    } else {
      console.log("unsupported resource : ", resource);
    }
  }

  /**
   * @param {String} endpoint endpoint of the client
   * @param {String} path path to a resource e.g. /3/0/1
   * @param {*} val value of the resource
   * @param {Boolean} supposed true means the value is supposed (not really send by the client)
   */
  newSingleResourceValue(endpoint, path, val, supposed = false) {
    let d = this.data(endpoint, path);
    // create & add data if not exist OR is not single resource.
    if (!d || !d.single) {
      d = { val: val, supposed: supposed, isSingle: true };
      Vue.set(this.state[endpoint].data, path, d);
    } else {
      // else just modify it
      d.val = val;
      d.supposed = supposed;
    }
  }

  /**
   * @param {String} endpoint endpoint of the client
   * @param {String} path path to a resource e.g. /3/0/1
   * @param {Object} values values of the resource as a map from id to resource instance value
   * @param {Boolean} supposed true means the value is supposed (not really send by the client)
   */
  newMultiResourceValue(endpoint, path, values, supposed = false) {
    let d = {};
    d.vals = {};
    for (const id in values) {
      d.vals[id] = {};
      d.vals[id].val = values[id];
      d.vals[id].supposed = supposed;
    }
    d.supposed = supposed;
    d.isSingle = false;
    Vue.set(this.state[endpoint].data, path, d);
  }

  /**
   * @param {String} endpoint endpoint of the client
   * @param {String} path path to a resource instance path e.g. /3/0/11/1
   * @param {*} val value of the resource
   * @param {Boolean} supposed true means the value is supposed (not really send by the client)
   */
  newResourceInstanceValueFromPath(endpoint, path, val, supposed = false) {
    // split resource instance path in 'resource path' and 'instance id'
    let splitIndex = path.lastIndexOf("/");
    let resourcepath = path.substring(0, splitIndex);
    let instanceID = path.substring(splitIndex + 1);
    this.newResourceInstanceValue(
      endpoint,
      resourcepath,
      Number(instanceID),
      val,
      supposed
    );
  }

  /**
   * @param {String} endpoint endpoint of the client
   * @param {String} path path to a resource e.g. /3/0/11
   * @param {Number} instanceID the ID of the instance
   * @param {*} val value of the resource
   * @param {Boolean} supposed true means the value is supposed (not really send by the client)
   */
  newResourceInstanceValue(endpoint, path, instanceID, val, supposed = false) {
    let d = this.data(endpoint, path);
    // create & add data if not exist OR is not single resource.
    if (!d || d.single) {
      d = {};
      d.vals = {};
      d.vals[instanceID] = {};
      d.vals[instanceID].val = val;
      d.vals[instanceID].supposed = supposed;
      d.supposed = supposed;
      d.isSingle = false;
      Vue.set(this.state[endpoint].data, path, d);
    } else {
      // else just modify it
      if (!d.vals[instanceID]) {
        let i = { val: val, supposed: supposed };
        Vue.set(d.vals, instanceID, i);
      } else {
        d.vals[instanceID].val = val;
        d.vals[instanceID].supposed = supposed;
      }
    }
  }

  /**
   * @param {String} endpoint endpoint of the client
   * @param {String} path path to the instance e.g. /3/0
   * @param {Array} resources Array of resources (example of resource : {id:0, value:"myvalue"})
   * @param {Boolean} supposed true means the value is supposed (not really send by the client)
   * @param {Boolean} update true if we should just update some resource of the instance OR
   *                         false if it should replace the whole instance (deleting all previous value).
   */
  newInstanceValue(
    endpoint,
    path,
    resources,
    supposed = false,
    update = false
  ) {
    if (!update) {
      this.removeInstanceValue(endpoint, path);
    }
    resources.forEach((res) => {
      this.newResourceValue(endpoint, path + "/" + res.id, res, supposed);
    });
  }

  /**
   * @param {String} endpoint endpoint of the client
   * @param {String} path path to the instance e.g. /3/0
   * @param {Array} instances Array of instances
   * @param {Boolean} supposed true means the value is supposed (not really send by the client)
   */
  newObjectValue(endpoint, path, instances, supposed = false) {
    this.removeObjectValue(endpoint, path);
    instances.forEach((ins) => {
      this.newInstanceValue(
        endpoint,
        path + "/" + ins.id,
        ins.resources,
        supposed
      );
    });
  }

  /**
   * @param {String} endpoint endpoint of the client
   * @param {String} path path to the node (object, instance, resource or resource instance)
   * @param {Array} node the node value
   * @param {Boolean} supposed true means the value is supposed (not really send by the client)
   */
  newNode(endpoint, path, node, supposed = false) {
    if (node.kind === "singleResource") {
      this.newResourceValue(endpoint, path, node, supposed);
    } else if (node.kind === "multiResource") {
      this.newMultiResourceValue(endpoint, path, node["values"], supposed);
    } else if (node.kind === "resourceInstance") {
      this.newResourceInstanceValueFromPath(
        endpoint,
        path,
        node.value,
        supposed
      );
    } else if (node.kind === "instance") {
      this.newInstanceValue(endpoint, path, node.resources, supposed);
    } else if (node.kind === "obj") {
      this.newObjectValue(endpoint, path, node.instances, supposed);
    } else {
      console.log(node.kind, " not yet supported");
    }
  }

  /**
   * @param {String} endpoint endpoint of the client
   * @param {Object} nodes the nodes map {path:node Object}
   * @param {Boolean} supposed true means the value is supposed (not really send by the client)
   */
  newNodes(endpoint, nodes, supposed = false) {
    Object.entries(nodes).forEach(([path, node]) => {
      this.newNode(endpoint, path, node, supposed);
    });
  }

  /**
   * @param {String} endpoint endpoint of the client
   * @param {String} path path to the instance e.g. /3/0
   */
  removeInstanceValue(endpoint, path) {
    let instancePath = path + "/";
    let s = this.state[endpoint];
    Object.keys(s.data).forEach((p) => {
      if (p.startsWith(instancePath)) {
        delete s.data[p];
      }
    });
  }

  /**
   * @param {String} endpoint endpoint of the client
   * @param {String} path path to the instance e.g. /3
   */
  removeObjectValue(endpoint, path) {
    let objectPath = path + "/";
    let s = this.state[endpoint];
    Object.keys(s.data).forEach((p) => {
      if (p.startsWith(objectPath)) {
        delete s.data[p];
      }
    });
  }

  setObserved(endpoint, path, observed) {
    let o = this.state[endpoint].observed[path];
    if (!o) {
      Vue.set(this.state[endpoint].observed, path, observed);
    } else {
      this.state[endpoint].observed[path] = observed;
    }
  }

  /**
   * @param {String} endpoint endpoint of the client
   * @param {Array} nodes the nodes map {path:node Object}
   * @param {Boolean} supposed true means the paths are "composite" observed.
   */
  setCompositePathsObserved(endpoint, paths, observed) {
    let key = paths.join(",");
    let o = this.state[endpoint].compositeObserved[key];
    if (!o) {
      Vue.set(this.state[endpoint].compositeObserved, key, observed);
    } else {
      this.state[endpoint].compositeObserved[key] = observed;
    }
  }

  /**
   * @param {Object} compositeObject
   * @returns the key used in state.compositeObserved for the given compositeObject
   */
  compositeObjectToKey(compositeObject) {
    return compositeObject.paths.join(",");
  }

  /**
   * @param {String} endpoint endpoint of the client
   * @param {Object} compositeObject A composite object
   * @param {Boolean} supposed true means the paths are "composite" observed.
   */
  setCompositeObjectObserved(endpoint, compositeObject, observed) {
    this.setCompositePathsObserved(endpoint, compositeObject.paths, observed);
  }
}

// create plugin which make data accessible on all vues
const _store = new Store();

let StorePlugin = {};
StorePlugin.install = function(Vue) {
  Object.defineProperties(Vue.prototype, {
    $store: {
      get() {
        return _store;
      },
    },
  });
};

Vue.use(StorePlugin);

export default StorePlugin;
