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
  <v-app-bar :dark="true" flat dense max-height="48">
    <v-toolbar-title>
      <!-- See more details about why we use "eager" : https://github.com/eclipse/leshan/issues/1134 -->
      <v-img
        src="@/assets/image/logo.png"
        max-height="32"
        eager
        contain
      ></v-img>
    </v-toolbar-title>

    <v-spacer></v-spacer>
    <v-tabs right>
      <v-tab
        v-for="page in pages"
        :key="page.title"
        fixed-tabs
        :to="page.route"
      >
        <v-icon class="pr-2">{{ page.icon }}</v-icon>
        <span v-if="$vuetify.breakpoint.smAndUp">
          {{ page.title }}
        </span></v-tab
      >
    </v-tabs>
  </v-app-bar>
</template>

<script>
export default {
  data: function() {
    return {
      pages: [
        { title: "Bootstrap", route: "/bootstrap", icon: "mdi-devices" },
        { title: "Server", route: "/server", icon: "mdi-server-security" },
        { title: "About", route: "/about", icon: "mdi-information-outline" },
      ],
    };
  },
};
</script>
