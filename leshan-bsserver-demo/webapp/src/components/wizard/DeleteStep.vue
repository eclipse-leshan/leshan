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
  <v-card class="mb-12" elevation="0">
    <v-card-text class="pb-0">
      <p>
        A LWM2M Bootstrap Session generally starts by deleting part of the
        existing configuration on the <strong>LWM2M client</strong>.
      </p>
      <p>
        By default, objects <code>/0</code> and <code>/1</code> are deleted,
        then you will be able to define LWM2M Server and LWM2M Bootstrap Server
        to add.
      </p>
    </v-card-text>
    <v-form ref="form" :value="valid" @input="$emit('update:valid', $event)">
      <span>
        <v-btn small @click="addPath"> Add Path to Delete </v-btn>
      </span>
      <v-btn small @click="removeAllPath"> Remove All </v-btn>
      <div v-for="(path, index) in value" :key="index">
        <v-text-field
          :value="path"
          :rules="[
            (v) => !!v || 'path can not be empty required',
            (v) => !v || /^((\/([1-9][0-9]{0,4}|[0])){0,4})$|^\/$/.test(v) || 'invalid path',
          ]"
          required
          dense
          @input="updatePath(index, $event)"
          append-outer-icon="mdi-delete"
          @click:append-outer="removePath(index)"
        ></v-text-field>
      </div>
    </v-form>
  </v-card>
</template>
<script>
export default {
  props: {
    value: Array, // path to delete
    valid: Boolean, // validation state of the form
  },
  methods: {
    resetValidation() {
      this.$refs.form.resetValidation();
    },
    addPath() {
      this.$emit("input", [...this.value, ""]);
    },
    removeAllPath() {
      this.$emit("input", []);
    },
    updatePath(index, value) {
      let res = [...this.value];
      res.splice(index, 1, value);
      this.$emit("input", res);
    },
    removePath(index) {
      let res = [...this.value];
      res.splice(index, 1);
      this.$emit("input", res);
    },
  },
};
</script>
