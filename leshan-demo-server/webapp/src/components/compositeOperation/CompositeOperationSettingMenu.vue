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
  <v-menu v-model="menu" :close-on-content-click="false">
    <template v-slot:activator="props">
      <slot name="activator" v-bind="props" />
    </template>
    <v-card>
      <v-list>
        <v-list-item>
          <p class="text-subtitle-2">
            ContentFormat used to encode <strong>LWM2M Paths</strong> for
            <strong>Read/Observe</strong>-Composite Operation :
          </p>
        </v-list-item>
        <v-list-item>
          <v-list-item-action>
            <v-select
              density="compact"
              :items="compositePathFormatList"
              v-model="$pref.compositePathFormat"
            ></v-select>
          </v-list-item-action>
        </v-list-item>
        <v-list-item>
          <p class="text-subtitle-2">
            ContentFormat used to encode/decode <strong>LWM2M nodes</strong> for
            <strong>Read/Observe/Write</strong>-Composite Operation :
          </p>
        </v-list-item>
        <v-list-item>
          <v-list-item-action>
            <v-select
              single-line
              density="compact"
              :items="compositeNodeFormatList"
              v-model="$pref.compositeNodeFormat"
            ></v-select>
          </v-list-item-action>
        </v-list-item>
      </v-list>
    </v-card>
  </v-menu>
</template>
<script>
export default {
  data() {
    return {
      menu: false,
      compositePathFormatList: ["SENML_JSON", "SENML_CBOR"],
      compositeNodeFormatList: ["SENML_JSON", "SENML_CBOR"],
    };
  },
};
</script>
