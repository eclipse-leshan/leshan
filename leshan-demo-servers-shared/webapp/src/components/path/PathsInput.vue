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
  <div class="bg-grey-lighten-4 pa-4 mb-1">
    <span class="pa-2">
      <v-btn size="small" @click="addPath"> {{ addButtonText }} </v-btn>
    </span>
    <v-btn size="small" @click="removeAllPath"> Remove All </v-btn>
    <div v-for="(path, index) in modelValue" :key="index">
      <v-text-field
        :model-value="path"
        :rules="[
          (v) => !!v || 'path can not be empty required',
          (v) =>
            !v ||
            /^((\/([1-9][0-9]{0,4}|[0])){0,4})$|^\/$/.test(v) ||
            'invalid path',
        ]"
        placeholder="a path to a LWM2M node (e.g. /3/0/1 or /3/0/11/0)"
        required
        density="compact"
        @update:model-value="updatePath(index, $event)"
        :append-icon="$icons.mdiDelete"
        @click:append="removePath(index)"
      ></v-text-field>
    </div>
  </div>
</template>
<script>
export default {
  props: {
    modelValue: Array, // path to edit
    addButtonText: {
      default: "Add Path",
      type: String,
    }, // label of Add button
  },
  methods: {
    addPath() {
      this.$emit("update:model-value", [...this.modelValue, ""]);
    },
    removeAllPath() {
      this.$emit("update:model-value", []);
    },
    updatePath(index, path) {
      let res = [...this.modelValue];
      res.splice(index, 1, path);
      this.$emit("update:model-value", res);
    },
    removePath(index) {
      let res = [...this.modelValue];
      res.splice(index, 1);
      this.$emit("update:model-value", res);
    },
  },
};
</script>
