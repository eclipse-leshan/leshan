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
        This information will be used to add a
        <strong>LWM2M Bootstrap Server</strong> to your LWM2M Client during the
        bootstrap Session by writing an instance for object <code>/0</code>.
      </p>
      <p>
        This data will also be used
        <a
          href="https://github.com/eclipse/leshan/issues/690#issuecomment-490949978"
          target="_blank"
          >to know how the client must connect to this server</a
        >.
      </p>
    </v-card-text>
    <v-form ref="form" :value="valid" @input="$emit('update:valid', $event)">
      <server-input
        :value="value"
        @input="$emit('input', $event)"
        :defaultNoSecValue="defaultNoSecValue"
        :defaultSecureValue="defaultSecureValue"
        :defaultx509="defaultx509"
        :defaultrpk="defaultrpk"
      />
    </v-form>
  </v-card>
</template>
<script>
import ServerInput from "../bsconfig/ServerInput.vue";
export default {
  components: { ServerInput },
  props: {
    value: Object, // Bootstrap Server config
    valid: Boolean, // validation state of the form
    defaultNoSecValue: String, // default url for nosec endpoint
    defaultSecureValue: String, // default url for secured endpoint
    defaultx509: Object, // default server certificate
    defaultrpk: Object, // default server public key
  },
  methods: {
    resetValidation() {
      this.$refs.form.resetValidation();
    },
  },
};
</script>
