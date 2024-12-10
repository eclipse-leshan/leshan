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
  <div class="pa-0">
    <div title="Timeout used to send request">
      <v-select
        single-line
        density="compact"
        variant="plain"
        hide-details
        :prefix="$vuetify.display.mdOnly ? null : 'Timeout :'"
        :items="timeoutList"
        item-value="val"
        item-title="txt"
        v-model="$pref.timeout"
        :append-icon="$icons.mdiHelpCircleOutline"
        @click:append="openTimeoutHelps"
      ></v-select>
    </div>
    <div title="Content Format used to send request handling single value">
      <v-select
        single-line
        density="compact"
        variant="plain"
        :prefix="$vuetify.display.mdOnly ? null : 'Single Value :'"
        hide-details
        :items="singleFormatList"
        v-model="$pref.singleFormat"
      ></v-select>
    </div>
    <div title="Content Format used to send request handling multiple values">
      <v-select
        single-line
        density="compact"
        variant="plain"
        hide-details
        :prefix="$vuetify.display.mdOnly ? null : 'Multi Value :'"
        :items="multiFormatList"
        v-model="$pref.multiFormat"
      ></v-select>
    </div>
  </div>
</template>
<script>
export default {
  data() {
    return {
      timeoutList: [
        { txt: "5s", val: 5 },
        { txt: "5min", val: 5 * 60 },
        { txt: "15min", val: 15 * 60 },
        { txt: "30min", val: 30 * 60 },
        { txt: "None", val: null },
      ],
      singleFormatList: [
        "TLV",
        "CBOR",
        "TEXT",
        "OPAQUE",
        "SENML_JSON",
        "SENML_CBOR",
        "JSON",
      ],
      multiFormatList: ["TLV", "SENML_JSON", "SENML_CBOR", "JSON"],
    };
  },
  methods: {
    openTimeoutHelps() {
      window.open(
        "https://github.com/eclipse/leshan/wiki/Request-Timeout",
        "_blank"
      );
    },
  },
};
</script>
