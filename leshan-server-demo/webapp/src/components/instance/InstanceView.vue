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
  <div>
    <h4 class="pa-1">
      <span class="d-flex">
        <span class="align-self-center"> Instance {{ instanceId }} : </span>

        <instance-control
          :objectdef="objectdef"
          :endpoint="endpoint"
          :id="instanceId"
          :path="instancePath"
          class="mr-auto"
        />

        <v-icon
          class="mr-3"
          small
          v-show="state.observed[instancePath]"
          :title="'Instance ' + instancePath + ' observed'"
          >{{ $icons.mdiEyeOutline }}</v-icon
        >
      </span>
    </h4>

    <v-expansion-panels
      accordion
      multiple
      dense
      tile
      v-if="objectdef && typeof instanceId == 'number' && endpoint"
    >
      <resource-expansion-panel
        v-for="resource in resources"
        :key="resource.path"
        :resource="resource"
        :endpoint="endpoint"
      />
    </v-expansion-panels>
  </div>
</template>
<script>
import InstanceControl from "./InstanceControl.vue";
import ResourceExpansionPanel from "../resources/view/ResourceExpansionPanel.vue";

export default {
  components: {
    InstanceControl,
    ResourceExpansionPanel,
  },
  props: {
    objectdef: Object,
    instanceId: Number,
    endpoint: String,
  },

  computed: {
    state() {
      return this.$store.state[this.endpoint];
    },
    resources() {
      return this.objectdef.resourcedefs.map((r) => {
        return {
          path: `/${this.objectdef.id}/${this.instanceId}/${r.id}`,
          def: r,
        };
      });
    },
    instancePath() {
      return `/${this.objectdef.id}/${this.instanceId}`;
    },
  },
};
</script>
