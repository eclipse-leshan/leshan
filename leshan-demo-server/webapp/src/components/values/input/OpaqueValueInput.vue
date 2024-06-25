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
  <v-row>
    <v-col cols="12" md="3">
      <v-file-input
        label="File input"
        v-model="file"
        show-size
        clearable
        :disabled="!!localOpaqueValue"
      ></v-file-input>
    </v-col>
    <v-col>
      <v-textarea
        auto-grow
        rows="1"
        label="Byte Value in Hexadecimal"
        :suffix="resourcedef.units"
        :rules="[
          (v) =>
            !v || /^[0-9a-fA-F]+$/.test(v) || 'Hexadecimal format is expected',
        ]"
        v-model="opaqueValue"
        clearable
        :disabled="!!localFile"
      ></v-textarea>
    </v-col>
  </v-row>
</template>
<script>
/**
 * An input for OPAQUE single value LWM2M node ("Single Instance Resource" or "Resource Instance")
 */
export default {
  props: {
    value: null, // the input value for this LWM2M Node (v-model)
    resourcedef: Object, // the model of the resource
  },
  data() {
    return {
      localOpaqueValue: null,
      localFile: null,
    };
  },
  watch: {
    value(v) {
      if (v == null) {
        this.localFile = null;
        this.localOpaqueValue = null;
      }
    },
  },
  computed: {
    opaqueValue: {
      get() {
        return this.localOpaqueValue;
      },
      set(v) {
        this.localOpaqueValue = v;
        this.$emit("input", this.localOpaqueValue);
      },
    },
    file: {
      get() {
        return this.localFile;
      },
      set(v) {
        this.localFile = v;
        if (v) {
          this.toByteArray(this.localFile).then((p) => this.$emit("input", p));
        } else {
          this.$emit("input", v);
        }
      },
    },
  },
  methods: {
    // utility to get hex value from file
    toByteArray(file) {
      return new Promise((resolve) => {
        var reader = new FileReader();
        reader.onload = function() {
          var u = new Uint8Array(this.result),
            a = new Array(u.length),
            i = u.length;
          while (
            i-- // map to hex
          )
            a[i] = (u[i] < 16 ? "0" : "") + u[i].toString(16);
          u = null; // free memory
          resolve(a.join(""));
        };
        reader.readAsArrayBuffer(file);
      });
    },
  },
};
</script>
