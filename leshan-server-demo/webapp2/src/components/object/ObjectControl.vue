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
      :title="'Create Instance /' + objectdef.id"
      >Create</request-button
    >
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
import { preference } from "vue-preferences";
import { instanceToREST } from "../../js/restutils";

const timeout = preference("timeout", { defaultValue: 5 });
const format = preference("multiformat", { defaultValue: "TLV" });

export default {
  components: { RequestButton, InstanceCreateDialog },
  props: { objectdef: Object, endpoint: String },
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
        this.$refs.C.resetState();
      },
    },
  },
  methods: {
    requestPath() {
      return `api/clients/${encodeURIComponent(this.endpoint)}/${
        this.objectdef.id
      }?timeout=${timeout.get()}&format=${format.get()}`;
    },
    updateState(content, requestButton) {
      let state = !content.valid
        ? "warning"
        : content.success
        ? "success"
        : "error";
      requestButton.changeState(state, content.status);
    },
    openCreateDialog() {
      this.dialog = true;
    },
    create(value) {
      let requestButton = this.$refs.C;
      let data = instanceToREST(this.objectdef, value.id, value.resources);
      this.axios
        .post(this.requestPath(), data)
        .then((response) => {
          this.updateState(response.data, requestButton);
        })
        .catch(() => {
          requestButton.resetState();
        });
    },
  },
};
</script>
