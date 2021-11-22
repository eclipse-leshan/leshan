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
      <!-- Title -->
      <v-card-title class="headline grey lighten-2">
        <span>Add Client Configuration</span>
      </v-card-title>

      <!-- Form -->
      <v-stepper v-model="currentStep">
        <v-stepper-header>
          <v-stepper-step :complete="currentStep > 1" step="1">
            Endpoint Name
          </v-stepper-step>
          <v-divider></v-divider>
          <v-stepper-step :complete="currentStep > 2" step="2">
            LWM2M Server Configuration
          </v-stepper-step>
          <v-divider></v-divider>
          <v-stepper-step :complete="currentStep > 3" step="3">
            LWM2M Bootstrap Server Configuration
          </v-stepper-step>
        </v-stepper-header>

        <v-stepper-items>
          <v-stepper-content step="1">
            <endpoint-step
              ref="step1"
              :valid.sync="valid[1]"
              v-model="config.endpoint"
            />
          </v-stepper-content>
          <v-stepper-content step="2">
            <server-step
              ref="step2"
              :valid.sync="valid[2]"
              v-model="config.dm"
              :defaultNoSecValue="defval.dm.url.nosec"
              :defaultSecureValue="defval.dm.url.sec"
            />
          </v-stepper-content>
          <v-stepper-content step="3">
            <bootstrap-server-step
              ref="step3"
              :valid.sync="valid[3]"
              v-model="config.bs"
              :defaultNoSecValue="defval.bs.url.nosec"
              :defaultSecureValue="defval.bs.url.sec"
              :defaultx509="defaultx509"
              :defaultrpk="defaultrpk"
            />
          </v-stepper-content>
        </v-stepper-items>
      </v-stepper>

      <!-- Buttons -->
      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn
          elevation="0"
          @click="currentStep = currentStep + 1"
          :disabled="!valid[currentStep] || currentStep == nbSteps"
        >
          Next
        </v-btn>
        <v-btn
          elevation="0"
          @click="$emit('add', applyDefault(config))"
          :disabled="!valid[currentStep]"
        >
          Add
        </v-btn>
        <v-btn
          text
          @click="currentStep = currentStep - 1"
          :disabled="currentStep == 1"
        >
          Previous
        </v-btn>
        <v-btn text @click="close">
          Cancel
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>
<script>
import { toHex, base64ToBytes } from "../../js/byteutils.js";
import EndpointStep from "./EndpointStep.vue";
import ServerStep from "./ServerStep.vue";
import BootstrapServerStep from "./BootstrapServerStep.vue";

export default {
  components: { EndpointStep, ServerStep, BootstrapServerStep },
  props: { value: Boolean /*open/close dialog*/ },
  data() {
    return {
      nbSteps: 3,
      config: {}, // local state for current config
      valid: [],
      currentStep: 1,
      defval: {
        dm: { url: {} },
        bs: { url: {} },
      },
      defaultrpk: {},
      defaultx509: {},
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
  beforeMount() {
    this.axios.get("api/server/endpoint").then((response) => {
      this.defval.dm.url.nosec = `coap://${location.hostname}:5683`;
      this.defval.dm.url.sec = `coaps://${location.hostname}:5684`;
      this.defval.bs.url.nosec = `coap://${location.hostname}:${response.data.unsecuredEndpointPort}`;
      this.defval.bs.url.sec = `coaps://${location.hostname}:${response.data.securedEndpointPort}`;
    });

    this.axios.get("api/server/security").then((response) => {
      if (response.data.certificate) {
        let certificate = response.data.certificate;
        this.defaultx509.server_certificate = toHex(
          base64ToBytes(certificate.b64Der)
        );
        let pubkey = response.data.certificate.pubkey;
        this.defaultrpk.server_pub_key = toHex(base64ToBytes(pubkey.b64Der));
      } else if (response.data.pubkey) {
        this.defaultx509 = {};
        let pubkey = response.data.certificate.pubkey;
        this.defaultrpk.server_pub_key = toHex(base64ToBytes(pubkey.b64Der));
      }
    });
  },
  watch: {
    value(v) {
      if (v) {
        // reset validation and set initial value when dialog opens
        this.config = {
          endpoint: null,
          dm: { mode: "no_sec" },
          bs: { mode: "no_sec" },
        };
        this.currentStep = 1;
        for (let i = 1; i <= this.nbSteps; i++) {
          this.valid[i] = true;
          if (this.$refs["step" + i]) this.$refs["step" + i].resetValidation();
        }
      }
    },
  },

  methods: {
    applyDefault(c) {
      // do a deep copy
      // we should maybe rather use cloneDeep from lodash
      let res = JSON.parse(JSON.stringify(c));
      if (!res.dm.url) {
        res.dm.url =
          res.dm.mode == "no_sec"
            ? this.defval.dm.url.nosec
            : this.defval.dm.url.sec;
      }
      if (!res.bs.url) {
        res.bs.url =
          res.bs.mode == "no_sec"
            ? this.defval.bs.url.nosec
            : this.defval.bs.url.sec;
      }
      // apply default rpk value for bs server
      if (res.bs.mode == "rpk") {
        for (const key in this.defaultrpk) {
          if (!res.bs.details[key]) {
            res.bs.details[key] = this.defaultrpk[key];
          }
        }
      }
      // apply default x509 value for bs server
      if (res.bs.mode == "x509") {
        for (const key in this.defaultx509) {
          if (!res.bs.details[key]) {
            res.bs.details[key] = this.defaultx509[key];
          }
        }
      }

      return res;
    },
    close() {
      this.show = false;
    },
  },
};
</script>
