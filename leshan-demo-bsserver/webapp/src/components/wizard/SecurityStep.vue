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
    <v-form ref="form" :value="valid" @input="$emit('update:valid', $event)">
      <security-info-input
        :tls.sync="internalSecurityInfo.tls"
        :oscore.sync="internalSecurityInfo.oscore"
        @update:tls="$emit('input', internalSecurityInfo)"
        @update:oscore="$emit('input', internalSecurityInfo)"
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
      internalSecurityInfo: {},
    };
  },
  watch: {
    value(v) {
      if (!v) {
        this.internalSecurityInfo = {};
      } else {
        this.internalSecurityInfo = v;
      }
    },
  },
  methods: {
    resetValidation() {
      this.$refs.form.resetValidation();
    },
  },
};
</script>
