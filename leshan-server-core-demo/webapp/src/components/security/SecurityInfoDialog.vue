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
          editMode ? "Edit Security Information" : "Add Security Information"
        }}</span>
      </v-card-title>

      <!-- Form -->
      <v-card-text>
        <v-form ref="form" v-model="valid">
          <v-text-field
            v-model="securityInfo.endpoint"
            :rules="[(v) => !!v || 'Endpoint is required']"
            label="Endpoint"
            required
            :autofocus="!editMode"
            :disabled="editMode"
          ></v-text-field>
          <security-info-input
            :tls.sync="securityInfo.tls"
            :oscore.sync="securityInfo.oscore"
          />
        </v-form>
      </v-card-text>
      <!-- Buttons -->
      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn
          text
          @click="$emit(editMode ? 'edit' : 'new', securityInfo)"
          :disabled="!isValid"
        >
          {{ editMode ? "Save" : "Add" }}
        </v-btn>
        <v-btn text @click="show = false"> Cancel </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>
<script>
import SecurityInfoInput from "./SecurityInfoInput.vue";
export default {
  components: { SecurityInfoInput },
  props: { value: Boolean /*open/close dialog*/, initialValue: Object },
  data() {
    return {
      securityInfo: {}, // local state for current value to edit
      valid: true, // true is form is valid
      editMode: false, // true:new_cred, false:edit_cred
    };
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
    isValid: {
      get() {
        return (
          this.securityInfo &&
          (this.securityInfo.tls || this.securityInfo.oscore) &&
          this.valid
        );
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
          this.securityInfo = JSON.parse(JSON.stringify(this.initialValue));
          this.editMode = true;
        } else {
          // default value for creation
          this.securityInfo = {
            endpoint: "",
            tls: { mode: "psk", details: {} },
          };
          this.editMode = false;
        }
      }
    },
  },
};
</script>
