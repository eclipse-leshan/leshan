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
        bootstrap Session by writing 1 instance for object <code>/0</code>.
      </p>
      <p>By default no LWM2M Bootstrap server is added.</p>
    </v-card-text>
    <v-form
      ref="form"
      :value="valid"
      @input="$emit('update:valid', !addServer || $event)"
    >
      <v-switch
        class="pl-5"
        v-model="addServer"
        label="Add a Bootstrap Server"
        @change="updateAddServer($event)"
      ></v-switch>
      <server-input
        v-show="addServer"
        :value="internalServer"
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
  data() {
    return {
      addServer: false,
      internalServer: { security: { mode: "no_sec" } }, // internal Bootstrap server Config
    };
  },
  watch: {
    value(v) {
      if (!v) {
        this.addServer = false;
        this.internalServer = { security: { mode: "no_sec" } };
      } else {
        this.addServer = true;
        this.internalServer = v;
      }
    },
  },
  methods: {
    updateAddServer(addServer) {
      if (addServer) {
        this.$emit("input", this.internalServer);
        this.resetValidation();
        this.$emit("update:valid", true);
      } else {
        this.$emit("input", null);
        this.$emit("update:valid", true);
      }
    },

    resetValidation() {
      this.$refs.form.resetValidation();
    },
  },
};
</script>
