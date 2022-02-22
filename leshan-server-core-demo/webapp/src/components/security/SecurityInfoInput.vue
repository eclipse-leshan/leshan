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
    <!-- (D)TLS security info -->
    <v-switch
      v-model="useDTLS"
      @change="useDTLSChanged($event)"
      label="Using (D)TLS"
    ></v-switch>

    <tls-input
      v-if="useDTLS && tlsInfo"
      :mode.sync="tlsInfo.mode"
      :details.sync="tlsInfo.details"
      @update:details="$emit('update:tls', tlsInfo)"
      @update:mode="$emit('update:tls', tlsInfo)"
    />
    <!-- OSCORE security info -->
    <v-switch
      v-model="useOSCORE"
      @change="useOSCOREChanged($event)"
      label="Using OSCORE (Experimental - for now can not be used with DTLS)"
    ></v-switch>
    <oscore-input
      v-if="useOSCORE && oscoreInfo"
      v-model="oscoreInfo"
      @input="$emit('update:oscore', oscoreInfo)"
    >
    </oscore-input>
  </div>
</template>
<script>
import OscoreInput from "./OscoreInput.vue";
import TlsInput from "./TlsInput.vue";

export default {
  components: { TlsInput, OscoreInput },
  props: { tls: Object, oscore: Object },
  data() {
    return {
      useDTLS: true, // true if (D)TLS is used
      useOSCORE: false, // true if OSCORE is used
      tlsInfo: null, // internal tls state
      oscoreInfo: null, // internal oscore state
    };
  },
  beforeMount() {
    this.initOscore(this.oscore);
    this.initTls(this.tls);
  },
  watch: {
    oscore(value) {
      this.initOscore(value);
    },
    tls(value) {
      this.initTls(value);
    },
  },
  methods: {
    initOscore(oscore) {
      // do a deep copy
      // we should maybe rather use cloneDeep from lodash
      if (oscore) {
        this.useOSCORE = true;
        this.oscoreInfo = JSON.parse(JSON.stringify(oscore));
      } else {
        this.useOSCORE = false;
        this.oscoreInfo = undefined;
      }
    },
    initTls(tls) {
      // do a deep copy
      // we should maybe rather use cloneDeep from lodash
      if (tls) {
        this.useDTLS = true;
        this.tlsInfo = JSON.parse(JSON.stringify(tls));
      } else {
        this.useDTLS = false;
        this.tlsInfo = undefined;
      }
    },
    useDTLSChanged(useDTLS) {
      if (useDTLS) this.useOSCORE = !useDTLS; // temporary code while we don't support OSCORE over DTLS
      this.exclusifTlsOrOSCORE();
    },
    useOSCOREChanged(useOSCORE) {
      if (useOSCORE) this.useDTLS = !useOSCORE; // temporary code while we don't support OSCORE over DTLS
      this.exclusifTlsOrOSCORE();
    },
    exclusifTlsOrOSCORE() {
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
    },
  },
};
</script>
