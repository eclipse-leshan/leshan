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
  <v-simple-table dense v-if="resource">
    <template v-slot:default>
      <thead>
        <tr>
          <th class="px-2 text-left">Id</th>
          <th></th>
          <th></th>
          <th class="text-left" style="width: 100%">Value</th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="(value, id) in resource.vals"
          :key="id"
          style="background-color: transparent !important"
        >
          <td class="px-2 text--disabled">{{ id }}</td>
          <td class="px-2">
            <v-icon
              class="pa-0"
              x-small
              v-show="state.observed[instancePath(id)]"
              :title="'Instance ' + instancePath(id) + ' observed'"
              >{{ $icons.mdiEyeOutline }}</v-icon
            >
          </td>
          <td class="px-2" style="white-space: nowrap">
            <resource-instance-control
              :endpoint="endpoint"
              :resourcedef="resourcedef"
              :resourcePath="path"
              :instanceId="Number(id)"
            />
          </td>
          <td :class="{ 'text--disabled': value.supposed }">
            {{ valueAsString(value.val) }}
          </td>
        </tr>
      </tbody>
    </template>
  </v-simple-table>
</template>
<script>
import { valueToString } from "../../../js/valueutils.js";
import ResourceInstanceControl from "../ResourceInstanceControl.vue";

/**
 * Display the state of a "Multi Instance" Resource.
 *
 * List all instances with its value and controls to execute operations on it.
 */
export default {
  components: { ResourceInstanceControl },
  props: {
    endpoint: String, // the endpoint of the client
    resourcedef: Object, // the model of the resource
    resource: Object, // the resource data as defined in store.js
    path: String, // the path of the resource (e.g. /3/0/1)
  },
  computed: {
    state() {
      return this.$store.state[this.endpoint];
    },
  },
  methods: {
    valueAsString(value) {
      return valueToString(value, this.resourcedef.type);
    },

    instancePath(id) {
      return this.path + "/" + id;
    },
  },
};
</script>
