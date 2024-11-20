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
    <!-- identity field -->
    <v-textarea
      variant="filled"
      label="Identity"
      v-model="psk.identity"
      :rules="[(v) => !!v || 'Identity is required']"
      @update:model-value="$emit('update:model-value', psk)"
      spellcheck="false"
      rows="1"
    ></v-textarea>

    <!-- key field -->
    <v-textarea
      variant="filled"
      label="Key"
      v-model="psk.key"
      hint="Hexadecimal format"
      :rules="[
        (v) => !!v || 'Key is required',
        (v) => /^[0-9a-fA-F]+$/.test(v) || 'Hexadecimal format is expected',
      ]"
      @update:model-value="$emit('update:model-value', psk)"
      spellcheck="false"
      rows="1"
    ></v-textarea>
  </div>
</template>
<script>
export default {
  props: { modelValue: Object },
  data() {
    return {
      psk: this.modelValue,
    };
  },

  watch: {
    modelValue(v) {
      this.psk = v;
    },
  },
};
</script>
