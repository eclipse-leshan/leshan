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
        <strong>LWM2M Server</strong> to your LWM2M Client during the bootstrap
        Session by writing 1 instance for objects <code>/0</code> and
        <code>/1</code>.
      </p>
      <p>
        By default a LWM2M Server <code>{{ defaultNoSecValue }}</code> with
        <strong>NO_SEC</strong> Security Mode is created.
      </p>
    </v-card-text>
    <v-form
      ref="form"
      :model-value="valid"
      @update:model-value="$emit('update:valid', !addServer || $event)"
    >
      <v-switch
        class="pl-5"
        v-model="addServer"
        label="Add a DM Server"
        @update:model-value="updateAddServer($event)"
      ></v-switch>
      <bs-server-input
        v-show="addServer"
        :model-value="internalServer"
        @update:model-value="$emit('update:model-value', $event)"
        :defaultNoSecValue="defaultNoSecValue"
        :defaultSecureValue="defaultSecureValue"
      />
    </v-form>
  </v-card>
</template>
<script>
import BsServerInput from "../bsconfig/BsServerInput.vue";
export default {
  components: { BsServerInput },
  props: {
    modelValue: Object, // ServerConfig
    valid: Boolean, // validation state of the form
    defaultNoSecValue: String, // default url for nosec endpoint
    defaultSecureValue: String, // default url for secured endpoint
  },
  data() {
    return {
      addServer: true,
      internalServer: { security: { mode: "no_sec" } }, // internal server Config
    };
  },
  watch: {
    modelValue(v) {
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
        this.$emit("update:model-value", this.internalServer);
        this.resetValidation();
        this.$emit("update:valid", true);
      } else {
        this.$emit("update:model-value", null);
        this.$emit("update:valid", true);
      }
    },

    resetValidation() {
      this.$refs.form.resetValidation();
    },
  },
};
</script>
