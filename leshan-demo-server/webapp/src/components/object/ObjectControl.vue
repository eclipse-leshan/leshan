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
    <request-button
      @on-click="openCreateDialog"
      ref="C"
      :title="'Create Instance ' + path"
      >Create</request-button
    >
    <request-button @on-click="observe" :title="'Observe ' + path"
      >Obs</request-button
    >
    <request-button
      @on-click="stopPassiveObserve"
      :title="'Passive Cancel Obverse ' + path"
    >
      <v-icon size="small">{{
        $icons.mdiEyeOffOutline
      }}</v-icon></request-button
    >
    <request-button
      @on-click="stopActiveObserve"
      :title="'Active Cancel Obverse ' + path"
    >
      <v-icon size="small">{{
        $icons.mdiEyeRemoveOutline
      }}</v-icon></request-button
    >
    <request-button @on-click="read" :title="'Read ' + path">R</request-button>
    <instance-create-dialog
      v-model="showDialog"
      :objectdef="objectdef"
      @create="create($event)"
    />
  </span>
</template>
<script>
import RequestButton from "../RequestButton.vue";
import InstanceCreateDialog from "../instance/InstanceCreateDialog.vue";
import { instanceToREST } from "../../js/restutils";

export default {
  components: { RequestButton, InstanceCreateDialog },
  props: { objectdef: Object, endpoint: String },
  data() {
    return {
      dialog: false,
    };
  },
  computed: {
    path() {
      return "/" + this.objectdef.id;
    },
    showDialog: {
      get() {
        return this.dialog;
      },
      set(value) {
        this.dialog = value;
        this.$refs.C.resetState();
      },
    },
  },
  methods: {
    requestPath() {
      return `api/clients/${encodeURIComponent(this.endpoint)}${this.path}`;
    },
    requestOption() {
      return `?timeout=${this.$pref.timeout}&format=${this.$pref.multiFormat}`;
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
    openCreateDialog() {
      this.dialog = true;
    },
    create(value) {
      let requestButton = this.$refs.C;
      let data = instanceToREST(this.objectdef, value.id, value.resources);
      this.axios
        .post(`${this.requestPath()}${this.requestOption()}`, data)
        .then((response) => {
          this.updateState(response.data, requestButton);
        })
        .catch(() => {
          requestButton.resetState();
        });
    },
    read(requestButton) {
      this.axios
        .get(`${this.requestPath()}${this.requestOption()}`)
        .then((response) => {
          this.updateState(response.data, requestButton);
          if (response.data.success) {
            this.$store.newObjectValue(
              this.endpoint,
              this.path,
              response.data.content.instances
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
            this.$store.newObjectValue(
              this.endpoint,
              this.path,
              response.data.content.instances
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
            this.$store.newObjectValue(
              this.endpoint,
              this.path,
              response.data.content.instances
            );
            this.$store.setObserved(this.endpoint, this.path, false);
          }
        })
        .catch(() => {
          requestButton.resetState();
        });
    },
  },
};
</script>
