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
              >{{ resource.path }}
            </v-col>
            <v-col
              cols="1"
              lg="1"
              align-self="center"
              class="pa-2 text--disabled"
            >
              <v-icon
                x-small
                v-show="state.observed[resource.path]"
                :title="'Resource ' + resource.path + ' observed'"
                >{{ $icons.mdiEyeOutline }}</v-icon
              ></v-col
            >

            <v-col cols="7" lg="3" align-self="center" class="pa-0">
              <v-fade-transition leave-absolute>
                <resource-control
                  v-show="!open"
                  :resourcedef="resource.def"
                  :endpoint="endpoint"
                  :path="resource.path"
                />
              </v-fade-transition>
            </v-col>
            <v-col
              cols="5"
              lg="4"
              align-self="center"
              class="pa-0"
              :class="{
                'text--disabled': state.data[resource.path]
                  ? state.data[resource.path].supposed
                  : false,
              }"
            >
              <v-fade-transition leave-absolute>
                <simple-resource-view
                  class="pr-3 text-truncate"
                  v-show="!open"
                  :resource="state.data[resource.path]"
                  :resourcedef="resource.def"
                />
              </v-fade-transition>
            </v-col>
          </v-row>
        </v-container>
      </template>
    </v-expansion-panel-header>
    <v-expansion-panel-content>
      <resource-view
        :endpoint="endpoint"
        :resource="state.data[resource.path]"
        :resourcedef="resource.def"
        :path="resource.path"
      />
    </v-expansion-panel-content>
  </v-expansion-panel>
</template>
<script>
import ResourceControl from "../ResourceControl.vue";
import SimpleResourceView from "../view/SimpleResourceView.vue";
import ResourceView from "../view/ResourceView.vue";

export default {
  components: {
    ResourceControl,
    SimpleResourceView,
    ResourceView,
  },
  props: {
    endpoint: String, // the endpoint name of the client
    resource: Object, // a resource object {path:String, def:Object} (def is the resourcedef)
  },

  computed: {
    state() {
      return this.$store.state[this.endpoint];
    },
  },
};
</script>
