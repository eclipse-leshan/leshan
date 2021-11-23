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
    <!-- Public Key field -->
    <v-textarea
      filled
      label="Client Public Key"
      v-model="rpk.key"
      :rules="[
        (v) => !!v || 'Public is required',
        (v) => /^[0-9a-fA-F]+$/.test(v) || 'Hexadecimal format is expected',
      ]"
      hint="SubjectPublicKeyInfo der encoded in Hexadecimal"
      @input="$emit('input', rpk)"
      spellcheck="false"
    ></v-textarea>
  </div>
</template>
<script>
export default {
  props: { value: Object },
  data() {
    return {
      rpk: this.value,
    };
  },

  watch: {
    value(v) {
      // on init create local copy
      if (v) {
        this.rpk = this.value;
      }
    },
  },
};
</script>
