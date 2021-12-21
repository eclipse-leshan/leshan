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
  <v-icon>{{ idToIcon(objectId) }}</v-icon>
</template>
<script>
export default {
  props: {
    objectId: Number,
    unknowObjectIcon: { type: String, default: "mdi-alpha-o-circle-outline" },
    invalidObjectIcon: { type: String, default: "mdi-alert-circle-outline" },
  },
  methods: {
    idToIcon(id) {
      if (id < 0 || id > 65535) return this.invalidObjectIcon;

      switch (id) {
        case 1: // server
          return "mdi-server";
        case 2: // ACL
          return "mdi-shield-check-outline";
        case 3: // device
          return "mdi-devices";
        case 4: // connectivity monitoring
          return "mdi-signal-variant";
        case 5: // firmware update
          return "mdi-cellphone-arrow-down";
        case 6: // location
          return "mdi-map-marker";
        case 7: // connectivity statistics
          return "mdi-signal-distance-variant";
        case 9: // LWM2M Software Management
          return "mdi-package-down";
        case 10: //  LWM2M Cellular Connectivity
          return "mdi-signal";
        case 11: // LWM2M APN Connection Profile
          return "mdi-sim";
        case 14: // LWM2M Software Component
          return "mdi-puzzle-outline";
        case 15: // DevCapMgmt
          return "mdi-widgets-outline";
        case 16: // Portfolio
          return "mdi-folder-multiple-outline";
        case 19: // BinaryAppDataContainer
          return "mdi-database-sync-outline";
        case 3300: // General Sensor
          return "mdi-gauge";
        case 3303: // Temperature
          return "mdi-thermometer";
        case 3304: // Humidity
          return "mdi-water-percent";
        case 3311: // Light Control
          return "mdi-lightbulb-on-outline";
        case 3313: // Accelerometer
          return "mdi-axis-arrow";
        case 3323: // Pressure
          return "mdi-arrow-expand-vertical";
        case 3335: // Colour
          return "mdi-palette-outline";
        case 3338: // Buzzer
          return "mdi-bell-ring";
        case 3341: // Addressable Text Display
          return "mdi-text-recognition";
        case 3342: // On/Off Switch
          return "mdi-toggle-switch";
        case 3347: // Push Button
          return "mdi-gesture-tap-button";
        case 3441: // LWM2M v1.0 Test Object
          return "mdi-list-status";
        case 3442: // LWM2M v1.1 Test Object
          return "mdi-list-status";
        case 10259: // System Log
          return "mdi-text-box-outline";
        case 10260: // RDB (Runtime Database)
          return "mdi-database";
        case 10351: // Door
          return "mdi-door";
        default:
          return this.unknowObjectIcon;
      }
    },
  },
};
</script>
