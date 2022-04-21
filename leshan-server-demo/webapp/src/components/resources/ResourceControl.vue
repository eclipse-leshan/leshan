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
    <request-button
      @on-click="observe"
      v-if="readable"
      :title="'Observe ' + path"
      >Obs</request-button
    >
    <request-button
      @on-click="stopObserve"
      v-if="readable"
      :title="'Passive Cancel Obverse ' + path"
    >
      <v-icon dense small>{{
        $icons.mdiEyeRemoveOutline
      }}</v-icon></request-button
    >
    <request-button @on-click="read" v-if="readable" :title="'Read ' + path"
      >R</request-button
    >
    <request-button
      @on-click="openWriteDialog"
      v-if="writable"
      ref="W"
      :title="'Write ' + path"
      >W</request-button
    >
    <request-button
      @on-click="exec"
      v-if="executable"
      :title="'Execute ' + path"
      >Exe</request-button
    >
    <request-button
      @on-click="execWithParams"
      v-if="executable"
      :title="'Execute with params ' + path"
      ><v-icon dense small>{{ $icons.mdiCogOutline }}</v-icon></request-button
    >
    <resource-write-dialog
      v-if="showDialog"
      v-model="showDialog"
      :resourcedef="resourcedef"
      :path="path"
      @write="write($event)"
    />
  </div>
</template>
<script>
import RequestButton from "../RequestButton.vue";
import ResourceWriteDialog from "./ResourceWriteDialog.vue";
import { preference } from "vue-preferences";
import { resourceToREST } from "../../js/restutils";

const timeout = preference("timeout", { defaultValue: 5 });
const singleFormat = preference("singleformat", { defaultValue: "TLV" });
const multiFormat = preference("multiformat", { defaultValue: "TLV" });

export default {
  components: { RequestButton, ResourceWriteDialog },
  props: { resourcedef: Object, path: String, endpoint: String },
  data() {
    return {
      dialog: false,
    };
  },
  computed: {
    showDialog: {
      get() {
        return this.dialog;
      },
      set(value) {
        this.dialog = value;
        this.$refs.W.resetState();
      },
    },
    readable() {
      return this.resourcedef.operations.includes("R");
    },
    writable() {
      return this.resourcedef.operations.includes("W");
    },
    executable() {
      return this.resourcedef.operations.includes("E");
    },
  },
  methods: {
    getFormat() {
      if (this.resourcedef.instancetype === "single") {
        return singleFormat.get();
      } else {
        return multiFormat.get();
      }
    },
    requestPath() {
      return `api/clients/${encodeURIComponent(this.endpoint)}${this.path}`;
    },
    requestOption() {
      return `?timeout=${timeout.get()}&format=${this.getFormat()}`;
    },
    updateState(content, requestButton) {
      let state = !content.valid
        ? "warning"
        : content.success
        ? "success"
        : "error";
      requestButton.changeState(state, content.status);
    },
    read(requestButton) {
      this.axios
        .get(this.requestPath() + this.requestOption())
        .then((response) => {
          this.updateState(response.data, requestButton);
          if (response.data.success)
            this.$store.newResourceValue(
              this.endpoint,
              this.path,
              response.data.content
            );
        })
        .catch(() => {
          requestButton.resetState();
        });
    },
    observe(requestButton) {
      this.axios
        .post(this.requestPath() + "/observe" + this.requestOption())
        .then((response) => {
          this.updateState(response.data, requestButton);
          if (response.data.success) {
            this.$store.newResourceValue(
              this.endpoint,
              this.path,
              response.data.content
            );
            this.$store.setObserved(this.endpoint, this.path, true);
          }
        })
        .catch(() => {
          requestButton.resetState();
        });
    },
    stopObserve(requestButton) {
      this.axios
        .delete(this.requestPath() + "/observe")
        .then(() => {
          requestButton.changeState("success");
          this.$store.setObserved(this.endpoint, this.path, false);
        })
        .catch(() => {
          requestButton.resetState();
        });
    },
    openWriteDialog() {
      this.dialog = true;
    },
    write(value) {
      let requestButton = this.$refs.W;
      let payload = resourceToREST(this.resourcedef, value);
      this.axios
        .put(this.requestPath() + this.requestOption(), payload)
        .then((response) => {
          this.updateState(response.data, requestButton);
          if (response.data.success)
            this.$store.newResourceValue(
              this.endpoint,
              this.path,
              payload,
              true
            );
        })
        .catch(() => {
          requestButton.resetState();
        });
    },
    exec(requestButton) {
      this.axios
        .post(this.requestPath())
        .then((response) => {
          this.updateState(response.data, requestButton);
        })
        .catch(() => {
          requestButton.resetState();
        });
    },
    execWithParams(requestButton) {
      this.$dialog
        .prompt({
          text: "Parameters for the execute request",
          title: "Execute " + this.path,
        })
        .then((params) => {
          if (params) {
            this.axios
              .post(this.requestPath(), params)
              .then((response) => {
                this.updateState(response.data, requestButton);
              })
              .catch(() => {
                requestButton.resetState();
              });
          } else {
            requestButton.resetState();
          }
        });
    },
  },
};
</script>
