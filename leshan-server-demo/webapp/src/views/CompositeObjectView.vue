<!-----------------------------------------------------------------------------
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
  ----------------------------------------------------------------------------->
<template>
  <v-sheet color="grey lighten-5" class="pa-4" width="100%">
    <div>
      <h3 class="text-capitalize">
        <composite-object-icons :compositeObject="compositeObject" />{{
          compositeObject.name
        }}
        <composite-object-control
          :endpoint="$route.params.endpoint"
          :compositeObject="compositeObject"
          :nodes="lwm2mNodes"
        />
        <v-icon
          class="pl-3"
          small
          v-show="state.compositeObserved[compositeObservationKey]"
          :title="compositeObservationKey + ' observed'"
          >{{ $icons.mdiEyeOutline }}</v-icon
        >
      </h3>
      <p>
        This Composite object is composed of following resources :
        {{ compositeObject.paths.join(", ") }}
      </p>
    </div>
    <v-divider></v-divider>

    <div v-for="(node, path) in lwm2mNodes" :key="path">
      <h4 class="pa-1">
        {{ path }}
        <span
          v-if="node.instanceNotAvailable"
          class="subtitle-2 grey--text text--darken-1"
        >
          - Instance {{ node.path.objectinstanceid }} was not reported as
          available by the client
        </span>
      </h4>
      <!-- root case -->
      <span v-if="node.path.type == 'root'" class="text-body-2">
        Root Path is not yet supported by Leshan.
      </span>
      <!-- object case -->
      <span v-else-if="node.unknowObject" class="text-body-2">
        Client does not report it supports Object {{ node.path.objectid }} OR
        Leshan server demo does not know this object model, see
        <a
          href="https://github.com/eclipse/leshan/wiki/Adding-new-objects#how-to-add-object-support-to-leshan-"
        >
          how to add the model</a
        >.
      </span>
      <span v-else-if="node.path.type == 'object'">
        <div v-for="instance in node.instances" :key="instance.path">
          <h5 class="pa-1">Instance {{ instance.id }}</h5>
          <v-expansion-panels accordion multiple dense tile>
            <resource-expansion-panel
              v-for="resource in instance.resources"
              :key="resource.path"
              :resource="resource"
              :endpoint="endpoint"
            />
          </v-expansion-panels>
        </div>
      </span>
      <!-- object instance-->
      <div v-else-if="node.path.type == 'objectinstance'">
        <v-expansion-panels accordion multiple dense tile>
          <resource-expansion-panel
            v-for="resource in node.resources"
            :key="resource.path"
            :resource="resource"
            :endpoint="endpoint"
          />
        </v-expansion-panels>
      </div>
      <!-- object instance-->
      <div v-else-if="node.path.type == 'resource'">
        <v-expansion-panels accordion multiple dense tile>
          <resource-expansion-panel
            :resource="node.resource"
            :endpoint="endpoint"
          />
        </v-expansion-panels>
      </div>
      <!-- resource instance case -->
      <div v-else-if="node.path.type == 'resourceinstance'">
        <v-expansion-panels accordion multiple dense tile>
          <resource-instance-expansion-panel
            :resource="node.resource"
            :resourceInstanceId="node.resourceInstanceId"
            :endpoint="endpoint"
          />
        </v-expansion-panels>
      </div>
      <!-- invalid node-->
      <span v-else-if="node.path.type == 'invalid'" class="text-body-2">
        Path seems to not be a valid one (ignored).
      </span>
      <!-- Unexpected node-->
      <span v-else class="text-body-2">
        Unexpected type of path {{ node.path.type }}.
      </span>
    </div>
  </v-sheet>
</template>
<script>
import ResourceExpansionPanel from "../components/resources/view/ResourceExpansionPanel.vue";
import ResourceInstanceExpansionPanel from "../components/resources/view/ResourceInstanceExpansionPanel.vue";
import { preference } from "vue-preferences";
import { LwM2mPath } from "../js/lwm2mpath.js";
import CompositeObjectIcons from "../components/compositeOperation/CompositeObjectIcons.vue";
import CompositeObjectControl from "../components/compositeOperation/CompositeObjectControl.vue";

