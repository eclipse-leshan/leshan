<template>
  <div>
    <v-text-field
      v-model="server.url"
      :label="
        server.mode == 'no_sec'
          ? 'Server URL (default :' + defaultNoSecValue + ')'
          : 'Server URL (default :' + defaultSecureValue + ')'
      "
      autofocus
      :placeholder="
        server.mode == 'no_sec' ? defaultNoSecValue : defaultSecureValue
      "
      class="examplePatch"
    ></v-text-field>
    <security-input
      :mode.sync="server.mode"
      :details.sync="server.details"
      :defaultrpk="defaultrpk"
      :defaultx509="defaultx509"
    />
  </div>
</template>
<script>
import securityInput from "./SecurityInput.vue";

export default {
  components: { securityInput },
  props: {
    value: Object,
    defaultNoSecValue: String,
    defaultSecureValue: String,
    defaultrpk: {
      default: function() {
        return {};
      },
      type: Object,
    },
    defaultx509: {
      default: function() {
        return {};
      },
      type: Object,
    },
  },
  computed: {
    server: {
      get() {
        return this.value;
      },
      set(value) {
        this.$emit("input", value);
      },
    },
  },
};
</script>
