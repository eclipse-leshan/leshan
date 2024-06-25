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
  <v-card class="mb-12" elevation="0">
    <v-card-text class="pb-0">
      <p>
        To allow a <strong>LWM2M client</strong> to bootstrap to this server you
        need to create a configuration for it.
      </p>
      <p>
        This wizard is pretty limitted, the Leshan library allow you much more.
      </p>
    </v-card-text>
    <v-form ref="form" :value="valid" @input="$emit('update:valid', $event)">
      <v-text-field
        :value="value"
        :rules="[(v) => !!v || 'Endpoint is required']"
        label="Your Client Endpoint Name"
        @input="$emit('input', $event)"
        required
        autofocus
      ></v-text-field>
    </v-form>
  </v-card>
</template>
<script>
export default {
  props: {
    value: String, // endpoint name
    valid: Boolean, // validation state of the form
  },
  methods: {
    resetValidation() {
      this.$refs.form.resetValidation();
    },
  },
};
</script>
