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
        This step aims to define how your <strong>LWM2M client</strong> will be
        authenticated by the <strong>LWM2M Bootstrap Server</strong>. <br />
        By the default there is no authentication expected and so the client
        should connect using any unsecured endpoint (e.g. using coap://
        endpoint). This behavior is only acceptable because this server is just
        a demo.
      </p>
    </v-card-text>
    <v-form ref="form" :value="valid" @input="$emit('update:valid', !useDTLS || $event)">
      <v-switch class="pl-5"
        v-model="useDTLS"
        @change="updateUseDTLS($event)"
        label="Using (D)TLS"
      ></v-switch>
      <security-info-input
        v-show="useDTLS"
        :mode="internalSecurityInfo.mode"
        :details="internalSecurityInfo.details"
        @update:mode="updateMode($event)"
        @update:details="updateDetails($event)"
      />
    </v-form>
  </v-card>
</template>
<script>
import SecurityInfoInput from "@leshan-server-core-demo/components/security/SecurityInfoInput.vue";
export default {
  components: { SecurityInfoInput },
  props: {
    value: Object, // Security Info
    valid: Boolean, // validation state of the form
  },
  data() {
    return {
      useDTLS: false,
      internalSecurityInfo: { mode: "psk", details: {} },
    };
  },
  watch: {
    value(v) {
      if (!v) {
        this.useDTLS = false;
        this.internalSecurityInfo = { mode: "psk", details: {} };
      } else {
        this.useDTLS = true;
        this.internalSecurityInfo = v;
      }
    },
  },
  methods: {
    updateUseDTLS(useDTLS) {
      if (useDTLS) {
        this.$emit("input", this.internalSecurityInfo);
        this.resetValidation();
        this.$emit('update:valid', false);
      } else {
        this.$emit("input", null);
        this.$emit('update:valid', true);
      }
    },
    updateMode(mode) {
      this.internalSecurityInfo.mode = mode;
      this.$emit("input", this.internalSecurityInfo);
    },
    updateDetails(mode) {
      this.internalSecurityInfo.details = mode;
      this.$emit("input", this.internalSecurityInfo);
    },
    resetValidation() {
      this.$refs.form.resetValidation();
    },
  },
};
</script>
