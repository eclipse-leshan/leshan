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
    <resource-instance-write-dialog
      v-if="showDialog"
      v-model="showDialog"
      :resourcedef="resourcedef"
      :path="path"
      :instanceId="instanceId"
      @write="write($event)"
    />
  </div>
</template>
<script>
import RequestButton from "../RequestButton.vue";
import ResourceInstanceWriteDialog from "./ResourceInstanceWriteDialog.vue";
import { preference } from "vue-preferences";
import { resourceInstanceToREST } from "../../js/restutils";

const timeout = preference("timeout", { defaultValue: 5 });
const singleFormat = preference("singleformat", { defaultValue: "TLV" });

export default {
  components: { RequestButton, ResourceInstanceWriteDialog },
  props: {
    resourcedef: Object,
    resourcePath: String,
    instanceId: Number,
    endpoint: String,
  },
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
    path() {
      return this.resourcePath + "/" + this.instanceId;
    },
    readable() {
      return this.resourcedef.operations.includes("R");
    },
    writable() {
      return this.resourcedef.operations.includes("W");
    },
  },
  methods: {
    requestPath() {
      return `api/clients/${encodeURIComponent(this.endpoint)}${this.path}`;
    },
    requestOption() {
      return `?timeout=${timeout.get()}&format=${singleFormat.get()}`;
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
            this.$store.newResourceInstanceValue(
              this.endpoint,
              this.resourcePath,
              this.instanceId,
              response.data.content.value
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
            this.$store.newResourceInstanceValue(
              this.endpoint,
              this.resourcePath,
              this.instanceId,
              response.data.content.value
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
      this.axios
        .put(
          this.requestPath() + this.requestOption(),
          resourceInstanceToREST(this.resourcedef, this.instanceId, value)
        )
        .then((response) => {
          this.updateState(response.data, requestButton);
          if (response.data.success)
            this.$store.newResourceInstanceValue(
              this.endpoint,
              this.resourcePath,
              this.instanceId,
              value,
              true
            );
        })
        .catch(() => {
          requestButton.resetState();
        });
    },
  },
};
</script>
