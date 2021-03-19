<template>
  <div id="app">
    <v-app>
      <leshan-nav-bar/>
      
      <v-main>
        <v-container fluid class="pa-0">
          <router-view >  </router-view>  
        </v-container>
      </v-main>
    </v-app>
  </div>
</template>

<script>
import LeshanNavBar from "@/components/LeshanNavBar.vue";

export default {
  components: {
    LeshanNavBar,
  },
};
</script>
