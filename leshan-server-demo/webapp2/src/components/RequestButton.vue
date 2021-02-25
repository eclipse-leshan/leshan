<template>
  <span>
    <v-badge :color="state" dot bottom overlap :title="details" :value="state">
      <v-btn
        class="ma-1"
        small
        elevation="0"
        :loading="loading"
        :disabled="loading"
        @click.stop="onClick"
      >
        <slot></slot>
      </v-btn>
    </v-badge>
  </span>
</template>
<script>
export default {
  data() {
    return {
      loading: false,
      state: null,
      details: null,
    };
  },
  methods: {
    onClick() {
      this.state = null;
      this.details = null;
      this.loading = true;
      this.$emit("on-click", this);
    },
    changeState(state, details) {
      this.state = state;
      this.details = details;
      this.loading = false;
    },
    resetState() {
      this.state = null;
      this.details = null;
      this.loading = false;
    },
  },
};
</script>
