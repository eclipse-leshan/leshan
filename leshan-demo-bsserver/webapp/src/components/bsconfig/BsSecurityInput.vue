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
  <div>
    <v-select
      :items="modes"
      item-title="label"
      item-value="id"
      label="Security Mode"
      v-model="internalMode"
      @update:model-value="modeChanged()"
    ></v-select>
    <bs-psk-input
      v-if="internalMode == 'psk'"
      v-model="pskValue"
      @update:model-value="$emit('update:details', pskValue)"
    />
    <bs-rpk-input
      v-if="internalMode == 'rpk'"
      v-model="rpkValue"
      :defaultvalue="defaultrpk"
      @update:model-value="$emit('update:details', rpkValue)"
    />
    <bs-x-509-input
      v-if="internalMode == 'x509'"
      v-model="x509Value"
      :defaultvalue="defaultx509"
      @update:model-value="$emit('update:details', x509Value)"
    />
  </div>
</template>
<script>
import BsPskInput from "./BsPskInput.vue";
import BsRpkInput from "./BsRpkInput.vue";
import BsX509Input from "./BsX509Input.vue";

export default {
  components: { BsPskInput, BsRpkInput, BsX509Input },
  props: { mode: String, details: Object, defaultrpk: {}, defaultx509: {} },
  data() {
    return {
      modes: [
        { id: "no_sec", label: "No Security" },
        { id: "psk", label: "Pre-shared Key" },
        { id: "rpk", label: "Raw Public Key" },
        { id: "x509", label: "x509 Certificate" },
      ],
      internalMode: "no_sec",
      pskValue: {},
      rpkValue: {},
      x509Value: {},
    };
  },
  beforeMount() {
    this.initMode();
    this.initDetails();
  },
  watch: {
    mode() {
      this.initMode();
    },
    details() {
      this.initDetails();
    },
  },
  methods: {
    initMode() {
      // init internal state from "mode" propertie
      this.internalMode = this.mode;
    },
    initDetails() {
      // init internal state from "details" propertie
      switch (this.mode) {
        case "no_sec":
          this.pskValue = {};
          this.rpkValue = {};
          this.x509Value = {};
          break;
        case "psk":
          this.pskValue = this.details;
          this.rpkValue = {};
          this.x509Value = {};
          break;
        case "rpk":
          this.pskValue = {};
          this.rpkValue = this.details;
          this.x509Value = {};
          break;
        case "x509":
          this.rpkValue = {};
          this.pskValue = {};
          this.x509Value = this.details;
          break;
      }
    },
    modeChanged() {
      this.$emit("update:mode", this.internalMode);
      switch (this.internalMode) {
        case "no_sec":
          this.$emit("update:details", {});
          break;
        case "psk":
          this.$emit("update:details", this.pskValue);
          break;
        case "rpk":
          this.$emit("update:details", this.rpkValue);
          break;
        case "x509":
          this.$emit("update:details", this.x509Value);
          break;
      }
    },
  },
};
</script>
