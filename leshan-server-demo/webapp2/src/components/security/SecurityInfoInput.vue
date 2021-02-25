<template>
  <div>
    <v-select
      :items="modes"
      label="Security Mode"
      v-model="internalMode"
      @input="modeChanged()"
    ></v-select>
    <psk-input
      v-if="internalMode == 'psk'"
      v-model="pskValue"
      @input="$emit('update:details', pskValue)"
    />
    <rpk-input
      v-if="internalMode == 'rpk'"
      v-model="rpkValue"
      @input="$emit('update:details', rpkValue)"
    />
    <x-509-input v-if="internalMode == 'x509'" />
  </div>
</template>
<script>
import PskInput from "./PskInput.vue";
import RpkInput from "./RpkInput.vue";
import X509Input from "./X509Input.vue";

export default {
  components: { PskInput, RpkInput, X509Input },
  props: { mode: String, details: Object },
  data() {
    return {
      modes: ["psk", "rpk", "x509"],
      internalMode: "psk",
      pskValue: {},
      rpkValue: {},
    };
  },
  beforeMount() {
    this.initMode();
    this.initDetails();
  },
  watch: {
    mode() {
      this.initMode();
    },
    details() {
      this.initDetails();
    },
  },
  methods: {
    initMode() {
      // init internal state from "mode" propertie
      this.internalMode = this.mode;
    },
    initDetails() {
      // init internal state from "details" propertie
      switch (this.mode) {
        case "psk":
          this.pskValue = this.details;
          this.rpkValue = {};
          break;
        case "rpk":
          this.pskValue = {};
          this.rpkValue = this.details;
          break;
        case "x509":
          this.rpkValue = {};
          this.pskValue = {};
          break;
      }
    },
    modeChanged() {
      this.$emit("update:mode", this.internalMode);
      switch (this.internalMode) {
        case "psk":
          this.$emit("update:details", this.pskValue);
          break;
        case "rpk":
          this.$emit("update:details", this.rpkValue);
          break;
        case "x509":
          this.$emit("update:details", {});
          break;
      }
    },
  },
};
</script>
