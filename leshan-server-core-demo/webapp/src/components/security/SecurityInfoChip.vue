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
  <div v-if="securityInfo">
    <v-chip small>
      <v-icon left small>
        {{ modeIcon }}
      </v-icon>
      {{ securityInfo.mode }}
    </v-chip>
  </div>
  <div v-else>
    <v-chip small>
      <v-icon left small> mdi-lock-open-remove </v-icon>
      Nothing
    </v-chip>
  </div>
</template>
<script>
import { getModeIcon } from "../../js/securityutils.js";

export default {
  props: { securityInfo: Object /*securityInfo to display*/ },
  computed: {
    modeIcon() {
      return getModeIcon(this.securityInfo.mode);
    },
  },
};
</script>
