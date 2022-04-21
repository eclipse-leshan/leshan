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
  <span>
    <request-button @on-click="observe" :title="'Observe ' + path"
      >Obs</request-button
    >
    <request-button
      @on-click="stopObserve"
      :title="'Passive Cancel Obverse ' + path"
    >
      <v-icon dense small>{{
        $icons.mdiEyeRemoveOutline
      }}</v-icon></request-button
    >
    <request-button @on-click="read" :title="'Read ' + path">R</request-button>
    <request-button @on-click="openWriteDialog" ref="W" :title="'Write ' + path"
      >W</request-button
    >
    <request-button @on-click="del" :title="'Delete ' + path"
      >Delete</request-button
    >
    <instance-write-dialog
      v-model="showDialog"
      :objectdef="objectdef"
      :id="id"
      @update="write($event, false)"
      @replace="write($event, true)"
    />
  </span>
</template>
<script>
import RequestButton from "../RequestButton.vue";
import InstanceWriteDialog from "../instance/InstanceWriteDialog.vue";
import { preference } from "vue-preferences";
import { instanceToREST } from "../../js/restutils";

const timeout = preference("timeout", { defaultValue: 5 });
const format = preference("multiformat", { defaultValue: "TLV" });

/**
 * List of Action button to execute operation (Read/Write/Observe ...) on a LWM2M Object Instance.
 */
export default {
  components: { RequestButton, InstanceWriteDialog },
  props: {
    endpoint: String, // endpoint name of the LWM2M client
    path: String, // path of the LWM2M object
    objectdef: Object, // model of the LWM2M object
    id: Number, // ID of the LWM2M Object Instance
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
  },
  methods: {
    requestPath() {
      return `api/clients/${encodeURIComponent(this.endpoint)}${this.path}`;
    },
    requestOption() {
      return `?timeout=${timeout.get()}&format=${format.get()}`;
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
        .get(`${this.requestPath()}${this.requestOption()}`)
        .then((response) => {
          this.updateState(response.data, requestButton);
          if (response.data.success) {
            this.$store.newInstanceValue(
              this.endpoint,
              this.path,
              response.data.content.resources
            );
          }
        })
        .catch(() => {
          requestButton.resetState();
        });
    },
    observe(requestButton) {
      this.axios
        .post(`${this.requestPath()}/observe${this.requestOption()}`)
        .then((response) => {
          this.updateState(response.data, requestButton);
          if (response.data.success) {
            this.$store.newInstanceValue(
              this.endpoint,
              this.path,
              response.data.content.resources
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
        .delete(`${this.requestPath()}/observe`)
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
    write(value, replace) {
      let requestButton = this.$refs.W;
      let data = instanceToREST(this.objectdef, this.id, value);
      this.axios
        .put(
          `${this.requestPath()}${this.requestOption()}&replace=${replace}`,
          data
        )
        .then((response) => {
          this.updateState(response.data, requestButton);
          if (response.data.success) {
            this.$store.newInstanceValue(
              this.endpoint,
              this.path,
              data.resources,
              true,
              !replace
            );
          }
        })
        .catch(() => {
          requestButton.resetState();
        });
    },
    del(requestButton) {
      this.axios
        .delete(`${this.requestPath()}?timeout=${timeout.get()}`)
        .then((response) => {
          this.updateState(response.data, requestButton);
          if (response.data.success) {
            this.$store.removeInstanceValue(this.endpoint, this.path);
          }
        })
        .catch(() => {
          requestButton.resetState();
        });
    },
  },
};
</script>
