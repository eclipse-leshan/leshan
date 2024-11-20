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
  <v-dialog
    v-model="show"
    :scrim="false"
    fullscreen
    transition="dialog-bottom-transition"
  >
    <v-card>
      <!-- dialog title -->
      <v-card-title class="text-h5 bg-grey-lighten-2">
        Write Instance "{{ id }}" for Object {{ objectdef.name }} ({{
          objectdef.id
        }})
      </v-card-title>

      <v-card-text>
        <v-form ref="form" @submit.prevent="update">
          <v-container fluid>
            <!-- resources fields -->
            <resource-input
              v-for="resourcedef in writableResourceDef"
              :key="resourcedef.id"
              v-model="instanceValue[resourcedef.id]"
              :resourcedef="resourcedef"
            ></resource-input>
          </v-container>
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-spacer></v-spacer>
        <!-- dialog buttons-->
        <v-btn variant="text" @click="update"> Update </v-btn>
        <v-btn variant="text" @click="replace"> Replace </v-btn>
        <v-btn variant="text" @click="show = false"> Cancel </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script>
import ResourceInput from "../resources/input/LabelledResourceInput.vue";
/**
 * A Dialog to Write a Object Instance.
 */
export default {
  components: { ResourceInput },
  props: {
    modelValue: Boolean, // control if the dialog is displayed (v-model)
    objectdef: Object, // the model of the object
    id: Number, // ID of the Object Instance
  },
  data() {
    return {
      instanceValue: {},
    };
  },
  watch: {
    modelValue(v) {
      // reset local state when dialog is open
      if (v) this.instanceValue = {};
    },
  },
  computed: {
    show: {
      get() {
        return this.modelValue;
      },
      set(value) {
        this.$emit("update:model-value", value);
      },
    },
    writableResourceDef() {
      return this.objectdef.resourcedefs.filter((def) =>
        def.operations.includes("W")
      );
    },
  },
  methods: {
    replace() {
      this.show = false;
      this.$emit("replace", this.instanceValue);
    },
    update() {
      this.show = false;
      this.$emit("update", this.instanceValue);
    },
  },
};
</script>
