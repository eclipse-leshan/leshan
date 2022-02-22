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
  <div>
    <!-- Sender ID field -->
    <v-textarea
      filled
      label="Sender ID"
      rows="2"
      v-model="oscore.sid"
      :rules="[
        (v) => !!v || 'Sender ID is required',
        (v) => /^[0-9a-fA-F]+$/.test(v) || 'Hexadecimal format is expected',
      ]"
      @input="$emit('input', oscore)"
      hint="Hexadecimal format"
      spellcheck="false"
    ></v-textarea>
    <!-- Master Secret field -->
    <v-textarea
      filled
      label="Master Secret"
      rows="2"
      v-model="oscore.msec"
      :rules="[
        (v) => !!v || 'Master Secret is required',
        (v) => /^[0-9a-fA-F]+$/.test(v) || 'Hexadecimal format is expected',
      ]"
      @input="$emit('input', oscore)"
      hint="Hexadecimal format"
      spellcheck="false"
    ></v-textarea>
    <!-- Recipient ID field -->
    <v-textarea
      filled
      label="Recipient ID"
      rows="2"
      v-model="oscore.rid"
      :rules="[
        (v) => !!v || 'Recipient ID is required',
        (v) => /^[0-9a-fA-F]+$/.test(v) || 'Hexadecimal format is expected',
      ]"
      @input="$emit('input', oscore)"
      hint="Hexadecimal format"
      spellcheck="false"
    ></v-textarea>
  </div>
</template>
<script>
export default {
  props: { value: Object },
  data() {
    return {
      oscore: this.value,
    };
  },

  watch: {
    value(v) {
      this.oscore = v;
    },
  },
};
</script>
