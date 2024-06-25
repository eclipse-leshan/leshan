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
  <v-expansion-panel>
    <v-expansion-panel-header style="min-height: 32px" class="pa-3">
      <template v-slot:default="{ open }">
        <!-- min-width is needed for avoid shift about truncated text 
          see : https://stackoverflow.com/a/36247448/5088764
          -->
        <v-container class="pa-0" style="min-width: 0">
          <v-row>
            <v-col cols="7" lg="3" align-self="center" class="pa-2"
              >{{ resource.def.name }}
            </v-col>
            <v-col
              cols="4"
              lg="1"
              align-self="center"
              class="pa-2 text--disabled"
              >{{ resourceInstancePath }}
            </v-col>
            <v-col
              cols="1"
              lg="1"
              align-self="center"
              class="pa-2 text--disabled"
            >
              <v-icon
                x-small
                v-show="state.observed[resourceInstancePath]"
                :title="
                  'Resource Instance' + resourceInstancePath + ' observed'
                "
                >{{ $icons.mdiEyeOutline }}</v-icon
              ></v-col
            >

            <v-col cols="7" lg="3" align-self="center" class="pa-0">
              <v-fade-transition leave-absolute>
                <resource-instance-control
                  v-show="!open"
                  :resourcedef="resource.def"
                  :endpoint="endpoint"
                  :resourcePath="resource.path"
                  :instanceId="resourceInstanceId"
                />
              </v-fade-transition>
            </v-col>
            <v-col
              cols="5"
              lg="4"
              align-self="center"
              class="pa-0"
              :class="{
                'text--disabled': resourceInstanceValue
                  ? resourceInstanceValue.supposed
                  : false,
              }"
            >
              <v-fade-transition leave-absolute>
                <simple-resource-instance-view
                  class="pr-3 text-truncate"
                  v-show="!open"
                  :resourceInstance="resourceInstanceValue"
                  :resourcedef="resource.def"
                />
              </v-fade-transition>
            </v-col>
          </v-row>
        </v-container>
      </template>
    </v-expansion-panel-header>
    <v-expansion-panel-content>
      <resource-instance-view
        :endpoint="endpoint"
        :resourceInstance="resourceInstanceValue"
        :resourcedef="resource.def"
        :resourcePath="resource.path"
        :resourceInstanceId="resourceInstanceId"
      />
    </v-expansion-panel-content>
  </v-expansion-panel>
</template>
<script>
import ResourceInstanceControl from "../ResourceInstanceControl.vue";
import ResourceInstanceView from "./ResourceInstanceView.vue";
import SimpleResourceInstanceView from "./SimpleResourceInstanceView.vue";

export default {
  components: {
    ResourceInstanceControl,
    SimpleResourceInstanceView,
    ResourceInstanceView,
  },
  props: {
    endpoint: String, // the endpoint name of the client
    resource: Object, // a resource object {path:String, def:Object} (def is the resourcedef)
    resourceInstanceId: Number, // the id of the resource instance to watch
  },

  computed: {
    state() {
      return this.$store.state[this.endpoint];
    },
    resourceInstancePath() {
      return `${this.resource.path}/${this.resourceInstanceId}`;
    },
    resourceInstanceValue() {
      let resourceval = this.state.data[this.resource.path];
      if (resourceval && !resourceval.isSingle) {
        return resourceval.vals[this.resourceInstanceId];
      }
      return null;
    },
  },
};
</script>
