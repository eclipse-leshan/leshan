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
  <v-row dense>
    <v-col cols="12" md="2">
      <v-subheader v-if="resourcedef.mandatory"
        ><strong>{{ resourcedef.name }} *</strong></v-subheader
      >
      <v-subheader v-else>{{ resourcedef.name }} </v-subheader>
    </v-col>
    <v-col cols="11" md="9">
      <resource-input
        :resourcedef="resourcedef"
        :hint="hint(resourcedef)"
        :value="value"
        @input="$emit('input', $event)"
      />
    </v-col>
    <v-col cols="1" md="1">
      <v-tooltip left>
        <template v-slot:activator="{ on }">
          <v-icon v-on="on">
            {{ $icons.mdiHelpCircleOutline }}
          </v-icon>
        </template>
        <p style="white-space: pre-wrap">{{ resourcedef.description }}</p>
      </v-tooltip>
    </v-col>
  </v-row>
</template>
<script>
import ResourceInput from "./ResourceInput.vue";

/**
 * An input for "Single Instance" and "Mult Instance" resource prefixed by a Label.
 */
export default {
  components: { ResourceInput },
  props: {
    value: null, // the input value for this resource (v-model)
    resourcedef: Object, // the model of the resource
  },
  methods: {
    hint(resourcedef) {
      let hint = "";
      if (resourcedef.mandatory) hint += " mandatory";
      if (resourcedef.range) hint += " (Range: " + resourcedef.range + ")";
      return hint;
    },
  },
};
</script>
