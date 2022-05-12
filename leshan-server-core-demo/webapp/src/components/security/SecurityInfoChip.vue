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
  <div v-if="securityInfo && (securityInfo.tls || securityInfo.oscore)">
    <v-chip small v-if="securityInfo.tls">
      <v-icon left small>
        {{ modeIcon }}
      </v-icon>
      {{ securityInfo.tls.mode }}
    </v-chip>
    <v-chip small v-if="securityInfo.oscore">
      <v-icon left small> {{ oscoreIcon }} </v-icon>
      oscore
    </v-chip>
  </div>
  <div v-else>
    <v-chip small>
      <v-icon left small> {{ noSecIcon }} </v-icon>
      Nothing
    </v-chip>
  </div>
</template>
<script>
import {
  getModeIcon,
  getOscoreIcon,
  getNoSecIcon,
} from "../../js/securityutils.js";

export default {
  props: { securityInfo: Object /*securityInfo to display*/ },
  computed: {
    modeIcon() {
      return getModeIcon(this.securityInfo.tls.mode);
    },
    oscoreIcon() {
      return getOscoreIcon();
    },
    noSecIcon() {
      return getNoSecIcon();
    },
  },
};
</script>
