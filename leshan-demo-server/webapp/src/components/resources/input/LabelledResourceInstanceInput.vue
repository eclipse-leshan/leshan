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
      <span class="text-subtitle-2" v-if="resourcedef.mandatory"
        ><strong>{{ resourcedef.name }} *</strong></span
      >
      <span class="text-subtitle-2" v-else>{{ resourcedef.name }} </span>
    </v-col>
    <v-col cols="11" md="9">
      <single-value-input
        :resourcedef="resourcedef"
        :hint="hint(resourcedef)"
        :model-value="modelValue"
        @update:model-value="$emit('update:model-value', $event)"
      />
    </v-col>
    <v-col cols="1" md="1">
      <v-tooltip location="left">
        <template v-slot:activator="{ props }">
          <v-icon v-bind="props">
            {{ $icons.mdiHelpCircleOutline }}
          </v-icon>
        </template>
        <p style="white-space: pre-wrap">{{ resourcedef.description }}</p>
      </v-tooltip>
    </v-col>
  </v-row>
</template>
<script>
import SingleValueInput from "../../values/input/SingleValueInput.vue";

/**
 * An input for "Resource Instance" prefixed by a Label.
 */
export default {
  components: { SingleValueInput },
  props: {
    modelValue: null, // the input value for this resource instance (v-model)
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
