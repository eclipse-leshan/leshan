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
  <v-app-bar theme="dark" flat density="compact">
    <v-app-bar-title>
      <v-img
        position="left"
        src="../assets/image/logo.png"
        max-height="32"
        v-if="$vuetify.display.smAndUp"
      ></v-img>
    </v-app-bar-title>
    <v-spacer></v-spacer>
    <!-- See more details about why we hide-slider and replace it by custom css (active-class="active") :
         https://github.com/eclipse/leshan/issues/1134 -->
    <v-tabs
      :align-tabs="$vuetify.display.smAndUp ? 'end' : undefined"
      :grow="!$vuetify.display.smAndUp"
      hide-slider
    >
      <v-tab
        v-for="page in pages"
        :key="page.title"
        fixed-tabs
        :to="page.route"
        selected-class="active"
        :aria-label="page.title"
      >
        <v-icon :icon="page.icon"></v-icon
        ><span class="pl-2" v-if="$vuetify.display.smAndUp">
          {{ page.title }}
        </span></v-tab
      >
    </v-tabs>
  </v-app-bar>
</template>

<script>
export default {
  /*
    pages looks like : 
    [
      {
        title: String,
        route: String,
        icon: String,
      },
    ],
  */
  props: { pages: Array },
};
</script>

<style scoped>
@keyframes becomeActive {
  from {
    border-bottom: 2px solid black;
  }
  to {
    border-bottom: 2px solid white;
  }
}

.active {
  border-spacing: 2px;
  border-bottom: 2px solid white;
  animation-name: becomeActive;
  animation-duration: 500ms;
}
</style>
