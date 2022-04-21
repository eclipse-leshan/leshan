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
import {
  mdiAlertCircleOutline,
  mdiAlphaOCircleOutline,
  mdiArrowExpandVertical,
  mdiAxisArrow,
  mdiBellRing,
  mdiCardTextOutline,
  mdiCellphoneArrowDown,
  mdiCellphoneWireless,
  mdiDatabase,
  mdiDatabaseSyncOutline,
  mdiDevices,
  mdiDoor,
  mdiFolderMultipleOutline,
  mdiGauge,
  mdiGestureTapButton,
  mdiLightbulbOnOutline,
  mdiListStatus,
  mdiMapMarker,
  mdiPackageDown,
  mdiPaletteOutline,
  mdiPuzzleOutline,
  mdiServer,
  mdiShieldCheckOutline,
  mdiSignal,
  mdiSignalDistanceVariant,
  mdiSignalVariant,
  mdiSim,
  mdiSimOutline,
  mdiTextBoxOutline,
  mdiTextRecognition,
  mdiThermometer,
  mdiTimerOutline,
  mdiToggleSwitch,
  mdiWaterPercent,
  mdiWidgetsOutline,
} from "@mdi/js";

export default {
  props: {
    objectId: Number,
    unknowObjectIcon: { type: String, default: mdiAlphaOCircleOutline },
    invalidObjectIcon: { type: String, default: mdiAlertCircleOutline },
  },
  methods: {
    idToIcon(id) {
      if (id < 0 || id > 65535) return this.invalidObjectIcon;

      switch (id) {
        case 1: // server
          return mdiServer;
        case 2: // ACL
          return mdiShieldCheckOutline;
        case 3: // device
          return mdiDevices;
        case 4: // connectivity monitoring
          return mdiSignalVariant;
        case 5: // firmware update
          return mdiCellphoneArrowDown;
        case 6: // location
          return mdiMapMarker;
        case 7: // connectivity statistics
          return mdiSignalDistanceVariant;
        case 9: // LWM2M Software Management
          return mdiPackageDown;
        case 10: //  LWM2M Cellular Connectivity
          return mdiSignal;
        case 11: // LWM2M APN Connection Profile
          return mdiSim;
        case 12: // WLAN connectivity
          return mdiCellphoneWireless;
        case 13: // LWM2M Bearer Selection
          return mdiSimOutline;
        case 14: // LWM2M Software Component
          return mdiPuzzleOutline;
        case 15: // DevCapMgmt
          return mdiWidgetsOutline;
        case 16: // Portfolio
          return mdiFolderMultipleOutline;
        case 19: // BinaryAppDataContainer
          return mdiDatabaseSyncOutline;
        case 3300: // General Sensor
          return mdiGauge;
        case 3303: // Temperature
          return mdiThermometer;
        case 3304: // Humidity
          return mdiWaterPercent;
        case 3311: // Light Control
          return mdiLightbulbOnOutline;
        case 3313: // Accelerometer
          return mdiAxisArrow;
        case 3323: // Pressure
          return mdiArrowExpandVertical;
        case 3335: // Colour
          return mdiPaletteOutline;
        case 3338: // Buzzer
          return mdiBellRing;
        case 3340: // Timer
          return mdiTimerOutline;
        case 3341: // Addressable Text Display
          return mdiTextRecognition;
        case 3342: // On/Off Switch
          return mdiToggleSwitch;
        case 3347: // Push Button
          return mdiGestureTapButton;
        case 3441: // LWM2M v1.0 Test Object
          return mdiListStatus;
        case 3442: // LWM2M v1.1 Test Object
          return mdiListStatus;
        case 3351: // powerupLog
          return mdiCardTextOutline;
        case 10259: // System Log
          return mdiTextBoxOutline;
        case 10260: // RDB (Runtime Database)
          return mdiDatabase;
        case 10351: // Door
          return mdiDoor;
        default:
          return this.unknowObjectIcon;
      }
    },
  },
};
</script>
