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
      @on-click="stopPassiveObserve"
      v-if="readable"
      :title="'Passive Cancel Obverse ' + path"
    >
      <v-icon size="small">{{
        $icons.mdiEyeOffOutline
      }}</v-icon></request-button
    >
    <request-button
      @on-click="stopActiveObserve"
      v-if="readable"
      :title="'Active Cancel Obverse ' + path"
    >
      <v-icon size="small">{{
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
      ><v-icon size="small">{{ $icons.mdiCogOutline }}</v-icon></request-button
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
import { resourceToREST } from "../../js/restutils";

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
      return this.resourcedef.operations === "E";
    },
  },
  methods: {
    getFormat() {
      if (this.resourcedef.instancetype === "single") {
        return this.$pref.singleFormat;
      } else {
        return this.$pref.multiFormat;
      }
    },
    requestPath() {
      return `api/clients/${encodeURIComponent(this.endpoint)}${this.path}`;
    },
    requestOption() {
      return `?timeout=${this.$pref.timeout}&format=${this.getFormat()}`;
    },
    updateState(content, requestButton) {
      if ("valid" in content || "success" in content) {
        let state = !content.valid
          ? "warning"
          : content.success
          ? "success"
          : "error";
        requestButton.changeState(state, content.status);
      } else {
        requestButton.resetState();
      }
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
    stopPassiveObserve(requestButton) {
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
    stopActiveObserve(requestButton) {
      this.axios
        .delete(
          this.requestPath() + `/observe?active&timeout=${this.$pref.timeout}`
        )
        .then((response) => {
          this.updateState(response.data, requestButton);
          if (response.data.success) {
            this.$store.newResourceValue(
              this.endpoint,
              this.path,
              response.data.content
            );
            this.$store.setObserved(this.endpoint, this.path, false);
          }
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
