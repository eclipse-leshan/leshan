<template>
  <v-app-bar :dark="true" flat dense max-height="48">
    <v-toolbar-title
      ><v-img src="@/assets/image/logo.png" max-height="48" contain></v-img>
    </v-toolbar-title>

    <v-spacer></v-spacer>
    <v-tabs right>
      <v-tab
        v-for="page in pages"
        :key="page.title"
        fixed-tabs
        router
        :to="page.route"
      >
        <v-icon class="pr-2">{{ page.icon }}</v-icon> {{ page.title }}</v-tab
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
        { title: "About", route: "/about", icon: "mdi-information-outline" },
      ],
    };
  },
};
</script>
