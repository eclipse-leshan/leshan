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
  <v-app-bar dark flat dense max-height="48">
    <v-toolbar-title v-if="$vuetify.breakpoint.smAndUp">
      <v-img src="@/assets/image/logo.png" max-height="32" contain></v-img>
    </v-toolbar-title>

    <!-- See more details about why we hide-slider and replace it by custom css (active-class="active") :
         https://github.com/eclipse/leshan/issues/1134 -->
    <v-tabs
      :right="$vuetify.breakpoint.smAndUp"
      :grow="!$vuetify.breakpoint.smAndUp"
      hide-slider
    >
      <v-tab
        v-for="page in pages"
        :key="page.title"
        fixed-tabs
        :to="page.route"
        active-class="active"
      >
        <v-icon>{{ page.icon }}</v-icon
        ><span class="pl-2" v-if="$vuetify.breakpoint.smAndUp">
          {{ page.title }}
        </span></v-tab
      >
    </v-tabs>
  </v-app-bar>
</template>

<script>
export default {
  data: function () {
    return {
      pages: [
        { title: "Clients", route: "/clients", icon: this.$icons.mdiDevices },
        {
          title: "Server",
          route: "/server",
          icon: this.$icons.mdiServerSecurity,
        },
        {
          title: "Security",
          route: "/security",
          icon: this.$icons.mdiShieldKey,
        },
        {
          title: "About",
          route: "/about",
          icon: this.$icons.mdiInformationOutline,
        },
      ],
    };
  },
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
