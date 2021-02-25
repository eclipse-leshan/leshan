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
            :mode.sync="securityInfo.mode"
            :details.sync="securityInfo.details"
          />
        </v-form>
      </v-card-text>

      <!-- Buttons -->
      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn text @click="close">
          Cancel
        </v-btn>
        <v-btn
          text
          @click="$emit(editMode ? 'edit' : 'new', securityInfo)"
          :disabled="!valid"
        >
          {{ editMode ? "Save" : "Add" }}
        </v-btn>
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
          this.securityInfo = { mode: "psk", details: {} };
          this.editMode = false;
        }
      }
    },
  },
  methods: {
    close() {
      this.show = false;
    },
  },
};
</script>
