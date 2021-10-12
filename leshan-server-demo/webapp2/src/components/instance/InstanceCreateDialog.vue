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
    hide-overlay
    fullscreen
    transition="dialog-bottom-transition"
  >
    <v-card>
      <!-- dialog title -->
      <v-card-title class="headline grey lighten-2">
        Create new Instance for Object {{ objectdef.name }} ({{ objectdef.id }})
      </v-card-title>
      <v-card-text>
        <v-form ref="form" @submit.prevent="write">
          <!-- instance if field -->
          <v-row dense>
            <v-col cols="12" md="2">
              <v-subheader>Instance Id </v-subheader>
            </v-col>
            <v-col cols="12" md="9">
              <v-text-field
                v-model="instance.id"
                label="Integer"
                hint="Let this field empty to let the client choose the instance ID"
              ></v-text-field>
            </v-col>
          </v-row>

          <!-- resources fields -->
          <resource-input
            v-for="resourcedef in writableResourceDef"
            :key="resourcedef.id"
            v-model="instance.resources[resourcedef.id]"
            :resourcedef="resourcedef"
          ></resource-input>
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-spacer></v-spacer>
        <!-- dialog buttons-->
        <v-btn text @click="create">
          Create
        </v-btn>
        <v-btn text @click="show = false">
          Cancel
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script>
import ResourceInput from "../resources/input/LabelledResourceInput.vue";
/**
 * A Dialog to Create a Object Instance.
 */
export default {
  components: { ResourceInput },
  props: {
    value: Boolean, // control if the dialog is displayed (v-model)
    objectdef: Object, // the model of the object
  },
  data() {
    return {
      instance: { id: null, resources: {} },
    };
  },
  watch: {
    value(v) {
      // reset local state when dialog is open
      if (v) {
        (this.instance = { id: null, resources: {} }), (this.id = null);
      }
    },
  },
  computed: {
    show: {
      get() {
        return this.value;
      },
      set(value) {
        this.$emit("input", value);
      },
    },
    writableResourceDef() {
      return this.objectdef.resourcedefs.filter((def) =>
        def.operations.includes("W")
      );
    },
  },
  methods: {
    create() {
      this.show = false;
      this.$emit("create", this.instance);
    },
  },
};
</script>
