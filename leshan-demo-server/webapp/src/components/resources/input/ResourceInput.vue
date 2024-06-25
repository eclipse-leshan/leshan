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
  <multi-instance-resource-input
    v-if="resourcedef.instancetype != 'single'"
    :resourcedef="resourcedef"
    :hint="hint"
    :value="value"
    @input="$emit('input', $event)"
  />
  <single-value-input
    v-else
    :resourcedef="resourcedef"
    :hint="hint"
    :value="value"
    @input="$emit('input', $event)"
  />
</template>
<script>
import SingleValueInput from "../../values/input/SingleValueInput.vue";
import MultiInstanceResourceInput from "./MultiInstanceResourceInput.vue";

/**
 * An input for "Single Instance" and "Mult Instance" resource.
 */
export default {
  components: { SingleValueInput, MultiInstanceResourceInput },
  props: {
    value: null, // the input value for this resource (v-model)
    resourcedef: Object, // the model of the resource
    hint: { type: String, default: null }, // hint displayed on `?` tooltip. If `null`, the "?" icon is not displayed"
  },
};
</script>
