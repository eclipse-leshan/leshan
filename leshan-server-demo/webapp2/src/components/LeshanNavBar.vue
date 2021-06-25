<template>
  <v-app-bar :dark="true" flat dense max-height="48">
    <v-toolbar-title v-if="$vuetify.breakpoint.smAndUp"
      ><v-img src="@/assets/image/logo.png" max-height="32" contain></v-img>
    </v-toolbar-title>

    <v-spacer></v-spacer>
    <v-tabs :right="$vuetify.breakpoint.smAndUp" :centered="!$vuetify.breakpoint.smAndUp">
      <v-tab
        v-for="page in pages"
        :key="page.title"
        fixed-tabs
        router
        :to="page.route"
      >
        <v-icon class="pr-2">{{ page.icon }}</v-icon
        ><span v-if="$vuetify.breakpoint.smAndUp">
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
        { title: "Clients", route: "/clients", icon: "mdi-devices" },
        { title: "Server", route: "/server", icon: "mdi-server-security" },
        { title: "Security", route: "/security", icon: "mdi-shield-key" },
        { title: "About", route: "/about", icon: "mdi-information-outline" },
      ],
    };
  },
};
</script>
