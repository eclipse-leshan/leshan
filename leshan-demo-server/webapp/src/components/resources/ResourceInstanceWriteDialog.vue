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
      <v-card-title class="headline grey lighten-2">
        Write Instance {{instanceId}} of "{{ resourcedef.name }}" Resource
      </v-card-title>
      <v-card-text>
        <div class="subtitle-1">Resource Instance {{ path }}</div>
        <div v-if="resourcedef['type']">
          Type : <span class="text-capitalize">{{ resourcedef["type"] }}</span>
        </div>
        <div v-if="resourcedef.range">Range : {{ resourcedef.range }}</div>
        <div v-if="resourcedef.units">Units : {{ resourcedef.units }}</div>
        <v-divider class="pa-2"/>
        <p style="white-space: pre-wrap">{{ resourcedef.description }}</p>
        <v-form ref="form" @submit.prevent="write">
          <single-value-input
            v-model="resourceValue"
            :resourcedef="resourcedef"
          />
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn text @click="write">
          Write
        </v-btn>
        <v-btn text @click="show = false">
          Cancel
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script>
import SingleValueInput from "../values/input/SingleValueInput.vue";

/**
 * A Dialog to Write a Resource Instance.
 */
export default {
  components: { SingleValueInput },
  props: {
    value: Boolean, // control if the dialog is displayed (v-model)
    resourcedef: Object, // the model of the resource
    path: String, // the path of the resource
    instanceId: Number, // the resource instance ID
  },
  data() {
    return {
      resourceValue: null,
    };
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
  },
  watch: {
    value(v) {
      // reset local state when dialog is open
      if (v) this.resourceValue = null;
    },
  },
  methods: {
    write() {
      this.show = false;
      this.$emit("write", this.resourceValue);
    },
  },
};
</script>
