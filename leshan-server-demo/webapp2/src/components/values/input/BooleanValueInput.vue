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
    <v-col cols="1">
      <v-checkbox
        v-model="booleanValue"
        true-value="true"
        false-value="false"
        :indeterminate="state !== ''"
        :key="state"
        :indeterminate-icon="icon"
        off-icon="mdi-close-box-outline"
      ></v-checkbox>
    </v-col>
    <v-col>
      <v-text-field
        single-line
        :label="resourcedef.type"
        :suffix="resourcedef.units"
        :rules="[(v) => isNotSet || !isNotBoolean || 'not a boolean']"
        v-model="booleanValue"
        clearable
      />
    </v-col>
  </v-row>
</template>
<script>
/**
 * An input for BOOLEAN single value LWM2M node ("Single Instance Resource" or "Resource Instance")
 */
export default {
  props: {
    value: null, // the input value for this LWM2M Node (v-model)
    resourcedef: Object, // the model of the resource
  },
  data() {
    return {
      localValue: null,
    };
  },
  watch: {
    value(v) {
      this.localValue = v;
    },
  },

  computed: {
    booleanValue: {
      get() {
        return this.localValue;
      },
      set(v) {
        this.localValue = v;
        this.$emit("input", this.localValue);
      },
    },
    state() {
      if (this.isNotSet) {
        return "notset";
      }
      if (this.isNotBoolean) {
        return "notbool";
      } else {
        return "";
      }
    },
    isNotSet() {
      return (
        this.booleanValue === undefined ||
        this.booleanValue === null ||
        this.booleanValue === ""
      );
    },
    isNotBoolean() {
      return this.booleanValue != "true" && this.booleanValue != "false";
    },
    icon() {
      if (this.isNotSet) {
        return "mdi-checkbox-blank-outline";
      } else if (this.isNotBoolean) {
        return "mdi-help-box";
      } else {
        return "";
      }
    },
  },
};
</script>
