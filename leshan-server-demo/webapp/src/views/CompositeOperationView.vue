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
  <!-- 1 composite object view -->
  <router-view
    v-if="$route.params.compositeObjectName != null"
    :objectdefs="objectdefs"
    :allInstances="allInstances"
  ></router-view>
  <!-- all composite object view -->
  <div v-else>
    <v-sheet color="grey lighten-5" class="pa-4" width="100%">
      <div>
        <h3>
          <v-icon>{{ $icons.mdiFormatListCheckbox }}</v-icon> Composite
          Operations
        </h3>
        <p>
          Create your "Composite Object", then you will be able to sent
          Composite Operation on it like Read-Composite, Write-Composite or
          Observe-Composite request.
        </p>
        <p class="subtitle-2">
          Note that "Composite Object" is not a LWM2M concept but just a UI way
          to expose Composite Operation.
        </p>
      </div>
      <v-divider></v-divider>
      <v-row>
        <v-col cols="0" md="3"> </v-col>
        <v-col cols="12" md="6">
          <composite-objects-selector />
        </v-col>
        <v-col cols="0" md="3"> </v-col>
      </v-row>
    </v-sheet>
  </div>
</template>
<script>
import CompositeObjectsSelector from "../components/compositeOperation/CompositeObjectsSelector.vue";
export default {
  components: { CompositeObjectsSelector },
  props: { objectdefs: Array, allInstances: Object },
};
</script>
