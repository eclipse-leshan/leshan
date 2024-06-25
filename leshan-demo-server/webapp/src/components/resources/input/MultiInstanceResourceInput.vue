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
  <div class="grey lighten-4 pa-4 mb-1">
    <span class="pa-2">
      <v-btn small @click="addNewInstance"> Add Instance </v-btn>
    </span>
    <v-btn small @click="removeAllInstances"> Remove All </v-btn>
    <span v-for="(instance, index) in instances" :key="index">
      <v-row dense align="center">
        <v-col cols="12" md="1">
          <v-text-field
            :ref="'id' + index"
            v-model="instance.id"
            hint="Id of the instance"
            type="number"
            :rules="[
              (v) => /^(0|[1-9]\d*)$/.test(v) || 'Must be a positive Integer',
              (v) => !isAlreadyUsed(v, index) || 'ID already used',
            ]"
            @input="triggerValidation(index)"
          />
        </v-col>
        <v-col>
          <single-value-input
            :resourcedef="resourcedef"
            :hint="hint"
            v-model="instance.val"
          />
        </v-col>
        <v-col cols="1" justify="center">
          <v-btn icon small @click="removeInstance(index)">
            <v-icon> {{ $icons.mdiDelete }} </v-icon>
          </v-btn>
        </v-col>
      </v-row>
    </span>
  </div>
</template>
<script>
import SingleValueInput from "../../values/input/SingleValueInput.vue";
/**
 * An input for "Multi Instance" resource.
 */
export default {
  components: { SingleValueInput },
  props: {
    value: null, // the input values for this Multi Instance resource (v-model) {id1:val1, id2:val2}
    resourcedef: Object, // the model of the resource
    hint: { type: String, default: null }, // hint displayed on `?` tooltip. If `null`, the "?" icon is not displayed"
  },
  computed: {
    instances: {
      get() {
        if (!this.value) {
          return [];
        } else {
          return this.value;
        }
      },
    },
  },
  methods: {
    triggerValidation(index) {
      // kind of HACK to trigger validation to other ID input field.
      // this is needed because of "ID already used" validation.
      this.instances.forEach((instance, i) => {
        if (i != index) {
          let input = this.$refs["id" + i];
          if (input) {
            input[0].validate(true);
          }
        }
      });
    },
    isAlreadyUsed(id, targetIndex = -1) {
      const instance = this.instances.find(
        (instance, index) => instance.id == id && index != targetIndex
      );
      return instance !== undefined;
    },
    addNewInstance() {
      this.$emit("input", [...this.instances, { id: this.newId(), val: null }]);
    },
    removeAllInstances() {
      this.$emit("input", []);
    },
    removeInstance(index) {
      this.instances.splice(index, 1);
      this.$emit("input", this.instances);
    },
    newId() {
      let i = 0;
      while (i <= 65535) {
        if (!this.isAlreadyUsed(i)) return i;
        else i++;
      }
    },
  },
};
</script>