export default {
  components: {
    ResourceExpansionPanel,
    ResourceInstanceExpansionPanel,
    CompositeObjectIcons,
    CompositeObjectControl,
  },
  props: { objectdefs: Array, allInstances: Object },
  data() {
    return {
      compositeObject: null,
    };
  },
  created() {
    // fetch the data when the view is created and the data is
    // already being observed
    this.fetchData();
  },
  watch: {
    // call again the method if the route changes
    $route: "fetchData",
  },
  methods: {
    fetchData() {
      let compositeObjects = preference("compositeObjects", {
        defaultValue: [],
      });
      this.compositeObject = compositeObjects
        .get()
        .find((o) => o.name == this.$route.params.compositeObjectName);
    },
    createResources(objectdef, instanceid) {
      if (objectdef)
        return objectdef.resourcedefs
          .map((r) => {
            return {
              path: `/${objectdef.id}/${instanceid}/${r.id}`,
              def: r,
            };
          })
          .filter(
            (r) =>
              r.def.operations.includes("R") || r.def.operations.includes("W")
          );
      return null;
    },
  },
  computed: {
    endpoint() {
      return this.$route.params.endpoint;
    },
    state() {
      return this.$store.state[this.endpoint];
    },
    compositeObservationKey() {
      return this.$store.compositeObjectToKey(this.compositeObject);
    },
    lwm2mNodes() {
      if (!this.objectdefs || this.objectdefs.length == 0) return {};

      let nodesArray = this.compositeObject.paths.map((p) => {
        let path = new LwM2mPath(p);
        // handle invalid path
        if (path.type == "invalid") {
          return { path: path };
        }
        // get object definition
        let objectdef = null;
        if (path.objectid != null) {
          objectdef = this.objectdefs.find((o) => o.id == path.objectid);
          if (objectdef == null) {
            return { path: path, unknowObject: true };
          }
        }
        // check if instance if available
        let instanceNotAvailable = true;
        if (path.objectinstanceid != null) {
          let instances = this.allInstances[path.objectid];
          if (instances) {
            instanceNotAvailable = !instances.includes(path.objectinstanceid);
          }
        }

        // handle each kind of path
        switch (path.type) {
          case "object": {
            let instances = this.allInstances[path.objectid];
            let instancesWithResources = [];
            if (instances) {
              instancesWithResources = instances.map((i) => {
                let instance = {};
                instance.id = i;
                instance.path = `/${objectdef.id}/${instance.id}`;
                instance.resources = this.createResources(
                  objectdef,
                  instance.id
                );
                return instance;
              });
            }
            return {
              path: path,
              objectdef: objectdef,
              instances: instancesWithResources,
            };
          }
          case "objectinstance": {
            // check if instance is available
            return {
              path: path,
              objectdef: objectdef,
              instanceNotAvailable: instanceNotAvailable,
              instanceid: path.objectinstanceid,
              resources: this.createResources(objectdef, path.objectinstanceid),
            };
          }
          case "resource": {
            let resourcedef = objectdef.resourcedefs.find(
              (r) => r.id == path.resourceid
            );
            return {
              path: path,
              instanceNotAvailable: instanceNotAvailable,
              resource: { path: path.toString(), def: resourcedef },
            };
          }
          case "resourceinstance": {
            let resourcedef = objectdef.resourcedefs.find(
              (r) => r.id == path.resourceid
            );
            return {
              path: path,
              instanceNotAvailable: instanceNotAvailable,
              resourceInstanceId: path.resourceinstanceid,
              resource: {
                path: path.toResourcePath(),
                def: resourcedef,
              },
            };
          }
          default:
            return { path: path };
        }
      });

      return nodesArray.reduce((NodesObject, node) => {
        NodesObject[node.path.toString()] = node;
        return NodesObject;
      }, {});
    },
  },
};
</script>
