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
      @input="$emit('input', server)"
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
    <security-input
      v-if="server"
      :mode.sync="server.security.mode"
      @update:mode="$emit('input', server)"
      :details.sync="server.security.details"
      @update:details="$emit('input', server)"
      :defaultrpk="defaultrpk"
      :defaultx509="defaultx509"
    />
    <!-- OSCORE Object -->
    <v-switch
      v-model="useOSCORE"
      @change="useOSCOREChanged($event)"
      label="Using OSCORE (Experimental - for now can not be used with DTLS)"
    ></v-switch>
    <oscore-input
      v-if="useOSCORE"
      v-model="server.oscore"
      @input="$emit('input', server)"
    >
    </oscore-input>
  </div>
</template>
<script>
import securityInput from "./SecurityInput.vue";
import OscoreInput from "@leshan-server-core-demo/components/security/OscoreInput.vue";

export default {
  components: { securityInput, OscoreInput },
  props: {
    value: Object,
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
    this.initValue(this.value);
  },
  watch: {
    value(v) {
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
      this.$emit("input", this.server);
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
