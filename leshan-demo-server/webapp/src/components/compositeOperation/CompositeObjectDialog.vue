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
        <span>{{
          editMode ? "Edit Composite Object" : "Add Composite Object"
        }}</span>
      </v-card-title>
      <!-- Form -->
      <v-card-text>
        <p class="subtitle-1">
          A Composite Object has a name and a list of path to LWM2M node
          (Object, Object Instance, Resource, Resource Instance).
        </p>
        <p class="subtitle-1">
          Root path is not yet supported. <br />
          Overlapped path will not work. (e.g. /3/0 with /3/0/1 is not valid)
        </p>
        <div>
          Note if you create Composite Object composed of too much resources,
          you could face some UI performance issue.
          <a href="https://github.com/eclipse/leshan/issues/1016"
            >This is a UI demo limitation</a
          >, if you have skills in vue.js your help will be welcome.
        </div>
        <v-form ref="form" v-model="valid">
          <v-text-field
            v-model="compositeObject.name"
            :rules="[
              (v) => !!v || 'name is required',
              (v) =>
                editMode ||
                !v ||
                !alreadyUsedName.includes(v) ||
                'name already used',
            ]"
            label="Name"
            required
            :autofocus="!editMode"
            :disabled="editMode"
          ></v-text-field>
          <paths-input v-model="compositeObject.paths" />
        </v-form>
      </v-card-text>

      <!-- Buttons -->
      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn
          text
          @click="$emit(editMode ? 'edit' : 'new', compositeObject)"
          :disabled="!valid"
        >
          {{ editMode ? "Save" : "Add" }}
        </v-btn>
        <v-btn text @click="show = false">
          Cancel
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>
<script>
import PathsInput from "@leshan-server-core-demo/components/path/PathsInput.vue";

export default {
  components: { PathsInput },
  props: {
    value: Boolean /*open/close dialog*/,
    initialValue: Object, // initial value : for edit mode.
    alreadyUsedName: { type: Array, default: () => [] }, // already used name to avoid to create 2 composite Object with same name
  },
  data() {
    return {
      compositeObject: {}, // local state for current value to edit
      valid: true, // true is form is valid
      editMode: false, // true:new, false:edit
    };
  },
  methods: {
    initialName() {
      let i = 1;
      let name;
      do {
        let name = "compositeObject" + i;
        if (!this.alreadyUsedName.includes(name)) {
          return name;
        }
        i++;
      } while (i < 1000);
      return name;
    },
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
  watch: {
    value(v) {
      if (v) {
        // reset validation and set initial value when dialog opens
        if (this.$refs.form) this.$refs.form.resetValidation();
        if (this.initialValue) {
          // do a deep copy
          // we should maybe rather use cloneDeep from lodash
          this.compositeObject = JSON.parse(JSON.stringify(this.initialValue));
          this.editMode = true;
        } else {
          // default value for creation
          this.compositeObject = { name: this.initialName(), paths: [""] };
          this.editMode = false;
        }
      }
    },
  },
};
</script>
