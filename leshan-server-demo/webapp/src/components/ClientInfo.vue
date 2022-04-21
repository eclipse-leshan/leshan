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
  <span>
    <!-- info icon -->
    <v-tooltip :left="tooltipleft" :bottom="tooltipbottom">
      <template v-slot:activator="{ on }">
        <v-icon v-on="on" :small="small">
          {{ $icons.mdiInformation }}
        </v-icon>
      </template>
      Lifetime : {{ registration.lifetime }}s
      <br />
      Binding mode : {{ registration.bindingMode }}
      <span v-if="registration.queuemode">
        <br />
        Using QueueMode
      </span>
      <br />
      Protocole version : {{ registration.lwM2mVersion }}
      <br />
      Address : {{ registration.address }}
      <span
        v-for="(val, key) in registration.additionalRegistrationAttributes"
        :key="key"
      >
        <br />
        {{ key }} {{ val }}
      </span>
    </v-tooltip>
    <!-- secure icon -->
    <v-tooltip :left="tooltipleft" :bottom="tooltipbottom">
      <template v-slot:activator="{ on }">
        <v-icon v-on="on" :small="small" v-visible="registration.secure">
          {{ $icons.mdiLock }}
        </v-icon>
      </template>
      Communication over DTLS
    </v-tooltip>
    <!-- secure icon -->
    <v-tooltip :left="tooltipleft" :bottom="tooltipbottom">
      <template v-slot:activator="{ on }">
        <v-icon v-on="on" :small="small" v-visible="registration.sleeping">
          {{ $icons.mdiSleep }}
        </v-icon>
      </template>
      Device using Queue mode is absent
    </v-tooltip>
  </span>
</template>
<script>
export default {
  props: {
    registration: Object,
    small: {
      type: Boolean,
      default: false,
    },
    tooltipleft: {
      type: Boolean,
      default: false,
    },
    tooltipbottom: {
      type: Boolean,
      default: false,
    },
  },
};
</script>
