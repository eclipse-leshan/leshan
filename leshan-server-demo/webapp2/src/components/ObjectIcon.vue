<template>
  <v-icon>{{ idToIcon(objectId) }}</v-icon>
</template>
<script>
export default {
  props: { objectId: Number },
  methods: {
    idToIcon(id) {
      switch (id) {
        case 1:
          return "mdi-server";
        case 3:
          return "mdi-devices";
        case 6:
          return "mdi-map-marker";
        case 3303:
          return "mdi-thermometer";
        default:
          return "mdi-alpha-o-circle-outline";
      }
    },
  },
};
</script>
