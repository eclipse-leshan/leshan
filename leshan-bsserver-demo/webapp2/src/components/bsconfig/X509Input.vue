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
    <!--Client Certificate field -->
    <v-textarea
      filled
      label="Client Certificate"
      v-model="x509.client_certificate"
      :rules="[
        (v) =>
          !!defaultvalue.client_certificate ||
          !!v ||
          'Client Certificate is required',
        (v) =>
          !v || /^[0-9a-fA-F]*$/.test(v) || 'Hexadecimal format is expected',
      ]"
      hint="x509v3 der encoded in Hexadecimal"
      @input="$emit('input', x509)"
      spellcheck="false"
      rows="3"
      :placeholder="defaultvalue.client_certificate"
      persistent-placeholder
    ></v-textarea>
    <!--Client Private Key field -->
    <v-textarea
      filled
      label="Client Private Key"
      v-model="x509.client_pri_key"
      :rules="[
        (v) =>
          !!defaultvalue.client_pri_key ||
          !!v ||
          'Client Private Key is required',
        (v) =>
          !v || /^[0-9a-fA-F]*$/.test(v) || 'Hexadecimal format is expected',
      ]"
      hint="PKCS8 format der encoded in Hexadecimal"
      @input="$emit('input', x509)"
      spellcheck="false"
      rows="1"
      :placeholder="defaultvalue.client_pri_key"
      persistent-placeholder
    ></v-textarea>
    <!--Server Certificate  field -->
    <v-textarea
      filled
      label="Server Certificate"
      v-model="x509.server_certificate"
      :rules="[
        (v) =>
          !!defaultvalue.server_certificate ||
          !!v ||
          'Server Certificate is required',
        (v) =>
          !v || /^[0-9a-fA-F]*$/.test(v) || 'Hexadecimal format is expected',
      ]"
      hint="x509v3 der encoded in Hexadecimal"
      @input="$emit('input', x509)"
      spellcheck="false"
      rows="3"
      :placeholder="defaultvalue.server_certificate"
      persistent-placeholder
    ></v-textarea>
    <!--Certificate Usagefield -->
    <v-select
      :items="usages"
      item-text="label"
      item-value="id"
      label="Certificate Usage"
      v-model="x509.certificate_usage"
      @input="$emit('input', x509)"
    ></v-select>
  </div>
</template>
<script>
export default {
  props: { value: Object, defaultvalue: Object },
  data() {
    return {
      x509: { certificate_usage: 3 },
      usages: [
        { id: 0, label: "CA constraint" },
        { id: 1, label: "service certificate constraint" },
        { id: 2, label: "trust anchor assertion" },
        { id: 3, label: "domain-issued certificate" },
      ],
    };
  },

  watch: {
    value(v) {
      // on init create local copy
      if (v) {
        this.x509 = this.value;
      }
    },
  },
};
</script>
