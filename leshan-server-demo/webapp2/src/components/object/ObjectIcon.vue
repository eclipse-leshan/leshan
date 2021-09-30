<template>
  <v-icon>{{ idToIcon(objectId) }}</v-icon>
</template>
<script>
export default {
  props: { objectId: Number },
  methods: {
    idToIcon(id) {
      switch (id) {
        case 1: // server
          return "mdi-server";
        case 2: // ACL
          return "mdi-shield-check-outline";
        case 3: // device
          return "mdi-devices";
        case 4: // connectivity monitoring
          return "mdi-signal-variant";
        case 5: // firmware update
          return "mdi-cellphone-arrow-down";
        case 6: // location
          return "mdi-map-marker";
        case 7: // connectivity statistics
          return "mdi-signal-distance-variant";
        case 11: // LWM2M APN Connection Profile
          return "mdi-sim";
        case 14: // LWM2M Software Component
          return "mdi-puzzle-outline";
        case 15: // DevCapMgmt
          return "mdi-widgets-outline";
        case 16: // Portfolio
          return "mdi-folder-multiple-outline";
        case 19: // BinaryAppDataContainer
          return "mdi-database-sync-outline";
        case 3303:
          return "mdi-thermometer";
        default:
          return "mdi-alpha-o-circle-outline";
      }
    },
  },
};
</script>
