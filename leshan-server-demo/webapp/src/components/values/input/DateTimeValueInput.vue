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
  <v-row>
    <v-col>
      <v-text-field
        single-line
        :label="resourcedef.type"
        suffix="seconds"
        v-model="dateValue"
        :rules="[(v) => !v || isValidTimestamp(v) || 'Invalid timestamp']"
        hint="Timestamp in seconds"
        clearable
      />
    </v-col>
  </v-row>
</template>
<script>
import { isInteger } from "../../../js/utils.js";


/**
 * An input for TIME single value LWM2M node ("Single Instance Resource" or "Resource Instance")
 */
export default {
  props: {
    value: null, // the input value for this LWM2M Node (v-model) : timestamp in milliseconds
    resourcedef: Object, // the model of the resource
  },
  data() {
    return {
      localValue: null, // timestamp in seconds
    };
  },
  watch: {
    value(v) {
      if (this.isValidTimestamp(v)) {
        this.localValue = Math.floor(parseInt(v) / 1000);
      } else {
        this.localValue = v;
      }
    },
  },

  methods: {
    isValidTimestamp(v) {
      return isInteger(v);
    },
  },

  computed: {
    dateValue: {
      get() {
        return this.localValue;
      },
      set(v) {
        this.$emit("input", this.isValidTimestamp(v) ? v * 1000 : v);
      },
    },
  },
};
</script>
