<template>
     <v-list dense>
        <v-list-item-group>
          <v-list-item
            v-for="object in objects"
            :key="object.id"
            :to="'/clients/' + $route.params.endpoint + '/' + object.id"
          >
            <v-list-item-icon class="me-2">
              <object-icon :objectId="object.id"/>
            </v-list-item-icon>
            <v-list-item-content>
              <v-list-item-title v-text="object.name"></v-list-item-title>
              <v-list-item-subtitle
                v-text="'/' + object.id"
              ></v-list-item-subtitle>
            </v-list-item-content>
          </v-list-item>
        </v-list-item-group>
      </v-list>
</template>
<script>
import ObjectIcon from './ObjectIcon.vue';
export default {
  components: { ObjectIcon },
  props: { objects: Array },
};
</script>