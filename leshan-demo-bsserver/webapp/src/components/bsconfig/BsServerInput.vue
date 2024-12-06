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
    <v-text-field
      v-model="server.url"
      @update:model-value="$emit('update:model-value', server)"
      :label="
        server.mode == 'no_sec'
          ? 'Server URL (default :' + defaultNoSecValue + ')'
          : 'Server URL (default :' + defaultSecureValue + ')'
      "
      autofocus
      :placeholder="
        server.mode == 'no_sec' ? defaultNoSecValue : defaultSecureValue
      "
      class="examplePatch"
    ></v-text-field>
    <bs-security-input
      v-if="server"
      v-model:mode="server.security.mode"
      v-model:details="server.security.details"
      @update:mode="$emit('update:model-value', server)"
      @update:details="$emit('update:model-value', server)"
      :defaultrpk="defaultrpk"
      :defaultx509="defaultx509"
    />
    <!-- OSCORE Object -->
    <v-switch
      v-model="useOSCORE"
      @update:model-value="useOSCOREChanged($event)"
      label="Using OSCORE (Experimental - for now can not be used with DTLS)"
    ></v-switch>
    <oscore-input
      v-if="useOSCORE"
      v-model="server.oscore"
      @update:model-value="$emit('update:model-value', server)"
    >
    </oscore-input>
  </div>
</template>
<script>
import BsSecurityInput from "./BsSecurityInput.vue";
import OscoreInput from "@leshan-demo-servers-shared/components/security/OscoreInput.vue";

export default {
  components: { BsSecurityInput, OscoreInput },
  props: {
    modelValue: Object,
    defaultNoSecValue: String,
    defaultSecureValue: String,
    defaultrpk: {
      default: function () {
        return {};
      },
      type: Object,
    },
    defaultx509: {
      default: function () {
        return {};
      },
      type: Object,
    },
  },
  data() {
    return {
      useOSCORE: false, // true if OSCORE is used
      server: null, // internal server Config
    };
  },
  beforeMount() {
    this.initValue(this.modelValue);
  },
  watch: {
    modelValue(v) {
      this.initValue(v);
    },
  },
  methods: {
    initValue(initialValue) {
      if (!initialValue) {
        this.server = { security: { mode: "no_sec" } };
        this.useOSCORE = false;
      } else {
        this.server = initialValue;
        this.useOSCORE = initialValue.oscore ? true : false;
      }
    },
    useOSCOREChanged(useOSCORE) {
      if (useOSCORE) {
        this.server.oscore = {};
      } else {
        this.server.oscore = undefined;
      }
      this.$emit("update:model-value", this.server);
    },
    /*exclusifTlsOrOSCORE() {
      if (this.useDTLS) {
        this.$emit("update:tls", { mode: "psk", details: {} });
        this.$emit("update:oscore", undefined);
      } else if (this.useOSCORE) {
        this.$emit("update:tls", undefined);
        this.$emit("update:oscore", {});
      } else {
        this.$emit("update:tls", undefined);
        this.$emit("update:oscore", undefined);
      }
    },*/
  },
};
</script>
