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
      <!-- dialog title -->
      <v-card-title class="text-h5 bg-grey-lighten-2">
        Write Composite Object "{{ compositeObject.name }}".
      </v-card-title>
      <v-card-text>
        <div>
          Only Writable "Singleresource", "Resource Instance" are supported.
          <br />
          Nodes {{ ignoredeNodes.join(", ") }} from "{{ compositeObject.name }}"
          will be ignored because there are not supported for Composite-Write.
        </div>
        <v-form ref="form" @submit.prevent="write">
          <v-container fluid>
            <div v-for="node in writableNodes" :key="node.path.toString()">
              <h4 class="pa-1">{{ node.path }}</h4>
              <!--resource field-->
              <labelled-resource-input
                v-if="node.path.type == 'resource'"
                v-model="nodesValue[node.path.toString()]"
                :resourcedef="node.resource.def"
              ></labelled-resource-input>
              <!--resource instance field-->
              <labelled-resource-instance-input
                v-else-if="node.path.type == 'resourceinstance'"
                v-model="nodesValue[node.path.toString()]"
                :resourcedef="node.resource.def"
              ></labelled-resource-instance-input>
            </div>
          </v-container>
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-spacer></v-spacer>
        <!-- dialog buttons-->
        <v-btn variant="text" @click="write"> Write </v-btn>
        <v-btn variant="text" @click="show = false"> Cancel </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script>
import LabelledResourceInput from "../resources/input/LabelledResourceInput.vue";
import LabelledResourceInstanceInput from "../resources/input/LabelledResourceInstanceInput.vue";
/**
 * A Dialog to Write a Object Instance.
 */
export default {
  components: { LabelledResourceInput, LabelledResourceInstanceInput },
  props: {
    modelValue: Boolean, // control if the dialog is displayed (v-model)
    compositeObject: Object, // the composite Object
    nodes: Object, // LWM2M Nodes indexed by path
  },
  data() {
    return {
      nodesValue: {}, // path => value
      supportedTypes: [],
    };
  },
  watch: {
    modelValue(v) {
      // reset local state when dialog is open
      if (v) this.nodesValue = {};
    },
  },
  computed: {
    show: {
      get() {
        return this.modelValue;
      },
      set(value) {
        this.$emit("update:model-value", value);
      },
    },
    ignoredeNodes() {
      return Object.values(this.nodes)
        .filter((n) => !this.isSupported(n))
        .map((n) => n.path.toString());
    },
    writableNodes() {
      console.log(this.nodes);
      return Object.values(this.nodes).filter((n) => this.isSupported(n));
    },
  },
  methods: {
    isSupported(node) {
      console.log(node);
      console.log(
        (node.path.type == "resourceinstance" ||
          (node.path.type == "resource" &&
            node.resource.def.instancetype == "single")) &&
          node.resource.def.operations.includes("W")
      );
      return (
        (node.path.type == "resourceinstance" ||
          (node.path.type == "resource" &&
            node.resource.def.instancetype == "single")) &&
        node.resource.def.operations.includes("W")
      );
    },
    write() {
      this.show = false;
      this.$emit("write", this.nodesValue);
    },
  },
};
</script>
