<template>
  <div id="app">
    <v-app>
      <leshan-nav-bar/>
      
      <v-main>
        <v-container fluid class="pa-0">
          <router-view >  </router-view>  
        </v-container>
      </v-main>
      <!--leshan-footer /-->
    </v-app>
  </div>
</template>

<script>
import LeshanNavBar from "@/components/LeshanNavBar.vue";
//import LeshanFooter from "@/components/LeshanFooter.vue";

export default {
  components: {
    LeshanNavBar,
    //LeshanFooter,
  },
};
</script>
