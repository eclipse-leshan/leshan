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
  <div id="app">
    <v-app>
      <leshan-nav-bar :pages="pages" />

      <v-main>
        <v-container fluid class="pa-0">
          <router-view> </router-view>
        </v-container>
      </v-main>
    </v-app>
  </div>
</template>

<script>
import LeshanNavBar from "@leshan-demo-servers-shared/components/LeshanNavBar.vue";

export default {
  components: {
    LeshanNavBar,
  },
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
