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
      <resource-definition-view :resourcedef="resourcedef" />
    </v-col>

    <v-col>
      <v-card elevation="0">
        <!-- Resource Controls -->
        <resource-instance-control
          :resourcedef="resourcedef"
          :endpoint="endpoint"
          :resourcePath="resourcePath"
          :instanceId="resourceInstanceId"
        />

        <!-- Value(s) -->
        <v-card-text
          class="font-weight-bold"
          :class="{
            'text--primary': resourceInstance && !resourceInstance.supposed,
          }"
        >
          {{ resourceInstanceValueAsString }}
        </v-card-text>
      </v-card>
    </v-col>
  </v-row>
</template>
<script>
import { valueToString } from "../../../js/valueutils.js";
import ResourceInstanceControl from "../ResourceInstanceControl.vue";
import ResourceDefinitionView from "./ResourceDefinitionView.vue";

/**
 * Complete view of a "Single Instance" or "Multi Instance" Resource.
 *
 * Show description, model, current value and control to execute operation on it.
 */
export default {
  components: {
    ResourceInstanceControl,
    ResourceDefinitionView,
  },
  props: {
    endpoint: String, // the endpoint of the client
    resourcedef: Object, // the model of the resource
    resourcePath:String, // resource path
    resourceInstance: Object, // the resource data as defined in store.js
    resourceInstanceId: Number, // the id of the resource instance to watch
  },

  computed: {
    resourceInstanceValueAsString: function() {
      if (!this.resourceInstance) {
        return "";
      } else {
        return valueToString(this.resourceInstance.val, this.resourcedef.type);
      }
    },
  },
};
</script>
