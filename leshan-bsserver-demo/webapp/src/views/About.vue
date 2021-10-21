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
  <div class="about">
    <v-row class="text-center pa-5">
      <v-col cols="12">
        <v-img
          :src="require('@/assets/image/multicolor-leshan.png')"
          class="my-3"
          contain
          height="200"
        />
      </v-col>

      <v-col class="mb-4" cols="12">
        <h1 class="display-1 font-weight-bold mb-3">
          Leshan Bootstrap Server Demo
        </h1>

        <p class="subheading font-weight-regular">
          This is a
          <a
            href="https://github.com/eclipse/leshan/wiki/F.A.Q.#could-i-reuse-leshan--demo-"
            target="_blank"
            ><strong>LWM2M Bootstrap Server Demo</strong></a
          >
          based on java
          <a href="https://www.eclipse.org/leshan/" target="_blank"
            ><strong>Leshan</strong></a
          >
          library. <br />If you find any issue, please
          <a href="https://www.eclipse.org/leshan/" target="_blank">report it</a
          >.
        </p>
        <p class="subheading font-weight-regular" v-if="version && commitid">
          Version
          <a
            :href="'https://github.com/eclipse/leshan/tree/' + commitid"
            target="_blank"
            ><strong>{{ version }}</strong></a
          >
        </p>
      </v-col>
    </v-row>
  </div>
</template>

<script>
export default {
  data() {
    return {
      version: process.env.VUE_APP_VERSION,
      commitid: process.env.VUE_APP_COMMIT_ID,
    };
  },
};
</script>
