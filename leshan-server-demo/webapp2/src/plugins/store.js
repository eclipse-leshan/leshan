import Vue from "vue";

class Store {
  /*
    State STRUCTURE : 
    ================
    
    state { endpoint : { 
      data : { "singleInstanceResourcepath":   { val: "a resource value", supposed: true },
               "multipleInstanceResourcepath": { id1: { val: "a resource value", supposed: true }
                                                 id1: { val: "a resource value", supposed: true }},    
             },
      observed : { path(object/instance/resource/resourceinstance) : boolean }
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
    });
  }

  data(endpoint, path) {
    let d = this.state[endpoint].data[path];
    if (!d) {
      d = { val: null, supposed: false };
      Vue.set(this.state[endpoint].data, path, d);
    }
    return d;
  }

  /**
   * @param {String} endpoint endpoint of the client
   * @param {String} path path to a resource e.g. /3/0/1
   * @param {*} val value of the resource
   * @param {Boolean} supposed true means the value is supposed (not really send by the client)
   */
  newResourceValue(endpoint, path, val, supposed = false) {
    let d = this.data(endpoint, path);
    d.val = val;
    d.supposed = supposed;
  }

  /**
   * @param {String} endpoint endpoint of the client
   * @param {String} path path to the instance e.g. /3/0
   * @param {Array} resources Array of resources (example of resource : {id:0, value:"myvalue"})
   * @param {Boolean} supposed true means the value is supposed (not really send by the client)
   */
  newInstanceValue(endpoint, path, resources, supposed = false) {
    resources.forEach((res) =>
      this.newResourceValue(endpoint, path + "/" + res.id, res.value, supposed)
    );
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

  setObserved(endpoint, path, observed) {
    let o = this.state[endpoint].observed[path];
    if (!o) {
      Vue.set(this.state[endpoint].observed, path, observed);
    } else{
      this.state[endpoint].observed[path] = observed;
    }
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
