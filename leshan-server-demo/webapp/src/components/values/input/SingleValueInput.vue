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
  <boolean-value-input
    v-if="resourcedef.type == 'boolean'"
    :resourcedef="resourcedef"
    :value="value"
    @input="$emit('input', $event)"
  />
  <opaque-value-input
    v-else-if="resourcedef.type == 'opaque'"
    :resourcedef="resourcedef"
    :value="value"
    @input="$emit('input', $event)"
  />
  <date-time-value-input
    v-else-if="resourcedef.type == 'time'"
    :resourcedef="resourcedef"
    :value="value"
    @input="$emit('input', $event)"
  />
  <obj-link-value-input
    v-else-if="resourcedef.type == 'objlnk'"
    :resourcedef="resourcedef"
    :value="value"
    @input="$emit('input', $event)"
  />
  <v-text-field
    v-else
    :label="resourcedef.type"
    :hint="hint"
    :suffix="resourcedef.units"
    :value="value"
    @input="$emit('input', $event)"
  />
</template>
<script>
import BooleanValueInput from "./BooleanValueInput.vue";
import DateTimeValueInput from "./DateTimeValueInput.vue";
import OpaqueValueInput from "./OpaqueValueInput.vue";
import ObjLinkValueInput from './ObjLinkValueInput.vue';

/**
 * An input for single value LWM2M node ("Single Instance Resource" or "Resource Instance")
 */
export default {
  components: { BooleanValueInput, OpaqueValueInput, DateTimeValueInput, ObjLinkValueInput },
  props: {
    value: null, // the input value for this LWM2M Node (v-model)
    resourcedef: Object, // the model of the resource
    hint: { type: String, default: null }, // hint displayed on `?` tooltip. If `null`, the "?" icon is not displayed"
  },
};
</script>
