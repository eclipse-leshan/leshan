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
  <v-list density="compact">
    <v-list-item
      density="compact"
      class="pa-1"
      slim
      v-for="object in objects"
      :key="object.id"
      :to="'/clients/' + $route.params.endpoint + '/' + object.id"
    >
      <template v-slot:prepend>
        <object-icon :objectId="object.id" class="me-2" />
      </template>

      <v-list-item-title density="compact">
        {{ object.name }}
        <v-icon
          v-if="shouldBeHidden(object)"
          size="small"
          color="red"
          :title="`The Object ${object.name} ID:${object.id} MUST NOT be part of Update Objects and Object Instances list in Register Request.`"
          >{{ $mdiAlertCircle }}</v-icon
        ></v-list-item-title
      >
      <v-list-item-subtitle density="compact">
        {{ "/" + object.id }}
      </v-list-item-subtitle>
    </v-list-item>
  </v-list>
</template>
<script>
import ObjectIcon from "./ObjectIcon.vue";
export default {
  components: { ObjectIcon },
  props: { objects: Array },
  methods: {
    shouldBeHidden(object) {
      if (object) {
        switch (object.id) {
          case 0:
          case 21:
            return true;
          default:
            return false;
        }
      }
      return false;
    },
  },
};
</script>

<style scoped>
.v-list {
  --v-list-item-height: 32px; /* Adjust to your preference */
}
</style>
