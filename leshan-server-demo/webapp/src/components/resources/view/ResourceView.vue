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
  <v-row color="grey lighten-5">
    <v-col cols="12" lg="6" xl="5">
      <v-card elevation="0" class="grey lighten-4">
        <!-- Model -->
        <v-card-subtitle class="pb-0 text-decoration-underline">
          Model :
        </v-card-subtitle>
        <v-card-text>
          <v-simple-table dense>
            <template v-slot:default>
              <thead>
                <tr>
                  <th class="text-left">
                    Operations
                  </th>
                  <th class="text-left">
                    Instances
                  </th>
                  <th class="text-left">
                    Mandatory
                  </th>
                  <th class="text-left">
                    Type
                  </th>
                  <th class="text-left">
                    Range or enumeration
                  </th>
                  <th class="text-left">
                    Units
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr style="pointer-events: none" class="text-capitalize">
                  <td>{{ resourcedef.operations }}</td>
                  <td>{{ resourcedef.instancetype }}</td>
                  <td>{{ resourcedef.mandatory }}</td>
                  <td>{{ resourcedef.type }}</td>
                  <td>{{ resourcedef.range }}</td>
                  <td>{{ resourcedef.units }}</td>
                </tr>
              </tbody>
            </template>
          </v-simple-table>
        </v-card-text>

        <!-- description -->
        <v-card-subtitle class="pb-0 text-decoration-underline">
          Description :
        </v-card-subtitle>
        <v-card-text style="white-space: pre-wrap">
          {{ resourcedef.description }}<br v-if="resourcedef.description2" />{{
            resourcedef.description2
          }}
        </v-card-text>
      </v-card>
    </v-col>

    <v-col>
      <v-card elevation="0">
        <!-- Resource Controls -->
        <resource-control
          :resourcedef="resourcedef"
          :endpoint="endpoint"
          :path="path"
        />

        <!-- Value(s) -->
        <v-card-text
          v-if="resourcedef.instancetype == 'single'"
          class="font-weight-bold"
          :class="{ 'text--primary': resource && !resource.supposed }"
        >
          {{ resourceValueAsString }}
        </v-card-text>
        <multi-instances-resource-view
          v-else
          :resourcedef="resourcedef"
          :endpoint="endpoint"
          :path="path"
          :resource="resource"
        />
      </v-card>
    </v-col>
  </v-row>
</template>
<script>
import { resourceToString } from "../../../js/valueutils.js";
import ResourceControl from "../ResourceControl.vue";
import MultiInstancesResourceView from "./MultiInstancesResourceView.vue";

/**
 * Complete view of a "Single Instance" or "Multi Instance" Resource.
 *
 * Show description, model, current value and control to execute operation on it.
 */
export default {
  components: {
    ResourceControl,
    MultiInstancesResourceView,
  },
  props: {
    endpoint: String, // the endpoint of the client 
    resourcedef: Object, // the model of the resource
    resource: Object, // the resource data as defined in store.js
    path: String, // the path of the resource (e.g. /3/0/1)
  },

  computed: {
    resourceValueAsString: function() {
      if (!this.resource) {
        return "";
      } else {
        return resourceToString(this.resource, this.resourcedef.type);
      }
    },
  },
};
</script>
