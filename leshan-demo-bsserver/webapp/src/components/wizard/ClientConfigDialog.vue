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
    :scrim="false"
    fullscreen
    transition="dialog-bottom-transition"
  >
    <v-card>
      <!-- Title -->
      <v-card-title class="text-h5 bg-grey-lighten-2">
        <span>Add Client Configuration</span>
      </v-card-title>

      <!-- Form -->
      <v-stepper v-model="currentStep">
        <v-stepper-header>
          <v-stepper-item
            :complete="currentStep > 1"
            :value="1"
            title="Endpoint Name"
          />
          <v-divider></v-divider>
          <v-stepper-item
            :complete="currentStep > 2"
            :value="2"
            title="Credentials"
          />
          <v-divider></v-divider>
          <v-stepper-item
            :complete="currentStep > 3"
            :value="3"
            title="Paths to delete"
          />
          <v-divider></v-divider>
          <v-stepper-item
            :complete="currentStep > 4"
            :value="4"
            title="LWM2M Server Configuration"
          />
          <v-divider></v-divider>
          <v-stepper-item
            :complete="currentStep > 5"
            :value="5"
            title="LWM2M Bootstrap Server Configuration"
          />
        </v-stepper-header>

        <v-stepper-window v-model="currentStep">
          <v-stepper-window-item :value="1">
            <endpoint-step
              ref="step1"
              v-model:valid="valid[1]"
              v-model="config.endpoint"
            />
          </v-stepper-window-item>
          <v-stepper-window-item :value="2">
            <security-step
              ref="step2"
              v-model:valid="valid[2]"
              v-model="config.security"
            />
          </v-stepper-window-item>
          <v-stepper-window-item :value="3">
            <delete-step
              ref="step3"
              v-model:valid="valid[3]"
              v-model:pathToDelete="config.toDelete"
              v-model:autoId="config.autoIdForSecurityObject"
            />
          </v-stepper-window-item>
          <v-stepper-window-item :value="4">
            <server-step
              ref="step4"
              v-model:valid="valid[4]"
              v-model="config.dm"
              :defaultNoSecValue="defval.dm.url.nosec"
              :defaultSecureValue="defval.dm.url.sec"
            />
          </v-stepper-window-item>
          <v-stepper-window-item :value="5">
            <bootstrap-server-step
              ref="step5"
              v-model:valid="valid[5]"
              v-model="config.bs"
              :defaultNoSecValue="defval.bs.url.nosec"
              :defaultSecureValue="defval.bs.url.sec"
              :defaultx509="defaultx509"
              :defaultrpk="defaultrpk"
            />
          </v-stepper-window-item>
        </v-stepper-window>
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
          variant="text"
          @click="currentStep = currentStep - 1"
          :disabled="currentStep == 1"
        >
          Previous
        </v-btn>
        <v-btn variant="text" @click="close"> Cancel </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>
<script>
import {
  toHex,
  base64ToBytes,
} from "@leshan-demo-servers-shared/js/byteutils.js";
import EndpointStep from "./EndpointStep.vue";
import SecurityStep from "./SecurityStep.vue";
import DeleteStep from "./DeleteStep.vue";
import ServerStep from "./ServerStep.vue";
import BootstrapServerStep from "./BootstrapServerStep.vue";

export default {
  components: {
    EndpointStep,
    SecurityStep,
    ServerStep,
    BootstrapServerStep,
    DeleteStep,
  },
  props: { modelValue: Boolean /*open/close dialog*/ },
  data() {
    return {
      nbSteps: 5,
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
        return this.modelValue;
      },
      set(value) {
        this.$emit("update:modelValue", value);
      },
    },
  },
  beforeMount() {
    this.axios.get("api/server/endpoint").then((response) => {
      // Search bootstrap coap(s) ports.
      let coapPort, coapsPort;
      for (const ep of response.data) {
        if (!coapPort && ep.uri.scheme == "coap") {
          coapPort = ep.uri.port;
        }
        if (!coapsPort && ep.uri.scheme == "coaps") {
          coapsPort = ep.uri.port;
        }
      }

      this.defval.dm.url.nosec = `coap://${location.hostname}:5683`;
      this.defval.dm.url.sec = `coaps://${location.hostname}:5684`;
      this.defval.bs.url.nosec = `coap://${location.hostname}:${coapPort}`;
      this.defval.bs.url.sec = `coaps://${location.hostname}:${coapsPort}`;
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
    modelValue(v) {
      if (v) {
        // reset validation and set initial value when dialog opens
        this.config = {
          endpoint: null,
          security: null,
          dm: { security: { mode: "no_sec" } },
          bs: null,
          toDelete: ["/0", "/1", "/21"],
          autoIdForSecurityObject: false,
        };
        this.currentStep = 1;
        for (let i = 1; i <= this.nbSteps; i++) {
          // Not so clean but didn't find better way for now than initialize valid value manually
          this.valid[i] = i == 1 ? false : true;
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
      // Apply default for dm
      if (res.dm) {
        if (!res.dm.url) {
          res.dm.url =
            res.dm.security.mode == "no_sec"
              ? this.defval.dm.url.nosec
              : this.defval.dm.url.sec;
        }
      }
      // Apply default for bs
      if (res.bs) {
        if (!res.bs.url) {
          res.bs.url =
            res.bs.security.mode == "no_sec"
              ? this.defval.bs.url.nosec
              : this.defval.bs.url.sec;
        }

        // apply default rpk value for bs server
        if (res.bs.security.mode == "rpk") {
          for (const key in this.defaultrpk) {
            if (!res.bs.security.details[key]) {
              res.bs.security.details[key] = this.defaultrpk[key];
            }
          }
        }
        // apply default x509 value for bs server
        if (res.bs.security.mode == "x509") {
          for (const key in this.defaultx509) {
            if (!res.bs.security.details[key]) {
              res.bs.security.details[key] = this.defaultx509[key];
            }
          }
        }
      }

      // apply endpoint to security
      if (res.security) {
        res.security.endpoint = res.endpoint;
      }

      return res;
    },
    close() {
      this.show = false;
    },
  },
};
</script>
