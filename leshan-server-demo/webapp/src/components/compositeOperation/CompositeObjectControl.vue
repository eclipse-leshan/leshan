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
    <request-button @on-click="observe" title="Composite Observe"
      >Obs</request-button
    >
    <request-button @on-click="stopObserve" title="Passive Cancel Obverse">
      <v-icon dense small>{{
        $icons.mdiEyeRemoveOutline
      }}</v-icon></request-button
    >
    <request-button @on-click="read" title="Composite Read">R</request-button>
    <request-button @on-click="openWriteDialog" ref="W" title="Composite Write"
      >W</request-button
    >
    <composite-object-write-dialog
      v-model="showDialog"
      :compositeObject="compositeObject"
      :nodes="nodes"
      @write="write($event)"
    />
    <composite-operation-setting-menu>
      <template v-slot:activator="{ on, attrs }">
        <v-btn
          class="ma-1"
          small
          tile
          min-width="0"
          elevation="0"
          v-bind="attrs"
          v-on="on"
          title="Composite Operation Settings"
        >
          <v-icon small> {{ $icons.mdiTune }}</v-icon>
        </v-btn>
      </template>
    </composite-operation-setting-menu>
  </span>
</template>
<script>
import RequestButton from "../RequestButton.vue";
import { preference } from "vue-preferences";
import CompositeOperationSettingMenu from "./CompositeOperationSettingMenu.vue";
import CompositeObjectWriteDialog from "./CompositeObjectWriteDialog.vue";
import { singleValueToREST } from "../../js/restutils";

const timeout = preference("timeout", { defaultValue: 5 });
const compositePathFormat = preference("CompositePathFormat", {
  defaultValue: "SENML_CBOR",
});
const compositeNodeFormat = preference("CompositeNodeFormat", {
  defaultValue: "SENML_CBOR",
});

/**
 * List of Action button to execute operation (Read/Write/Observe ...) on a LWM2M Object Instance.
 */
export default {
  components: {
    RequestButton,
    CompositeOperationSettingMenu,
    CompositeObjectWriteDialog,
  },
  props: {
    endpoint: String, // endpoint name of the LWM2M client
    compositeObject: Object, // composite object to control
    nodes: Object, // LWM2M Nodes indexed by path
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
      return `api/clients/${encodeURIComponent(this.endpoint)}/composite`;
    },
    requestOption() {
      return `?timeout=${timeout.get()}&pathformat=${compositePathFormat.get()}&nodeformat=${compositeNodeFormat.get()}`;
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
        .get(`${this.requestPath()}${this.requestOption()}`, {
          params: { paths: this.compositeObject.paths.join(",") },
        })
        .then((response) => {
          this.updateState(response.data, requestButton);
          if (response.data.success) {
            this.$store.newNodes(this.endpoint, response.data.content);
          }
        })
        .catch(() => {
          requestButton.resetState();
        });
    },
    observe(requestButton) {
      this.axios
        .post(
          `${this.requestPath()}/observe${this.requestOption()}&paths=${this.compositeObject.paths.join(
            ","
          )}`
        )
        .then((response) => {
          this.updateState(response.data, requestButton);
          if (response.data.success) {
            this.$store.newNodes(this.endpoint, response.data.content);
            this.$store.setCompositeObjectObserved(
              this.endpoint,
              this.compositeObject,
              true
            );
          }
        })
        .catch(() => {
          requestButton.resetState();
        });
    },
    stopObserve(requestButton) {
      this.axios
        .delete(
          `${this.requestPath()}/observe?paths=${this.compositeObject.paths.join(
            ","
          )}`
        )
        .then(() => {
          requestButton.changeState("success");
          this.$store.setObserved(this.endpoint, this.path, false);
          this.$store.setCompositeObjectObserved(
            this.endpoint,
            this.compositeObject,
            false
          );
        })
        .catch(() => {
          requestButton.resetState();
        });
    },
    openWriteDialog() {
      this.dialog = true;
    },
    write(values) {
      let requestButton = this.$refs.W;
      // create Data for REST API
      let data = Object.entries(values).reduce((restNodes, [path, value]) => {
        let node = this.nodes[path];
        restNodes[path] = singleValueToREST(
          node.resource.def,
          node.path,
          value
        );
        return restNodes;
      }, {});
      console.log(data);
      // send Request
      this.axios
        .put(`${this.requestPath()}${this.requestOption()}`, data)
        .then((response) => {
          this.updateState(response.data, requestButton);
          if (response.data.success) {
            this.$store.newNodes(this.endpoint, data, true);
          }
        })
        .catch(() => {
          requestButton.resetState();
        });
    },
  },
};
</script>
