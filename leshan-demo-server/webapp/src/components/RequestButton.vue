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
  <v-badge :color="state" dot bottom overlap :title="details" :value="state">
    <v-btn
      class="ma-1"
      small
      tile
      min-width="0"
      elevation="0"
      :title="title"
      :loading="loading"
      :disabled="loading"
      @click.stop="onClick"
    >
      <slot></slot>
    </v-btn>
  </v-badge>
</template>
<script>
export default {
  props: {
    title: String,
  },
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
